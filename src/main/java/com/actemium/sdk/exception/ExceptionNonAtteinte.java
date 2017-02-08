package com.actemium.sdk.exception;

public class ExceptionNonAtteinte extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1919333607942167558L;

	public ExceptionNonAtteinte() {
		super();
	}

	public ExceptionNonAtteinte(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ExceptionNonAtteinte(String message, Throwable cause) {
		super(message, cause);
	}

	public ExceptionNonAtteinte(String message) {
		super(message);
	}

	public ExceptionNonAtteinte(Throwable cause) {
		super(cause);
	}

}
