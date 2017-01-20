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

public class AnnuaireWS {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnuaireWS.class);
	
	private  static AnnuaireWS instance = null;
	private static final  String WS_CLASSE_BT_TO_URL="annuaire";
		
	private Map<String, String> dicoNomClasseToUrl;
	private   final RestClient restClient;
	private  final  String gisementTravauxBaseUrl;
	
	private AnnuaireWS(RestClient restClient, String gisementTravauxBaseUrl){
		this.restClient=restClient;
		this.gisementTravauxBaseUrl=gisementTravauxBaseUrl;
	}
	
	public static void init(RestClient restClient, String gisementTravauxBaseUrl){
		instance = new AnnuaireWS(restClient, gisementTravauxBaseUrl);	
	}
	
	public static AnnuaireWS getInstance(){
		return instance;
	}

	public Map<String, String> getDicoNomClasseToUrl() throws RestException, IOException{
		if(dicoNomClasseToUrl==null){
			loadDicoNomClasseToUrl(WS_CLASSE_BT_TO_URL);
		}
		if(dicoNomClasseToUrl==null)
			return null;
		return dicoNomClasseToUrl;
	}
	
	public String getUrl(Class<?> clazz) throws RestException, IOException{
		Map<String, String> dico = getDicoNomClasseToUrl();
		String urn = dico.get(clazz.getCanonicalName());
		if (urn==null)
			return null;
		return (gisementTravauxBaseUrl+urn);
	}
	
	private void loadDicoNomClasseToUrl(String url_classeToLoad) throws RestException, IOException{		
			Reader br = restClient.getReader(gisementTravauxBaseUrl+url_classeToLoad);
			Map<String, String> obj = null;
			if (br != null) {
				obj = fromJson(br);
				br.close();
			}
			if (obj==null)
				return;
			dicoNomClasseToUrl = new HashMap<>();
			dicoNomClasseToUrl.putAll(obj);
	}
		
		private <U> U fromJson(Reader br){
			try {
				return JsonUnmarshaller.fromJson(br, null);
			} catch (UnmarshallExeption e) {
				LOGGER.error("", e);
			} 
			return null;
		}
	
}
	