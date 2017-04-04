package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.restclient.RestException;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.exception.InstanciationException;
import utils.TypeExtension;
import utils.champ.Champ;

class TacheChargementWebService implements Callable<Object> {
	private final GlobalObjectManager gom;
	private final Object objetATraiter;

	public TacheChargementWebService(Object objetATraiter, GlobalObjectManager gom) {
		super();
		this.gom = gom;
		this.objetATraiter = objetATraiter;
	}

	@Override
	public Object call() throws IOException, RestException, SAXException, InstanciationException, InterruptedException, ParserConfigurationException,
			ReflectiveOperationException {
		if (!Thread.currentThread().isInterrupted()) {
			String id = gom.getId(objetATraiter);
			Class<?> clazz = objetATraiter.getClass();
			Object o = gom.persistanceManager.getObjectById(clazz, id, gom);
			if (o != null) {
				gom.setEstCharge(objetATraiter);
				setFilsEstCharge(objetATraiter);
			}
		}
		return objetATraiter;
	}

	private void setFilsEstCharge(Object objetPere) throws IllegalAccessException {
		if (objetPere == null)
			return;
		List<Champ> champs = TypeExtension.getSerializableFields(objetPere.getClass());
		for (Champ champ : champs) {
			Object value = champ.get(objetPere);
			if (!champ.isSimple() && champ.getRelation().equals(TypeRelation.COMPOSITION) && value != null) {
				if (value instanceof Collection<?>)
					traiteCollection((Collection<?>) value);
				else if (value instanceof Map<?, ?>)
					traiteMap((Map<?, ?>) value);
				else // objet
					gom.setEstCharge(value);
			}

		}
	}

	private void traiteCollection(Collection<?> value) {
		for (Object o : value)
			if(o != null && !TypeExtension.isSimple(o.getClass()))
				gom.setEstCharge(o);
	}
	
	private void traiteMap(Map<?, ?> map) {
		for(Entry<?,?> entry : map.entrySet()) {
			Object k = entry.getKey();
			Object v = entry.getValue();
			if(k != null && !TypeExtension.isSimple(k.getClass()))
				gom.setEstCharge(k);
			if(v != null && !TypeExtension.isSimple(v.getClass()))						
				gom.setEstCharge(v);
		}
	}
}
