package com.actemium.sdk.exception;


/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Si la purge automatique du cache n'a pas été activée lors de l'init, la gestion des incohérences du Cache incombe à l'utilisateur.
 *  */
public class GetObjetEnProfondeurException extends Exception{

	private final transient Object objetRacine;
	
	private static final long serialVersionUID = -6480142152730314695L;
	
	
	public GetObjetEnProfondeurException(Object objetRacine, Exception cause) {
		super(cause);
		this.objetRacine = objetRacine;
	}



	public Object getObjetRacine() {
		return objetRacine;
	}
	
	
}
