package com.actemium.basicTvx_sdk;

import static com.actemium.basicTvx_sdk.Helper.isNetworkException;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;

import utils.TypeExtension;
import utils.champ.Champ;


public class ManagerChargementEnProfondeur extends ManagerChargementSDK {

	static final int WEB_SERVICE_REQUEST_TIME = 50000;
	static final int LOAD_OBJECT_TIME = 5000;
	static final int RATIO_WORK = WEB_SERVICE_REQUEST_TIME/LOAD_OBJECT_TIME;


	private final Map<Object,Integer> dejaVu = new IdentityHashMap<>();
	private final Map<Future<Object>, Object> mapFutureToObject = new IdentityHashMap<>();
	private final CompletionService<Object> completion;
	private final CompteurdeTaches compteurdeTaches = new CompteurdeTaches();

	ManagerChargementEnProfondeur(GlobalObjectManager gom, Object objetRacine){
		super(gom, objetRacine, Executors.newFixedThreadPool(determinePoolSize()));
		completion = new ExecutorCompletionService<>(getExecutor());
	}

	private static int determinePoolSize(){
		int numberCores = Runtime.getRuntime().availableProcessors();
		return Math.max(1, numberCores/2*(1+RATIO_WORK));
	}

	@Override
	protected void execute() throws GetObjetEnProfondeurException{
		prendEnChargePourChargementEnProfondeur(getObjetRacine(), false);
		try{
			while(!isAllCompleted()){
				Future<Object> future = null;
				try {
					future = waitForATaskToComplete();
					Object aInspecter = traiterTacheTerminee(future);
					addSousObject(aInspecter);
				}
				catch (InterruptedException e) { 
					Thread.currentThread().interrupt();					
					throw new GetObjetEnProfondeurException(getObjetRacine(), e);
				}
				catch (IllegalArgumentException | IllegalAccessException e){
					throw new GetObjetEnProfondeurException(getObjetRacine(), e);
				}
				finally{
					Object objetInterrompu = getObjectFromFutur(future);
					getGom().interromptChargement(objetInterrompu);
					oneTaskCompleted(); 
				}
			}
		}
		finally{
			chargementTermineAndShutdownNow();
		}
	}

	private synchronized boolean createNewTacheChargementProfondeur(Object o, boolean retry){
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
	protected Future<Object> submit(Object o) {
		compteurdeTaches.beforeSubmitTask();
		Future<Object> future = getGom().createFuture(getExecutor(), o);
		mapFutureToObject.put(future, o);
		return future;
	}

	private Future<Object> waitForATaskToComplete() throws InterruptedException{
		return completion.take();
	}

	private boolean isAllCompleted(){
		return compteurdeTaches.isAllCompleted();
	}

	private void oneTaskCompleted(){
		compteurdeTaches.oneTaskCompleted();
	}


	private synchronized Object getObjectFromFutur(Future<Object> future){
		if (mapFutureToObject.containsKey(future))
			return mapFutureToObject.get(future);
		return null;
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

	/**
	 * @param o l'objet à charger en profondeur
	 * @param managerChargementEnProfondeur le ManagerChargementEnProfondeur
	 * @param retry le boolean indiquant si on reessaye de charger l'objet o
	 * @return boolean permettant de savoir si une tâche de chargement a été lancée
	 */
	private boolean prendEnChargePourChargementEnProfondeur(Object o, boolean retry) {
		if(createNewTacheChargementProfondeur(o, retry)){
			submit(o);
			return true;
		}
		return false;
	}

	/**
	 * Donne le résultat de la TâcheChargement d'un objet
	 * 
	 * @param obj l'objet racine du chargement
	 * @param managerChargementEnProfondeur
	 * @param future le Futur contenant la tâche de chargement à traiter
	 * @throws GetObjetEnProfondeurException
	 * @throws InterruptedException
	 */
	private Object traiterTacheTerminee(Future<Object> future) throws GetObjetEnProfondeurException, InterruptedException {
		try{
			return future.get();
		}
		catch( ExecutionException e){ 
			gererExecutionExceptionEnProfondeur(getObjetRacine(), future, e);
			return null;
		}
	}

	/**
	 * gestion des exceptions lors d'un chargement en profondeur.
	 * 
	 * @param obj
	 * @param managerChargementEnProfondeur
	 * @param future
	 * @param e
	 * @throws GetObjetEnProfondeurException
	 */

	private void gererExecutionExceptionEnProfondeur(Object obj, Future<Object> future,ExecutionException e) throws GetObjetEnProfondeurException{
		boolean retry = false;
		Object objectToRecharge = getObjectFromFutur(future);
		getGom().interromptChargement(objectToRecharge);
		if (isNetworkException(e)){
			retry = prendEnChargePourChargementEnProfondeur(objectToRecharge, true);
		}
		if(!retry) {
			Class<?> type = objectToRecharge == null ? void.class : objectToRecharge.getClass();
			GetObjectException objectException = new GetObjectException(getGom().getId(objectToRecharge), type, e);
			throw new GetObjetEnProfondeurException(obj, objectException);
		}	
	}

	private void addSousObject(Object obj) throws IllegalAccessException {
		if(obj == null)
			return;
		List<Champ> champs = TypeExtension.getSerializableFields(obj.getClass());
		for(Champ champ : champs){
			Object value = champ.get(obj);
			if(!champ.isSimple() && value != null){
				if(value instanceof Collection<?>)
					traiteCollection((Collection<?>)value);
				else if(value instanceof Map<?,?>)
					traiteMap((Map<?,?>)value);
				else //objet
					prendEnChargePourChargementEnProfondeur(value, false);
			}
		}
	}

	private void traiteMap(Map<?, ?> map) {
		for(Entry<?,?> entry : map.entrySet()){
			Object k = entry.getKey();
			Object v = entry.getValue();
			if(k != null && !TypeExtension.isSimple(k.getClass()))
				prendEnChargePourChargementEnProfondeur(entry.getKey(), false);
			if(v != null && !TypeExtension.isSimple(v.getClass()))						
				prendEnChargePourChargementEnProfondeur(entry.getValue(), false);
		}
	}

	private void traiteCollection(Collection<?> value) {
		for(Object o : value){
			if(o != null && !TypeExtension.isSimple(o.getClass()))
				prendEnChargePourChargementEnProfondeur(o, false);
		}
	}

	private class CompteurdeTaches{
		private int value = 0;
		private Object lock = new Object();

		private void beforeSubmitTask() {
			synchronized(lock) {
				value++;
			}
		}

		private void oneTaskCompleted() {
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
