package com.actemium.basicTvx_sdk.restclient;

public class RestException extends Exception{
	private int statusCodeHttp;
	public RestException(int statusCodeHttp) {
		this.statusCodeHttp = statusCodeHttp;
	}
	
	@Override
	public String getMessage() {
		return "HttpStatus code : " + statusCodeHttp;
	}
	
}
