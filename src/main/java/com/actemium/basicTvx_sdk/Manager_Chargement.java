package com.actemium.basicTvx_sdk;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface Manager_Chargement {
	public Future<Object> getFuturFromObject(Object o);
	public Future<Object> submit(Object o, Callable<Object> task);
	public void chargementTermineAndShutdownNow() ;
	public boolean isChargementTermine();
}
