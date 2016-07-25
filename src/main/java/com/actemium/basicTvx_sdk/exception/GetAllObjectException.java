package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est purgé. Les objets déclarés précédemment
 * au moyen du GlobalObjectManager (avec createObject, getObject, getAllObject ou getReponse)
 * ne sont plus utilisables.
 */
public class GetAllObjectException extends Exception {

	
	private static final long serialVersionUID = 8766623874379130509L;

	public GetAllObjectException(Exception e) {
		super(e);
	}
	
}
