package com.actemium.sdk;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.actemium.sdk.exception.GetAllObjectException;
import com.actemium.sdk.exception.GetObjectException;
import com.actemium.sdk.exception.GetObjetEnProfondeurException;
import com.actemium.sdk.exception.GomException;
import com.actemium.sdk.restclient.RestException;
import com.actemium.sdk.runtimeaspect.AspectException;
import com.actemium.sdk.runtimeaspect.LoadAgent;
import com.actemium.sdk.runtimeaspect.Transformer;
import com.rff.wstools.Reponse;
import com.rff.wstools.Requete;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.deserialisation.text.json.JsonUnmarshaller;
import giraudsa.marshall.exception.FabriqueInstantiationException;
import giraudsa.marshall.exception.InstanciationException;
import giraudsa.marshall.exception.MarshallExeption;
import giraudsa.marshall.exception.UnmarshallExeption;
import giraudsa.marshall.serialisation.text.json.JsonMarshaller;
import utils.TypeExtension;
import utils.champ.Champ;


/**
 * Le manager global des objets communiquants avec basic travaux
 */
public class GlobalObjectManager implements EntityManager {


	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalObjectManager.class);


	private static final String IMPOSSIBLE_DE_SAUVEGARDER = "impossible de sauvegarder";


	private static final AtomicReference<GlobalObjectManager> instance = new AtomicReference<>(null);


	private final Set<Class<?>> nonRecuperableViaWebService = new HashSet<>();


	/** l'usine de creation des objets. */
	private final ObjectFactory<?> factory;


	/**la gestion du cache. */
	private final GestionCache gestionCache;
	
	
	/**permet de manipuler des id sans etre dépendant de la bibliothèque métier */
	private final IdHelper<?> idHelper;


	/** attribut determinant la purge automatique du cache en cas d'exception GetAllObjectException 
	 * GetObjectException, GetObjectEnProfondeurException, SaveAllException, SaveException**/
	private final boolean isCachePurgeAutomatiquementSiException;


	/** Le persistance manager. */
	private final PersistanceManagerAbstrait persistanceManager;
	
	
	/** L'objet permettant de modifier le comportement des constructeurs pour inscrire les objets nouveaux dans le cache */
	private final Transformer transformer;


	/**
	 * Instantiates a new global object manager.
	 * @param remplirIdReseau 
	 * @throws FabriqueInstantiationException 
	 */
	private GlobalObjectManager(String httpLogin, String httpPwd, String gisementBaseUrl, boolean isCachePurgeAutomatiquementSiException, int connectTimeout, int socketTimeout, IdHelper<?> idHelper, List<String> annuaires, Collection<Class<?>> aGererDansCache) throws GomException{
		this.idHelper = idHelper;
		try {
			this.factory = new ObjectFactory<>(idHelper);
		} catch (FabriqueInstantiationException e1) {
			LOGGER.error("Impossible de créer la factory d'objet bas niveau.", e1);
			throw new GomException("Impossible de créer la factory d'objet bas niveau.", e1);
		}
		this.persistanceManager = new PersistanceManagerRest(httpLogin,  httpPwd, gisementBaseUrl, connectTimeout, socketTimeout, annuaires);
		this.gestionCache = new GestionCache();
		this.isCachePurgeAutomatiquementSiException = isCachePurgeAutomatiquementSiException;
		try{
			this.transformer = new Transformer(LoadAgent.init());
			for(Class<?> clazz : aGererDansCache){
				transformer.transform(clazz);
			}
		}catch(Exception e){
			LOGGER.error("Impossible de modifier les constructeurs des classes demandées", e);
			throw new GomException("Impossible de modifier les constructeurs des classes demandées", e);
		}
	}


	/**
	 * methode d'initialisation du GlobalObjectManager. 
	 * 
	 * @param gomConfiguration
	 * @throws FabriqueInstantiationException 
	 */
	static GlobalObjectManager init(String httpLogin, String httpPwd, String gisementBaseUrl,
			boolean isCachePurgeAutomatiquement, int connectTimeout, int socketTimeout, IdHelper<?> idHelper,
			List<String> annuaires, Collection<Class<?>> aGererDansCache) throws GomException {
		if(instance.get() != null){
			LOGGER.error("le GOM a déjà été initialisé !");
			throw new GomException("le GOM a déjà été initialisé !");
		}
		instance.set(new GlobalObjectManager(httpLogin, httpPwd, gisementBaseUrl, isCachePurgeAutomatiquement, connectTimeout, socketTimeout, idHelper, annuaires, aGererDansCache));
		return instance.get();
	}


	/**
	 * permet de définir à partir de combien de temps apres le chargement un objet est considéré comme obsolète
	 * @param duree
	 * @param unite
	 */
	void setDureeCache(long duree){
		gestionCache.setDureeCache(duree);
	}


	/**
	 * Charge un objet traitant du hors périmètre (objets qui ne sont pas dans les annuaires)
	 * 
	 * @param horsPerimetre l'objet implémentant l'interface HorsPerimetre traitant les cas n'étant pas dans annuaires.
	 */
	void setHorsPerimetre(HorsPerimetre horsPerimetre){
		persistanceManager.setHorsPerimetre(horsPerimetre);
	}


	/**
	 * Gets the single instance of GlobalObjectManager.
	 *
	 * @return single instance of GlobalObjectManager
	 */
	public static GlobalObjectManager getInstance(){
		return instance.get();
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
	 * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
	 *
	 * @throws SaveAllException 
	 */
	public synchronized void saveAll(CallBack... callbacks) throws GomException {
		try{
			Set<Object> objetsASauvegarder = gestionCache.objetsModifiesDepuisChargementOuNouveau();
			save(objetsASauvegarder, callbacks);
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
	public synchronized <U> void save(U objet, CallBack... callbacks) throws GomException{
		if (objet == null)
			return;
		if (!isNew(objet) && !hasChanged(objet)) 
			return;
		try{
			Set<Object> objetsASauvegarder = new HashSet<>();
			objetsASauvegarder.add(objet);
			save(objetsASauvegarder, callbacks);
		}catch(MarshallExeption | IllegalAccessException | IOException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw new GomException(e);
		}
		catch(RestException e){
			LOGGER.error(IMPOSSIBLE_DE_SAUVEGARDER, e);
			if (purgeCacheAutomatiquementSiException())
				LOGGER.error("erreur dans save(), Cache reinitialisé");
			throw e;
		}
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
				setEstCharge(o);//on dit au cache que c'est chargé...
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
	public <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur) throws GomException{
		try{
			if(id == null || clazz == null) 
				return null;
			U obj = findObjectOrCreate(id, clazz);
			ManagerChargementSDK manager = enProfondeur ? new ManagerChargementEnProfondeur(this, obj) : new ManagerChargementUnique(this, obj);
			manager.execute();
			if(isNew(obj)){
				remove(obj);
				return null;
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
	public void remove(Object obj){
		gestionCache.remove(obj);
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
	public Reponse getReponse(Requete request, boolean enProfondeur) throws GomException  {
		Reponse reponse;
		try {
			reponse = persistanceManager.getReponse(request, this);
			if(enProfondeur){
				ManagerChargementSDK manager = new ManagerChargementEnProfondeur(this, reponse);
				manager.execute();
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
	 * Verifie si un objet est nouveau (c'est à dire s'il a été fabriqué localement).
	 *
	 * @param <U> the generic type
	 * @param obj l'objet
	 * @return true, if is new
	 */
	public <U> boolean isNew(final U obj) {
		return gestionCache.isNew(obj);
	}

	public synchronized void metEnCache(Object objetPere,boolean enProfondeur, boolean isNew) throws IllegalAccessException{
		if(objetPere instanceof Collection<?>){
			final Iterable<?> collection = (Iterable<?>) objetPere;
			for(final Object objet : collection)
				this.metEnCache(objet, enProfondeur, isNew);
		}else if(idHelper.getId(objetPere) != null)
			this.gestionCache.metEnCache(idHelper.getId(objetPere).toString(), objetPere, isNew);
		if(enProfondeur){
			final List<Champ> champs = TypeExtension.getSerializableFields(objetPere.getClass());
			for(final Champ champ : champs){
				final Object objetFils = champ.get(objetPere);
				if(objetFils != null && !champ.isSimple()){
					metEnCache(objetFils, enProfondeur, isNew);
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
	public synchronized <U> U findObjectOrCreate(final String id, final Class<U> clazz) throws InstanciationException{
		U obj = gestionCache.getObject(clazz, id); //on regarde en cache
		if(obj == null){
			obj = this.factory.newObjectWithOnlyId(clazz, id, gestionCache);
		}
		if (obj!=null){
			this.gestionCache.setNotNew(obj);
		}
		return obj;
	}
	
	

	/**sauvegarde l'ensemble des objets du gisement dans un fichier, au format sérialisé json
	 * 
	 * warning 1 cette méthode va purger le cache avant de se lancer; 
	 * warning 2 : dangereux (temps d'exécution + mémoire + espace de stockage) si BDD large !
	 * @throws GomException 
	 */
	public  void dumpGisementToJson(String pathFile) throws GomException{
		try {
			loadGisementInCache();
			dumpCacheToJson(pathFile);
		} catch (SecurityException | IllegalArgumentException | GetAllObjectException e) {
			LOGGER.error("Dump du gisement impossible à réaliser", e);
			throw new GomException("Dump du gisement impossible à réaliser", e);
		}
	}

	/**charge les objets à partir du fichier json, puis les sauvegarde sur le gisement
	 * 
	 *
	 * warning, cette méthode purge le cache avant de se lancer
	 * @throws GomException 
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


	/**charge l'ensemble des objets du gisement dans le gom
	 * 
	 * warning 1 cette méthode va purger le cache avant de se lancer; 
	 * warning 2 : dangereux (temps d'exécution + mémoire ) si BDD large !
	 * 
	 * @throws GetAllObjectException
	 */
	public void loadGisementInCache() throws GetAllObjectException{
		this.purgeCache();
		Set<Class<?>> classes = persistanceManager.getAllClasses();
		this.purgeCache(); //peut etre pas necessaire
		for(Class<?> clazz : classes){
			getAllObject(clazz);
		}
	}

	/**sauvegarde tous les objets du cache dans un fichier json
	 * @throws GomException 
	 * @throws IOException 
	 * 
	 */
	private  void dumpCacheToJson(String pathFile) throws GomException{
		File dump = new File(pathFile);
		try (FileOutputStream file = new FileOutputStream(dump);
				Writer writer = new BufferedWriter(new OutputStreamWriter(file,"UTF-8"))){
			for(Object objetToWrite : gestionCache)
				JsonMarshaller.toJson(objetToWrite, writer);	
		}catch(IOException e){
			LOGGER.error("Erreur lors de l'écriture: ", e);
			throw new GomException("Erreur lors de l'écriture", e);
		}catch(MarshallExeption e){
			LOGGER.error("Erreur lors de la sérialisation: ", e);
			throw new GomException("Erreur lors de la sérialisation", e);
		}
	}

	/**charge les objets à partir du fichier json,
	 * 
	 *
	 * warning, cette méthode purge le cache avant de se lancer
	 * @throws GomException 
	 */
	private void loadJsonFile(String pathFile) throws GomException{
		File dump = new File(pathFile);
		try(FileInputStream file = new FileInputStream(dump);
				Reader reader = new BufferedReader(new InputStreamReader(file,"UTF-8"))){
			this.purgeCache();
			JsonUnmarshaller.fromJson(reader,this); 
			for(Object objetToWrite : gestionCache){
				gestionCache.setNew(objetToWrite);
			}
		}catch(IOException e){
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



	protected String getId(Object objetDontOnVeutLId) {
		return gestionCache.getId(objetDontOnVeutLId);
	}


	protected void interromptChargement(Object objetInterrompu) {
		gestionCache.interromptChargement(objetInterrompu);
	}


	protected <U> boolean chargeObjectFromGisement(Class<U> clazz, String id) throws IOException, RestException, SAXException, InstanciationException {
		U o = persistanceManager.getObjectById(clazz, id, this);
		return o != null;
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
	private void save(Set<Object> objetsASauvegarder, CallBack... callbacks) throws IllegalAccessException, MarshallExeption, IOException, RestException{
		Object obj = getObjetToSave(objetsASauvegarder);
		while(obj != null){
			this.save(obj, this.hasChanged(obj), objetsASauvegarder, callbacks);
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
	private <U> void save(final U l, final boolean hasChanged, Set<Object> objetsASauvegarder, CallBack... callbacks) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		if(this.isNew(l) || hasChanged){
			boolean wasNew = isNew(l);
			String ancienHash = gestionCache.getHash(l);
			boolean wasCharge= gestionCache.estCharge(l);
			gestionCache.setEstEnregistreDansGisement(l);
			objetsASauvegarder.remove(l);
			this.saveReferences(l, TypeRelation.COMPOSITION, objetsASauvegarder, callbacks);
			try {
				this.persistanceManager.save(l);
				for(CallBack cb : callbacks){
					cb.objetEnSucces(l, wasNew);
				}
			} catch (MarshallExeption | RestException e) {
				LOGGER.error("impossible d'enregistrer " + gestionCache.getId(l) + " de type " + l.getClass().getName());
				gestionCache.setNEstPasEnregistreDansGisement(l, wasNew, ancienHash,wasCharge);
				for(CallBack cb : callbacks){
					cb.objetEnErreur(l, e);
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
	private <U> void saveReferences(final U l, final TypeRelation relation, Set<Object> objetsASauvegarder, CallBack... callbacks) throws IllegalAccessException, MarshallExeption, IOException, RestException {
		Class<U> type = (Class<U>) l.getClass();
		if(type.getPackage() != null && type.getPackage().getName().startsWith("System"))
			return;
		else if(l instanceof Collection<?>){
			for(final Object objet : (Collection<?>)l) {
				this.saveReferences(objet, relation, objetsASauvegarder, callbacks);
			}
		}else if(relation == TypeRelation.COMPOSITION){
			final List<Champ> champs = TypeExtension.getSerializableFields(l.getClass());
			for(final Champ champ : champs){
				final Object toSave = champ.get(l);
				if (!champ.isSimple() && toSave != null)
					this.saveReferences(toSave, champ.getRelation(), objetsASauvegarder, callbacks);
			}
		}else{
			this.save(l, this.hasChanged(l), objetsASauvegarder, callbacks);
		}
	}
}
