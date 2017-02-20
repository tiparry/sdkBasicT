package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import com.actemium.basicTvx_sdk.exception.GetAllObjectException;
import com.actemium.basicTvx_sdk.restclient.RestClient;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.actemium.basicTvx_sdk.restclient.Serialisation;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import ariane.modele.ressource.RessourceAbstraite;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.exception.UnmarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;
import utils.ConfigurationMarshalling;

 class PersistanceManagerRest extends PersistanceManagerAbstrait {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistanceManagerRest.class);
	private RestClient restClient;
	private AnnuaireWS annuaireWS;

	
////////////////Hack pour CORTE qui veut les idReseau dans les objets Ariane
	private String gaiaUrl = null;
	private UsernamePasswordCredentials credentialsGaia;
	private static final String RESSOURCEABSTRAITEGAIA = "/referentiel/infrastructure/gaia/v2/RA/{id}/xml";
////////////////Hack pour CORTE qui veut les idReseau dans les objets Ariane
	
	
	
	
	PersistanceManagerRest(String httpLogin, String httpPwd, String gisementBaseUrl, int connectTimeout, int socketTimeout, List<String> annuaires) throws RestException{
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
	 <U> void save(U obj) throws MarshallExeption, RestException{
		String dataToSend = toJson(obj);
		String url = annuaireWS.getUrl(obj.getClass());
		if (url == null) {
			return;
		}
		restClient.put(url, dataToSend, Serialisation.JSON);
	}
	
	@Override
	Reponse getReponse(Requete requete, EntityManager entityManager) throws MarshallExeption, RestException, IOException{
		String url = annuaireWS.getUrl(requete.getClass());
		if (url == null) {
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
			return chargeIdReseau(clazz, id, entityManager);
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
	
	
	////////////////Hack pour CORTE qui veut les idReseau dans les objets Ariane
	
	
	
	public void setConfigAriane(String host, String username, String password){
		 UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		gaiaUrl = host + RESSOURCEABSTRAITEGAIA;
		this.credentialsGaia = credentials;
	}
	
	private <U> U chargeIdReseau(Class<U> clazz, String id, EntityManager entityManager) throws IOException, SAXException, InstanciationException, RestException{
		if(gaiaUrl != null && RessourceAbstraite.class.isAssignableFrom(clazz)){
			return extractIdReseau(clazz, id, gaiaUrl, entityManager);
		}
		return null;
	}
	
	private <U> U extractIdReseau(Class<U> clazz, String id, String urn, EntityManager entityManager) throws IOException, SAXException, InstanciationException, RestException{
		U ret;
		String url = urn.replace("{id}", id);
		Reader br = restClient.getReader(url, credentialsGaia);
		if(br == null) 
			return null;
		ret = entityManager.findObjectOrCreate(id, clazz);
		Long idReseau = getIdReseau(br);
		try{
			Field idReseauFields = RessourceAbstraite.class.getDeclaredField("idReseau");
			idReseauFields.setAccessible(true);
			idReseauFields.set(ret, idReseau);
		}catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e){
			throw new InstanciationException("impossible d'affecter " + idReseau + " dans idReseau de " + ret.getClass().toString(), e);
		}
		br.close();
		return ret;
	}



	private Long getIdReseau(Reader br) throws SAXException, IOException{
		if (br == null) 
			return null;
		try{
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			RessourceAbstraiteArianeHandler handler = new RessourceAbstraiteArianeHandler();
			saxParser.parse(new InputSource(br), handler);
			return handler.getIdExterne();
		} catch (ParserConfigurationException e) {
			LOGGER.error("impossible de construire le SAXPARSER", e);
			throw new SAXException("impossible de construire le SAXPARSER", e);
		}
	}
	
	class RessourceAbstraiteArianeHandler extends DefaultHandler2 {
		
		private static final String ID_EXTERNE_TAG = "idReseau";
		

		private int niveau = 0;
		private boolean isIdExterne=false;

		private String idExterne = null;
		
		
		public Long getIdExterne(){
			if(idExterne == null) 
				return null;
			return Long.valueOf(idExterne);
		}
		
		@Override 
		public void endElement(String uri, String localName, String qName) throws SAXException {
			--niveau;
		}
		
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			++niveau;
			if (niveau == 3 && idExterne == null && qName.equals(ID_EXTERNE_TAG)){
				isIdExterne = true;
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String value = new String(ch, start, length).trim();
	        if(value.isEmpty())
	        	return; // ignore white space
	        if (isIdExterne){
	        	idExterne = value;
	        	isIdExterne=false;
	        }
		}

	}
}
