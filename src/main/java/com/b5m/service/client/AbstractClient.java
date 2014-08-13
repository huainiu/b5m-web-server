package com.b5m.service.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

/**
 * @Company B5M.com
 * @description client 的 父类
 * @author echo
 * @since 2013-8-29
 * @email wuming@b5m.com
 */
public abstract class AbstractClient {
	protected String url;

	protected HttpClientFactory httpClientFactory;

	protected Logger logger = Logger.getLogger(this.getClass());

	public AbstractClient(String url, HttpClientFactory httpClientFactory) {
		this.url = url;
		this.httpClientFactory = httpClientFactory;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public HttpClientFactory getHttpClientFactory() {
		return httpClientFactory;
	}

	public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
		this.httpClientFactory = httpClientFactory;
	}

	protected static PostMethod createPostMethod(String URL, String content) throws UnsupportedEncodingException {
		PostMethod method = new PostMethod(URL);
		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");

		method.setRequestEntity(new StringRequestEntity(content, "application/json", "UTF-8"));
		method.setRequestHeader("Connection", "Keep-Alive");
		return method;
	}

	protected static PostMethod createPostMethod(String URL, String name, String content) throws UnsupportedEncodingException {
		PostMethod method = new PostMethod(URL);

		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		NameValuePair[] parts = { new NameValuePair(name, content) };
		method.setRequestBody(parts);
		return method;
	}

	protected static PostMethod createPostMethod(String URL, Map<String, String> params) throws UnsupportedEncodingException {
		PostMethod method = new PostMethod(URL);
		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		List<NameValuePair> parts = new ArrayList<NameValuePair>();
		for (String key : params.keySet()) {
			parts.add(new NameValuePair(key, params.get(key)));
		}
		method.setRequestBody(parts.toArray(new NameValuePair[] {}));
		method.setRequestHeader("Connection", "Keep-Alive");
		return method;
	}

	protected <T> T sendPost(String request, CallBack<T> call) throws HttpException, IOException {
		HttpClient httpClient = httpClientFactory.getHttpClient();
		PostMethod method = createPostMethod(url, request);
		int statusCode = httpClient.executeMethod(method);
		if (statusCode != HttpStatus.SC_OK) {
			return call.call("", statusCode);
		}
		String resultMsg = method.getResponseBodyAsString().trim();
		return call.call(resultMsg);
	}

	protected <T> T sendPost(String name, String request, CallBack<T> call) throws HttpException, IOException {
		HttpClient httpClient = httpClientFactory.getHttpClient();
		PostMethod method = createPostMethod(url, name, request);
		int statusCode = httpClient.executeMethod(method);
		if (statusCode != HttpStatus.SC_OK) {
			return call.call("", statusCode);
		}
		String resultMsg = method.getResponseBodyAsString().trim();
		return call.call(resultMsg);
	}

	protected GetMethod createGetMethod(String url, String request) {
		GetMethod method = new GetMethod(url + "?" + request);
		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		return method;
	}

	protected <T> T sendGet(String request, CallBack<T> call) throws HttpException, IOException {
		HttpClient httpClient = httpClientFactory.getHttpClient();
		GetMethod method = new GetMethod(url + "?" + request);
		int statusCode = httpClient.executeMethod(method);
		if (statusCode != HttpStatus.SC_OK) {
			return call.call("", statusCode);
		}
		String resultMsg = method.getResponseBodyAsString().trim();
		return call.call(resultMsg);
	}
}
