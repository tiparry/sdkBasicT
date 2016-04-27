package com.actemium.basicTvx_sdk;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ManagerChargementUnique implements Manager_Chargement {
	Future<Object> future;
	ExecutorService executor;
	
	public ManagerChargementUnique(){
		executor = Executors.newSingleThreadExecutor();
	}
	
	public synchronized Future<Object> submit(Object o, Callable<Object> task){
		Future<Object> future = executor.submit(task);
		this.future=future;
		return future;
	}
	
	@Override
	public synchronized Future<Object> getFuturFromObject(Object o) {
			return future;
	}

	public void chargementTermineAndShutdownNow() {
		synchronized(executor){
			executor.shutdownNow();
		}
	}

	public boolean isChargementTermine(){
		synchronized(executor){
			return executor.isShutdown();
		}
	}
	
}
