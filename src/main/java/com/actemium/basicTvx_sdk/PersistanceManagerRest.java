package com.actemium.basicTvx_sdk;

import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.JsonHandlerException;
import giraudsa.marshall.exception.NotImplementedSerializeException;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.ParseException;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.actemium.basicTvx_sdk.restclient.RestClient;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.AnnuaireWS;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;

import ariane.modele.base.ObjetPersistant;
import ariane.modele.ressource.RessourceAbstraite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class PersistanceManagerRest extends PersistanceManagerAbstrait {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistanceManagerRest.class);
	private RestClient restClient;
	
	private String gisementTravauxBaseUrl;

	private AnnuaireWS annuaire;

	PersistanceManagerRest(String httpLogin, String httpPwd, String gisementBaseUrl) {
		super();
		restClient = new RestClient(httpLogin, httpPwd);
		gisementTravauxBaseUrl = gisementBaseUrl;
		annuaire = AnnuaireWS.getInstance();
	}


	@Override
	 <U> void save(U obj) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException,
			RestException, NotImplementedSerializeException {
		String dataToSend = ToJson(obj);
		String constante = annuaire.putUrlExtension(obj.getClass());
		if (constante == null) {
			return;
		}
		restClient.put(gisementTravauxBaseUrl + constante, dataToSend);
	}
	
	@Override
	Reponse getReponse(Requete requete, EntityManager entityManager) throws IOException, RestException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NotImplementedSerializeException, ClassNotFoundException, SAXException {
		String urn = annuaire.getRequestUrl(requete.getClass());
		if (urn == null) {
			return null;
		}
		String uri = gisementTravauxBaseUrl + urn;
		String message = ToJson(requete);
		Reader br = restClient.postReader(uri, message);
		Reponse reponse = null;
		if (br != null) {
			reponse = fromJson(br, entityManager);
			br.close();
		}
		return reponse;
	}

	@Override
	<U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException {
		String urn = annuaire.getUrlExtension(clazz);
		if (urn == null) {
			return chargeIdReseau(clazz, id, entityManager);
		}
		String url = gisementTravauxBaseUrl + String.format(urn, id);
		Reader br = restClient.getReader(url);
		U obj = null;
		
		if (br != null) {
			obj = fromJson(br, entityManager);
			br.close();
		}
		
		return obj;
	}


	@Override
	<U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listeARemplir) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException {
		String urn = annuaire.getAllUrlExtension(clazz);
		if (urn == null) {
			return false;
		}
		String url = gisementTravauxBaseUrl + urn;
		Reader br = restClient.getReader(url);
		List<U> lObj = null;
		if (br != null) {
			lObj = fromJson(br, entityManager);
			listeARemplir.addAll(lObj);
			br.close();
		}
		if (lObj == null || lObj.size() == 0) {
			//pas d'instance
		}
		return true;

	}

	private <U> U fromJson(Reader br, EntityManager entityManager) throws ClassNotFoundException, IOException, SAXException {
		try {
			return JsonUnmarshaller.fromJson(br, entityManager);
		} catch (InstantiationException e) {
			LOGGER.error("", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("", e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("", e);
		} catch (InvocationTargetException e) {
			LOGGER.error("", e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("", e);
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (NotImplementedSerializeException e) {
			LOGGER.error("", e);
		} catch (JsonHandlerException e) {
			LOGGER.error("", e);
		} catch (java.text.ParseException e) {
			LOGGER.error("", e);
		}
		return null;
	}

	private String ToJson(Object obj) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException,
			NotImplementedSerializeException {
		return JsonMarshaller.ToJson(obj);
	}
	
	
	////////////////Hack pour CORTE qui veut les idReseau dans les objets Ariane
	
	private String gaiaUrl = null;
	private UsernamePasswordCredentials credentialsGaia;
	private static final String ressourceAbstraiteGaia = "/referentiel/infrastructure/gaia/v2/RA/{id}/xml";
	
	public void setConfigAriane(String host, String username, String password){
		 UsernamePasswordCredentials credentialsGaia = new UsernamePasswordCredentials(username, password);
		gaiaUrl = host + ressourceAbstraiteGaia;
		this.credentialsGaia = credentialsGaia;
	}
	
	private <U> U chargeIdReseau(Class<U> clazz, String id, EntityManager entityManager) throws ParseException, RestException, IOException {
		if(gaiaUrl != null && RessourceAbstraite.class.isAssignableFrom(clazz)){
			return extractIdReseau(clazz, id, gaiaUrl, entityManager);
		}
		return null;
	}
	
	private <U> U extractIdReseau(Class<U> clazz, String id, String urn, EntityManager entityManager) throws ParseException, RestException, IOException {
		U ret;
		String url = urn.replace("{id}", id);
		Reader br = restClient.getReader(url, credentialsGaia);
		if(br == null) return null;
		synchronized (entityManager) {
			ret = entityManager.findObject(id, clazz);
			if(ret == null){
				try {
					ret = clazz.newInstance();
					((ObjetPersistant)ret).setId(UUID.fromString(id));
					entityManager.metEnCache(id, ret);
				} catch (InstantiationException | IllegalAccessException e) {
					LOGGER.debug(e.getMessage());
				}
			}
		}
		((RessourceAbstraite)ret).setIdReseau(getIdReseau(br));
		br.close();
		return ret;
	}



	private Long getIdReseau(Reader br){
		if (br == null) return null;
		try{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			RessourceAbstraiteArianeHandler handler = new RessourceAbstraiteArianeHandler();
			saxParser.parse(new InputSource(br), handler);
			return handler.getIdExterne();
		}catch(ParserConfigurationException | SAXException | IOException e){
			LOGGER.debug(e.getMessage());
		}
		return null;
	}
	
	
	class RessourceAbstraiteArianeHandler extends DefaultHandler2 {
		
		private static final String ID_EXTERNE_TAG = "idReseau";
		

		private int niveau = 0;
		private boolean isIdExterne=false;

		private String idExterne = null;
		
		public Long getIdExterne(){
			if(idExterne == null) return null;
			return Long.valueOf(idExterne);
		}
		
		@Override 
		public void endElement(String uri, String localName, String qName) throws SAXException {
			--niveau;
		};
		
		
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
	        if(value.length() == 0) return; // ignore white space
	        if (isIdExterne){
	        	idExterne = value;
	        	isIdExterne=false;
	        }
		}
		
		public RessourceAbstraiteArianeHandler() {
		}

	}

}
