package com.actemium.sdk;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.exception.UnmarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;
import utils.ConfigurationMarshalling;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import com.actemium.sdk.exception.GetAllObjectException;
import com.actemium.sdk.restclient.RestClient;
import com.actemium.sdk.restclient.RestException;
import com.actemium.sdk.restclient.Serialisation;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

 class PersistanceManagerRest extends PersistanceManagerAbstrait {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistanceManagerRest.class);
	private RestClient restClient;
	private AnnuaireWS annuaireWS;
	

	PersistanceManagerRest(String httpLogin, String httpPwd, String gisementBaseUrl, int connectTimeout, int socketTimeout, List<String> annuaires) throws RestException {
		super();
		restClient = new RestClient(httpLogin, httpPwd, connectTimeout, socketTimeout);
		annuaireWS = new AnnuaireWS(gisementBaseUrl);
		for (String annuaire : annuaires) {
			try {
				annuaireWS.loadAnnuaire(restClient, annuaire);
			} catch (RestException e) {
				LOGGER.error("Erreur lors de la récupération de l'annuaire : " + annuaire, e);
				throw e;
			}
		}
		ConfigurationMarshalling.setIdUniversel();
	}
	


	@Override
	 <U> void save(U obj) throws MarshallExeption, RestException {
		String dataToSend = toJson(obj);
		String url = annuaireWS.getUrl(obj.getClass());
		if (url == null) {
			if(getHorsPerimetre() != null)
				getHorsPerimetre().save(obj);
			return;
		}
		restClient.put(url, dataToSend, Serialisation.JSON);
	}
	
	@Override
	Reponse getReponse(Requete requete, EntityManager entityManager) throws MarshallExeption, RestException, IOException{
		String url = annuaireWS.getUrl(requete.getClass());
		if (url == null) {
			if(getHorsPerimetre() != null)
				return getHorsPerimetre().traiteRequete(requete, entityManager);
			LOGGER.error("la requete " + requete.getClass() + " n'est pas dans l'annuaire");
			throw new RestException(0, "la requete " + requete.getClass() + " n'est pas dans l'annuaire");
		}
		String message = toJson(requete);
		Reader br = restClient.postReader(url, message, Serialisation.JSON);
		Reponse reponse = null;
		if (br != null) {
			reponse = fromJson(br, entityManager);
			br.close();
		}
		return reponse;
	}

	@Override
	<U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws IOException, RestException, SAXException, InstanciationException{
		String urn = annuaireWS.getUrl(clazz);
		if (urn == null) {
			if(getHorsPerimetre() != null)
				return getHorsPerimetre().getObjetById(clazz, id, entityManager);
			return null;
		}
		String url = urn+"/"+id;
		try{
			Reader br = restClient.getReader(url);
			U obj = null;
			if (br != null) {
				obj = fromJson(br, entityManager);
				br.close();
			}			
			return obj;
		}catch(RestException restException){
			if (restException.getStatusCodeHttp() == HttpStatus.SC_NOT_FOUND)
				return null;
			else
				throw restException;
		}
	}


	@Override
	<U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listeARemplir) throws RestException, IOException{
		String url = annuaireWS.getUrl(clazz);
		if (url == null) {
			if(getHorsPerimetre() != null)
				return getHorsPerimetre().getAllObject(clazz, entityManager, listeARemplir);
			return false;
		}
		Reader br = restClient.getReader(url);
		List<U> lObj = null;
		if (br != null) {
			lObj = fromJson(br, entityManager);
			listeARemplir.addAll(lObj);
			br.close();
		}
		if (lObj == null || lObj.isEmpty()) {
			//pas d'instance
		}
		return true;

	}

	@Override
	Set<Class<?>> getAllClasses() throws GetAllObjectException {
		Set<Class<?>> classes = new HashSet<>();
		for (String nomClasse : annuaireWS.getDicoNomClasseToUrl().keySet()){
			try {
				classes.add(Class.forName(nomClasse));
			} catch (ClassNotFoundException e) {
				LOGGER.error("Impossible de trouver la classe " + nomClasse + " dans le classloader", e);
				throw new GetAllObjectException("Impossible de trouver la classe " + nomClasse + " dans le classloader", e);
			}
		}
		return classes;
	}



	private <U> U fromJson(Reader br, EntityManager entityManager){
		try {
			return JsonUnmarshaller.fromJson(br, entityManager);
		} catch (UnmarshallExeption e) {
			LOGGER.error("", e);
		} 
		return null;
	}

	private String toJson(Object obj) throws MarshallExeption  {
		return JsonMarshaller.toJson(obj);
	}

}