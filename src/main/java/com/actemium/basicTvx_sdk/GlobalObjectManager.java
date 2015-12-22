package com.actemium.basicTvx_sdk;

import giraudsa.marshall.annotations.TypeRelation;
import giraudsa.marshall.deserialisation.EntityManager;
import giraudsa.marshall.exception.ChampNotFund;
import giraudsa.marshall.exception.NotImplementedSerializeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.ParseException;
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

    /** l'usine de creation des objets. */
    private final ObjectFactory factory;
    
    /**la gestion du cache. */
	private GestionCache gestionCache;
	
	private SetQueue<Object> objetsEnDevenir;
	
	private final Set<Class<?>> nonRecuperableViaWebService = new HashSet<Class<?>>();
	
    /** The persistance manager. */
    private final PersistanceManagerAbstrait persistanceManager;

    private final Map<Object, Boolean> setIdObjHasChangedIndicator = new IdentityHashMap<Object, Boolean>();

    private static GlobalObjectManager instance = new GlobalObjectManager();
    

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
     * Met l'objet en cache.
     *
     * @param <U> the generic type
     * @param obj the obj
     */
    private <U> void putCache(final U obj){
    	gestionCache.metEnCache(obj);
    }



    /**
     * Instantiates a new global object manager.
     */
    private GlobalObjectManager(){
        this.factory = new ObjectFactory();
        this.persistanceManager = new PersistanceManagerRest();
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
	    final U obj = this.factory.newObject(clazz, date);
	    this.putCache(obj);
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
	public <U> U getObjectByTypeAndId(final Class<U> clazz, final String id) throws ParseException, InstantiationException, IllegalAccessException, RestException, IOException, SAXException, IllegalArgumentException, ChampNotFund, ClassNotFoundException{
	    if(id == null || clazz == null) return null;
		U obj = gestionCache.getObjectCharge(clazz, id); //on regarde en cache
	    if(obj == null)
	    	gestionCache.metEnCacheObjectCharge(gestionCache.getObject(clazz, id)); //s'il existe mais non chargé, il est mis dans le cache des objets chargés pour éviter qu'un autre thread le charge également.
	    	obj = persistanceManager.getObjectById(clazz, id, this); //on regarde dans le gisement
	    if (obj == null) {
	        obj = this.factory.newObjectById(clazz, id);//s'il n'existe pas, c'est qu'il faut le créer
	        this.putCache(obj);
	    }
	    gestionCache.metEnCacheObjectCharge(obj);
	    return obj;
	}

	@SuppressWarnings("unchecked")
	public <U> U getObjectEnProfondeurByTypeAndId(final Class<U> clazz, final String id) throws ParseException, InstantiationException, IllegalAccessException, RestException, IOException, SAXException, IllegalArgumentException, ChampNotFund, ClassNotFoundException, InterruptedException{
	    Object obj = getObjectByTypeAndId(clazz, id);
	    getObjetEnProfondeur(obj);
	    return (U) obj;
	}

	private void getObjetEnProfondeur(Object obj) throws InterruptedException {
		Queue<Object> queueAObtenirEnProfondeur = new SetQueue<Object>();
		Queue<Object> enCoursDeTraitement = new SetQueue<Object>();
		queueAObtenirEnProfondeur.add(obj);
	    ExecutorService executor = Executors.newFixedThreadPool(20);
	    for (int i = 0; i < 20; i++) {
	        executor.submit(new NewTask(enCoursDeTraitement, queueAObtenirEnProfondeur));
	    }
	    executor.shutdown();
	    executor.awaitTermination(100, TimeUnit.MILLISECONDS);
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

	public Reponse getReponse(Requete request, boolean enProfondeur) throws InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException, RestException, NotImplementedSerializeException, SAXException{
		Reponse reponse = persistanceManager.getReponse(request, this);
		if(enProfondeur){
			getObjetEnProfondeur(request);
		}
		return reponse;
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

    private void chargeObjectEnProfondeur(Queue<Object> aObtenir, Queue<Object> enCoursDeTraitement) throws ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, ClassNotFoundException, RestException, IOException, SAXException, ChampNotFund, InterruptedException{
    	Object tampon = new Object();
    	while(!aObtenir.isEmpty() && !enCoursDeTraitement.isEmpty()){
    		while(!aObtenir.isEmpty()){
    			//on fait en sorte que les deux listes ne soient jamais null en meme temps...
    			enCoursDeTraitement.add(tampon);
    			Object obj = aObtenir.poll();
    			boolean estDejaEnTraitement = !enCoursDeTraitement.add(obj);
    			enCoursDeTraitement.remove(tampon);
    			//
    			if(!gestionCache.isObjectChargeEnProfondeur(obj)){ //l'objet est peut etre déjà chargé en profondeur
    				gestionCache.metEnCacheObjectChargeEnProfondeur(obj); //s'il existe mais non chargé, il est mis dans le cache des objets chargés en profondeur pour éviter qu'un autre thread le charge également.
    				getObjectByTypeAndId(obj.getClass(), ArianeHelper.getId(obj));
    				ArianeHelper.addSousObject(obj, aObtenir);
    				enCoursDeTraitement.remove(obj);
    			}else if (!estDejaEnTraitement){
    				enCoursDeTraitement.remove(obj);
    			}
    		}
    		wait(100);
    	}
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
     * Méthode qui met en cache l'objet et son id comme clef de la map du cache.
     * @param id
     * @param obj
     * @see giraudsa.marshall.deserialisation.EntityManager#metEnCache(java.lang.String, java.lang.Object)
     */
    @Override public void metEnCache(final String id, final Object obj) {
    	if(objetsEnDevenir != null) objetsEnDevenir.add(obj);
        this.gestionCache.metEnCache(obj);
    }
    
    class NewTask implements Runnable {
    	
    	private Queue<Object> queueAObtenir; 
    	private Queue<Object> enCoursDeTraitement;
        
		public NewTask(Queue<Object> queueAObtenir, Queue<Object> enCoursDeTraitement) {
			super();
			this.queueAObtenir = queueAObtenir;
			this.enCoursDeTraitement = enCoursDeTraitement;
		}

		@Override
        public void run() {
            try {
				chargeObjectEnProfondeur(queueAObtenir, enCoursDeTraitement);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ChampNotFund e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }   
    }
    
    public class SetQueue<E> implements Queue<E>{

        private final Set<E> queue = new LinkedHashSet<E>();
        @Override public synchronized int size() {return this.queue.size();}
        @Override public synchronized boolean isEmpty() {return this.queue.isEmpty();}
        @Override public synchronized boolean contains(final Object o) {return this.queue.contains(o);}
        @Override public synchronized Iterator<E> iterator() {return this.queue.iterator();}
        @Override public synchronized Object[] toArray() {return this.queue.toArray();}
        @Override public synchronized <T> T[] toArray(final T[] a) {return this.queue.toArray(a);}
        @Override public synchronized boolean remove(final Object o) {return this.queue.remove(o);}
        @Override public synchronized boolean containsAll(final Collection<?> c) {return this.queue.containsAll(c);}
        @Override public synchronized boolean addAll(final Collection<? extends E> c) {return this.queue.addAll(c);}
        @Override public synchronized boolean removeAll(final Collection<?> c) {return this.queue.removeAll(c);}
        @Override public synchronized boolean retainAll(final Collection<?> c) {return this.queue.retainAll(c);}
        @Override public synchronized void clear() {this.queue.clear();}
        @Override public synchronized boolean add(final E e) {return this.queue.add(e);}
        @Override public synchronized boolean offer(final E e) {return this.queue.add(e);}
        @Override public synchronized E remove() {
            if(this.queue.isEmpty()) {
                throw new NoSuchElementException();
            }
            final E e = this.queue.iterator().next();
            this.queue.remove(e);
            return e;
        }
        @Override public synchronized E poll() {
            if(this.queue.isEmpty()) {
                return null;
            }
            final E e = this.queue.iterator().next();
            this.queue.remove(e);
            return e;
        }
        @Override public synchronized E element() {
            if(this.queue.isEmpty()) {
                return null;
            }
            final E e = this.queue.iterator().next();
            return e;
        }
        @Override public synchronized E peek() {
        	if(this.queue.isEmpty()) {
        		throw new NoSuchElementException();
        	}
        	final E e = this.queue.iterator().next();
        	return e;
        }
    }


}
