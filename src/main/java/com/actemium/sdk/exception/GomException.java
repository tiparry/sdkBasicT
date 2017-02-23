package com.actemium.sdk.exception;


public class GomException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3120674349204096431L;

	public GomException(Exception e){
		super(e);
	}
	
	public GomException(String message, Exception e){
		super(message,e);
	}

	public GomException(Throwable t){
		super(t);
	}
	
	public GomException(String message, Throwable t){
		super(message,t);
	}
	
	public GomException(String message){
		super(message);
	}
	
	public GomException(){
		super();
	}
}