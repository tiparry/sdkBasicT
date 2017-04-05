package com.actemium.basicTvx_sdk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actemium.basicTvx_sdk.util.BiHashMap;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;
import utils.TypeExtension;
import utils.champ.Champ;

/**
 * le gestionnaire des objets du cache.
 */
public class GestionCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(GestionCache.class);
	private static final long TEMPS_DE_CACHE_MINIMUM = 1000L * 60L; //une minute 
	private long tempsDeCache = 1000L * 60L * 60L; //une heure 
	private BiHashMap<Class<?>, String, Object> classAndIdToObject = new BiHashMap<>();
	private final Map<Object, Stockage> dejaCharge = new IdentityHashMap<>();
	private Map<Class<?>, Stockage> dicoClasseDejaChargee = new HashMap<>();
	
	synchronized int getNombreObjetEnCache(){
		return dejaCharge.size();
	}
	
	synchronized boolean setDureeCache(long nouveauTempsDeCache) {
		if (nouveauTempsDeCache < TEMPS_DE_CACHE_MINIMUM){
			tempsDeCache = TEMPS_DE_CACHE_MINIMUM;
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
	
	synchronized Future<Object> getOrCreateFuture(GlobalObjectManager gom, ManagerChargementSDK managerChargement, Object obj) {
		Stockage s = dejaCharge.get(obj);
		if(s == null)
			return null;
		return s.getOrCreateFuture(gom, managerChargement);
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
	
	synchronized void setFilsEstCharge(Object objetPere) {
		if (objetPere == null)
			return;
		Queue<Object> objetsATraites = new ArrayDeque<>();
		objetsATraites.add(objetPere);
		while(!objetsATraites.isEmpty())
			objetsATraites.addAll(traiteChamps(objetsATraites.poll()));
	}
	
	private Collection<Object> traiteChamps(Object o) {
		Collection<Object> objetsATraites = new ArrayList<>();
		Queue<Champ> champs = new ArrayDeque<>();
		champs.addAll(TypeExtension.getSerializableFields(o.getClass()));
		while(!champs.isEmpty()) {
			Champ champ = champs.poll();
			Object value = getValue(champ, o);
			if (!champ.isSimple() && champ.getRelation().equals(TypeRelation.COMPOSITION) && value != null) {
				if (value instanceof Collection<?>)
					objetsATraites.addAll(traiteCollection((Collection<?>) value));
				else if (value instanceof Map<?, ?>)
					traiteMap((Map<?, ?>) value);
				else // objet
					objetsATraites.add(setEstChargeAndReturn(value));
			}
		}
		return objetsATraites;
	}
	
	private Object getValue(Champ champ, Object o) {
		try {
			return champ.get(o);
		} catch (IllegalAccessException e) {
			LOGGER.error("Impossible de récupérer la valeur du champ " + champ.getName() + " de l'objet " + o.getClass().getName(), e);
			return null;
		}
	}

	private Collection<Object> traiteCollection(Collection<?> value) {
		Collection<Object> objetsATraites = new ArrayList<>();
		for (Object o : value)
			if(o != null && !TypeExtension.isSimple(o.getClass()))
				objetsATraites.add(setEstChargeAndReturn(o));
		return objetsATraites;
	}
	
	private Collection<Object> traiteMap(Map<?, ?> map) {
		Collection<Object> objetsATraites = new ArrayList<>();
		for(Entry<?,?> entry : map.entrySet()) {
			Object k = entry.getKey();
			Object v = entry.getValue();
			if(k != null && !TypeExtension.isSimple(k.getClass()))
				objetsATraites.add(setEstChargeAndReturn(k));
			if(v != null && !TypeExtension.isSimple(v.getClass()))						
				objetsATraites.add(setEstChargeAndReturn(v));
		}
		return objetsATraites;
	}
	
	private Object setEstChargeAndReturn(Object o) {
		setEstCharge(o);
		return o;
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
		private Future<Object> getOrCreateFuture(GlobalObjectManager gom, ManagerChargementSDK managerChargement) {
			if(future != null)
				return managerChargement.submitFuture(new TacheAttenteWebService(future));
			if (estCharge())
				return managerChargement.submitFuture(new TacheRetourneObjet(obj));
			future = managerChargement.submitFuture(new TacheChargementWebService(obj, gom));
			return future;
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
				LOGGER.info("pour info", e);
				ret = UUID.randomUUID().toString(); //on n'arrive pas a creer un id, donc il sera sauvegardé automatiquement
			}
			return ret;
		}
		
		private boolean aChangeDepuisChargement(){
			if(estCharge()){
				return !hash.equals(calculHash());
			}
			return false;
		}
	}
}
