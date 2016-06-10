package com.actemium.basicTvx_sdk;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.ChampNotFund;
import giraudsa.marshall.exception.MarshallExeption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

import utils.TypeExtension;
import utils.champ.Champ;

import com.actemium.basicTvx_sdk.exception.GetAllObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;
import com.actemium.basicTvx_sdk.exception.SaveAllException;
import com.actemium.basicTvx_sdk.exception.SaveException;
import com.actemium.basicTvx_sdk.restclient.RestException;
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
	private GlobalObjectManager(String httpLogin, String httpPwd, String gisementBaseUrl){
		this.factory = new ObjectFactory();
		this.persistanceManager = new PersistanceManagerRest(httpLogin,  httpPwd, gisementBaseUrl);
		this.gestionCache = new GestionCache();
	}

	/**
	 * Gets the single instance of GlobalObjectManager.
	 *
	 * @return single instance of GlobalObjectManager
	 */
	public static GlobalObjectManager getInstance(){
		return instance;
	}


	public static void init(String httpLogin, String httpPwd, String gisementBaseUrl){
		instance = new GlobalObjectManager(httpLogin, httpPwd, gisementBaseUrl);
	}

	public void nourrirIdReseau(String host, String username, String password){
		((PersistanceManagerRest)persistanceManager).setConfigAriane(host, username, password);
	}

	/**
	 * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
	 *
	 * @param <U> the generic type
	 * @throws SaveAllException 
	 */
	public synchronized void saveAll() throws SaveAllException {
		try{
			Set<Object> objetsASauvegarder = gestionCache.objetsModifiesDepuisChargementOuNouveau();
			save(objetsASauvegarder);
		}catch(MarshallExeption | IllegalAccessException | IOException | RestException e){
			LOGGER.error("impossible de sauvegarder", e);
			gestionCache.purge();
			LOGGER.error("erreur dans saveAll(), Cache reinitialisé");
			throw new SaveAllException("impossible de sauvegarder", e);
		}
	}
	/**
	 * Sauvegarde de l'objet avec sa grappe d'objet
	 * @param objet
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
			gestionCache.purge();
			LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw new SaveException(e);
		}
	}

	/**
	 * Creates the object.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param date the date
	 * @return the u
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public <U> U createObject(final Class<U> clazz, final Date date) throws InstantiationException, IllegalAccessException {
		return (U) this.factory.newObject(clazz, date, gestionCache);
	}

	/**
	 * Gets the all object by type.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @return the all object by type
	 * @throws GetAllObjectException 
	 */
	public synchronized <U> List<U> getAllObject(final Class<U> clazz) throws GetAllObjectException{
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
		}catch(ParseException | ClassNotFoundException | RestException | IOException | SAXException e){
			LOGGER.error("impossible de récupérer l'objet", e);
			gestionCache.purge();
			LOGGER.error("erreur dans getAllObject(), Cache reinitialisé");
			throw new GetAllObjectException(e);
		}
	}

	/**
	 * Recupere un objet en fonction de son type et de son id. Le crée s'il n'existe pas.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param id the id
	 * @param enProfondeur boolean permettant de provoquer une recuperation de la grappe d'objet en profondeur
	 * @return the object by type and id
	 * @throws GetObjectException 
	 * @throws GetObjetEnProfondeurException 
	 */

	public synchronized <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur) throws GetObjectException, GetObjetEnProfondeurException{
		try{
			if(id == null || clazz == null) 
				return null;
			U obj = gestionCache.getObject(clazz, id); //on regarde en cache
			if(obj == null){
				obj = this.factory.newObjectById(clazz, id, gestionCache);
			}
			if(enProfondeur) 
				getObjetEnProfondeur(obj);
			else nourritObjet(obj);
			return obj;
		}catch(InstantiationException  |  IllegalAccessException e){
			LOGGER.error("impossible de récupérer l'objet", e);
			gestionCache.purge();
			LOGGER.error("erreur dans getObject(), Cache reinitialisé");
			throw new GetObjectException(id, clazz, e);
		}
		catch(GetObjetEnProfondeurException e){
			gestionCache.purge();
			LOGGER.error("erreur dans getObjectEnProfondeur(), Cache reinitialisé");
			throw e;
		}
	}

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

	private void traiterTacheTerminee(Object obj, ManagerChargementEnProfondeur managerChargementEnProfondeur, Future<Object> future) throws GetObjetEnProfondeurException, InterruptedException {
		try{
			future.get();
		}
		catch( ExecutionException e){ 
			gererExecutionExceptionEnProfondeur(obj, managerChargementEnProfondeur, future, e);
		}
	}

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

	private <U> void nourritEffectivementObjet(U obj) throws  GetObjectException {
		ManagerChargementUnique managerChargementUnique= new ManagerChargementUnique();
		try{
			Future<Object> future = managerChargementUnique.submit(null,new TacheWebServiceGetObjet(obj, managerChargementUnique));
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
				Future<Object> future = managerChargementUnique.submit(null,new TacheWebServiceGetObjet(obj, managerChargementUnique));
				future.get();
			}
			catch( ExecutionException | InterruptedException e2){
				unwrappExceptionInGetObjectException(obj, e2);;
			}
		}
		else{
			unwrappExceptionInGetObjectException(obj, e1);;
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
		} catch (ParseException | IllegalArgumentException | RestException | IOException | SAXException | ChampNotFund | InterruptedException | ParserConfigurationException
				| ReflectiveOperationException  e1) {
			throw new GetObjectException(gestionCache.getId(obj), obj.getClass(), e1);
		}
	}


	private <U> void chargeObjetAppelWebService(U obj, ManagerChargementSDK manager) throws RestException, IOException, SAXException, InterruptedException, ParserConfigurationException, ReflectiveOperationException, ChampNotFund{
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
	ChampNotFund, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
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
		if (t instanceof ChampNotFund)
			throw (ChampNotFund)t;
		if (t instanceof InterruptedException)
			throw (InterruptedException)t;
		if (t instanceof ParserConfigurationException)
			throw (ParserConfigurationException)t;
		if (t instanceof ReflectiveOperationException)
			throw (ReflectiveOperationException)t;
	}


	/**
	 * Purge le Cache du GOM pour éviter les fuites mémoires lorsqu'on a fini un traitement.
	 *
	 */
	public void purgeCache() {
		this.gestionCache.purge();
	}

	/**
	 * supprime un objet du cache
	 * @param obj
	 */
	public void remove(Object obj){
		gestionCache.remove(obj);
	}

	public void setDureeCache(long duree, TimeUnit unite){
		gestionCache.setDureeCache(unite.toMillis(duree));
	}


	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 *
	 * @param Requete
	 * @param enProfondeur true si l'on veut récuperer toute l'arborescence de la réponse
	 * @throws GetObjetEnProfondeurException 
	 * @throws GetObjectException 
	 */
	public synchronized Reponse getReponse(Requete request, boolean enProfondeur) throws GetObjetEnProfondeurException, GetObjectException  {
		Reponse reponse;
		try {
			reponse = persistanceManager.getReponse(request, this);
			if(enProfondeur){
				getObjetEnProfondeur(reponse);
			}
		} catch (MarshallExeption | IOException | RestException e) {
			gestionCache.purge();
			LOGGER.error("erreur dans getReponse(), Cache reinitialisé");
			throw new GetObjectException("objet sans id", request.getClass(), e);
		}
		catch(GetObjetEnProfondeurException e){
			gestionCache.purge();
			LOGGER.error("erreur dans getObjectEnProfondeur() de getReponse(), Cache reinitialisé");
			throw e;
		}
		return reponse;
	}

	/**
	 * Gets the objet to save.
	 * @param objetsASauvegarder 
	 *
	 * @param <U> the generic type
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
	 * @throws ClientProtocolException 
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
	 * @param l the l
	 * @param hasChanged the has changed
	 * @return the int
	 * @throws MarshallExeption 
	 * @throws IllegalAccessException 
	 * @throws RestException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	private <U> void save(final U l, final boolean hasChanged, Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		if(this.isNew(l) || hasChanged){
			boolean wasNew = isNew(l);
			String ancienHash = gestionCache.getHash(l);
			gestionCache.setEstEnregistreDansGisement(l);
			objetsASauvegarder.remove(l);
			this.saveReferences(l, TypeRelation.COMPOSITION, objetsASauvegarder);
			try {
				this.persistanceManager.save(l);
			} catch (MarshallExeption | IOException | RestException e) {
				LOGGER.error("impossible d'enregistrer " + gestionCache.getId(l) + " de type " + l.getClass().getName());
				gestionCache.setNEstPasEnregistreDansGisement(l, wasNew, ancienHash);
				throw e;
			}

		} 
	}

	/**
	 * Sauvegarde les objets en tenant compte des relations de composition...
	 *
	 * @param <U> the generic type
	 * @param l l'objet à sauvegarder
	 * @param relation le type de relation (composition, agregation, association)
	 * @return the int
	 * @throws IllegalAccessException 
	 * @throws MarshallExeption 
	 * @throws RestException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
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
	 * @param obj the obj
	 * @return true, if is new
	 */
	public <U> boolean isNew(final U obj) {
		return gestionCache.isNew(obj);
	}


	/**
	 * Méthode pour récupérer un objet depuis le cache du global object manager.
	 * @param id
	 * @param clazz
	 * @return
	 * @see giraudsa.marshall.deserialisation.EntityManager#findObject(java.lang.String, java.lang.Class)
	 */
	@Override public <U> U findObject(final String id, final Class<U> clazz) {
		U obj = this.gestionCache.getObject(clazz, id);
		if(obj != null)
			this.gestionCache.setNotNew(obj);
		return obj;
	}

	/**
	 * Méthode qui met en cache l'objet et son id comme clef de la map du cache.
	 * @param id
	 * @param obj
	 * @see giraudsa.marshall.deserialisation.EntityManager#metEnCache(java.lang.String, java.lang.Object)
	 */
	@Override public void metEnCache(final String id, final Object obj) {
		gestionCache.metEnCache(id, obj, false);
	}

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
		public Object call() throws RestException, IOException, SAXException, ChampNotFund, InterruptedException, ParserConfigurationException, ReflectiveOperationException {
			if (!Thread.currentThread().isInterrupted()) {
			chargeObjetAppelWebService(objetATraiter, managerChargementEnProfondeur);
			ArianeHelper.addSousObject(objetATraiter, gom, managerChargementEnProfondeur);
			}
			return objetATraiter;
		}   
	}

	class TacheWebServiceGetObjet implements Callable<Object> {

		private final Object objetATraiter;
		private ManagerChargementUnique managerChargementUnique;
		public TacheWebServiceGetObjet(Object objetATraiter, ManagerChargementUnique managerChargementUnique) {
			super();
			this.objetATraiter = objetATraiter;
			this.managerChargementUnique=managerChargementUnique;
		}

		@Override
		public Object call() throws RestException, IOException, SAXException, ParserConfigurationException, ReflectiveOperationException, InterruptedException, ChampNotFund {
			if (!Thread.currentThread().isInterrupted()) 
				chargeObjetAppelWebService(objetATraiter, managerChargementUnique);
			return objetATraiter;
		}   
	}
}
