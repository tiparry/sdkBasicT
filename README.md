1- Dependance maven
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

Tous les objets doivent passer par l'instance singleton du GlobalObjectManager (GOM).

Le GOM doit être instancié avant le premier usage de la manière suivante :

	String login = "APP_CLIENT";
	String pwd = "APP_PASSWORD";
	String baseUrl = "http://ip:port/BasicTravaux/Maintenance/GisementDeDonneeMaintenance/v1/";
	GlobalObjectManager.init(login, pwd, baseUrl);

Après cette phase d'initialisation, le GOM est disponible sur le simple appel suivant :

	GlobalObjectManager gom = GlobalObjectManager.getInstance();



Les méthodes publiques sont : 

- public void setHasChanged(Object objet) : permet de spécifier si on a modifié un objet (sera ajouté à la liste des objets à sauvegarder)

- public void saveAll() : sauvegarde ou met à jour dans le gisement tous les objets qui ont été créés ou modifiés 


-
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
	 * @return the object by type and id
	 */
	public <U> U getObjectByTypeAndId(final Class<U> clazz, final String id) 


	/**
	 * Recupere un objet de type U et tout ses fils jusqu'aux feuilles
	 *
	 * @param <U> the generic type
	 * @param clazz the clazz
	 * @param id the id
	 * @return the object by type and id
	 */
	public <U> U getObjectEnProfondeur(final Class<U> clazz, final String id)
	    

	
	
	/**
	 * Poste l'objet Requete au serveur et récupere l'objet Reponse
	 *
	 * @param Requete
	 * @param enProfondeur true si l'on veut récuperer toute l'arborescence de la réponse
	 */
	public Reponse getReponse(Requete request, boolean enProfondeur)


	