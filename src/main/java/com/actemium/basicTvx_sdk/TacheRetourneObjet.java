package com.actemium.basicTvx_sdk;

import java.util.concurrent.Callable;

class TacheRetourneObjet implements Callable<Object> {
	private final Object objetARetourner;
	public TacheRetourneObjet(Object objetARetourner) {
		super();
		this.objetARetourner = objetARetourner;
	}

	@Override
	public Object call() {
		return objetARetourner;
	}   
}