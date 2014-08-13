package com.b5m.service.client.ontimeprice;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.b5m.service.client.AbstractClient;
import com.b5m.service.client.HttpClientFactory;
import com.b5m.service.ontime.OntimeLogUtils;
import com.google.code.ssm.api.ParameterValueKeyProvider;
import com.google.code.ssm.api.ReadThroughSingleCache;

/**
 * @author echo
 * @time 2014年5月8日
 * @mail wuming@b5m.com
 */
public class OntimeClientImpl extends AbstractClient implements OntimeClient {
	@Resource(name = "config")
	private Properties properties;
	private String[] fetchPriceSits;
	private String[] fetchSKUSits;

	String DETAIL_URL;

	public OntimeClientImpl(String url, String detailUrl, HttpClientFactory httpClientFactory) {
		super(url, httpClientFactory);
		this.DETAIL_URL = detailUrl;
	}

	@Override
	@ReadThroughSingleCache(namespace = "queryPrices", expiration = 7200)
	public JSONArray queryPrices(@ParameterValueKeyProvider String request) throws HttpException, IOException {
		if(filterFetchPrice(request)) return new JSONArray();
		PostMethod method = null;
		try {
			HttpClient httpClient = httpClientFactory.getHttpClient();
			httpClient.getParams().setSoTimeout(3000);
			method = createPostMethod(url, request);
			int statusCode = httpClient.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				return new JSONArray();
			}
			String resultMsg = method.getResponseBodyAsString().trim();
			OntimeLogUtils.info("on time query price for result[" + resultMsg + "], for request[" + request + "]");
			return JSON.parseArray(resultMsg);
		} finally {
			if(method  != null) method.releaseConnection();
		}
	}
	
	private boolean filterFetchPrice(String request){
		JSONObject jsonObject = JSONObject.parseObject(request);
		JSONArray jsonArray = jsonObject.getJSONArray("list");
		if(jsonArray.size() < 1) return true;
		int length = jsonArray.size();
		String[] fetchPriceSits = canFetchPriceSit();
		for(int index = 0; index < length; index++){
			JSONObject rel = jsonArray.getJSONObject(index);
			String url = rel.getString("url");
			if(canFetch(fetchPriceSits, url)){
				return false;
			}
		}
		return true;
	}
	
	private boolean canFetch(String[] fetchSits, String url){
		if(fetchSits == null) return true;
		for (int i = 0; i < fetchSits.length; i++) {
			if(url.indexOf(fetchSits[i]) >= 0) {
				return true;
			}
		}
		return false;
	}
	
	private String[] canFetchPriceSit(){
		if(fetchPriceSits == null){
			String fetchPriceSit = properties.getProperty("ontime.fetch.price.sources");
			fetchPriceSits = StringUtils.split(fetchPriceSit, ",");
		}
		return fetchPriceSits;
	}
	
	private String[] canFetchSKUSit(){
		if(fetchSKUSits == null){
			String fetchSKUSit = properties.getProperty("ontime.fetch.sku.sources");
			fetchSKUSits = StringUtils.split(fetchSKUSit, ",");
		}
		return fetchSKUSits;
	}
	
	@Override
	@ReadThroughSingleCache(namespace = "querySku", expiration = 7200)
	public JSONObject querySku(@ParameterValueKeyProvider String request) throws HttpException, IOException {
		if(filterFetchSKU(request)) return new JSONObject();
		PostMethod method = null;
		try {
			HttpClient httpClient = httpClientFactory.getHttpClient();
			httpClient.getParams().setSoTimeout(5000);
			method = createPostMethod(DETAIL_URL, request);
			int statusCode = httpClient.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				return new JSONObject();
			}
			String resultMsg = method.getResponseBodyAsString().trim();
			OntimeLogUtils.info("on time query sku for result[" + resultMsg + "], for request[" + request + "]");
			return JSON.parseObject(resultMsg);
		} finally {
			if(method != null){
				method.releaseConnection();
			}
		}
	}
	
	private boolean filterFetchSKU(String request){
		JSONObject jsonObject = JSONObject.parseObject(request);
		String url = jsonObject.getString("url");
		String[] fetchSKUSits = canFetchSKUSit();
		if(canFetch(fetchSKUSits, url)){
			return false;
		}
		return true;
	}
	
	@Override
	@ReadThroughSingleCache(namespace = "queryDetailPrice", expiration = 7200)
	public JSONObject queryDetailPrice(@ParameterValueKeyProvider String request) throws HttpException, IOException {
		PostMethod method = null;
		try {
			HttpClient httpClient = httpClientFactory.getHttpClient();
			method = createPostMethod(DETAIL_URL, request);
			int statusCode = httpClient.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				return new JSONObject();
			}
			String resultMsg = method.getResponseBodyAsString().trim();
			OntimeLogUtils.info("on detail price for result[" + resultMsg + "], for request[" + request + "]");
			return JSON.parseObject(resultMsg);
		}  finally {
			if(method != null){
				method.releaseConnection();
			}
		}
	}

}
