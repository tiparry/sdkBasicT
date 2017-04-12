package com.actemium.basicTvx_sdk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.actemium.basicTvx_sdk.exception.GetAllObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.exception.GetObjetEnProfondeurException;
import com.actemium.basicTvx_sdk.exception.GomException;
import com.actemium.basicTvx_sdk.restclient.RestException;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import ariane.modele.ressource.RessourceAbstraite;
import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.exception.UnmarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;
import utils.EntityManager;
import utils.TypeExtension;
import utils.champ.Champ;


/**
 * Le manager global des objets communiquants avec basic travaux
 */
public class GlobalObjectManager implements EntityManager {


	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalObjectManager.class);


	private static final String IMPOSSIBLE_DE_SAUVEGARDER = "impossible de sauvegarder";


	private static GlobalObjectManager instance = null;


	private final Set<Class<?>> nonRecuperableViaWebService = new HashSet<>();


	/** l'usine de creation des objets. */
	private final ObjectFactory<?> factory;


	/**la gestion du cache. */
	private GestionCache gestionCache;
	
	
	/**permet de manipuler des id sans etre dépendant de la bibliothèque métier */
	private final IdHelper<?> idHelper;


	/** attribut determinant la purge automatique du cache en cas d'exception GetAllObjectException 
	 * GetObjectException, GetObjectEnProfondeurException, SaveAllException, SaveException**/
	private final boolean isCachePurgeAutomatiquementSiException;

	private final Object lockFindOrCreate = new Object();

	/** The persistance manager. */
	final PersistanceManagerAbstrait persistanceManager;


	/**
	 * Instantiates a new global object manager.
	 * @param remplirIdReseau 
	 */
	private GlobalObjectManager(String httpLogin, String httpPwd, String gisementBaseUrl, boolean isCachePurgeAutomatiquementSiException, int connectTimeout, int socketTimeout, IdHelper<?> idHelper, List<String> annuaires)throws RestException{
		this.idHelper = idHelper;
		this.factory = new ObjectFactory<>(idHelper);
		this.persistanceManager = new PersistanceManagerRest(httpLogin,  httpPwd, gisementBaseUrl, connectTimeout, socketTimeout, annuaires);
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
	 * methode d'initialisation du GlobalObjectManager. 
	 * 
	 * @param gomConfiguration
	 */
	public static GlobalObjectManager init(GOMConfiguration gomConfiguration)throws RestException{
		instance = new GlobalObjectManager(gomConfiguration.getHttpLogin(), gomConfiguration.getHttpPwd(), gomConfiguration.getGisementBaseUrl(),gomConfiguration.isCachePurgeAutomatiquement(),
				gomConfiguration.getConnectTimeout(), gomConfiguration.getSocketTimeout(), gomConfiguration.getIdHelper(), gomConfiguration.getAnnuaires());
		return instance;
	}
	
	public int getNombreObjetEnCache(){
		return gestionCache.getNombreObjetEnCache();
	}

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
	 * méthode d'initialisation des paramètres gaia
	 * 
	 * @param host l'adresse url du gisement gaia
	 * @param username le login gaia
	 * @param password le mot de passe gaia
	 */
	public void nourrirIdReseau(String host, String username, String password){
		((PersistanceManagerRest)persistanceManager).setConfigAriane(host, username, password);
	}


	public <U> U  setIdReseau(Class<U> clazz, String id) throws GomException{
		if(clazz==null || id==null)
			return null;
		if(!RessourceAbstraite.class.isAssignableFrom(RessourceAbstraite.class))
			return null;
		try {
			 return ((PersistanceManagerRest)persistanceManager).setIdReseauManuellement(clazz, id, this);
		} catch (RestException | IOException | SAXException | InstanciationException e) {
			LOGGER.error("erreur dans l'attribution de l'id reseau");
			throw new GomException("erreur dans l'attribution de l'id reseau", e);
		}
	}
	
	/**
	 * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
	 *
	 * @throws SaveAllException 
	 */
	public synchronized void saveAll(CallBack... callBacks) throws GomException {
		try{
			Set<Object> objetsASauvegarder = gestionCache.objetsModifiesDepuisChargementOuNouveau();
			save(objetsASauvegarder, callBacks);
		}catch(MarshallExeption | IllegalAccessException | IOException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans saveAll(), Cache reinitialisé");
			throw new GomException(IMPOSSIBLE_DE_SAUVEGARDER, e);
		}
		catch(RestException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans saveAll(), Cache reinitialisé");
			throw e;
		}
	}
	/**
	 * Sauvegarde de l'objet avec sa grappe d'objet
	 * 
	 * @param <U> le type generique
	 * @param  objet l'objet de type U
	 * @throws SaveException
	 */
	public synchronized <U> void save(U objet, CallBack... callBacks) throws GomException{
		if (objet == null)
			return;
		if (!isNew(objet) && !hasChanged(objet)) 
			return;
		try{
			Set<Object> objetsASauvegarder = new HashSet<>();
			objetsASauvegarder.add(objet);
			save(objetsASauvegarder, callBacks);
		}catch(MarshallExeption | IllegalAccessException | IOException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw new GomException(e);
		}
		catch( RestException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw e;
		}
	}
	
	private void getGrappe(Object objet, Set<Object> grappe) throws IllegalAccessException{
		if(objet == null || grappe.contains(objet))
			return;
		Class<?> type = objet.getClass();
		if(objet instanceof Collection<?>)
			for(Object o : (Collection<?>) objet)
				getGrappe(o, grappe);
		else if (type.getPackage() != null && type.getPackage().getName().startsWith("java")){
			return;
		}
		else if(!TypeExtension.isSimple(objet.getClass())  ){
			grappe.add(objet);
			for(Champ champ : TypeExtension.getSerializableFields(objet.getClass())){
				getGrappe(champ.get(objet), grappe);
			}
		}
	}
	
	public synchronized <U> void saveEnProfondeur(U objet, CallBack... callBacks) throws GomException{
		if (objet == null)
			return;
		try{
			Set<Object> objetsASauvegarder = new LinkedHashSet<>();
			getGrappe(objet, objetsASauvegarder);
			save(objetsASauvegarder, callBacks);
		}catch(MarshallExeption | IllegalAccessException | IOException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans saveEnProfondeur(), Cache reinitialisé");
			throw new GomException(e);
		}
		catch( RestException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans saveEnProfondeur(), Cache reinitialisé");
			throw e;
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
		return factory.newObject(clazz, date, gestionCache);
	}

	/**
	 * Gets  all object by type.
	 *
	 * @param <U> le type generique
	 * @param clazz the clazz
	 * @return all object by type
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
				setEstCharge(o);//on dit au cache que c'est chargé...
			}
			return gestionCache.getClasse(clazz);
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
	public synchronized <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur) throws GetObjectException, GetObjetEnProfondeurException{
		try{
			if(id == null || clazz == null) 
				return null;
			U obj = findObjectOrCreate(id, clazz);
			ManagerChargementSDK manager = enProfondeur ? new ManagerChargementEnProfondeur(this, obj) : new ManagerChargementUnique(this, obj);
			manager.execute();
			if( !isNew(obj) && !gestionCache.estCharge(obj)){
				obj = factory.newObjectAndId(clazz, id, gestionCache);
			}
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
	public synchronized void remove(Object obj){
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
	public synchronized Reponse getReponse(Requete request, boolean enProfondeur) throws GetObjetEnProfondeurException, GetObjectException  {
		Reponse reponse;
		try {
			reponse = persistanceManager.getReponse(request, this);
			setFilsEstCharge(reponse);
			if(enProfondeur){
				ManagerChargementSDK manager = new ManagerChargementEnProfondeur(this, reponse);
				manager.execute();
			}
		} catch (MarshallExeption | IOException | RestException e) {
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getReponse(), Cache reinitialisé");
			throw new GetObjectException("Impossible de récupérer la réponse", request.getClass(), e);
		}
		catch(GetObjetEnProfondeurException e){
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans getObjectEnProfondeur() de getReponse(), Cache reinitialisé");
			throw e;
		}
		return reponse;
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

	public synchronized  void metEnCache(Object objetPere,boolean enProfondeur, boolean isNew) throws IllegalAccessException{
		metEnCache(objetPere, enProfondeur, isNew, new HashSet<Object>());
	}
	
	private void metEnCache(Object objetPere,boolean enProfondeur, boolean isNew, HashSet<Object> dejaVu) throws IllegalAccessException{
		if(dejaVu.contains(objetPere))
			return;
		dejaVu.add(objetPere);
		if(objetPere instanceof Collection<?>){
			final Iterable<?> collection = (Iterable<?>) objetPere;
			for(final Object objet : collection)
				this.metEnCache(objet, enProfondeur, isNew, dejaVu);
		}else if(idHelper.getId(objetPere) != null )
			this.gestionCache.metEnCache(idHelper.getId(objetPere).toString(), objetPere, isNew);
		if(enProfondeur){
			final List<Champ> champs = TypeExtension.getSerializableFields(objetPere.getClass());
			for(final Champ champ : champs){
				final Object objetFils = champ.get(objetPere);
				if(objetFils != null && !champ.isSimple()){
					metEnCache(objetFils, enProfondeur, isNew, dejaVu);
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
	public <U> U findObjectOrCreate(final String id, final Class<U> clazz) throws InstanciationException {
		synchronized(lockFindOrCreate){
			U obj = gestionCache.getObject(clazz, id); //on regarde en cache
			if(obj == null){
				obj = this.factory.newObjectWithOnlyId(clazz, id, gestionCache);
			}
			return obj;
		}
	}



	/**sauvegarde l'ensemble des objets du gisement dans un fichier, au format sérialisé json
	 * 
	 * warning 1 cette méthode va purger le cache avant de se lancer; 
	 * warning 2 : dangereux (temps d'exécution + mémoire + espace de stockage) si BDD large !
	 * @throws DumpException 
	 */
	public synchronized void dumpGisementToJson(String pathFile) throws GomException{
		try {
			loadGisementInCache();
			dumpCacheToJson(pathFile);
		} catch (SecurityException | IllegalArgumentException | GetAllObjectException e) {
			LOGGER.error("Dump du gisement impossible à réaliser", e);
			throw new GomException("Dump du gisement impossible à réaliser", e);
		}
	}

	/**charge l'ensemble des objets du gisement dans le gom
	 * 
	 * warning 1 cette méthode va purger le cache avant de se lancer; 
	 * warning 2 : dangereux (temps d'exécution + mémoire ) si BDD large !
	 * 
	 * @throws GetAllObjectException
	 */
	public synchronized void loadGisementInCache() throws GetAllObjectException{
		this.purgeCache();
		Set<Class<?>> classes = persistanceManager.getAllClasses();
		this.purgeCache(); //peut etre pas necessaire
		for(Class<?> clazz : classes){
			getAllObject(clazz);
		}
	}

	/**sauvegarde tous les objets du cache dans un fichier json
	 * @throws DumpException 
	 * @throws IOException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public synchronized void dumpCacheToJson(String pathFile) throws GomException{
		File dump = new File(pathFile);
		try (FileOutputStream file = new FileOutputStream(dump);
				Writer writer = new BufferedWriter(new OutputStreamWriter(file,"UTF-8"))){
			Field objetsCacheField = this.gestionCache.getClass().getDeclaredField("dejaCharge");
			objetsCacheField.setAccessible(true);
			Map<Object, ?> objetsCache = (Map<Object, ?>)objetsCacheField.get(this.gestionCache);
			for(Object objetToWrite : objetsCache.keySet())
				JsonMarshaller.toJson(objetToWrite, writer, this);	
		}
		catch( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e){
			LOGGER.error("Erreur lors de la reflection: ", e);
			throw new GomException("Erreur lors de la reflection", e);
		}
		catch(IOException e){
			LOGGER.error("Erreur lors de l'écriture: ", e);
			throw new GomException("Erreur lors de l'écriture", e);
		}
		catch(MarshallExeption e){
			LOGGER.error("Erreur lors de la sérialisation: ", e);
			throw new GomException("Erreur lors de la sérialisation", e);
		}
	}

	/**charge les objets à partir du fichier json, puis les sauvegarde sur le gisement
	 * 
	 *
	 * warning, cette méthode purge le cache avant de se lancer
	 * @throws DumpException 
	 * 
	 * @throws IOException
	 * @throws UnmarshallExeption
	 * @throws SaveAllException
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	public void saveToGisementFromJsonFile(String pathFile) throws GomException{
		try {
			loadJsonFile(pathFile);
			this.saveAll();
		} catch (GomException e) {
			LOGGER.error("impossible de sauvegarder le gisement sur un fichier json " + pathFile, e);
			throw e;
		} 
	}

	/**charge les objets à partir du fichier json,
	 * 
	 *
	 * warning, cette méthode purge le cache avant de se lancer
	 * @throws DumpException 
	 */
	@SuppressWarnings("unchecked")
	public void loadJsonFile(String pathFile) throws GomException{
		File dump = new File(pathFile);
		try(FileInputStream file = new FileInputStream(dump);
				Reader reader = new BufferedReader(new InputStreamReader(file,"UTF-8"))){
			this.purgeCache();
			JsonUnmarshaller.fromJson(reader,this); 
			Field objetsCacheField = this.gestionCache.getClass().getDeclaredField("dejaCharge");
			objetsCacheField.setAccessible(true);
			Map<Object, ?> objetsCache = (Map<Object, ?>)objetsCacheField.get(this.gestionCache);
			for(Object objetToWrite : objetsCache.keySet()){
				gestionCache.setNew(objetToWrite);
			}
		}
		catch( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e){
			LOGGER.error("Erreur lors de la reflection: ", e);
			throw new GomException("erreur de la reflection", e);
		}
		catch(IOException e){
			LOGGER.error("erreur de lecture du fichier: ", e);
			throw new GomException("erreur de lecture du fichier", e);
		} catch (UnmarshallExeption e) {
			LOGGER.error("Erreur lors de la désérialisation ", e);
			throw new GomException("erreur de la désérialisation", e);
		} 
	}

	protected Future<Object> createFuture(ManagerChargementSDK managerChargement, Object o){
		return gestionCache.getOrCreateFuture(this, managerChargement, o);
	}


	protected void setEstCharge(Object objetCharge) {
		gestionCache.setEstCharge(objetCharge);
	}
	
	protected void setFilsEstCharge(Object objetPere) {
		gestionCache.setFilsEstCharge(objetPere);
	}


	@Override
	public String getId(Object objetDontOnVeutLId) {
		return gestionCache.getId(objetDontOnVeutLId);
	}


	protected void interromptChargement(Object objetInterrompu) {
		gestionCache.interromptChargement(objetInterrompu);
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
	 * Gets the objet to save.
	 * 
	 * @param <U> the generic type
	 * @param objetsASauvegarder 
	 * @return the objet to save
	 */
	@SuppressWarnings("unchecked")
	private <U> U getObjetToSave(Iterator<Object> iterator) {
		if(iterator.hasNext())
			return (U) iterator.next();
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
	private void save(Set<Object> objetsASauvegarder, CallBack... callBacks) throws IllegalAccessException, MarshallExeption, IOException, RestException{
		Iterator<Object> iterator = objetsASauvegarder.iterator();
		Object obj = getObjetToSave(iterator);
		while(obj != null){
			this.save(obj, this.hasChanged(obj), objetsASauvegarder, callBacks);
			obj = getObjetToSave(iterator);
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
	private <U> void save(final U l, final boolean hasChanged, Set<Object> objetsASauvegarder, CallBack... callBacks) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		if(this.isNew(l) || hasChanged){
			boolean wasNew = isNew(l);
			String ancienHash = gestionCache.getHash(l);
			boolean wasCharge= gestionCache.estCharge(l);
			gestionCache.setEstEnregistreDansGisement(l);
			this.saveReferences(l, TypeRelation.COMPOSITION, objetsASauvegarder, callBacks);
			try {
				if(this.persistanceManager.save(l, this)){
					for(CallBack c :  callBacks){
						c.objetEnSucces(l, wasNew);
					}
				}
			} catch (MarshallExeption | IOException | RestException e) {
				LOGGER.error("impossible d'enregistrer " + gestionCache.getId(l) + " de type " + l.getClass().getName());
				gestionCache.setNEstPasEnregistreDansGisement(l, wasNew, ancienHash,wasCharge);
				for(CallBack c :  callBacks){
					c.objetEnErreur(l, e);
				}
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
	@SuppressWarnings("unchecked")
	private <U> void saveReferences(final U l, final TypeRelation relation, Set<Object> objetsASauvegarder, CallBack... callBacks) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		Class<U> type = (Class<U>) l.getClass();
		if(l instanceof Collection<?>){
			for(final Object objet : (Collection<?>)l) {
				if(objet!=null)
					this.saveReferences(objet, relation, objetsASauvegarder, callBacks);
			}
		}else if(type.getPackage() != null && type.getPackage().getName().startsWith("java")){
			return;
		} else if (relation == TypeRelation.COMPOSITION) {
			final List<Champ> champs = TypeExtension.getSerializableFields(l.getClass());
			for(final Champ champ : champs){
				final Object toSave = champ.get(l);
				if (!champ.isSimple() && toSave != null)
					this.saveReferences(toSave, champ.getRelation(), objetsASauvegarder, callBacks);
			}
		}else{
			this.save(l, this.hasChanged(l), objetsASauvegarder, callBacks);
		}
	}

}
