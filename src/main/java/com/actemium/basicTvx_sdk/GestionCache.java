package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.actemium.basicTvx_sdk.util.BiHashMap;

public class GestionCache {
	private long tempsDeCache = 1000 * 60 * 60; //une heure 
	private BiHashMap<Class<?>, String, Object> classAndIdToObject = new BiHashMap<Class<?>, String, Object>();
	private final Map<Object, Stockage> dejaCharge = new HashMap<>();
	private Map<Class<?>, Stockage> dicoClasseDejaChargee = new HashMap<Class<?>, Stockage>();
	
	
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
	
	synchronized void metEnCache(String id, Object obj){
		if (obj == null || id == null || id.length() == 0) return;
		Class<?> clazz = obj.getClass();
		classAndIdToObject.put(clazz, id, obj);
		if(!dejaCharge.containsKey(obj)){
			dejaCharge.put(obj, new Stockage(obj, id));
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

	synchronized void remove(Object obj) {
		if (obj == null) return;
		Stockage s = dejaCharge.get(obj);
		String id = s.id;
		Class<?> clazz = obj.getClass();
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
	public synchronized boolean  estDejaCharge(Class<?> clazz) {
		return dicoClasseDejaChargee.containsKey(clazz) && (dicoClasseDejaChargee.get(clazz).getObject() != null);
	}
	public synchronized void setClasseDejaChargee(Class<?> clazz){
		dicoClasseDejaChargee.put(clazz, new Stockage(clazz, clazz.getName()));
	}
	public synchronized <U> List<U> getClasse(Class<U> clazz){
		List<U> ret = new ArrayList<U>();
		//TODO
		return ret;
	}
	
	private class Stockage{
		private Object obj;
		private String id;
		private boolean estChargeEnProfondeur = false;
		private boolean estCharge = false;
		private long dateChargement = 0;
		private boolean prisEnChargePourChargement = false;
		private boolean prisEnChargePourChargementEnProfondeur = false;
		private Stockage(Object obj, String id){
			this.obj = obj;
			this.id = id;
		}
		private Object getObject(){
			return obj;
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
		}
		private boolean isObsolete(){
			return System.currentTimeMillis() - dateChargement > tempsDeCache;
		}
	}
}
