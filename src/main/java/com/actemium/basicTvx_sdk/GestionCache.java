package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.actemium.basicTvx_sdk.util.BiHashMap;

import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

/**
 * le gestionnaire des objets du cache.
 */
public class GestionCache {
	private final static long tempsDeCacheMinimum = 1000 * 60; //une minute 
	private long tempsDeCache = 1000 * 60 * 60; //une heure 
	private BiHashMap<Class<?>, String, Object> classAndIdToObject = new BiHashMap<Class<?>, String, Object>();
	private final Map<Object, Stockage> dejaCharge = new IdentityHashMap<>();
	private Map<Class<?>, Stockage> dicoClasseDejaChargee = new HashMap<Class<?>, Stockage>();
	
	synchronized boolean setDureeCache(long nouveauTempsDeCache) {
		if (nouveauTempsDeCache < tempsDeCacheMinimum){
			tempsDeCache = tempsDeCacheMinimum;
			return false;
		}
		tempsDeCache = nouveauTempsDeCache;
		return true;
	}
	synchronized boolean estCharge(Object obj){
		Stockage s = dejaCharge.get(obj);
		if (s == null)
			return true;
		return s.estCharge();
	}

	synchronized void setEstCharge(Object obj){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return;
		s.setEstCharge();
	}
	
	synchronized Future<Object> getOrCreateFuture(GlobalObjectManager gom, ExecutorService executor, Object obj) {
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return null;
		return s.getOrCreateFuture(gom, executor);
	}
	
	synchronized Future<Object> getChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return null;
		return s.getChargement();
	}
	
	synchronized void setEnTrainDeNourrir(Object obj, Future<Object> future){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return;
		s.setEnTrainDeNourrir(future);
	}
		
	synchronized boolean interromptChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return false ;
		s.interromptChargement();
		return true;
	}
	
	synchronized void metEnCache(String id, Object obj, boolean estNouveau){
		if (obj == null || id == null || id.length() == 0)
			return;
		Class<?> clazz = obj.getClass();
		Object previous =classAndIdToObject.get(clazz, id);
		if(previous!=obj){
			dejaCharge.remove(previous);
			classAndIdToObject.put(clazz, id, obj);
		}
		if(!dejaCharge.containsKey(obj)){
			Stockage s = new Stockage(obj, id);
			s.isNew = estNouveau;
			dejaCharge.put(obj, s);
		}else{
			Stockage s = dejaCharge.get(obj);
			s.isNew = estNouveau;
		}
	}

	@SuppressWarnings("unchecked")
	synchronized <U> U getObject(Class<U> clazz, String id){
		if(id == null || id.length() == 0 || clazz == null)
			return null;
		return (U) classAndIdToObject.get(clazz, id);
	}
	
	synchronized  String getId(Object o){
		if(o == null) 
			return null;
		Stockage s = dejaCharge.get(o);
		if(s == null)
			return null;
		return s.id;
	}

	synchronized void purge() {
		classAndIdToObject.clear();
		dejaCharge.clear();
		dicoClasseDejaChargee.clear();
	}
	
	synchronized boolean aChangeDepuisChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
		if(s == null) 
			return false;
		return s.aChangeDepuisChargement();
	}
	
	synchronized Set<Object> objetsModifiesDepuisChargementOuNouveau(){
		Set<Object> objetsModifies = new LinkedHashSet<>();
		for(Entry<Object, Stockage> entry : dejaCharge.entrySet()){
			Object o = entry.getKey();
			Stockage s = entry.getValue();
			if(s.estCharge() && s.aChangeDepuisChargement() || s.isNew){
				objetsModifies.add(o);
			}
		}
		return objetsModifies;
	}
	
	synchronized void setEstEnregistreDansGisement(Object obj){
		setEstCharge(obj);
		setNotNew(obj);
	}
	
	synchronized void setNEstPasEnregistreDansGisement(Object obj, boolean wasNew, String ancienHash, boolean wasCharge){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return;
		s.isNew = wasNew;
		s.hash = ancienHash;
		s.estCharge=wasCharge;
	}
	
	synchronized String getHash(Object obj){
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return null;
		return s.hash;
	}
	
	synchronized boolean isNew(Object obj) {
		Stockage s = dejaCharge.get(obj);
		if (s == null) 
			return false;
		return s.isNew;
	}
	synchronized void setNotNew(Object obj) {
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return;
		s.isNew = false;
	}
	
	synchronized void setNew(Object obj) {
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return;
		s.isNew = true;
	}

	synchronized void remove(Object obj) {
		if(obj == null) 
			return;
		Stockage s = dejaCharge.get(obj);
		Class<?> clazz = obj.getClass();
		String id = s.id;
		classAndIdToObject.removeObj(clazz, id);
		dejaCharge.remove(obj);
	}
	
	synchronized boolean  estDejaCharge(Class<?> clazz) {
		return dicoClasseDejaChargee.containsKey(clazz) && !(dicoClasseDejaChargee.get(clazz).isObsolete());
	}
	synchronized void setClasseDejaChargee(Class<?> clazz){
		dicoClasseDejaChargee.put(clazz, new Stockage(clazz, clazz.getName()));
	}
	@SuppressWarnings("unchecked")
	synchronized <U> List<U> getClasse(Class<U> clazz){
		List<U> ret = new ArrayList<>();
		for(Entry<Object, Stockage> entry : dejaCharge.entrySet()){
			Object o = entry.getKey();
			if (clazz.isInstance(o))
				ret.add((U) o);
		}
		return ret;
	}
	

	
	
	private class Stockage{
		private Object obj;
		private String id;
		private String hash;
		private boolean isNew = false;
		private boolean estCharge = false;
		private long dateChargement = 0;
		private Future<Object> future = null;

		
		private Stockage(Object obj, String id){
			this.obj = obj;
			this.id = id;
		}
		private Future<Object> getOrCreateFuture(GlobalObjectManager gom, ExecutorService executor) {
			if(future != null)
				return executor.submit(new TacheAttenteWebService(future));
			if (estCharge())
				return executor.submit(new TacheRetourneObjet(obj));
			return future = executor.submit(new TacheChargementWebService(obj, gom));
		}
		private boolean estCharge(){
			return isObsolete()? false: estCharge;
		}
	
		private void interromptChargement(){
			future = null;
		}
		
		private void setEnTrainDeNourrir(Future<Object> future){
			this.future = future;
		}
		
		private Future<Object> getChargement(){
			return future;
		}
		private void setEstCharge(){
			dateChargement = System.currentTimeMillis();
			estCharge = true;
			hash = calculHash();
			future = null;
		}
		
		private boolean isObsolete(){
			if(isNew) 
				return false;
			return System.currentTimeMillis() - dateChargement > tempsDeCache;
		}
		
		private String calculHash() {
			String ret;
			try {
				ret = JsonMarshaller.toJson(obj);
			} catch (MarshallExeption e) {
				ret = UUID.randomUUID().toString(); //on n'arrive pas a creer un id, donc il sera sauvegardé automatiquement
			}
			return ret;
		}
		
		private boolean aChangeDepuisChargement(){
			if(estCharge()){
				return !hash.equals(calculHash());
			}
			return true;
		}
	}
}
