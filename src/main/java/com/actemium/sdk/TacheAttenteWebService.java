package com.actemium.sdk;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class TacheAttenteWebService implements Callable<Object> {

	private final Future<Object> future;
	public TacheAttenteWebService(Future<Object> future) {
		super();
		this.future = future;
	}

	@Override
	public Object call() throws InterruptedException, ExecutionException{
		return future.get();
	}   
}
