package com.actemium.sdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.actemium.sdk.exception.GomException;

/**Objet permettant la configuration du GlobalObjectManager
 * 
 * 	La purge automatique du cache en cas d'exception est desactivee.
 * 	Les timeout HTTP par defaut sont de connecttimeout->10s et sockettimeout->60s 
 * 	L'idHelper par defaut est la classe UUIDFactoryRandomImpl
 * 	L'annuaire par defaut est celui contenant l'URL des WS entites et taches des objets metier
 */
public class GOMConfiguration {
	private final String httpLogin;
	private final String httpPwd;
	private final String gisementBaseUrl;
	private boolean isCachePurgeAutomatiquement =true;
	private int connectTimeout=10000;
	private int socketTimeout=60000;
	private IdHelper<?> idHelper = new DefaultUUIDHelper();
	private final List<String> annuaires = new ArrayList<>();
	private final Collection<Class<?>> aGererDansCache = new LinkedHashSet<>();
	
	public GOMConfiguration(String httpLogin, String httpPwd, String gisementBaseUrl){
		super();
		this.httpLogin=httpLogin;
		this.httpPwd=httpPwd;
		this.gisementBaseUrl=gisementBaseUrl;
		annuaires.add("annuaire");
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
	
	public GOMConfiguration addClasseAGererDansGom(Class<?> clazz){
		this.aGererDansCache.add(clazz);
		return this;
	}

	public GlobalObjectManager init() throws GomException {
		return GlobalObjectManager.init(httpLogin, httpPwd, gisementBaseUrl, isCachePurgeAutomatiquement,
				connectTimeout, socketTimeout, idHelper, annuaires, aGererDansCache);
	}

}