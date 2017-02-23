1- Librairies à utiliser - Dependance maven
-------------------------------------------------------------------------------------------------

L'usage du SDK client BasicTravaux nécessite l'ajout des dépendances suivantes dans votre projet (versions des dépendances à mettre à jour si besoin):

<!-- SDK basic Travaux-->
	<dependency>
			<groupId>com.actemium</groupId>
	  		<artifactId>basicTvx_sdk</artifactId>
	  		<version>1.1.1</version>
	</dependency>

<!-- Apache HTTP Client-->
	<dependency>
		<groupId>org.apache.httpcomponents</groupId>
		<artifactId>httpclient</artifactId>
		<version>4.4.1</version>
	</dependency>		
    
	
<!-- lib de serialisation-->
	<dependency>
  		<groupId>com.actemium</groupId>
  		<artifactId>Marshalling</artifactId>
  		<version>1.0.7</version>
	</dependency>
	
<!-- logs-->
	<dependency>
	    <groupId>org.slf4j</groupId>
	    <artifactId>slf4j-api</artifactId>
           <version>1.7.5</version>
        </dependency>

	<dependency>
	   <groupId>org.slf4j</groupId>
	   <artifactId>slf4j-log4j12</artifactId>
	   <version>1.7.5</version>
	</dependency>

2- Usage de la librairie
-----------------------------------------------------------------------------------------------

	2.1- Initialisation du GOM
	---------------------------

Tous les objets doivent passer par l'instance singleton du GlobalObjectManager (GOM).

Le GOM doit être instancié avant le premier usage de la manière suivante :

	String baseUrl = "http://ip:partie/commune/de/l/url/";
						
	final GlobalObjectManager gom = new GOMConfiguration("login", "pwd", baseUrl)
						.addAnnuaire("annuaireTraitement") // fin de l'url pour l'annuaire des WS
						.addClasseAGererDansGom(ObjetPersistant.class) //classe avec l'attribut "id" à gérer par le GOM 
						.setCachePurgeAutomatiquement(false) //purge le cache en cas d'erreur (défaut : true)
						.setConnectTimeout(900) // défaut : 1000 ms
						.setSocketTimeout(5000) //défaut 6000 ms
						.setHorsPerimetre(new NourritIdReseau(args)) //comportement hors annuaire
						.setDureeCache(12, TimeUnit.HOURS) //par défaut 1 heure 
						.setIdHelper(new MyIdHelper())//par défaut : DefaultUUIDHelper extends 
						.init();
		    		
Le paramètre setCachePurgeAutomatiquement(true) instancie le gom de telle manière qu'il se purge automatiquement en cas de certaines exceptions, pour se prémunir d'un état potentiellement incohérent, susceptible de générer des erreurs par la suite.

Après cette phase d'initialisation, le GOM est disponible sur le simple appel suivant :

	GlobalObjectManager gom = GlobalObjectManager.getInstance();
	
	
	2.2 - Gestion du cache
	----------------------
	
La durée de cache par défaut des objets dans le GOM est de 1h. Il est possible de modifier cette valeur en appelant :

	gOMConfiguration.setDureeCache(12, TimeUnit.HOURS)
	
La valeur du cache minimum est de 1 minute. 
	
Afin d'éviter au GOM d'occuper toujours plus de mémoire au fil du temps si la JVM n'est jamais redémarrée, il est nécessaire d'invoquer la méthode suivante en fin de traitement pour nettoyer le cache. 
	
	/**
	 * Purge le Cache du GOM pour éviter les fuites mémoires lorsqu'on a fini un traitement.
	 *
	 */
	public synchronized void purgeCache()
	
il est aussi possible de supprimer un objet du cache unitairement si par exemple il est créé en local et qu'on veut finalement ne pas le garder

	 /**
	 * supprime un objet du cache
	 * @param obj
	 */
	public void remove(Object obj)

 
	
	2.2 - Autres méthodes publiques du GOM
	--------------------------------------

Voila la liste des autres méthodes publiques disponible dans le GOM : 

!!!!!!!!!
ATTENTION : lorsque les méthodes saveAll, save, getAllObject, getObject, getReponse génèrent leurs exceptions, le cache n'est pas par défaut purgé. Il est potentiellement dans un état incohérent. 
			Le choix de la purge automatique est fortement conseillé, accessible en apellant la methode init() avec le paramètre boolean true.
!!!!!!!!!

	   
	 /**
     * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
     *
     * @param <U> the generic type
     */
    public synchronized <U> void saveAll()
    
     
     /**
     * Sauvegarde de l'objet avec sa grappe d'objet
     * @param objet
     */
    public synchronized <U> void save(U objet) 
	   
	    
	   
	/**
	 * Retourne tous les objets présent dans le gisement pour le type U .
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @return the all object by type
	 */
	public  <U> List<U> getAllObject(final Class<U> clazz)
	
	

	/**
	 * Recupere un objet en fonction de son type et de son id. Le crée s'il n'existe pas.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param id the id
	 * @param enProfondeur boolean permettant de provoquer une recuperation de la grappe d'objet en profondeur
	 * @return the object by type and id
	 * @throws InterruptedException 
	 */
	public  <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur)

	 
	
	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 *
	 * @param Requete
	 * @param enProfondeur true si l'on veut récuperer toute la grappe de la réponse
	 */
	public  Reponse getReponse(Requete request, boolean enProfondeur)

 
	 
	 
	 
	 /**
     * Verifie si un objet est nouveau (c'est à dire s'il a été fabriqué localement).
     *
     * @param <U> the generic type
     * @param obj the obj
     * @return true, if is new
     */
     public <U> boolean isNew(final U obj)
     
     /**
     * verifie si un objet a été modifié depuis son chargement du gisement
     *
     * @param objet the objet
     * @return the boolean
     */
     public boolean hasChanged(final Object objet){
	
	

	
3- Configuration des logs
-------------------------------------------------------------------------------------------------
	
Il est possible d'obtenir des logs internes à l'usage de la librairies (basé sur log4j).
Ajouter pour cela les categories suivantes à votre fichier log4j.xml
	
	<!-- Les logs du SDK BT -->
	 <category name="com.actemium">
    	<priority value="debug"/>
   	</category>
   	
     <!-- Les logs du client apache -->
     <category name="org.apache">
    	<priority value="debug"/>
   	</category>
   	
    <!-- Les logs du Serialiseur/Deserialiseur -->
    <category name="giraudsa">
    	<priority value="debug"/>
	</category>
	
	
	
	
	
	