package com.b5m.service.client;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

public class HttpClientFactory {

	private final MultiThreadedHttpConnectionManager connectionMgr;
	private final ThreadLocal<HttpClient> threadLocal = new ThreadLocal<HttpClient>();
	
	public HttpClientFactory(MultiThreadedHttpConnectionManager connectionMgr){
		this.connectionMgr = connectionMgr;
	}
	
	public HttpClient getHttpClient(){
		HttpClient client = threadLocal.get();
		if(client == null){
			client = new HttpClient(connectionMgr);
			threadLocal.set(client);
		}
		return client;
	}
}
