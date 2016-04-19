package com.actemium.basicTvx_sdk.restclient;

public class RestException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2876474690720809161L;
	private int statusCodeHttp;
	public RestException(int statusCodeHttp) {
		this.statusCodeHttp = statusCodeHttp;
	}
	
	public RestException(int statusCode, Exception e) {
		super(e);
		this.statusCodeHttp = statusCode;
	}

	public RestException(int statusCode, String message) {
		super(message);
		this.statusCodeHttp = statusCode;
	}

	public RestException(int statusCode, String message, Exception e) {
		super(message, e);
		this.statusCodeHttp = statusCode;
	}

	@Override
	public String getMessage() {
		return "HttpStatus code : " + statusCodeHttp + System.lineSeparator() + super.getMessage();
	}
	
	public int getStatusCodeHttp(){
		return this.statusCodeHttp;
	}
	
}
