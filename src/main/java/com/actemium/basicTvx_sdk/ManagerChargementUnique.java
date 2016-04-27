package com.actemium.basicTvx_sdk;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ManagerChargementUnique implements ManagerChargementSDK {
	Future<Object> future;
	ExecutorService executor;
	
	public ManagerChargementUnique(){
		executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public synchronized Future<Object> submit(Object o, Callable<Object> task){
		future= executor.submit(task);
		return future;
	}
	
	@Override
	public synchronized Future<Object> getFuturFromObject(Object o) {
			return future;
	}

	@Override
	public void chargementTermineAndShutdownNow() {
		synchronized(executor){
			executor.shutdownNow();
		}
	}

	@Override
	public boolean isChargementTermine(){
		synchronized(executor){
			return executor.isShutdown();
		}
	}
	
}
