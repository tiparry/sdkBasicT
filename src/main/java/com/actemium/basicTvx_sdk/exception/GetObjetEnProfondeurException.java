package com.actemium.basicTvx_sdk.exception;

import java.util.List;

public class GetObjetEnProfondeurException extends Exception{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6480142152730314695L;
	private List<Exception> e;
	public GetObjetEnProfondeurException(List<Exception> e) {
		this.e = e;
	}

	@Override
	public String toString() {
		return e.toString();
	}
}
