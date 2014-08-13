package com.b5m.service.client;

public interface CallBack<T> {
	
	T call(Object ... args);
	
}
