package com.actemium.basicTvx_sdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rff.basictravaux.model.bdd.ObjetPersistant;

import utils.Constants;

public class ObjectFactory {
	private static Logger LOGGER = LoggerFactory.getLogger(ObjectFactory.class);

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

	<U> U newObjectById(Class<U> clazz, String id, GestionCache cache) throws InstantiationException, IllegalAccessException {
		U ret = clazz.newInstance();
		setId(ret, UUID.fromString(id));
		cache.metEnCache(id, ret, true);
		return ret;
	}


	<U> U newObject(Class<U> clazz, Date date, GestionCache cache) throws InstantiationException , IllegalAccessException{
		U ret;
		try{
			ret = clazz.newInstance();
		}catch(InstantiationException | IllegalAccessException e){
			Constructor<?> constr;
			try {
				constr = clazz.getDeclaredConstructor(Constants.getClassVide());
				constr.setAccessible(true);
				ret = (U) constr.newInstance(Constants.getNullArgument());
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				LOGGER.debug("impossible d'instancier la classe " + clazz.getName());
				throw e;
			}
		}
		
		UUID id = newUuid();
		setId(ret, id);
		setDateCration(ret, date);
		cache.metEnCache(id.toString(), ret, true);
		return ret;
	}
}
