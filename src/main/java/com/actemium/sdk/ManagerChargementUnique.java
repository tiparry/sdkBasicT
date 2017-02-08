package com.actemium.sdk;

import static com.actemium.sdk.Helper.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.actemium.sdk.exception.GetObjectException;

public class ManagerChargementUnique extends ManagerChargementSDK {
	
	
	ManagerChargementUnique(GlobalObjectManager gom, Object objetRacine){
		super(gom, objetRacine, Executors.newSingleThreadExecutor());
	}
	
	@Override
	protected Future<Object> submit(Object o){
		return getGom().createFuture(getExecutor(), o);
	}

	@Override
	protected void execute() throws GetObjectException {
		try{
			Future<Object> future = submit(getObjetRacine());
			future.get();
		}
		catch(ExecutionException e1){ 
			gererExecutionExceptionUnique(getObjetRacine(), e1);
		}
		catch(InterruptedException ie){
			Thread.currentThread().interrupt();
			unwrappExceptionInGetObjectException(getGom().getId(getObjetRacine()), getObjetRacine(), ie);
		}
		finally{
			getGom().interromptChargement(getObjetRacine());//chargement fini de toute facon.
			chargementTermineAndShutdownNow();
		}
	}
	
	private <U> void gererExecutionExceptionUnique(U obj, ExecutionException e1) throws 
	GetObjectException {
		getGom().interromptChargement(obj);
		if (isNetworkException(e1)){
			try{
				Future<Object> future = submit(obj);
				future.get();
			}
			catch( ExecutionException | InterruptedException e2){
				unwrappExceptionInGetObjectException(getGom().getId(obj), obj, e2);
			}
		}
		else{
			unwrappExceptionInGetObjectException(getGom().getId(obj), obj, e1);
		}
	}
	
}
