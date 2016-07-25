package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est purgé. Les objets déclarés précédemment
 * au moyen du GlobalObjectManager (avec createObject, getObject, getAllObject ou getReponse)
 * ne sont plus utilisables.
 */
public class GetObjectException extends Exception {

	
	private static final long serialVersionUID = 8766623874379130509L;
	private final String idObjet;
	private final Class<?> type;
	public GetObjectException(String idObjet, Class<?> type, Exception e) {
		super("impossible de récupérer l'objet de type " + type.getCanonicalName() + " d'id " + idObjet + " ", e);
		this.idObjet = idObjet;
		this.type = type;
	}
	
	public String getIdObjet() {
		return idObjet;
	}
	
	public Class<?> getType() {
		return type;
	}

}
