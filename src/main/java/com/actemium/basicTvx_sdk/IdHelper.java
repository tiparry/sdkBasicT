package com.actemium.basicTvx_sdk;

public interface IdHelper<U> {

	public U getNewId();
	public U getId(Object obj);
	public void setId(Object obj, U id);
	public U convertId(String id);

}
