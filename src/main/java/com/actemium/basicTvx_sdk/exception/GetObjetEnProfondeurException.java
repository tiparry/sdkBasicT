package com.actemium.basicTvx_sdk.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Lorsque cette exception est générée, le cache est purgé. Les objets déclarés précédemment
 * au moyen du GlobalObjectManager (avec createObject, getObject, getAllObject ou getReponse)
 * ne sont plus utilisables.
 */
public class GetObjetEnProfondeurException extends Exception{

	private Object objetRacine;
	
	private static final long serialVersionUID = -6480142152730314695L;
	
	private GetObjectException cause;
	private InterruptedException interruptedException;
	
	public GetObjetEnProfondeurException(Object obj, GetObjectException e) {
		this.cause = e;
	}
	
	
	public GetObjetEnProfondeurException(Object objetRacine, InterruptedException interruptedException) {
		super();
		this.objetRacine = objetRacine;
		this.interruptedException = interruptedException;
	}


	public GetObjectException getCause(){
		return cause;
	}
	
	public InterruptedException getInterruptedException(){
		return interruptedException;
	}

	@Override
	public String toString() {
		return cause.toString();
	}

	public Object getObjetRacine() {
		return objetRacine;
	}

}
