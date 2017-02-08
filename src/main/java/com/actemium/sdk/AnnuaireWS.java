package com.actemium.sdk;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actemium.sdk.restclient.RestClient;
import com.actemium.sdk.restclient.RestException;

import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.UnmarshallExeption;

public class AnnuaireWS {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnuaireWS.class);

	private final Map<String, String> dicoNomClasseToUrl = new HashMap<>();
	private final String gisementTravauxBaseUrl;

	public AnnuaireWS(String gisementTravauxBaseUrl) {
		this.gisementTravauxBaseUrl=gisementTravauxBaseUrl;
	}
	
	public Map<String, String> getDicoNomClasseToUrl() {
		return dicoNomClasseToUrl;
	}

	public String getUrl(Class<?> clazz){
		String urn = dicoNomClasseToUrl.get(clazz.getCanonicalName());
		if (urn==null)
			return null;
		return gisementTravauxBaseUrl+urn;
	}

	public void loadAnnuaire(RestClient restClient, String urlClasseToLoad) throws RestException {
		try(Reader br = restClient.getReader(gisementTravauxBaseUrl+urlClasseToLoad)){
			Map<String, String> obj = null;
			if (br != null) {
				obj = fromJson(br);
			}
			if (obj==null)
				return;
			dicoNomClasseToUrl.putAll(obj);
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
