package com.actemium.sdk;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.actemium.sdk.restclient.RestException;

import giraudsa.marshall.exception.InstanciationException;

class TacheChargementWebService implements Callable<Object> {
	private final GlobalObjectManager gom;
	private final Object objetATraiter;
	public TacheChargementWebService(Object objetATraiter, GlobalObjectManager gom) {
		super();
		this.gom = gom;
		this.objetATraiter = objetATraiter;
	}

	@Override
	public Object call() throws IOException, RestException, SAXException, InstanciationException, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
		if (!Thread.currentThread().isInterrupted()) {
			String id = gom.getId(objetATraiter);
			Class<?> clazz = objetATraiter.getClass();
			if(gom.chargeObjectFromGisement(clazz, id, gom))
				gom.setEstCharge(objetATraiter);
		}
		return objetATraiter;
	}   
}
