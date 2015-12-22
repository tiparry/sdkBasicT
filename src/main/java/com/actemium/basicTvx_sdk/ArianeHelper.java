package com.actemium.basicTvx_sdk;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import com.rff.basictravaux.model.bdd.ObjetPersistant;

import utils.TypeExtension;
import utils.champ.Champ;

public class ArianeHelper {
	public static <U> String getId(U objet){
		if(objet instanceof ObjetPersistant)
			return ((ObjetPersistant) objet).getId().toString();
		if(objet instanceof ariane.modele.base.ObjetPersistant)
			return ((ariane.modele.base.ObjetPersistant) objet).getId().toString();
		//return UUID.randomUUID().toString();
		return null; 
	}

	@SuppressWarnings("rawtypes")
	public static void addSousObject(Object obj, Queue<Object> sousObjects) throws IllegalArgumentException, IllegalAccessException {
		List<Champ> champs = TypeExtension.getSerializableFields(obj.getClass());
		for(Champ champ : champs){
			Object value = champ.get(obj);
			if(!champ.isSimple && value != null){
				Class<?> type = value.getClass();
				if(Collection.class.isAssignableFrom(type)){
					for(Object o : (Collection)value){
						sousObjects.add(o);
					}
				}else if(Map.class.isAssignableFrom(type)){
					Map<?,?> map = (Map<?,?>)value;
					for(Entry<?,?> entry : map.entrySet()){
						sousObjects.add(entry.getKey());
						sousObjects.add(entry.getValue());
					}
				}else{ //objet
					sousObjects.add(value);
				}
			}
		}
	}
}
