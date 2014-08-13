package com.b5m.service.client.ontimeprice;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public interface OntimeClient {
	
	JSONArray queryPrices(String request) throws HttpException, IOException;
	
	JSONObject querySku(String request) throws HttpException, IOException;

	JSONObject queryDetailPrice(String request) throws HttpException, IOException;
	
}
