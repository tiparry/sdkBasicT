package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Si la purge automatique du cache n'a pas été activée lors de l'init, la gestion des incohérences du Cache incombe à l'utilisateur.
 *  */
public class SaveException extends Exception {
	
	
	private static final long serialVersionUID = 5654150626291838529L;
	public SaveException(Exception e) {
		super(e);
	}

}
