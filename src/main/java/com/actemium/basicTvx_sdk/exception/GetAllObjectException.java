package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Il est fortement conseiller à l'utilisateur de purger le cache et de reconstruire ses objets.
 *  */
public class GetAllObjectException extends Exception {

	
	private static final long serialVersionUID = 8766623874379130509L;

	public GetAllObjectException(Exception e) {
		super(e);
	}
	
}
