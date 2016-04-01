package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;

public class CacheChargementEnProfondeur {
	private Map<Object,Boolean> dejaFait = new IdentityHashMap<>();
	private Map<Object,Integer> pasEncoreFait = new IdentityHashMap<>();
	private List<Exception> exceptions = new ArrayList<>();
	
	synchronized void add(Object o){
		int nombreEssais = pasEncoreFait.containsKey(o)? pasEncoreFait.get(o) + 1 : 0;
		pasEncoreFait.put(o, nombreEssais);
	}
	
	synchronized boolean dejaVu(Object o){
		return dejaFait.containsKey(o) || pasEncoreFait.containsKey(o);
	}
	
	synchronized void estTraite(Object o){
		dejaFait.put(o, true);
		pasEncoreFait.remove(o);
	}

	synchronized boolean estFini() {
		return pasEncoreFait.isEmpty();
	}
	
	synchronized int nombreEssais(Object o){
		return pasEncoreFait.containsKey(o)? pasEncoreFait.get(o) : 0;
	}
	
	synchronized void ajouteException(Exception ex){
		exceptions.add(ex);
	}
	
	synchronized void toutSestBienPasse() throws GetObjetEnProfondeurException{
		if(!exceptions.isEmpty())
			throw new GetObjetEnProfondeurException(exceptions);
	}
}
