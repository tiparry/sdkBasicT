package com.actemium.sdk;

public interface CallBack {
	
	public void objetEnSucces(Object object, boolean wasNew);
	public void objetEnErreur(Object object, Exception e);
}