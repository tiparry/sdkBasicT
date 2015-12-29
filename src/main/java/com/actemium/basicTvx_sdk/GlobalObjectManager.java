package com.actemium.basicTvx_sdk;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.ChampNotFund;
import giraudsa.marshall.exception.NotImplementedSerializeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import utils.TypeExtension;
import utils.champ.Champ;

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
    
    private ExecutorService executor = Executors.newFixedThreadPool(20);
    private Set<Object> objetEnChargement = Collections.synchronizedSet(new HashSet<>());
    boolean estDisponible() {
		return objetEnChargement.isEmpty();
	}

    private final Map<Object, Boolean> setIdObjHasChangedIndicator = new IdentityHashMap<Object, Boolean>();

    private static GlobalObjectManager instance = null;
    

    /**
     * Checks for changed.
     *
     * @param objet the objet
     * @return the boolean
     */
    private Boolean hasChanged(final Object objet){
        return this.setIdObjHasChangedIndicator.containsKey(objet);
    }

    /**
     * Instantiates a new global object manager.
     */
    private GlobalObjectManager(String httpLogin, String httpPwd, String gisementBaseUrl){
        this.factory = new ObjectFactory();
        this.persistanceManager = new PersistanceManagerRest(httpLogin,  httpPwd, gisementBaseUrl);
        this.gestionCache = new GestionCache();
    }
    
   



    /**
	 * Sets the checks for changed.
	 *
	 * @param objet the new checks for changed
	 */
	public void setHasChanged(final Object objet){
	    this.setIdObjHasChangedIndicator.put(objet, true);
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

    /**
     * Sauvegarde les objets passés en paramètre.
     *
     * @param <U> the generic type
     */
    public <U> void saveAll() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, RestException, NotImplementedSerializeException {
        U obj = this.getObjetToSave();
        while(obj != null){
            this.save(obj);
            obj = this.getObjetToSave();
        }
    }

    /**
	 * Creates the object.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param date the date
	 * @return the u
	 */
	public <U> U createObject(final Class<U> clazz, final Date date) throws InstantiationException, IllegalAccessException{
	    final U obj = this.factory.newObject(clazz, date, gestionCache);
	    return obj;
	}

	/**
	 * Gets the all object by type.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @return the all object by type
	 */
	public <U> List<U> getAllObject(final Class<U> clazz) throws ParseException, IllegalArgumentException, IllegalAccessException, RestException, IOException, SAXException, ChampNotFund, ClassNotFoundException {
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
	    return listeObj;
	}

	/**
	 * Recupere un objet en fonction de son type et de son id. Le crée s'il n'existe pas.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param id the id
	 * @return the object by type and id
	 */
	public <U> U getObject(final Class<U> clazz, final String id) throws ParseException, InstantiationException, IllegalAccessException, RestException, IOException, SAXException, IllegalArgumentException, ChampNotFund, ClassNotFoundException{
	    if(id == null || clazz == null) return null;
		U obj = gestionCache.getObject(clazz, id); //on regarde en cache
	    if(obj == null){
	    	obj = this.factory.newObjectById(clazz, id, gestionCache);
	    }
	    nourritObjet(obj);
	    return obj;
	}
	
	private <U> void nourritObjet(U obj) throws ParseException, ClassNotFoundException, RestException, IOException, SAXException{
		if(!gestionCache.estCharge(obj) && !gestionCache.enChargement(obj) && gestionCache.setPrisEnChargePourChargement(obj)){
			String id = gestionCache.getId(obj);
			Class<?> clazz = obj.getClass();
			persistanceManager.getObjectById(clazz, id, this);
			gestionCache.setEstCharge(obj);
		}
	}

	
	/**
	 * Recupere un objet de type U et tout ses fils jusqu'aux feuilles
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param id the id
	 * @return the object by type and id
	 */
	@SuppressWarnings("unchecked")
	public <U> U getObjectEnProfondeur(final Class<U> clazz, final String id) throws ParseException, InstantiationException, IllegalAccessException, RestException, IOException, SAXException, IllegalArgumentException, ChampNotFund, ClassNotFoundException, InterruptedException{
	    Object obj = getObject(clazz, id);
	    getObjetEnProfondeur(obj);
	    return (U) obj;
	}

	private void getObjetEnProfondeur(Object obj) throws InterruptedException {
		prendEnChargePourChargementEnProfondeur(obj);
	    while(!estDisponible()){
	    	Thread.sleep(100);
	    }
	}

	/**
	 * Removes the.
	 *
	 * @param <U> the generic type
	 * @param l the l
	 */
	public <U> void remove(final U l) {
	    this.gestionCache.remove(l);
	    this.factory.noMoreNew(l);
	    this.setIdObjHasChangedIndicator.remove(l);
	}

	
	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 *
	 * @param Requete
	 * @param enProfondeur true si l'on veut récuperer toute l'arborescence de la réponse
	 */
	public Reponse getReponse(Requete request, boolean enProfondeur) throws InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException, RestException, NotImplementedSerializeException, SAXException{
		Reponse reponse = persistanceManager.getReponse(request, this);
		if(enProfondeur){
			getObjetEnProfondeur(request);
		}
		return reponse;
	}

	/**
     * Gets the objet to save.
     *
     * @param <U> the generic type
     * @return the objet to save
     */
    private <U> U getObjetToSave() {
        U ret = factory.getObjetInNewObject();
        if (ret == null) {
            ret = this.getObectHasChanged();
        }
        return ret;
    }

    /**
     * Gets the obect has changed.
     *
     * @param <U> the generic type
     * @return the obect has changed
     */
    @SuppressWarnings("unchecked")
	private <U> U getObectHasChanged() {
        if (this.setIdObjHasChangedIndicator.isEmpty()) {
            return null;
        }
        return (U) setIdObjHasChangedIndicator.entrySet().iterator().next().getKey();       
    }

    /**
     * Save.
     *
     * @param <U> the generic type
     * @param value the value
     * @return the int
     */
    private <U> int save(final U value) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, RestException, NotImplementedSerializeException {
        return this.save(value, this.hasChanged(value));
    }

    /**
     * Save.
     *
     * @param <U> the generic type
     * @param l the l
     * @param hasChanged the has changed
     * @return the int
     */
    private <U> int save(final U l, final boolean hasChanged) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, RestException, NotImplementedSerializeException {
        if(this.isNew(l) || hasChanged){
            this.factory.noMoreNew(ArianeHelper.getId(l));
            this.setIdObjHasChangedIndicator.remove(l);
            final int s = this.saveReferences(l, TypeRelation.COMPOSITION);
            this.persistanceManager.save(l);
            return s+1;
        }
        return 0;
    }

    /**
     * Sauvegarde les objets en tenant compte des relations de composition...
     *
     * @param <U> the generic type
     * @param l the l
     * @param relation the relation
     * @return the int
     */
    private <U> int saveReferences(final U l, final TypeRelation relation) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, RestException, NotImplementedSerializeException {
        int i = 0;
        if(relation == TypeRelation.COMPOSITION){
            final List<Champ> champs = TypeExtension.getSerializableFields(l.getClass());
            for(final Champ champ : champs){
                if (!champ.isSimple){
                    final Class<?>  type = champ.info.getType();
                    if(Collection.class.isAssignableFrom(type)){
                        final Iterable<?> collection = (Iterable<?>) champ.get(l);
                        if(collection != null){
                            for(final Object objet : collection) {
                                i += this.saveReferences(objet, champ.relation);
                            }
                        }
                    }else if (type.getPackage() == null || ! type.getPackage().getName().startsWith("System")) {//object
                        final Object toSave = champ.get(l);
                        if(toSave != null) {
                            i += this.saveReferences(champ.get(l), champ.relation);
                        }
                    }

                }
            }
        }else{
            i = this.save(l, this.hasChanged(l));
        }
        return i;
    }

    private void chargeObjectEnProfondeur(Object objetATraiter) throws ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, ClassNotFoundException, RestException, IOException, SAXException, ChampNotFund, InterruptedException{
    	nourritObjet(objetATraiter);
    	ArianeHelper.addSousObject(objetATraiter, this);
    	gestionCache.setChargeEnProfondeur(objetATraiter);
    }
    
    
    
    /**
     * Checks if is new.
     *
     * @param <U> the generic type
     * @param obj the obj
     * @return true, if is new
     */
    private <U> boolean isNew(final U obj) {
        return this.factory.isNew(obj);
    }


    /**
	 * Méthode pour récupérer un objet depuis le cache du global object manager.
	 * @param id
	 * @param clazz
	 * @return
	 * @see giraudsa.marshall.deserialisation.EntityManager#findObject(java.lang.String, java.lang.Class)
	 */
	@Override public <U> U findObject(final String id, final Class<U> clazz) {
	    return this.gestionCache.getObject(clazz, id);
	}

	/**
     * Méthode qui met en cache l'objet et son id comme clef de la map du cache.
     * @param id
     * @param obj
     * @see giraudsa.marshall.deserialisation.EntityManager#metEnCache(java.lang.String, java.lang.Object)
     */
    @Override public void metEnCache(final String id, final Object obj) {
   		gestionCache.metEnCache(id, obj);
    }
    
    void addAChargerEnProfondeur(Object o) {
		gestionCache.addAChargerEnProfondeur(o, this);
	}
    
    void prendEnChargePourChargementEnProfondeur(Object o) {
    	executor.submit(new TacheChargementProfondeur(o));//multithread
    	//new TacheChargementProfondeur(o).run();//monothread
	}
    
    class TacheChargementProfondeur implements Runnable {
    	
    	private Object objetATraiter;
    	
		public TacheChargementProfondeur(Object objetATraiter) {
			super();
			objetEnChargement.add(objetATraiter);
			this.objetATraiter = objetATraiter;
		}

		@Override
        public void run() {
            try {
				chargeObjectEnProfondeur(objetATraiter);
				objetEnChargement.remove(objetATraiter);
			} catch (ParseException | InstantiationException | IllegalAccessException | IllegalArgumentException | ClassNotFoundException | RestException | IOException | SAXException | ChampNotFund | InterruptedException e) {
				LOGGER.error("",e);
			}
        }   
    }
}
