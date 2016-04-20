package com.actemium.basicTvx_sdk;


import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ManagerChargementEnProfondeur {
	
	static final int WEB_SERVICE_REQUEST_TIME = 50000;
	static final int LOAD_OBJECT_TIME = 5000;
	static final int RATIO_WORK = WEB_SERVICE_REQUEST_TIME/LOAD_OBJECT_TIME;

	private Map<Object,Integer> dejaVu = new IdentityHashMap<>();
	private Map<Future<Object>, Object> mapFutureToObject = new IdentityHashMap<>();
	public CompletionService<Object> completion;
	public ExecutorService executor;	
	private CompteurdeTaches compteurdeTaches = new CompteurdeTaches();
	
	public ManagerChargementEnProfondeur(){
		executor = Executors.newFixedThreadPool(determinePoolSize());
		completion = new ExecutorCompletionService<Object>(executor);
	}
	
	private int determinePoolSize(){
		int number_cores = Runtime.getRuntime().availableProcessors();
		return Math.max(1, number_cores/2*(1+RATIO_WORK));
	}
	

	public  Future<Object> submit(Object o, Callable<Object> task){
		compteurdeTaches.beforeSubmitTask();
		Future<Object> future = completion.submit(task);
		mapFutureToObject.put(future, o);
		return future;
	}
	
	public Future<Object> waitForATaskToComplete() throws InterruptedException{
		return completion.take();
	}
	
	public boolean isAllCompleted(){
		return compteurdeTaches.isAllCompleted();
	}
	
	public void oneTaskCompleted(){
		compteurdeTaches.oneTaskCompleted();
	}
	
	synchronized void add(Object o){
		int nombreEssais = dejaVu.containsKey(o)? dejaVu.get(o) + 1 : 1;
		dejaVu.put(o, nombreEssais);
	}
	
	synchronized boolean dejaVu(Object o){
		return dejaVu.containsKey(o) ;
	}
	
	synchronized int nombreEssais(Object o){
		return dejaVu.containsKey(o)? dejaVu.get(o) : 0;
	}
	
	public Object retrieveObjectFromFutur(Future<Object> future){
		if (mapFutureToObject.containsKey(future))
			return mapFutureToObject.get(future);
		return null;
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
	
	public class CompteurdeTaches{
		private int value = 0;
		private Object lock = new Object();

		public void beforeSubmitTask() {
			synchronized(lock) {
				value++;
			}
		}

		public void oneTaskCompleted() {
			synchronized(lock) {
				value--;
			}
		}

		private boolean isAllCompleted(){
			synchronized(lock) {
				if(value > 0) return false;
				return true;
			}
		}
	}
}
