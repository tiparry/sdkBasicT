package com.actemium.basicTvx_sdk.restclient;

public enum Serialisation {
	JSON("application/json"), XML("application/xml") ;  
    
    private String contentType ;  
     
    private Serialisation(String contentType) {  
        this.contentType = contentType ;  
   }  
     
    public String getContentType() {  
        return  this.contentType ;  
   }  

}
