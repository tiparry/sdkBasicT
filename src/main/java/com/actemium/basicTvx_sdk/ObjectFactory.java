package com.actemium.basicTvx_sdk;

import java.util.Date;
import java.util.UUID;
import com.rff.basictravaux.model.bdd.ObjetPersistant;

public class ObjectFactory {

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


	<U> U newObject(Class<U> clazz, Date date, GestionCache cache) throws InstantiationException, IllegalAccessException{
		U ret = clazz.newInstance();
		UUID id = newUuid();
		setId(ret, id);
		setDateCration(ret, date);
		cache.metEnCache(id.toString(), ret, true);
		return ret;
	}
}
