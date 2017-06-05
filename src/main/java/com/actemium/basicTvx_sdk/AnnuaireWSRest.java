package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actemium.basicTvx_sdk.restclient.RestClient;
import com.actemium.basicTvx_sdk.restclient.RestException;


import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.UnmarshallExeption;
import utils.BiHashMap;

public class AnnuaireWSRest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnuaireWSRest.class);

	private final BiHashMap<String, String, String> dicoRestAndClassToUrl = new BiHashMap<>();
	private final String gisementTravauxBaseUrl;

	public AnnuaireWSRest(String gisementTravauxBaseUrl) {
		this.gisementTravauxBaseUrl=gisementTravauxBaseUrl;
	}
	
	public BiHashMap<String, String, String> getDicoRestAndClasseToUrl() {
		return dicoRestAndClassToUrl;
	}

	public String getUrl(String restCommand, Class<?> clazz){
		String urn = dicoRestAndClassToUrl.get(restCommand, clazz.getCanonicalName());
		if (urn==null)
			return null;
		return gisementTravauxBaseUrl+urn;
	}

	public void loadAnnuaire(RestClient restClient, String urlClasseToLoad) throws RestException {
		try(Reader br = restClient.getReader(gisementTravauxBaseUrl+urlClasseToLoad)){
			BiHashMap<String, String, String> obj = null;
			if (br != null) {
				obj = fromJson(br);
			}
			if (obj==null)
				return;
			dicoRestAndClassToUrl.putAll(obj);//TODO putall marche pas je pense? ajouter manuellement ou faire méthode putall
		} catch (IOException e) {
			throw new RestException(-1, "problème avec la connexion", e);
		}
	}

	private <U> U fromJson(Reader br) {
		try {
			return JsonUnmarshaller.fromJson(br, null);
		} catch (UnmarshallExeption e) {
			LOGGER.error("", e);
		} 
		return null;
	}
}
