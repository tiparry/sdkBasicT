package com.actemium.basicTvx_sdk.exception;

/**
 * Lorsque cette exception est générée, le cache est potentiellement dans un état incohérent pouvant mener à des erreurs par la suite. 
 * Il est fortement conseiller à l'utilisateur de purger le cache et de reconstruire ses objets.
 *  */
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
