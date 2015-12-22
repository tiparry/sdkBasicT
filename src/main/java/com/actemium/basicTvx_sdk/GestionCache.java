package com.actemium.basicTvx_sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class GestionCache {
	private long tempsDeCache = 1000 * 60 * 60; //une heure 
	private Map<Class<?>, Map<String, Object>> dicoClassToAllObject = new HashMap<Class<?>, Map<String,Object>>();
	private Map<Class<?>, Map<String, Stockage>> dicoClassToObjectsCharges = new HashMap<Class<?>,  Map<String, Stockage>>();
	private Map<Class<?>, Map<String, Stockage>> dicoClassToObjectsChargesEnProfondeur = new HashMap<Class<?>,  Map<String, Stockage>>();
	private Map<Class<?>, Stockage> dicoClasseDejaChargee = new HashMap<Class<?>, Stockage>();
	
	public void metEnCache(Object obj){
		if (obj == null) return;
		String id = ArianeHelper.getId(obj);
		if (id == null) return;
		Class<?> clazz = obj.getClass();
		Map<String, Object> dicoIdToObj = getDicoAllObject(clazz);
		dicoIdToObj.put(id, obj);
	}
	
	public void metEnCacheObjectCharge(Object obj){
		metEnCacheObjectCharge(new Stockage(obj));
	}
	public void metEnCacheObjectChargeEnProfondeur(Object obj){
		metEnCacheObjectChargeEnProfondeur(new Stockage(obj));
	}
	
	private void metEnCacheObjectCharge(Stockage stock){
		Object obj = stock.getObject();
		if (obj == null) return;
		String id = ArianeHelper.getId(obj);
		if (id == null) return;
		Class<?> clazz = obj.getClass();
		Map<String, Stockage> dico = getAllObjectsCharges(clazz);
		if(!dico.containsKey(id))
			dico.put(id, stock);
	}
	
	private void metEnCacheObjectChargeEnProfondeur(Stockage stock){
		Object obj = stock.getObject();
		if (obj == null){
			setObsolete(stock.getOldObject());
			return;
		}
		String id = ArianeHelper.getId(obj);
		if (id == null) return;
		Class<?> clazz = obj.getClass();
		Map<String, Stockage> dico = getAllObjectsChargesEnProfondeur(clazz);
		if(!dico.containsKey(id))
			dico.put(id, stock);
	}

	@SuppressWarnings("unchecked")
	public <U> U getObject(Class<U> clazz, String id){
		if(id == null || id.length() == 0 || clazz == null) return null;
		Map<String, Object> dicoIdToObj = getDicoAllObject(clazz);
		return (U) dicoIdToObj.get(id);
	}
	
	@SuppressWarnings("unchecked")
	public <U> U getObjectCharge(Class<U> clazz, String id){
		if(id == null || id.length() == 0 || clazz == null) return null;
		Map<String, Stockage> dico = getAllObjectsCharges(clazz);
		Stockage stock = dico.get(id);
		if(stock != null) return (U) stock.getObject();
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <U> U getObjectChargeEnProfondeur(Class<U> clazz, String id){
		if(id == null || id.length() == 0 || clazz == null) return null;
		Map<String, Stockage> dico = getAllObjectsChargesEnProfondeur(clazz);
		Stockage stock = dico.get(id);
		if(stock != null) return (U) stock.getObject();
		return null;
	}
	public boolean isObjectChargeEnProfondeur(Object obj) {
		String id = ArianeHelper.getId(obj);
		Class<?> clazz = obj.getClass();
		return getObjectChargeEnProfondeur(clazz, id) != null;
	}

	public synchronized void remove(Object obj) {
		if (obj == null) return;
		String id = ArianeHelper.getId(obj);
		if (id == null) return;
		Class<?> clazz = obj.getClass();
		getDicoAllObject(clazz).remove(id);
		getAllObjectsCharges(clazz).remove(id);
		getAllObjectsChargesEnProfondeur(clazz).remove(id);
	}
	
	private synchronized void setObsolete(Object obj){
		if (obj == null) return;
		String id = ArianeHelper.getId(obj);
		if (id == null) return;
		Class<?> clazz = obj.getClass();
		getAllObjectsCharges(clazz).remove(id);
		getAllObjectsChargesEnProfondeur(clazz).remove(id);
	}

	
	private synchronized Map<String, Object> getDicoAllObject(Class<?> clazz) {
		Map<String, Object> dico = dicoClassToAllObject.get(clazz);
		if(dico == null){
			dico = new ConcurrentHashMap<String, Object>();
			dicoClassToAllObject.put(clazz, dico);
		}
		return dico;
	}
	private synchronized Map<String, Stockage> getAllObjectsCharges(Class<?> clazz) {
		Map<String, Stockage> dico = dicoClassToObjectsCharges.get(clazz);
		if(dico == null){
			dico = new ConcurrentHashMap<String, Stockage>();
			dicoClassToObjectsCharges.put(clazz, dico);
		}
		return dico;
	}
	private synchronized Map<String, Stockage> getAllObjectsChargesEnProfondeur(Class<?> clazz) {
		Map<String, Stockage> dico = dicoClassToObjectsChargesEnProfondeur.get(clazz);
		if(dico == null){
			dico = new ConcurrentHashMap<String, Stockage>();
			dicoClassToObjectsChargesEnProfondeur.put(clazz, dico);
		}
		return dico;
	}
	
	public boolean estDejaCharge(Class<?> clazz) {
		return dicoClasseDejaChargee.containsKey(clazz) && (dicoClasseDejaChargee.get(clazz).getObject() != null);
	}
	public void setClasseDejaChargee(Class<?> clazz){
		dicoClasseDejaChargee.put(clazz, new Stockage(clazz));
	}
	
	@SuppressWarnings("unchecked")
	public <U> List<U> getClasse(Class<U> clazz){
		List<U> ret = new ArrayList<U>();
		for(Entry<?, ?> entry : dicoClassToAllObject.get(clazz).entrySet()){
			ret.add((U) entry.getValue());
		}
		return ret;
	}
	
	private class Stockage{
		private Object obj;
		private long dateEntree = System.currentTimeMillis();
		private Stockage(Object obj){
			this.obj = obj;
		}
		private Object getObject(){
			if(System.currentTimeMillis() - dateEntree > tempsDeCache){
				setObsolete(obj);
				return null;
			}
			return obj;
		}
		private Object getOldObject(){
			return obj;
		}
	}
}
