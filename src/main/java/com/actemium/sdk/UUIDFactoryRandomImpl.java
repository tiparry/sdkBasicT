package com.actemium.sdk;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import giraudsa.marshall.exception.SetValueException;
import utils.TypeExtension;
import utils.champ.Champ;

public class UUIDFactoryRandomImpl extends IdHelper<UUID> {

	private static final Logger LOGGER = LoggerFactory.getLogger(UUIDFactoryRandomImpl.class);
	
	@Override
	public UUID getId(Object obj) {
		Champ champId = TypeExtension.getChampId(obj.getClass());
		if (!champId.isFakeId()){
			if(!champId.getValueType().equals(UUID.class)){
				LOGGER.error("l'id de l'objet n'est pas de type UUID");
				return null;
			}
			try {
				return (UUID) champId.get(obj);
			} catch (IllegalAccessException e) {
				LOGGER.error("impossible de récupérer la valeur de l'id", e);
			}
		}
		return null;
	}

	@Override
	public void setId(Object obj, UUID id) {
		try{
			Champ champId = TypeExtension.getChampId(obj.getClass());
			if (!champId.isFakeId()){
				Class<?> typeId = champId.getValueType();
				if(typeId.equals(UUID.class))
					champId.set(obj, id, null);
				else 
					throw new SetValueException("l'id n'est pas de type UUID.");
			}
		}catch(SetValueException e){
			LOGGER.error("impossible d'affecter l'id", e);
		}
	}


	@Override
	public UUID convertId(String id) {
		return UUID.fromString(id);
	}
}
