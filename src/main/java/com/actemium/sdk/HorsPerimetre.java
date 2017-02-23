package com.actemium.sdk;

import java.util.List;

import com.actemium.sdk.restclient.RestException;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import giraudsa.marshall.deserialisation.EntityManager;

public interface HorsPerimetre {

	Reponse traiteRequete(Requete requete, EntityManager entityManager) throws RestException;

	<U> void save(U obj)throws RestException;

	<U> U getObjetById(Class<U> clazz, String id, EntityManager entityManager) throws RestException;

	<U> boolean getAllObject(Class<U> clazz, EntityManager entityManager, List<U> listeARemplir) throws RestException;

}
