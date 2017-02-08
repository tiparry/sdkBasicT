package com.actemium.sdk;

public abstract class IdHelper<U> {
	private static IdHelper<?> instance;
	
	protected IdHelper(){
		instance = this;
	}
	@SuppressWarnings("unchecked")
	public static <I> IdHelper<I> getIdHelper(){
		if(instance != null){
			instance = new UUIDFactoryRandomImpl();
		}
		return (IdHelper<I>) instance;
	}
	public abstract U getId(Object obj);
	public abstract void setId(Object obj, U id);
	public abstract U convertId(String id);

}
