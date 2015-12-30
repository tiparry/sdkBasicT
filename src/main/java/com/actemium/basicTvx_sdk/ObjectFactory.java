package com.actemium.basicTvx_sdk;

import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import com.rff.basictravaux.model.bdd.ObjetPersistant;

public class ObjectFactory {

	private Map<Object, Boolean> mapNewObject = new IdentityHashMap<Object, Boolean>();
	
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
		cache.metEnCache(id, ret);
		return ret;
	}


	<U> U newObject(Class<U> clazz, Date date, GestionCache cache) throws InstantiationException, IllegalAccessException{
		U ret = clazz.newInstance();
		UUID id = newUuid();
		setId(ret, id);
		setDateCration(ret, date);
		mapNewObject.put(ret, true);
		cache.metEnCache(id.toString(), ret);
		return ret;
	}

	<U> boolean isNew(U obj){
		return mapNewObject.containsKey(obj);
	}
	
	<U> void noMoreNew(U obj){
		mapNewObject.remove(obj);
	}

	@SuppressWarnings("unchecked")
	<U> U getObjetInNewObject() {
		if (mapNewObject.isEmpty())
			return null;
		return (U) mapNewObject.entrySet().iterator().next().getValue();
	}

	void purge() {
		mapNewObject.clear();
	}
}
