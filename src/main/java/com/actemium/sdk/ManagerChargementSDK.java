package com.actemium.sdk;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.actemium.sdk.exception.GetObjectException;
import com.actemium.sdk.exception.GetObjetEnProfondeurException;

public abstract class ManagerChargementSDK {
	private final Object objetRacine;
	private final GlobalObjectManager gom;
	private final ExecutorService executor;
	
	protected ManagerChargementSDK(GlobalObjectManager gom, Object objetRacine, ExecutorService executor) {
		this.objetRacine = objetRacine;
		this.gom = gom;
		this.executor = executor;
	}
	protected void chargementTermineAndShutdownNow() {
		synchronized(getExecutor()){
			getExecutor().shutdownNow();
		}
	}
	protected boolean isChargementTermine(){
		synchronized(executor){
			return executor.isShutdown();
		}
	}
	protected abstract Future<Object> submit(Object o);
	protected abstract void execute() throws GetObjetEnProfondeurException, GetObjectException;
	
	protected Object getObjetRacine(){
		return objetRacine;
	}
	
	protected GlobalObjectManager getGom(){
		return gom;
	}
	protected ExecutorService getExecutor(){
		return executor;
	}
	
	
	protected Future<Object> submitFuture(Callable<Object> task){
		return executor.submit(task);
	}
}
