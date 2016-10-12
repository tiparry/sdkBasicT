package com.actemium.basicTvx_sdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rff.basictravaux.model.bdd.ObjetPersistant;

import giraudsa.marshall.deserialisation.Fabrique;
import giraudsa.marshall.exception.FabriqueInstantiationException;
import giraudsa.marshall.exception.InstanciationException;
import utils.Constants;

 class ObjectFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFactory.class);
	private Fabrique constructeur;
	
	protected ObjectFactory() {
		try {
			constructeur = Fabrique.getInstance();
		} catch (FabriqueInstantiationException e) {
			LOGGER.info("on ne va pas passer par la fabrique du Marshaller pour instancier les objets", e);
		}
	}

	private <U> void setId(U ret, UUID id) {
		if(ret instanceof ObjetPersistant)
			((ObjetPersistant) ret).setId(id);
		if(ret instanceof ariane.modele.base.ObjetPersistant)
			((ariane.modele.base.ObjetPersistant) ret).setId(id);
	}
	
	private <U> void setDateCration(U ret, Date date) {
		if(ret instanceof ObjetPersistant)
			((ObjetPersistant) ret).setDateCreation(date);
		if(ret instanceof ariane.modele.base.ObjetPersistant)
			((ariane.modele.base.ObjetPersistant) ret).setDateCreation(date);
	}
	
	
	
	private UUID newUuid(){
//	return UUID.randomUUID();
//		LOGGER.debug("BEFORE");
//		getLoadedLibraries(this.getClass().getClassLoader());
		UUID res =  UUID.randomUUID();
//		LOGGER.debug("AFTER");
//		getLoadedLibraries(this.getClass().getClassLoader());
		return res;
	}

	protected <U> U newObjectWithOnlyId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, true);
	}
	
	protected <U> U newObjectAndId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, false);
	}
	
	protected <U> U newObject(Class<U> clazz, Date date, GestionCache cache) throws InstanciationException {
		U ret = newInstanceConstructeur(clazz);
		UUID id = newUuid();
		setId(ret, id);
		setDateCration(ret, date);
		cache.metEnCache(id.toString(), ret, true);
		return ret;
	}
	
	private <U> U newObjectById(Class<U> clazz, String id, GestionCache cache, boolean onlyId) throws InstanciationException{
		U ret = null;
		ret = onlyId ? newInstanceBasNiveau(clazz) : newInstanceConstructeur(clazz);
		setId(ret, UUID.fromString(id));
		cache.metEnCache(id, ret, true);
		return ret;
	}

	private <U> U newInstanceConstructeur(Class<U> clazz) throws InstanciationException{
		try {
			Constructor<U> constr = clazz.getDeclaredConstructor(Constants.getClassVide());
			constr.setAccessible(true);
			return (U) constr.newInstance(Constants.getNullArgument());
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			LOGGER.debug("impossible d'instancier la classe " + clazz.getName(), e1);
			throw new InstanciationException("impossible d'instancier la classe " + clazz.getName(), e1);
		}
	}

	private <U> U newInstanceBasNiveau(Class<U> clazz) throws InstanciationException{
		return constructeur.newObject(clazz);
	}
}
