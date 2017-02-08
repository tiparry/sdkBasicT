package com.actemium.sdk.runtimeaspect;

public class AspectException extends RuntimeException{


	/**
	 * 
	 */
	private static final long serialVersionUID = 6273019135328723945L;

	public AspectException() {
		super();
	}

	public AspectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AspectException(String message, Throwable cause) {
		super(message, cause);
	}

	public AspectException(String message) {
		super(message);
	}

	public AspectException(Throwable cause) {
		super(cause);
	}

}
