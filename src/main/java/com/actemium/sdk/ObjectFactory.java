package com.actemium.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import giraudsa.marshall.deserialisation.Fabrique;
import giraudsa.marshall.exception.FabriqueInstantiationException;
import giraudsa.marshall.exception.InstanciationException;

 class ObjectFactory<I> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFactory.class);
	private Fabrique constructeur;
	private IdHelper<I> idHelper;
	
	protected ObjectFactory(IdHelper<I> idHelper) throws FabriqueInstantiationException {
		this.idHelper = idHelper;
		try {
			constructeur = Fabrique.getInstance();
		} catch (FabriqueInstantiationException e) {
			LOGGER.info("on ne peux pas passer par la fabrique bas niveau du Marshaller pour instancier les objets", e);
			throw e;
		}
	}


	protected <U> U newObjectWithOnlyId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		U ret = constructeur.newObject(clazz);
		idHelper.setId(ret, idHelper.convertId(id));
		cache.metEnCache(id, ret, true);
		return ret;
	}

}
