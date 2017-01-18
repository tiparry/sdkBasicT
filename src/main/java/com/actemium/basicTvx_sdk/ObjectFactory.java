package com.actemium.basicTvx_sdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import giraudsa.marshall.deserialisation.Fabrique;
import giraudsa.marshall.exception.FabriqueInstantiationException;
import giraudsa.marshall.exception.InstanciationException;
import utils.Constants;

 class ObjectFactory<I> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFactory.class);
	private Fabrique constructeur;
	private IdHelper<I> idHelper;
	
	protected ObjectFactory(IdHelper<I> idHelper) {
		this.idHelper = idHelper;
		try {
			constructeur = Fabrique.getInstance();
		} catch (FabriqueInstantiationException e) {
			LOGGER.info("on ne va pas passer par la fabrique du Marshaller pour instancier les objets", e);
		}
	}


	protected <U> U newObjectWithOnlyId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, true);
	}
	
	protected <U> U newObjectAndId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, false);
	}
	
	
	private <U> U newObjectById(Class<U> clazz, String id, GestionCache cache, boolean onlyId) throws InstanciationException{
		U ret;
		ret = onlyId ? newInstanceBasNiveau(clazz) : newInstanceConstructeur(clazz);
		idHelper.setId(ret, idHelper.convertId(id));
		cache.metEnCache(id, ret, true);
		return ret;
	}

	private <U> U newInstanceConstructeur(Class<U> clazz) throws InstanciationException{
		try {
			Constructor<U> constr = clazz.getDeclaredConstructor(Constants.getClassVide());
			constr.setAccessible(true);
			return constr.newInstance(Constants.getNullArgument());
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			LOGGER.debug("impossible d'instancier la classe " + clazz.getName(), e1);
			throw new InstanciationException("impossible d'instancier la classe " + clazz.getName(), e1);
		}
	}

	private <U> U newInstanceBasNiveau(Class<U> clazz) throws InstanciationException{
		return constructeur.newObject(clazz);
	}
}
