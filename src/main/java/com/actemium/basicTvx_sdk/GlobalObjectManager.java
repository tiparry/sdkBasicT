package com.actemium.basicTvx_sdk;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.exception.UnmarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.rff.basictravaux.model.bdd.ObjetPersistant;
import utils.TypeExtension;
import utils.champ.Champ;

import com.actemium.basicTvx_sdk.exception.GetAllObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;
import com.actemium.basicTvx_sdk.exception.SaveAllException;
import com.actemium.basicTvx_sdk.exception.SaveException;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.basictravaux.model.AnnuaireWS;
import com.rff.basictravaux.model.webservice.reponse.Reponse;
import com.rff.basictravaux.model.webservice.requete.Requete;


/**
 * Le manager global des objets communiquants avec basic travaux
 */
public class GlobalObjectManager implements EntityManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalObjectManager.class);

	/** l'usine de creation des objets. */
	final ObjectFactory factory;

	/**la gestion du cache. */
	GestionCache gestionCache;

	private final Set<Class<?>> nonRecuperableViaWebService = new HashSet<>();

	/** The persistance manager. */
	private final PersistanceManagerAbstrait persistanceManager;


	private static GlobalObjectManager instance = null;
	
	/** attribut determinant la purge automatique du cache en cas d'exception GetAllObjectException 
	 * GetObjectException, GetObjectEnProfondeurException, SaveAllException, SaveException**/
	private final boolean isCachePurgeAutomatiquementSiException;


	/**
	 * verifie si un objet a été modifié depuis son chargement du gisement
	 *
	 * @param objet the objet
	 * @return the boolean
	 */
	public boolean hasChanged(final Object objet){
		return gestionCache.aChangeDepuisChargement(objet);
	}

	/**
	 * Instantiates a new global object manager.
	 * @param remplirIdReseau 
	 */
	private GlobalObjectManager(String httpLogin, String httpPwd, String gisementBaseUrl, boolean isCachePurgeAutomatiquementSiException){
		this.factory = new ObjectFactory();
		this.persistanceManager = new PersistanceManagerRest(httpLogin,  httpPwd, gisementBaseUrl);
		this.gestionCache = new GestionCache();
		this.isCachePurgeAutomatiquementSiException=isCachePurgeAutomatiquementSiException;
	}

	/**
	 * Gets the single instance of GlobalObjectManager.
	 *
	 * @return single instance of GlobalObjectManager
	 */
	public static GlobalObjectManager getInstance(){
		return instance;
	}


	/**
	 * methode d'initialisation du GlobalObjectManager. La purge automatique du cache en cas d'exception est désactivée. 
	 * 
	 * @param httpLogin le login http
	 * @param httpPwd le mot de pase http
	 * @param gisementBaseUrl l'adresse url du gisement BasicTravaux auquel on veut se connecter
	 */
	public static void init(String httpLogin, String httpPwd, String gisementBaseUrl){
		instance = new GlobalObjectManager(httpLogin, httpPwd, gisementBaseUrl, false);
	}
	
	/**
	 * méthode d'initialisation du GlobalObjectmanager, permet de choisir ou non la purge automatique du cache en cas d'exception.
	 * 
	 * @param httpLogin le login BasicTravaux
	 * @param httpPwd le mot de passe BasicTravaux
	 * @param gisementBaseUrl l'adresse url du gisement BasicTravaux auquel on veut se connecter
	 * @param isCachePurgeAutomatiquementSiException le boolean pour decider de la purge automatique du cache en cas d'exception
	 */
	public static void init(String httpLogin, String httpPwd, String gisementBaseUrl, boolean isCachePurgeAutomatiquementSiException){
		instance = new GlobalObjectManager(httpLogin, httpPwd, gisementBaseUrl, isCachePurgeAutomatiquementSiException);
	}

	/**
	 * méthode d'initialisation des paramètres gaia
	 * 
	 * @param host l'adresse url du gisement gaia
	 * @param username le login gaia
	 * @param password le mot de passe gaia
	 */
	public void nourrirIdReseau(String host, String username, String password){
		((PersistanceManagerRest)persistanceManager).setConfigAriane(host, username, password);
	}

	/**
	 * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
	 *
	 * @throws SaveAllException 
	 */
	public synchronized void saveAll() throws SaveAllException {
		try{
			Set<Object> objetsASauvegarder = gestionCache.objetsModifiesDepuisChargementOuNouveau();
			save(objetsASauvegarder);
		}catch(MarshallExeption | IllegalAccessException | IOException | RestException e){
			LOGGER.error("impossible de sauvegarder", e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans saveAll(), Cache reinitialisé");
			throw new SaveAllException("impossible de sauvegarder", e);
		}
	}
	/**
	 * Sauvegarde de l'objet avec sa grappe d'objet
	 * 
	 * @param <U> le type generique
	 * @param  objet l'objet de type U
	 * @throws SaveException
	 */
	public synchronized <U> void save(U objet) throws SaveException{
		if (objet == null)
			return;
		if (!isNew(objet) && !hasChanged(objet)) 
			return;
		try{
			Set<Object> objetsASauvegarder = new HashSet<>();
			objetsASauvegarder.add(objet);
			save(objetsASauvegarder);
		}catch(MarshallExeption | IllegalAccessException | IOException | RestException e){
			LOGGER.error("impossible de sauvegarder", e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw new SaveException(e);
		}
	}

	/**
	 * Creates the object.
	 *
	 * @param <U> le type generique
	 * @param clazz la classe de type U
	 * @param date the date
	 * @return  l'objet crée
	 * @throws InstanciationException 
	 */
	public synchronized <U> U createObject(final Class<U> clazz, final Date date) throws InstanciationException {
		return this.factory.newObject(clazz, date, gestionCache);
	}

	/**
	 * Gets  all object by type.
	 *
	 * @param <U> le type generique
	 * @param clazz the clazz
	 * @return all object by type
	 * @throws GetAllObjectException 
	 */
	public <U> List<U> getAllObject(final Class<U> clazz) throws GetAllObjectException{
		try{
			if(gestionCache.estDejaCharge(clazz)) {
				return gestionCache.getClasse(clazz);
			}
			final List<U> listeObj = new ArrayList<>();
			final boolean estRecupereViaWebServiceDirectement = !this.nonRecuperableViaWebService.contains(clazz) && this.persistanceManager.getAllObject(clazz, this, listeObj);
			if(estRecupereViaWebServiceDirectement) {
				gestionCache.setClasseDejaChargee(clazz);
			} else {
				this.nonRecuperableViaWebService.add(clazz);
			}
			for(Object o : listeObj){
				gestionCache.setEstCharge(o);//on dit au cache que c'est chargé...
			}
			return listeObj;
		}catch(ParseException | RestException | IOException  e){
			LOGGER.error("impossible de récupérer l'objet", e);
			if(purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getAllObject(), Cache reinitialisé");
			throw new GetAllObjectException(e);
		}
	}

	/**
	 * Recupere un objet du gisement en fonction de son type et de son id. Le crée localement dans le cache s'il n'existe pas.
	 * 
	 * @param <U> le type generique
	 * @param clazz 
	 * @param id l'id
	 * @param enProfondeur  boolean permettant de provoquer une recuperation de la grappe d'objet en profondeur
	 * @return l'objet, par type et id
	 * @throws GetObjectException
	 * @throws GetObjetEnProfondeurException
	 */
	public <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur) throws GetObjectException, GetObjetEnProfondeurException{
		try{
			if(id == null || clazz == null) 
				return null;
			U obj = findObjectOrCreate(id, clazz, false);
			if(enProfondeur) 
				getObjetEnProfondeur(obj);
			else nourritObjet(obj);
			return obj;
		}catch(InstanciationException e){
			LOGGER.error("impossible de récupérer l'objet", e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getObject(), Cache reinitialisé");
			throw new GetObjectException(id, clazz, e);
		}
		catch(GetObjetEnProfondeurException e){
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getObjectEnProfondeur(), Cache reinitialisé", e);
			throw e;
		}
	}

	/**
	 * Charge un objet depuis le gisement ainsi que sa grappe entière d'objets fils.
	 * Fonctionne en multi-threading : une thread est créé par appel au gisement.
	 * 
	 * @param obj
	 * @throws GetObjetEnProfondeurException
	 */
	private void getObjetEnProfondeur(Object obj) throws GetObjetEnProfondeurException{
		ManagerChargementEnProfondeur managerChargementEnProfondeur = new ManagerChargementEnProfondeur();
		prendEnChargePourChargementEnProfondeur(obj, managerChargementEnProfondeur, false);
		try{
			while(!managerChargementEnProfondeur.isAllCompleted()){
				Future<Object> future = null;
				try {
					future = managerChargementEnProfondeur.waitForATaskToComplete();
					traiterTacheTerminee(obj, managerChargementEnProfondeur, future);
				}
				catch (InterruptedException e) { 
					Thread.currentThread().interrupt();
					Object objetInterrompu = managerChargementEnProfondeur.getObjectFromFutur(future);
					gestionCache.finitNourrir(objetInterrompu);
					throw new GetObjetEnProfondeurException(obj, e);
				}
				finally{
					managerChargementEnProfondeur.oneTaskCompleted(); 
				}
			}
		}
		finally{
				managerChargementEnProfondeur.chargementTermineAndShutdownNow();
		}
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
	private void traiterTacheTerminee(Object obj, ManagerChargementEnProfondeur managerChargementEnProfondeur, Future<Object> future) throws GetObjetEnProfondeurException, InterruptedException {
		try{
			future.get();
		}
		catch( ExecutionException e){ 
			gererExecutionExceptionEnProfondeur(obj, managerChargementEnProfondeur, future, e);
		}
	}

	/**
	 * Vérifie que l'objet n'est ni déja chargé ni en train d'être chargé avant de lancer le chargement.
	 * 
	 * @param obj
	 * @throws GetObjectException
	 */
	private <U> void nourritObjet(U obj) throws  GetObjectException{
		if(gestionCache.estCharge(obj))
			return;
		if(gestionCache.isEnTrainDeNourrir(obj) & gestionCache.getChargement(obj)!= null){
			try {
				gestionCache.getChargement(obj).get();
			}
			catch (Exception e){
				gestionCache.finitNourrir(obj);
				unwrappExceptionInGetObjectException(obj, e);
			}	
		}
		else {
				nourritEffectivementObjet(obj);
		}
	}

	/**
	 * Lance le chargement d'un unique objet depuis le gisement BasicTravaux
	 * 
	 * @param obj
	 * @throws GetObjectException
	 */
	private <U> void nourritEffectivementObjet(U obj) throws  GetObjectException {
		ManagerChargementUnique managerChargementUnique= new ManagerChargementUnique();
		try{
			Future<Object> future = managerChargementUnique.submit(null,new TacheChargementUnique(obj, managerChargementUnique));
			future.get();
		}
		catch( ExecutionException e1){ 
			gererExecutionExceptionUnique(obj, managerChargementUnique, e1);
		}
		catch(InterruptedException ie){
			Thread.currentThread().interrupt();
			unwrappExceptionInGetObjectException(obj, ie);
		}
		finally{
			gestionCache.finitNourrir(obj);
			managerChargementUnique.chargementTermineAndShutdownNow();
		}
	}

	private <U> void gererExecutionExceptionUnique(U obj, ManagerChargementUnique managerChargementUnique, ExecutionException e1) throws 
	GetObjectException {
		gestionCache.finitNourrir(obj);
		if (isNetworkException(e1)){
			try{
				Future<Object> future = managerChargementUnique.submit(null,new TacheChargementUnique(obj, managerChargementUnique));
				future.get();
			}
			catch( ExecutionException | InterruptedException e2){
				unwrappExceptionInGetObjectException(obj, e2);
			}
		}
		else{
			unwrappExceptionInGetObjectException(obj, e1);
		}
	}

	private void unwrappExceptionInGetObjectException(Object obj,Exception e) throws GetObjectException{
		if(e instanceof GetObjectException)
			throw (GetObjectException)e;
		try{
			if(e instanceof ExecutionException)
				unwrapAndThrowExecutionException((ExecutionException)e);
			else if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
				throw (InterruptedException)e;
			}
		} catch (NullPointerException | InterruptedException | RestException | IOException | SAXException | ParserConfigurationException | ReflectiveOperationException  e1) {
			throw new GetObjectException(gestionCache.getId(obj), obj.getClass(), e1);
		}
	}

	/**
	 * Charge l'objet depuis le gisement par appel au webService
	 * 
	 * @param obj
	 * @param manager
	 * @throws IOException
	 * @throws RestException
	 * @throws SAXException
	 * @throws InstanciationException
	 * @throws InterruptedException
	 * @throws ParserConfigurationException
	 * @throws ReflectiveOperationException
	 */
	private <U> void chargeObjetAppelWebService(U obj, ManagerChargementSDK manager) throws IOException, RestException, SAXException, InstanciationException, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
		if (gestionCache.estCharge(obj))
			return;
		if( gestionCache.setEnTrainDeNourrir(obj,manager)){
			String id = gestionCache.getId(obj);
			Class<?> clazz = obj.getClass();
			persistanceManager.getObjectById(clazz, id, this); 
			gestionCache.setEstCharge(obj);
		}else if( gestionCache.getChargement(obj) != null){
			try {
				gestionCache.getChargement(obj).get();
			}
			catch (ExecutionException e) { 
				unwrapAndThrowExecutionException(e);
			}
		}
	}

	/**
	 * @param o l'objet à charger en profondeur
	 * @param managerChargementEnProfondeur le ManagerChargementEnProfondeur
	 * @param retry le boolean indiquant si on reessaye de charger l'objet o
	 * @return boolean permettant de savoir si une tâche de chargement a été lancée
	 */
	boolean prendEnChargePourChargementEnProfondeur(Object o, ManagerChargementEnProfondeur managerChargementEnProfondeur, boolean retry) {
		if(managerChargementEnProfondeur.createNewTacheChargementProfondeur(o, retry)){
			managerChargementEnProfondeur.submit(o,new TacheChargementProfondeur(o, managerChargementEnProfondeur, this));
			return true;
		}
		return false;
	}

	private void gererExecutionExceptionEnProfondeur(Object obj, ManagerChargementEnProfondeur managerChargementEnProfondeur, Future<Object> future,ExecutionException e) throws GetObjetEnProfondeurException{
		boolean retry = false;
		Object objectToRecharge = managerChargementEnProfondeur.getObjectFromFutur(future);
		gestionCache.finitNourrir(objectToRecharge);
		if (isNetworkException(e)){
			retry = prendEnChargePourChargementEnProfondeur(objectToRecharge, managerChargementEnProfondeur, true);
		}
		if(!retry) {
			GetObjectException objectException = new GetObjectException(gestionCache.getId(objectToRecharge), objectToRecharge.getClass(), e);
			throw new GetObjetEnProfondeurException(obj, objectException);
		}	
	}

	private boolean isNetworkException(ExecutionException e){
		if(e.getCause() instanceof RestException || e.getCause() instanceof IOException)
			return true;
		return false;
	}

	private void unwrapAndThrowExecutionException(ExecutionException e) throws  RestException, IOException, SAXException, 
	 InterruptedException, ParserConfigurationException, ReflectiveOperationException {
		Throwable t = e.getCause();
		if (t instanceof ParseException)
			throw (ParseException)t;
		if (t instanceof ClassNotFoundException)
			throw (ClassNotFoundException)t;
		if (t instanceof RestException)
			throw (RestException)t;
		if (t instanceof IOException)
			throw (IOException)t;
		if (t instanceof SAXException)
			throw (SAXException)t;
		if (t instanceof IllegalArgumentException)
			throw (IllegalArgumentException)t;
		if (t instanceof InterruptedException)
			throw (InterruptedException)t;
		if (t instanceof ParserConfigurationException)
			throw (ParserConfigurationException)t;
		if (t instanceof ReflectiveOperationException)
			throw (ReflectiveOperationException)t;
	}

	
	/**
	 * purge le cache si la variable boolean isCachePurgeAutomatiquementSiException est true
	 * 
	 * @return la valeur de la variable boolean isCachePurgeAutomatiquementSiException
	 */
	private boolean purgeCacheAutomatiquementSiException(){
		if(isCachePurgeAutomatiquementSiException){
			purgeCache();
			return true;
		}
		return false;
	}
	
	/**
	 * Purge le Cache du GOM pour éviter les fuites mémoires lorsqu'on a fini un traitement.
	 *
	 */
	public synchronized void purgeCache() {
		this.gestionCache.purge();
	}

	/**
	 * supprime un objet du cache
	 * @param obj
	 */
	public void remove(Object obj){
		gestionCache.remove(obj);
	}

	/**
	 * permet de définir à partir de combien de temps apres le chargement un objet est considéré comme obsolète
	 * @param duree
	 * @param unite
	 */
	public void setDureeCache(long duree, TimeUnit unite){
		gestionCache.setDureeCache(unite.toMillis(duree));
	}

	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 * 
	 * @param request
	 * @param enProfondeur true si l'on veut récuperer toute l'arborescence de la réponse
	 * @return la reponse correspondant à la requête
	 * @throws GetObjetEnProfondeurException
	 * @throws GetObjectException
	 */
	public Reponse getReponse(Requete request, boolean enProfondeur) throws GetObjetEnProfondeurException, GetObjectException  {
		Reponse reponse;
		try {
			reponse = persistanceManager.getReponse(request, this);
			if(enProfondeur){
				getObjetEnProfondeur(reponse);
			}
		} catch (MarshallExeption | IOException | RestException e) {
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getReponse(), Cache reinitialisé");
			throw new GetObjectException("objet sans id", request.getClass(), e);
		}
		catch(GetObjetEnProfondeurException e){
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getObjectEnProfondeur() de getReponse(), Cache reinitialisé");
			throw e;
		}
		return reponse;
	}

	/**
	 * Gets the objet to save.
	 * 
	 * @param <U> the generic type
	 * @param objetsASauvegarder 
	 * @return the objet to save
	 */
	@SuppressWarnings("unchecked")
	private <U> U getObjetToSave(Set<Object> objetsASauvegarder) {
		if(objetsASauvegarder.iterator().hasNext())
			return (U) objetsASauvegarder.iterator().next();
		return null;
	}


	/**
	 * Save.
	 *
	 * @param value the value
	 * @throws MarshallExeption 
	 * @throws IllegalAccessException 
	 * @throws RestException 
	 * @throws IOException 
	 */
	private void save(Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, IOException, RestException{
		Object obj = getObjetToSave(objetsASauvegarder);
		while(obj != null){
			this.save(obj, this.hasChanged(obj), objetsASauvegarder);
			obj = this.getObjetToSave(objetsASauvegarder);
		}
	}

	/**
	 * Save.
	 *
	 * @param <U> the generic type
	 * @param l l'objet à sauvegarder
	 * @param hasChanged 
	 * @throws MarshallExeption 
	 * @throws IllegalAccessException 
	 * @throws RestException 
	 * @throws IOException 
	 */
	private <U> void save(final U l, final boolean hasChanged, Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		if(this.isNew(l) || hasChanged){
			boolean wasNew = isNew(l);
			String ancienHash = gestionCache.getHash(l);
			boolean wasCharge= gestionCache.estCharge(l);
			gestionCache.setEstEnregistreDansGisement(l);
			objetsASauvegarder.remove(l);
			this.saveReferences(l, TypeRelation.COMPOSITION, objetsASauvegarder);
			try {
				this.persistanceManager.save(l);
			} catch (MarshallExeption | IOException | RestException e) {
				LOGGER.error("impossible d'enregistrer " + gestionCache.getId(l) + " de type " + l.getClass().getName());
				gestionCache.setNEstPasEnregistreDansGisement(l, wasNew, ancienHash,wasCharge);
				throw e;
			}

		} 
	}

	/**
	 * Sauvergarde les objets en tenant compte des relations de composition
	 * 
	 * @param l l'objet à sauvegarder
	 * @param relation le type de relation (composition, agregation, association)
	 * @param objetsASauvegarder le set d'objets à sauvergarder
	 * @throws IllegalAccessException
	 * @throws MarshallExeption
	 * @throws IOException
	 * @throws RestException
	 */
	private <U> void saveReferences(final U l, final TypeRelation relation, Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		if(relation == TypeRelation.COMPOSITION){
			final List<Champ> champs = TypeExtension.getSerializableFields(l.getClass());
			for(final Champ champ : champs){
				if (!champ.isSimple()){
					final Class<?>  type = champ.getValueType();
					if(Collection.class.isAssignableFrom(type)){
						final Iterable<?> collection = (Iterable<?>) champ.get(l);
						if(collection != null){
							for(final Object objet : collection) {
								this.saveReferences(objet, champ.getRelation(), objetsASauvegarder);
							}
						}
					}else if (type.getPackage() == null || ! type.getPackage().getName().startsWith("System")) {//object
						final Object toSave = champ.get(l);
					if(toSave != null) {
						this.saveReferences(champ.get(l), champ.getRelation(), objetsASauvegarder);
					}
					}

				}
			}
		}else{
			this.save(l, this.hasChanged(l), objetsASauvegarder);
		}
	}





	/**
	 * Verifie si un objet est nouveau (c'est à dire s'il a été fabriqué localement).
	 *
	 * @param <U> the generic type
	 * @param obj l'objet
	 * @return true, if is new
	 */
	public <U> boolean isNew(final U obj) {
		return gestionCache.isNew(obj);
	}

	public synchronized void metEnCache(Object objetPere,boolean recursif, boolean isNew) throws IllegalAccessException{
		if(objetPere instanceof ObjetPersistant)
			this.gestionCache.metEnCache(((ObjetPersistant) objetPere).getId().toString(), objetPere, isNew);
		if(objetPere instanceof ariane.modele.base.ObjetPersistant)
			this.gestionCache.metEnCache(((ariane.modele.base.ObjetPersistant) objetPere).getId().toString(), objetPere, isNew);
		if(!recursif){
			return;
		}
		final List<Champ> champs = TypeExtension.getSerializableFields(objetPere.getClass());
		for(final Champ champ : champs){
			if (!champ.isSimple()){
				final Class<?>  type = champ.getValueType();
				if(Collection.class.isAssignableFrom(type)){
					final Iterable<?> collection = (Iterable<?>) champ.get(objetPere);
					if(collection != null){
						for(final Object objet : collection) {
							this.metEnCache(objet, recursif, isNew);
						}
					}
				}else if (type.getPackage() == null || ! type.getPackage().getName().startsWith("System")) {//object
					final Object objetFils = champ.get(objetPere);
					if(objetFils!= null) {
						this.metEnCache(objetFils, recursif, isNew);
					}
				}

			}
		}
	}

	/**
	 * Méthode pour récupérer un objet du cache ou le creer s'in n'existe pas.
	 * 
	 * @param id
	 * @param clazz
	 * @param fromExt true si l'objet désiré à une existence en dehors du cache local du GlobalObjectManager (ex: dans le gisement BasicTravaux)
	 * @return l'objet
	 * @throws InstanciationException 
	 * @see giraudsa.marshall.deserialisation.EntityManager#findObjectOrCreate(java.lang.String, java.lang.Class, boolean )
	 */
	@Override
	public synchronized <U> U findObjectOrCreate(final String id, final Class<U> clazz, final boolean fromExt) throws InstanciationException {
		U obj = gestionCache.getObject(clazz, id); //on regarde en cache
		if(obj == null){
			obj = this.factory.newObjectById(clazz, id, gestionCache);
		}
		if (obj!=null && fromExt){
			this.gestionCache.setNotNew(obj);
		}
		return obj;
	}



	/**sauvegarde l'ensemble des objets du gisement dans un fichier, au format sérialisé json
	 * 
	 * warning 1 cette méthode va purger le cache avant de se lancer; 
	 * warning 2 : dangereux (temps d'exécution + mémoire + espace de stockage) si BDD large !
	 * 
	 * @throws ClassNotFoundException
	 * @throws GetAllObjectException
	 * @throws GetObjetEnProfondeurException 
	 * @throws GetObjectException 
	 * @throws MarshallExeption 
	 * @throws IOException 
	 */
	public  void dumpGisementToJson(String pathFile) throws ClassNotFoundException, GetAllObjectException, GetObjectException, GetObjetEnProfondeurException, MarshallExeption, IOException{
		AnnuaireWS annuaire = AnnuaireWS.getInstance();
		Map<String, String> dicoClasseToPut = annuaire.getDicoClasseToPutUrl();
		Set<Class<?>> classes = new HashSet<>();
		for (String nomClasse : dicoClasseToPut.keySet()){
			classes.add(Class.forName(nomClasse));
		}
		this.purgeCache(); //peut etre pas necessaire
		List<Object>  objetsToSave = new ArrayList<>();
		for(Class<?> clazz : classes){
			objetsToSave.addAll(getAllEnProfondeur(clazz,false));
		}
		File dump = new File(pathFile);
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(dump)));
			for(Object objetToWrite : objetsToSave){
				pw.println(JsonMarshaller.toJson(objetToWrite));

			}
			pw.close();
		}
		catch(IOException e){
			LOGGER.error("Erreur lors de l'écriture: "+e.getMessage());
			throw e;
		}
	}

	/**charge les objets à partir du fichier json, puis les sauvegarde sur le gisement
	 * 
	 *
	 * warning, cette méthode purge le cache avant de se lancer
	 * 
	 * @throws IOException
	 * @throws UnmarshallExeption
	 * @throws SaveAllException
	 */
	public void saveFromJsonFileToGisement(String pathFile) throws IOException, UnmarshallExeption, SaveAllException{
		File dump = new File(pathFile);
		try{
			BufferedReader br = new BufferedReader(new FileReader(dump));
			this.purgeCache();
			try{
				String objetJson = br.readLine();
				while(objetJson!=null){
					Object objet = JsonUnmarshaller.fromJson(objetJson,this);
					gestionCache.setNew(objet);
					objetJson = br.readLine();
				}
				br.close();
			}
			catch(IOException e){
				LOGGER.error("erreur de lecture du fichier: "+e.getMessage());
				throw e;
			}
		}
		catch(FileNotFoundException e){
			LOGGER.error("le fichier de dump n'a pas été trouvé");
			throw e;
		}
		this.saveAll(); 
	}

		
	private <U> List<Object> getAllEnProfondeur(Class<U> classe, boolean enProfondeur) throws GetAllObjectException, GetObjectException, GetObjetEnProfondeurException{
		String typeObjet="autre";
		List<Object> objetsComplets = new ArrayList<>();
		List<U> liste = getAllObject(classe);
		if (ariane.modele.base.ObjetPersistant.class.isAssignableFrom(classe)){
			typeObjet="ariane";
		}
		if (ObjetPersistant.class.isAssignableFrom(classe)){
			typeObjet="basictravaux";
		}
		switch (typeObjet){
		case "basictravaux":
			for(U objet : liste){
					objetsComplets.add(getObject(classe, ((ObjetPersistant)objet).getId().toString(), enProfondeur));
			}
			break;
		case "ariane":
			for(U objet : liste){
				objetsComplets.add(getObject(classe, ((ariane.modele.base.ObjetPersistant)objet).getId().toString(), enProfondeur));
			}
			break;
		default:;
		}
		return objetsComplets;
	}

	/**
	 * 
	 * Appelle le webservice approprié pour charger l'objet demandé, et initie les tâches de chargement de ses objets fils direct.
	 * S'exécute dans une thread distincte.
	 *
	 */
	class TacheChargementProfondeur implements Callable<Object> {

		private final Object objetATraiter;
		private final ManagerChargementEnProfondeur managerChargementEnProfondeur;
		private final GlobalObjectManager gom;

		public TacheChargementProfondeur(Object objetATraiter, ManagerChargementEnProfondeur managerChargementEnProfondeur, GlobalObjectManager gom) {
			super();
			this.objetATraiter = objetATraiter;
			this.managerChargementEnProfondeur = managerChargementEnProfondeur;
			this.gom=gom;
		}

		@Override
		public Object call() throws IOException, RestException, SAXException, InstanciationException, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
			if (!Thread.currentThread().isInterrupted()) {
			chargeObjetAppelWebService(objetATraiter, managerChargementEnProfondeur);
			ArianeHelper.addSousObject(objetATraiter, gom, managerChargementEnProfondeur);
			}
			return objetATraiter;
		}   
	}

	/**
	 * 
	 * Appelle le webservice approprié pour charger l'objet demandé.
	 * S'exécute dans une thread distincte.
	 *
	 */
	class TacheChargementUnique implements Callable<Object> {

		private final Object objetATraiter;
		private ManagerChargementUnique managerChargementUnique;
		public TacheChargementUnique(Object objetATraiter, ManagerChargementUnique managerChargementUnique) {
			super();
			this.objetATraiter = objetATraiter;
			this.managerChargementUnique=managerChargementUnique;
		}

		@Override
		public Object call() throws IOException, RestException, SAXException, InstanciationException, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
			if (!Thread.currentThread().isInterrupted()) 
				chargeObjetAppelWebService(objetATraiter, managerChargementUnique);
			return objetATraiter;
		}   
	}
}
