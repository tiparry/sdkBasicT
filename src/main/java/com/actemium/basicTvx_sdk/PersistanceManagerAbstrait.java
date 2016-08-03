package com.actemium.basicTvx_sdk;

import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import java.io.IOException;
import java.util.List;

import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;


 abstract class PersistanceManagerAbstrait {

	abstract <U> void save(U l) throws MarshallExeption, IOException, RestException;

	abstract <U> U getObjectById(Class<U> clazz, String id, EntityManager entityManager) throws IOException, RestException, SAXException, InstanciationException;

	abstract <U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listARemplir) throws RestException, IOException;
	
	abstract Reponse getReponse(Requete requete, EntityManager entityManager) throws MarshallExeption, RestException, IOException;

}
