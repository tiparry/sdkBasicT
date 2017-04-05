package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
				gom.setFilsEstCharge(objetATraiter);
			}
		}
		return objetATraiter;
	}
}
