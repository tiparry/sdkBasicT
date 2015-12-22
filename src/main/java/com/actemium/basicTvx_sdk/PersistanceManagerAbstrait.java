package com.actemium.basicTvx_sdk;

import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.NotImplementedSerializeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.http.ParseException;
import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;


public abstract class PersistanceManagerAbstrait {

	public abstract <U> void save(U l) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, RestException, NotImplementedSerializeException;

	public abstract <U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws ParseException, RestException, IOException, SAXException, ClassNotFoundException;

	public abstract <U> U getObjectByIdExterne(Class<U> clazz, String id, EntityManager entityManager)
			throws ParseException, RestException, IOException, SAXException, ClassNotFoundException;


	public abstract <U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listARemplir) 
			throws ParseException, RestException, IOException, SAXException, ClassNotFoundException;
	
	public abstract Reponse getReponse(Requete requete, EntityManager entityManager) throws IOException, RestException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NotImplementedSerializeException, ClassNotFoundException, SAXException;

}
