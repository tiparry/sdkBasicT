utilisation

tous les objets doivent passer par l'instance singleton du GOM.
les méthodes publiques sont : 

-public void setHasChanged(Object objet) : permet de spécifier si on a modifier un objet (sera ajouté à la liste des objets à sauvegarder)

-void saveAll() : sauvegarde tous les objets qui ont été créés ou modifié

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
	    return (U) obj;
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