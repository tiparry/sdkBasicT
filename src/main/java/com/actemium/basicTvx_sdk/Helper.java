package com.actemium.basicTvx_sdk;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.actemium.basicTvx_sdk.exception.GetObjectException;
import com.actemium.basicTvx_sdk.restclient.RestException;

public class Helper {
	private Helper(){
		//private constructor to hide the implicit public one.
	}


	static void unwrappExceptionInGetObjectException(String idObjet, Object obj,Exception e) throws GetObjectException{
		if(e instanceof GetObjectException)
			throw (GetObjectException)e;
		else if(e instanceof ExecutionException)
			throw new GetObjectException(idObjet, obj.getClass(), e.getCause());
		else if (e instanceof InterruptedException){
			Thread.currentThread().interrupt();
			throw new GetObjectException(idObjet, obj.getClass(), e);
		}
	}

	static boolean isNetworkException(ExecutionException e){
		if(e.getCause() instanceof RestException || e.getCause() instanceof IOException)
			return true;
		return false;
	}

}
