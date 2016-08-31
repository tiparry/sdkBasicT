package com.actemium.basicTvx_sdk.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Si la purge automatique du cache n'a pas été activée lors de l'init, la gestion des incohérences du Cache incombe à l'utilisateur.
 *  */
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
