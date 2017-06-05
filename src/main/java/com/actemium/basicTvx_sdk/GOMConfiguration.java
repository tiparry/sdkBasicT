package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.List;

/**Objet permettant la configuration du GlobalObjectManager
 * 
 * 	La purge automatique du cache en cas d'exception est desactivee.
 * 	Les timeout HTTP par defaut sont de connecttimeout->10s et sockettimeout->60s 
 * 	L'idHelper par defaut est la classe UUIDFactoryRandomImpl
 * 	L'annuaire par defaut est celui contenant l'URL des WS entites et taches des objets BasicTravaux
 */
public class GOMConfiguration {
	private final String httpLogin;
	private final String httpPwd;
	private final String gisementBaseUrl;
	private boolean isCachePurgeAutomatiquement =false;
	private int connectTimeout=10000;
	private int socketTimeout=60000;
	private IdHelper<?> idHelper = new UUIDFactoryRandomImpl();
	private List<String> annuaires = new ArrayList<>();
	
	public GOMConfiguration(String httpLogin, String httpPwd, String gisementBaseUrl){
		super();
		this.httpLogin=httpLogin;
		this.httpPwd=httpPwd;
		this.gisementBaseUrl=gisementBaseUrl;
		annuaires.add("annuaire_rest");
	}
	
	public GOMConfiguration setCachePurgeAutomatiquement(boolean isCachePurgeAutomatiquement) {
		this.isCachePurgeAutomatiquement = isCachePurgeAutomatiquement;
		return this;
	}

	public GOMConfiguration setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		return this;
	}

	public GOMConfiguration setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
		return this;
	}

	public GOMConfiguration setIdHelper(IdHelper<?> idHelper) {
		this.idHelper = idHelper;
		return this;
	}

	public GOMConfiguration addAnnuaire(String autreAnnuaire){
		this.annuaires.add(autreAnnuaire);
		return this;
	}
	
	public List<String> getAnnuaires() {
		return annuaires;
	}

	public String getHttpLogin() {
		return httpLogin;
	}

	public String getHttpPwd() {
		return httpPwd;
	}

	public String getGisementBaseUrl() {
		return gisementBaseUrl;
	}

	public boolean isCachePurgeAutomatiquement() {
		return isCachePurgeAutomatiquement;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public IdHelper<?> getIdHelper() {
		return idHelper;
	}

}
