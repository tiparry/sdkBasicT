package com.actemium.basicTvx_sdk.util;

import java.util.concurrent.ConcurrentHashMap;

public class BiHashMap<K1, K2, V> extends ConcurrentHashMap<Pair<K1, K2>, V> {
	private static final long serialVersionUID = 1L;
	public void put(K1 key1, K2 key2, V value) {
		put(key(key1, key2), value);
	}
	public V get(K1 key1, K2 key2) {
		return get(key(key1, key2));
	}
	public void removeObj(K1 key1, K2 key2){
		remove(key(key1, key2));
	}
	private Pair<K1, K2> key(K1 key1, K2 key2) {
		return new Pair<K1, K2>(key1, key2);
	}
	
}
