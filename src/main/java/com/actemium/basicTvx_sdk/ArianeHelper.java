package com.actemium.basicTvx_sdk;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.rff.basictravaux.model.bdd.ObjetPersistant;

import utils.TypeExtension;
import utils.champ.Champ;

public class ArianeHelper {
	static <U> String getId(U objet){
		if(objet instanceof ObjetPersistant)
			return ((ObjetPersistant) objet).getId().toString();
		if(objet instanceof ariane.modele.base.ObjetPersistant)
			return ((ariane.modele.base.ObjetPersistant) objet).getId().toString();
		//return UUID.randomUUID().toString();
		return null; 
	}
	
	@SuppressWarnings("rawtypes")
	 static void addSousObject(Object obj, GlobalObjectManager gom, CacheChargementEnProfondeur cacheChargementEnProfondeur) throws IllegalArgumentException, IllegalAccessException {
		List<Champ> champs = TypeExtension.getSerializableFields(obj.getClass());
		for(Champ champ : champs){
			Object value = champ.get(obj);
			if(!champ.isSimple() && value != null){
				Class<?> type = value.getClass();
				if(Collection.class.isAssignableFrom(type)){
					for(Object o : (Collection)value){
						if(o != null && !TypeExtension.isSimple(o.getClass()))
							gom.prendEnChargePourChargementEnProfondeur(o, cacheChargementEnProfondeur);
					}
				}else if(Map.class.isAssignableFrom(type)){
					Map<?,?> map = (Map<?,?>)value;
					for(Entry<?,?> entry : map.entrySet()){
						Object k = entry.getKey();
						Object v = entry.getValue();
						if(k != null && !TypeExtension.isSimple(k.getClass()))
							gom.prendEnChargePourChargementEnProfondeur(entry.getKey(), cacheChargementEnProfondeur);
						if(v != null && !TypeExtension.isSimple(v.getClass()))						
							gom.prendEnChargePourChargementEnProfondeur(entry.getValue(), cacheChargementEnProfondeur);
					}
				}else{ //objet
					gom.prendEnChargePourChargementEnProfondeur(value, cacheChargementEnProfondeur);
				}
			}
		}
	}
}
