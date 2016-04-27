package com.actemium.basicTvx_sdk;


import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ManagerChargementEnProfondeur implements ManagerChargementSDK {

	static final int WEB_SERVICE_REQUEST_TIME = 50000;
	static final int LOAD_OBJECT_TIME = 5000;
	static final int RATIO_WORK = WEB_SERVICE_REQUEST_TIME/LOAD_OBJECT_TIME;

	private Map<Object,Integer> dejaVu = new IdentityHashMap<>();
	private Map<Future<Object>, Object> mapFutureToObject = new IdentityHashMap<>();
	private Map<Object, Future<Object>> mapObjectToFuture = new IdentityHashMap<>();
	private CompletionService<Object> completion;
	private ExecutorService executor;	
	private CompteurdeTaches compteurdeTaches = new CompteurdeTaches();

	public ManagerChargementEnProfondeur(){
		executor = Executors.newFixedThreadPool(determinePoolSize());
		completion = new ExecutorCompletionService<>(executor);
	}

	private int determinePoolSize(){
		int numberCores = Runtime.getRuntime().availableProcessors();
		return Math.max(1, numberCores/2*(1+RATIO_WORK));
	}


	public synchronized boolean createNewTacheChargementProfondeur(Object o, boolean retry){
		if (isChargementTermine())
			return false;
		if(retry)
			return retryChargementEnProfondeur(o);
		if (!dejaVu(o) ){
			add(o);
			return true;
		}
		return false;
	}

	private boolean retryChargementEnProfondeur(Object o) {
		if(nombreEssais(o) < 3){ //3 : le mettre parametrtable ?
			add(o);
			return true;
		}
		return false;
	}
	
	@Override
	public synchronized Future<Object> submit(Object o, Callable<Object> task){
		compteurdeTaches.beforeSubmitTask();
		Future<Object> future = completion.submit(task);
		mapFutureToObject.put(future, o);
		mapObjectToFuture.put(o, future); //on peut remplacer l'ancien future : pour un meme manager, les taches sur un meme objet s'effectuent sequentiellement de toute facon, 
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


	public synchronized Object getObjectFromFutur(Future<Object> future){
		if (mapFutureToObject.containsKey(future))
			return mapFutureToObject.get(future);
		return null;
	}

	@Override
	public synchronized Future<Object> getFuturFromObject(Object o){
		if (mapObjectToFuture.containsKey(o))
			return mapObjectToFuture.get(o);
		return null;
	}

	@Override
	public synchronized  void chargementTermineAndShutdownNow() {
		synchronized(executor){
			executor.shutdownNow();
		}
		//TODO gestionCache.finitnourrir sur tous les objets du manager ?
		// comment reperer ceux sur lesquels d'autres potentiels manager travaillent encore ?
	}

	@Override
	public boolean isChargementTermine(){
		synchronized(executor){
			return executor.isShutdown();
		}
	}

	private void add(Object o){
		int nombreEssais = dejaVu.containsKey(o)? dejaVu.get(o) + 1 : 1;
		dejaVu.put(o, nombreEssais);
	}

	private boolean dejaVu(Object o){
		return dejaVu.containsKey(o) ;
	}

	private int nombreEssais(Object o){
		return dejaVu.containsKey(o)? dejaVu.get(o) : 0;
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
				if(value > 0) 
					return false;
				return true;
			}
		}
	}
}
