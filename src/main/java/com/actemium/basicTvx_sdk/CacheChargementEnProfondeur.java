package com.actemium.basicTvx_sdk;

import java.util.IdentityHashMap;
import java.util.Map;

public class CacheChargementEnProfondeur {
	private Map<Object,Boolean> dejaFait = new IdentityHashMap<>();
	private Map<Object,Boolean> pasEncoreFait = new IdentityHashMap<>();
	
	synchronized void add(Object o){
		pasEncoreFait.put(o, true);
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
}
