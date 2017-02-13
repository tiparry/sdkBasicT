package com.actemium.sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Si la purge automatique du cache n'a pas été activée lors de l'init, la gestion des incohérences du Cache incombe à l'utilisateur.
 *  */
public class SaveAllException extends Exception {

	
	private static final long serialVersionUID = 1709497872764266477L;
	public SaveAllException(String string, Exception e) {
		super(string, e);
	}
	
	
}