package com.actemium.basicTvx_sdk.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public class SaveAllException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1709497872764266477L;
	private Exception e;
	public SaveAllException(Exception e) {
		this.e = e;
	}
	
	@Override
	public String getMessage() {
		return e.getMessage();
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
}
