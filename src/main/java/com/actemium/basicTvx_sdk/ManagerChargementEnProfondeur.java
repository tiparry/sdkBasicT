package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;

public class ManagerChargementEnProfondeur {

	private Map<Object,Integer> dejaVu = new IdentityHashMap<>();
	private Map<Future<Object>, Object> mapFutureToObject = new IdentityHashMap<>();
	private List<GetObjectException> exceptions = new ArrayList<>();
	public CompletionService<Object> completion;
	public ExecutorService executor;	
	private CompteurdeTaches compteurdeTaches = new CompteurdeTaches();
	
	public ManagerChargementEnProfondeur(){
		//TODO créer mon executor( : adapter le nombre de thread aux ressources dispos) et mon completion
		executor = Executors.newFixedThreadPool(10);
		completion = new ExecutorCompletionService<Object>(executor);
	}
	

	public  void submit(Object o, Callable<Object> task){
		compteurdeTaches.beforeSubmitTask();
		completion.submit(task);
		mapFutureToObject.put(completion.submit(task), o);
	}
	
	public Future<Object> waitForATaskToComplete() throws InterruptedException{
		return completion.take();
	}
	
	public boolean isAllCompleted(){
		boolean test = compteurdeTaches.isAllCompleted();
		boolean u = !test;
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
	
	public void chargementTermine() {
		executor.shutdown();
	}
	
	public boolean isChargementTermine(){
		return executor.isShutdown();
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
