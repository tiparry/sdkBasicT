package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.exception.GetAllObjectException;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import utils.EntityManager;


 abstract class PersistanceManagerAbstrait {

	abstract <U> boolean save(U l, EntityManager entityManager) throws MarshallExeption, IOException, RestException;

	abstract <U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws IOException, RestException, SAXException, InstanciationException;

	abstract <U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listARemplir) throws RestException, IOException;
	
	abstract Reponse getReponse(Requete requete, EntityManager entityManager) throws MarshallExeption, RestException, IOException;
	
	abstract Set<Class<?>> getAllClasses() throws GetAllObjectException;
	
	abstract void closeHttpClient() throws RestException;

}
