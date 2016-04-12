package com.actemium.basicTvx_sdk;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.ChampNotFund;
import giraudsa.marshall.exception.MarshallExeption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	
	private final Set<Class<?>> nonRecuperableViaWebService = new HashSet<Class<?>>();
	
    /** The persistance manager. */
    private final PersistanceManagerAbstrait persistanceManager;
    
    private ExecutorService executor = Executors.newFixedThreadPool(10);

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
    public <U> void saveAll() throws SaveAllException {
    	try{
		    Set<Object> objetsASauvegarder = gestionCache.objetsModifiesDepuisChargementOuNouveau();
		    save(objetsASauvegarder);
    	}catch(MarshallExeption | IllegalAccessException | IOException | RestException e){
    		LOGGER.error("impossible de sauvegarder", e);
    		throw new SaveAllException("impossible de sauvegarder", e);
    	}
    }
    /**
     * Sauvegarde de l'objet avec sa grappe d'objet
     * @param objet
     * @throws SaveException
     */
    public <U> void save(U objet) throws SaveException{
    	if (objet == null) return;
    	if (!isNew(objet) && !hasChanged(objet)) return;
    	try{
    		Set<Object> objetsASauvegarder = new HashSet<>();
    		objetsASauvegarder.add(objet);
    		save(objetsASauvegarder);
    	}catch(MarshallExeption | IllegalAccessException | IOException | RestException e){
    		LOGGER.error("impossible de sauvegarder", e);
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
	    final U obj = this.factory.newObject(clazz, date, gestionCache);
	    return obj;
	}

	/**
	 * Gets the all object by type.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @return the all object by type
	 * @throws GetAllObjectException 
	 */
	public <U> List<U> getAllObject(final Class<U> clazz) throws GetAllObjectException{
		try{
		    if(gestionCache.estDejaCharge(clazz)) {
		        return gestionCache.getClasse(clazz);
		    }
		    final List<U> listeObj = new ArrayList<U>();
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
	public <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur) throws GetObjectException, GetObjetEnProfondeurException{
	    try{
			if(id == null || clazz == null) return null;
			U obj = gestionCache.getObject(clazz, id); //on regarde en cache
		    if(obj == null){
		    	obj = this.factory.newObjectById(clazz, id, gestionCache);
		    }
		    nourritObjet(obj);
		    if(enProfondeur) getObjetEnProfondeur(obj);
		    return obj;
	    }catch(ParseException | RestException | IOException | SAXException | InterruptedException | ParserConfigurationException | ReflectiveOperationException e){
	    	LOGGER.error("impossible de récupérer l'objet", e);
    		throw new GetObjectException(id, clazz, e);
	    }
	}
	
	private <U> void nourritObjet(U obj) throws ParseException, RestException, IOException, SAXException, InterruptedException, ParserConfigurationException, ReflectiveOperationException{
		if(!gestionCache.estCharge(obj) && !gestionCache.enChargement(obj) && gestionCache.setPrisEnChargePourChargement(obj)){
			String id = gestionCache.getId(obj);
			Class<?> clazz = obj.getClass();
			persistanceManager.getObjectById(clazz, id, this);
			gestionCache.setEstCharge(obj);
		}else if(gestionCache.enChargement(obj)){
			while(gestionCache.enChargement(obj)){
				Thread.sleep(100);
			}
		}
	}


	private void getObjetEnProfondeur(Object obj) throws GetObjetEnProfondeurException {
		CacheChargementEnProfondeur cacheChargementEnProfondeur = new CacheChargementEnProfondeur();
		prendEnChargePourChargementEnProfondeur(obj, cacheChargementEnProfondeur);
	    while(!cacheChargementEnProfondeur.estFini()){
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new GetObjetEnProfondeurException(obj, e);
			}
	    }
	    cacheChargementEnProfondeur.toutSestBienPasse(obj);
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
	 * @throws MarshallExeption 
	 * @throws InterruptedException 
	 * @throws RestException 
	 * @throws IOException 
	 */
	public Reponse getReponse(Requete request, boolean enProfondeur) throws GetObjetEnProfondeurException, GetObjectException  {
		Reponse reponse;
		try {
			reponse = persistanceManager.getReponse(request, this);
		} catch (MarshallExeption | IOException | RestException e) {
			throw new GetObjectException("objet sans id", request.getClass(), e);
		}
		if(enProfondeur){
			getObjetEnProfondeur(reponse);
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
    private void save(Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, ClientProtocolException, IOException, RestException{
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
    private <U> void save(final U l, final boolean hasChanged, Set<Object> objetsASauvegarder) throws MarshallExeption, IllegalAccessException, ClientProtocolException, IOException, RestException {
        if(this.isNew(l) || hasChanged){
            gestionCache.setEstEnregistreDansGisement(l);
            objetsASauvegarder.remove(l);
            this.saveReferences(l, TypeRelation.COMPOSITION, objetsASauvegarder);
            this.persistanceManager.save(l);
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
    private <U> void saveReferences(final U l, final TypeRelation relation, Set<Object> objetsASauvegarder) throws IllegalAccessException, MarshallExeption, ClientProtocolException, IOException, RestException {
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

    private void chargeObjectEnProfondeur(Object objetATraiter, CacheChargementEnProfondeur cacheChargementEnProfondeur) throws ParseException, IllegalArgumentException, RestException, IOException, SAXException, ChampNotFund, InterruptedException, ParserConfigurationException, ReflectiveOperationException{
    	nourritObjet(objetATraiter);
    	ArianeHelper.addSousObject(objetATraiter, this, cacheChargementEnProfondeur);
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
    
    void prendEnChargePourChargementEnProfondeur(Object o, CacheChargementEnProfondeur cacheChargementEnProfondeur) {
    	if(createNewTacheChargementProfondeur(o, cacheChargementEnProfondeur)){
    		executor.submit(new TacheChargementProfondeur(o, cacheChargementEnProfondeur));//multithread
    	}
    	//new TacheChargementProfondeur(o).run();//monothread
	}
    
    private synchronized boolean createNewTacheChargementProfondeur(Object o, CacheChargementEnProfondeur cacheChargementEnProfondeur){
    	if (!cacheChargementEnProfondeur.dejaVu(o))
    		cacheChargementEnProfondeur.add(o);
    	return !cacheChargementEnProfondeur.dejaVu(o);
    }
    
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	executor.shutdown();
    }
    
    class TacheChargementProfondeur implements Runnable {
    	
    	private final Object objetATraiter;
    	private final CacheChargementEnProfondeur cacheChargementEnProfondeur;
    	
		public TacheChargementProfondeur(Object objetATraiter, CacheChargementEnProfondeur cacheChargementEnProfondeur) {
			super();
			this.objetATraiter = objetATraiter;
			this.cacheChargementEnProfondeur = cacheChargementEnProfondeur;
		}

		@Override
        public void run() {
            try {
				chargeObjectEnProfondeur(objetATraiter, cacheChargementEnProfondeur);
				cacheChargementEnProfondeur.estTraite(objetATraiter);
			} catch (ParseException | IllegalArgumentException | RestException | IOException | SAXException | ChampNotFund | InterruptedException | ParserConfigurationException | ReflectiveOperationException e) {
				if(cacheChargementEnProfondeur.nombreEssais(objetATraiter) < 2){
					prendEnChargePourChargementEnProfondeur(objetATraiter, cacheChargementEnProfondeur);
				}else{
					cacheChargementEnProfondeur.estTraite(objetATraiter);
					gestionCache.setEstCharge(objetATraiter);//pour arreter d'essayer de le recharger
					cacheChargementEnProfondeur.ajouteException(new GetObjectException(gestionCache.getId(objetATraiter), objetATraiter.getClass(), e));
				}
				LOGGER.error("",e);
			}
        }   
    }
}
