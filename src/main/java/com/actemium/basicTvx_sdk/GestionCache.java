package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.actemium.basicTvx_sdk.util.BiHashMap;
import giraudsa.marshall.exception.NotImplementedSerializeException;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

public class GestionCache {
	private final static long tempsDeCacheMinimum = 1000 * 60; //une minute 
	private long tempsDeCache = 1000 * 60 * 60; //une heure 
	private BiHashMap<Class<?>, String, Object> classAndIdToObject = new BiHashMap<Class<?>, String, Object>();
	private final Map<Object, Stockage> dejaCharge = new HashMap<>();
	private Map<Class<?>, Stockage> dicoClasseDejaChargee = new HashMap<Class<?>, Stockage>();
	
	synchronized boolean setDureeCache(long nouveauTempsDeCache) {
		if (nouveauTempsDeCache < tempsDeCacheMinimum){
			tempsDeCache = tempsDeCacheMinimum;
			return false;
		}
		tempsDeCache = nouveauTempsDeCache;
		return true;
	}
	
	synchronized boolean estChargeEnProfondeur(Object obj){
		Stockage s = dejaCharge.get(obj);
		return s.estChargeEnProfondeur() ;
	}
	synchronized boolean estCharge(Object obj){
		Stockage s = dejaCharge.get(obj);
		return s.estCharge();
	}
	synchronized boolean enChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
		return s.prisEnChargePourChargement();
	}
	synchronized void setChargeEnProfondeur(Object obj){
		Stockage s = dejaCharge.get(obj);
		s.setChargeEnProfondeur();
	}
	synchronized void setEstCharge(Object obj){
		Stockage s = dejaCharge.get(obj);
		s.setEstCharge();
	}
	synchronized boolean setPrisEnChargePourChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
		return s.setPrisEnChargePourChargement();
	}
	
	synchronized void addAChargerEnProfondeur(Object o, GlobalObjectManager gom){
		if(!estChargeEnProfondeur(o) && !prisEnChargePourChargementEnProfondeur(o)){
			setPrisEnChargePourChargementEnProfondeur(o);
			gom.prendEnChargePourChargementEnProfondeur(o);
		}
	}
	
	synchronized void metEnCache(String id, Object obj, boolean estNouveau){
		if (obj == null || id == null || id.length() == 0) return;
		Class<?> clazz = obj.getClass();
		classAndIdToObject.put(clazz, id, obj);
		if(!dejaCharge.containsKey(obj)){
			Stockage s = new Stockage(obj, id);
			s.isNew = estNouveau;
			dejaCharge.put(obj, new Stockage(obj, id));
		}else{
			Stockage s = dejaCharge.get(obj);
			s.isNew = estNouveau;
		}
	}

	@SuppressWarnings("unchecked")
	synchronized <U> U getObject(Class<U> clazz, String id){
		if(id == null || id.length() == 0 || clazz == null) return null;
		return (U) classAndIdToObject.get(clazz, id);
	}
	
	synchronized <U> String getId(Object o){
		if(o == null) return null;
		return dejaCharge.get(o).id;
	}

	synchronized void purge() {
		classAndIdToObject.clear();
		dejaCharge.clear();
		dicoClasseDejaChargee.clear();
	}
	
	synchronized boolean aChangeDepuisChargement(Object obj){
		Stockage s = dejaCharge.get(obj);
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
	}
	synchronized boolean isNew(Object obj) {
		Stockage s = dejaCharge.get(obj);
		return s.isNew;
	}
	synchronized void setNotNew(Object obj) {
		Stockage s = dejaCharge.get(obj);
		s.isNew = false;
	}

	synchronized void remove(Object obj) {
		if(obj == null) return;
		Stockage s = dejaCharge.get(obj);
		Class<?> clazz = obj.getClass();
		String id = s.id;
		classAndIdToObject.removeObj(clazz, id);
		dejaCharge.remove(obj);
	}
	
	private boolean prisEnChargePourChargementEnProfondeur(Object obj){
		Stockage s = dejaCharge.get(obj);
		return s.prisEnChargePourChargementEnProfondeur();
	}
	private void setPrisEnChargePourChargementEnProfondeur(Object obj){
		Stockage s = dejaCharge.get(obj);
		s.setPrisEnChargePourChargementEnProfondeur();
	}
	
	synchronized boolean  estDejaCharge(Class<?> clazz) {
		return dicoClasseDejaChargee.containsKey(clazz) && !(dicoClasseDejaChargee.get(clazz).isObsolete());
	}
	synchronized void setClasseDejaChargee(Class<?> clazz){
		dicoClasseDejaChargee.put(clazz, new Stockage(clazz, clazz.getName()));
	}
	@SuppressWarnings("unchecked")
	synchronized <U> List<U> getClasse(Class<U> clazz){
		List<U> ret = new ArrayList<U>();
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
		private boolean estChargeEnProfondeur = false;
		private boolean estCharge = false;
		private long dateChargement = 0;
		private boolean prisEnChargePourChargement = false;
		private boolean prisEnChargePourChargementEnProfondeur = false;
		private Stockage(Object obj, String id){
			this.obj = obj;
			this.id = id;
		}
		private boolean estChargeEnProfondeur(){
			return isObsolete() ? false : estChargeEnProfondeur ;
		}
		private boolean estCharge(){
			return isObsolete()? false: estCharge;
		}
		private boolean prisEnChargePourChargement(){
			return prisEnChargePourChargement;
		}
		private boolean prisEnChargePourChargementEnProfondeur(){
			return prisEnChargePourChargementEnProfondeur;
		}
		private void setChargeEnProfondeur(){
			this.estChargeEnProfondeur = true;
			this.prisEnChargePourChargementEnProfondeur = false;
			this.prisEnChargePourChargement = false;
		}
		private void setPrisEnChargePourChargementEnProfondeur(){
			this.prisEnChargePourChargementEnProfondeur = true;
		}
		private boolean setPrisEnChargePourChargement(){
			if(this.prisEnChargePourChargement) return false;
			return this.prisEnChargePourChargement = true;
		}
		private void setEstCharge(){
			dateChargement = System.currentTimeMillis();
			prisEnChargePourChargement = false;
			estCharge = true;
			hash = calculHash();
		}
		private boolean isObsolete(){
			if(isNew) return false;
			return System.currentTimeMillis() - dateChargement > tempsDeCache;
		}
		
		private String calculHash() {
			String ret;
			try {
				ret = JsonMarshaller.ToJson(obj);
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException | SecurityException | IOException | NotImplementedSerializeException e) {
				ret = UUID.randomUUID().toString(); //on n'arrive pas a creer un id, donc il sera sauvegardé automatiquement
			}
			return ret;
		}
		
		private boolean aChangeDepuisChargement(){
			if(estCharge()){
				return hash.equals(calculHash());
			}
			return true;
		}
	}
}
