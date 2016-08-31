package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Il est fortement conseiller à l'utilisateur de purger le cache et de reconstruire ses objets.
 *  */
public class SaveAllException extends Exception {

	
	private static final long serialVersionUID = 1709497872764266477L;
	public SaveAllException(String string, Exception e) {
		super(string, e);
	}
	
	
}
