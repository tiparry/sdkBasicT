package com.actemium.sdk;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ObjetPersistantHolderAspect {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@AfterReturning(pointcut="call((*.ObjetPersistant+).new(..)) || call((*.Requete+).new(..)) || call((*.Reponse+).new(..))", returning="obj")
    public void objetWithIdInit(Object obj) {
        if(GlobalObjectManager.getInstance() != null){
        	log.info("mise en cache du sdk");
        	try {
        		GlobalObjectManager.getInstance().metEnCache(obj, false, true);
			} catch (IllegalAccessException e) {
				log.error("impossible car enProfondeur = false donc ne cherche pas les champs null", e);
			}
        }
    }
    
    
	
}
