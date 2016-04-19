package com.actemium.basicTvx_sdk;

import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.MarshallExeption;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;


public abstract class PersistanceManagerAbstrait {

	abstract <U> void save(U l) throws MarshallExeption, ClientProtocolException, IOException, RestException;

	abstract <U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws ParserConfigurationException, RestException, IOException, ReflectiveOperationException, SAXException;

	abstract <U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listARemplir) 
			throws ParseException, RestException, IOException, SAXException, ClassNotFoundException;
	
	abstract Reponse getReponse(Requete requete, EntityManager entityManager) throws MarshallExeption, IOException, RestException;

}
