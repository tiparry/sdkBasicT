package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est purgé. Les objets déclarés précédemment
 * au moyen du GlobalObjectManager (avec createObject, getObject, getAllObject ou getReponse)
 * ne sont plus utilisables.
 */
public class SaveAllException extends Exception {

	
	private static final long serialVersionUID = 1709497872764266477L;
	public SaveAllException(String string, Exception e) {
		super(string, e);
	}
	
	
}
