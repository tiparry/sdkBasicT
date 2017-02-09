package com.actemium.basicTvx_sdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import giraudsa.marshall.deserialisation.Fabrique;
import giraudsa.marshall.exception.ChampNotFound;
import giraudsa.marshall.exception.FabriqueInstantiationException;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.SetValueException;
import utils.Constants;
import utils.TypeExtension;
import utils.champ.FieldInformations;

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
	
	private <U> void setDateCration(U ret, Date date) {
		if (date == null)
			return;
		try {
			FieldInformations dateChamp = TypeExtension.getChampByName(ret.getClass(), "dateCreation");
			dateChamp.set(ret, date, null);
		} catch (SetValueException | ChampNotFound e) {
			LOGGER.error("impossible d'affecter la date " + date.toString() + " dans dateCreation.", e);
		}
	}
	
	
	

	protected <U> U newObjectWithOnlyId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, true);
	}
	
	protected <U> U newObjectAndId(Class<U> clazz, String id, GestionCache cache) throws InstanciationException{
		return newObjectById(clazz, id, cache, false);
	}
	
	protected <U> U newObject(Class<U> clazz, Date date, GestionCache cache) throws InstanciationException {
		U ret = newInstanceConstructeur(clazz);
		I id = idHelper.getNewId();
		idHelper.setId(ret, id);
		setDateCration(ret, date);
		cache.metEnCache(id.toString(), ret, true);
		return ret;
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
