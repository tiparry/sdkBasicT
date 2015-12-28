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
	
	@Override
	public String getMessage() {
		return "HttpStatus code : " + statusCodeHttp;
	}
	
}
