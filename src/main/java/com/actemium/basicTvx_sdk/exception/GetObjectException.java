package com.actemium.basicTvx_sdk.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public class GetObjectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8766623874379130509L;
	private Exception e;
	private String idObjet;
	private Class<?> type;
	public GetObjectException(String idObjet, Class<?> type, Exception e) {
		this.e = e;
	}
	
	@Override
	public String getMessage() {
		return "problème sur l'objet de type " + type.getName() + " d'id " + idObjet + " " + e.getMessage();
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return e.fillInStackTrace();
	}

	@Override
	public synchronized Throwable getCause() {
		return e.getCause();
	}

	@Override
	public String getLocalizedMessage() {
		return e.getLocalizedMessage();
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return e.getStackTrace();
	}

	@Override
	public synchronized Throwable initCause(Throwable cause) {
		return e.initCause(cause);
	}

	@Override
	public void printStackTrace() {
		e.printStackTrace();
	}

	@Override
	public void printStackTrace(PrintStream s) {
		e.printStackTrace(s);
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		e.printStackTrace(s);
	}

	@Override
	public void setStackTrace(StackTraceElement[] stackTrace) {
		e.setStackTrace(stackTrace);
	}

	@Override
	public String toString() {
		return e.toString();
	}

	public String getIdObjet() {
		return idObjet;
	}
	
	public Class<?> getType() {
		return type;
	}

}
