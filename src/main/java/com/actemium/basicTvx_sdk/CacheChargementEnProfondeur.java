package com.actemium.basicTvx_sdk;

import java.util.HashSet;
import java.util.Set;

public class CacheChargementEnProfondeur {
	private Set<Object> dejaFait = new HashSet<>();
	private Set<Object> pasEncoreFait = new HashSet<>();
	
	synchronized void add(Object o){
		pasEncoreFait.add(o);
	}
	
	synchronized boolean dejaVu(Object o){
		return dejaFait.contains(o) || pasEncoreFait.contains(o);
	}
	
	synchronized void estTraite(Object o){
		dejaFait.add(o);
		pasEncoreFait.remove(o);
	}

	synchronized boolean estFini() {
		return pasEncoreFait.isEmpty();
	}
}
