package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Si la purge automatique du cache n'a pas été activée lors de l'init, la gestion des incohérences du Cache incombe à l'utilisateur.
 *  */
public class GetAllObjectException extends Exception {

	
	private static final long serialVersionUID = 8766623874379130509L;

	public GetAllObjectException(Exception e) {
		super(e);
	}
	
	public GetAllObjectException(String message, Exception e) {
		super(message, e);
	}
	
}
