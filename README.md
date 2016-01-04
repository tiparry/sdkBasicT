1- Librairies à utiliser - Dependance maven
-------------------------------------------------------------------------------------------------

L'usage du SDK client BasicTravaux nécessite l'ajout des dépendances suivantes dans votre projet:

	<!-- SDK basic Travaux-->
	<dependency>
			<groupId>com.actemium</groupId>
	  		<artifactId>basicTvx_sdk</artifactId>
	  		<version>0.0.1-SNAPSHOT</version>
	</dependency>

	<!-- Apache HTTP Client-->
	<dependency>
		<groupId>org.apache.httpcomponents</groupId>
		<artifactId>httpclient</artifactId>
		<version>4.4</version>
	</dependency>		
    
    <!-- Model basic travaux -->
	<dependency>
	  <groupId>com.rff</groupId>
	  <artifactId>BasicTravaux</artifactId>
	  <version>1.0-SNAPSHOT</version>
	</dependency>
	
	<!-- lib de serialisation-->
	<dependency>
  		<groupId>com.actemium</groupId>
  		<artifactId>Marshalling</artifactId>
  		<version>1.0.0-SNAPSHOT</version>
	</dependency>


Il faudra au préalable ajouter les repository maven suivants dans votre projet : 
	<repository>
        	<id>actemium_nexus_release</id>
            <name> Nexus Release Repository</name>
            <url>http://46.105.48.117:8081/nexus/content/repositories/releases/</url>
            <releases>
		    	<enabled>true</enabled>
		 	</releases>
		 	<snapshots>
		    	<enabled>false</enabled>
		 	</snapshots>
        </repository>  
        
        <repository>
            <id>actemium_nexus_snapshot</id>
            <name> Nexus Snapshot Repository</name>
            <url>http://46.105.48.117:8081/nexus/content/repositories/snapshots/</url>
            <releases>
		    	<enabled>false</enabled>
		 	</releases>
		 	<snapshots>
		    	<enabled>true</enabled>
		    	<updatePolicy>always</updatePolicy>
		    	<checksumPolicy>warn</checksumPolicy>
		 	</snapshots>
       </repository>




2- Usage de la librairie
-----------------------------------------------------------------------------------------------

	2.1- Initialisation du GOM
	---------------------------

Tous les objets doivent passer par l'instance singleton du GlobalObjectManager (GOM).

Le GOM doit être instancié avant le premier usage de la manière suivante :

	String login = "APP_CLIENT";
	String pwd = "APP_PASSWORD";
	String baseUrl = "http://ip:port/BasicTravaux/Maintenance/GisementDeDonneeMaintenance/v1/";
	GlobalObjectManager.init(login, pwd, baseUrl);

Après cette phase d'initialisation, le GOM est disponible sur le simple appel suivant :

	GlobalObjectManager gom = GlobalObjectManager.getInstance();
	
On peut demander au GOM de nourrir automatiquement les id Reseau lorsqu'on fait un appel en profondeur d'un objet en faisant des requetes au gisement GAIA. Il faut configurer le serveur de gisement Gaia.
	
	gom.nourrirIdReseau("https://int-ws-gaia.rff.ferre", "BASIC2T_2014", "!h=e3MpWmp");
	
	2.2 - Gestion du cache
	----------------------
	
	La durée de cache par défaut des objets dans le GOM est de 1h. Il est possible de modifier cette valeur en appelant :
	gom.setDureeCache(15, TimeUnit.HOURS); // durée du cache à 15h
	La valeur du cache minimum est de 1 minute. 
	
	Afin d'éviter au GOM d'occuper toujours plus de mémoire au fil du temps si la JVM n'est jamais redémarrée, il est nécessaire d'invoquer la méthode suivante
	en fin de traitement pour nettoyer le cache. 
	
	/**
	 * Purge le Cache du GOM pour éviter les fuites mémoires lorsqu'on a fini un traitement.
	 *
	 */
	public void purgeCache()


	
	2.2 - Autres méthodes publiques du GOM
	--------------------------------------

Voila la liste des autres méthodes publiques disponible dans le GOM : 



 	/**
     * Sauvegarde ou update dans le gisement les objets nouveaux ou modifies.
     *
     * @param <U> the generic type
     */
    public <U> void saveAll()
    
     

    /**
	 * Creates the object.
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param date the date
	 * @return the u
	 */
	 public <U> U createObject(final Class<U> clazz, final Date date) 
	   
	   
	   
	/**
	 * Retourne tous les objets présent dans le gisement pour le type U .
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @return the all object by type
	 */
	public <U> List<U> getAllObject(final Class<U> clazz)
	
	

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
	public <U> U getObject(final Class<U> clazz, final String id, boolean enProfondeur)


	/**
	 * supprime un objet du cache
	 * @param obj
	 */
	 public void remove(Object obj)
	 
	 
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
	
	
	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 *
	 * @param Requete
	 * @param enProfondeur true si l'on veut récuperer toute la grappe de la réponse
	 */
	public Reponse getReponse(Requete request, boolean enProfondeur)


	
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
	
	
	