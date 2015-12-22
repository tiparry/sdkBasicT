package com.actemium.basicTvx_sdk;

import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.JsonHandlerException;
import giraudsa.marshall.exception.NotImplementedSerializeException;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.http.ParseException;

import com.actemium.basicTvx_sdk.restclient.RestClient;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.AnnuaireWS;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;

import org.xml.sax.SAXException;

public class PersistanceManagerRest extends PersistanceManagerAbstrait {
	private RestClient restClient;
	
	private String gisementTravauxBaseUrl;

	private AnnuaireWS annuaire;
	

	public PersistanceManagerRest() {
		super();
		restClient = new RestClient(System.getProperty("http.login"), System.getProperty("http.pwd"));
		gisementTravauxBaseUrl = System.getProperty("gisement.travaux.baseurl");
		annuaire = AnnuaireWS.getInstance();
	}

	

	@Override
	public  <U> void save(U obj) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException,
			RestException, NotImplementedSerializeException {
		String dataToSend = ToJson(obj);
		String constante = annuaire.putUrlExtension(obj.getClass());
		if (constante == null) {
			return;
		}
		restClient.put(gisementTravauxBaseUrl + constante, dataToSend);
	}
	
	@Override
	public Reponse getReponse(Requete requete, EntityManager entityManager) throws IOException, RestException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NotImplementedSerializeException, ClassNotFoundException, SAXException {
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
	public <U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException {
		return getObject(clazz, id, annuaire.getUrlExtension(clazz), entityManager);
	}

	@Override
	public <U> U getObjectByIdExterne(Class<U> clazz, String id, EntityManager entityManager) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException {
		return getObject(clazz, URLEncoder.encode(id, "UTF-8"), annuaire.getByIdExterneUrlExtension(clazz), entityManager);
	}

	private <U> U getObject(Class<U> clazz, String id, String urn, EntityManager entityManager) throws RestException, IOException, SAXException, ClassNotFoundException {
		if (urn == null) {
			return null;
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
	public <U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listeARemplir) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotImplementedSerializeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonHandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String ToJson(Object obj) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException,
			NotImplementedSerializeException {
		return JsonMarshaller.ToJson(obj);
	}

}
