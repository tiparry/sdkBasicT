package com.actemium.basicTvx_sdk.exception;

import java.util.ArrayList;
import java.util.List;

public class GetObjetEnProfondeurException extends Exception{

	private Object objetRacine;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6480142152730314695L;
	private List<GetObjectException> causes = new ArrayList<>();
	private InterruptedException interruptedException;
	public GetObjetEnProfondeurException(Object obj, List<GetObjectException> e) {
		this.causes = e;
	}
	
	
	public GetObjetEnProfondeurException(Object objetRacine, InterruptedException interruptedException) {
		super();
		this.objetRacine = objetRacine;
		this.interruptedException = interruptedException;
	}


	public List<GetObjectException> getCauses(){
		return causes;
	}
	
	public InterruptedException getInterruptedException(){
		return interruptedException;
	}

	@Override
	public String toString() {
		return causes.toString();
	}

	public Object getObjetRacine() {
		return objetRacine;
	}

}
