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
    
   <!--classes BasicTravaux nécessaires pour le sdk (Requete, Reponse, RessourceAbstraite) -->
	<dependency>
	  <groupId>com.actemium</groupId>
	  <artifactId>BTForSDK</artifactId>
	  <version>1.0.0</version>
	</dependency>
	
	<!-- lib de serialisation-->
	<dependency>
  		<groupId>com.actemium</groupId>
  		<artifactId>Marshalling</artifactId>
  		<version>1.0.9.d</version>
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

On passe par une classe de configuration pour les parametres du GOM, qui utilise certaines valeurs par defaut si pas d'instruction contraire.
	String login = "LOGIN_BT";
	String pwd = "PASSWORD_BT";
	String baseUrl = "http://ip:port/BasicTravaux/Maintenance/GisementDeDonneeMaintenance/v1/";
	GOMConfiguration gomConfiguration= new GOMConfiguration(login, pwd, baseUrl);
	GlobalObjectManager.init(gomConfiguration);
	
	
Des instanciations avec un autre paramétrage sont possibles  : 

String login = "LOGIN_BT";
	String pwd = "PASSWORD_BT";
	String baseUrl = "http://ip:port/BasicTravaux/Maintenance/GisementDeDonneeMaintenance/v1/";
	GOMConfiguration gomConfiguration= new GOMConfiguration(login, pwd, baseUrl);
	gomConfiguration.setCachePurgeAutomatiquement(true);
	gomConfiguration.setConnectTimeout(15000) //15000 ms
	gomConfiguration.setSocketTimeout(-1) // la valeur -1 correspond a un timeout infini
	GlobalObjectManager.init(gomConfiguration);
	
Nous conseillons  d'instancier le gom de telle manière qu'il se purge automatiquement en cas de certaines exceptions, pour se prémunir d'un état potentiellement incohérent, susceptible de générer des erreurs par la suite.

Après cette phase d'initialisation, le GOM est disponible sur le simple appel suivant :

	GlobalObjectManager gom = GlobalObjectManager.getInstance();
	
On peut demander au GOM de nourrir automatiquement les id Reseau lorsqu'on fait un appel en profondeur d'un objet en faisant des requetes au gisement GAIA. Il faut configurer le serveur de gisement Gaia.
	
	gom.nourrirIdReseau("https://int-ws-gaia.rff.ferre", "loginGaia", "mdpGaia");
	
	2.2 - Gestion du cache
	----------------------
	
La durée de cache par défaut des objets dans le GOM est de 1h. Il est possible de modifier cette valeur en appelant :

	gom.setDureeCache(15, TimeUnit.HOURS); // durée du cache à 15h
	
La valeur du cache minimum est de 1 minute. 
	
Afin d'éviter au GOM d'occuper toujours plus de mémoire au fil du temps si la JVM n'est jamais redémarrée, il est nécessaire d'invoquer la méthode suivante en fin de traitement ou perodiquement pour nettoyer le cache. 
	
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
ATTENTION : lorsque les méthodes saveAll, save, getAllObject, getObject, getReponse génèrent des exceptions, le cache n'est pas par défaut purgé. Il est potentiellement dans un état incohérent. 
			Le choix de la purge automatique est fortement conseillé, à faire via l'objet GOMConfiguration.
!!!!!!!!!

    /**
	 * Creates the object.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param date the date
	 * @return the u
	 */
	 public synchronized <U> U createObject(final Class<U> clazz, final Date date) 
	   
	   
	     /**
     * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
     *
     * @param <U> the generic type
     */
    public synchronized <U> void saveAll()
    
     
     /**
     * Sauvegarde de l'objet et de sa grappe d'objets nécessaires
     * @param objet
     */
    public synchronized <U> void save(U objet, CallBack... callBacks) 


	/**
	*Sauvegarde de l'objet et de sa grappe d'objets dans leur intégralité
	*/
	public synchronized <U> void saveEnProfondeur(U objet, CallBack... callBacks)
	   
	    
	   
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
	
	
	
