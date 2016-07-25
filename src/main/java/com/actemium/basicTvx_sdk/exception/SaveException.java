package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est purgé. Les objets déclarés précédemment
 * au moyen du GlobalObjectManager (avec createObject, getObject, getAllObject ou getReponse)
 * ne sont plus utilisables.
 */
public class SaveException extends Exception {
	
	
	private static final long serialVersionUID = 5654150626291838529L;
	public SaveException(Exception e) {
		super(e);
	}

}
