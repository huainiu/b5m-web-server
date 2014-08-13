package com.b5m.service.client.autofill;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.b5m.base.common.utils.CollectionTools;
import com.b5m.base.common.utils.StringTools;
import com.b5m.service.client.AbstractClient;
import com.b5m.service.client.HttpClientFactory;
import com.b5m.service.sf1.bean.AutoFillInfo;

/**
 * @Company B5M.com
 * @description
 * 
 * @author echo
 * @since 2013-11-5
 * @email wuming@b5m.com
 */
public class SF1AutoFillClientImpl extends AbstractClient implements SF1AutoFillClient{

	public SF1AutoFillClientImpl(String url, HttpClientFactory httpClientFactory) {
		super(url, httpClientFactory);
	}
	
	public Map<String, List<AutoFillInfo>> allAutoFillSearch(String prefix, Integer pageSize) throws Exception{
		try {
			Map<String, List<AutoFillInfo>> map = CollectionTools.newMap();
			HttpClient httpClient = httpClientFactory.getHttpClient();
			GetMethod method = createGetMethod(prefix, pageSize);
			int statusCode = httpClient.executeMethod(method);
			if(statusCode == HttpStatus.SC_OK){
				String body = method.getResponseBodyAsString();
				if(StringUtils.isEmpty(body)) return map;
				parseBody(prefix, body, map);
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void parseBody(String prefix, String body, Map<String, List<AutoFillInfo>> map){
		JSONObject jsonObject = JSON.parseObject(body);
		JSONObject keys = jsonObject.getJSONObject("keywords");
		String correction = jsonObject.getString("correction");
		for(String key : keys.keySet()){
//			if("haiwaiinfo".equals(key)) continue;//先过滤haiwaiinfo
			map.put(getName(key), parseList(prefix, keys.getJSONArray(key)));
		}
		if(!StringUtils.isEmpty(correction)){
			AutoFillInfo autoFillInfo = new AutoFillInfo();
			autoFillInfo.setValue(correction);
			map.put("correction", CollectionTools.newList(autoFillInfo));
		}
	}
	
	protected List<AutoFillInfo> parseList(String prefix, JSONArray jsonArray){
		if(jsonArray == null) return CollectionTools.newListWithSize(0);
		List<AutoFillInfo> autoFillInfos = CollectionTools.newListWithSize(10);
		int length = jsonArray.size();
		for(int i = 0; i < length; i++){
			JSONObject json = jsonArray.getJSONObject(i);
			if(json == null) continue;
			AutoFillInfo autoFillInfo = new AutoFillInfo();
			String name = json.getString("name");
			if(StringUtils.isEmpty(name)) continue;
			autoFillInfo.setValue(name);
			//autoFillInfo.setHl_value(hlValue(prefix, name));
			JSONArray sub_keywords = json.getJSONArray("sub_keywords");
			if (sub_keywords != null) {
				List<AutoFillInfo> subKeywords = new ArrayList<AutoFillInfo>();
				for (int j = 0; j < sub_keywords.size(); j++) {
					AutoFillInfo _autoFillInfo = new AutoFillInfo();
					JSONObject js = sub_keywords.getJSONObject(j);
					String names = js.getString("name");
					if(StringUtils.isEmpty(names)) continue;
					_autoFillInfo.setValue(names);
					//_autoFillInfo.setHl_value(hlValue(prefix, names));
					subKeywords.add(_autoFillInfo);
				}
				autoFillInfo.setSubKeywords(subKeywords);
			}
			autoFillInfos.add(autoFillInfo);
		}
		return autoFillInfos;
	}
	
	protected String hlValue(String prefix, String name){
		int cityIndex = prefix.indexOf("@");
		if(cityIndex > 0){
			prefix = prefix.substring(0, cityIndex);
		}
		int index = name.indexOf(prefix);
		if(index < 0) return name;
		return "<font style='color:red'>" + prefix + "</font>" + name.substring(index + prefix.length(), name.length());
	}
	
	protected GetMethod createGetMethod(String prefix, Integer pageSize) throws UnsupportedEncodingException{
		GetMethod httpMethod = new GetMethod();
		httpMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		
		httpMethod.setPath(url);
		NameValuePair[] nameValuePairs = new NameValuePair[1];
		NameValuePair nameValuePair = new NameValuePair();
		nameValuePair.setName("json");
	    String[] vs = StringTools.split(prefix, "|");
	    String city = "";
	    if(vs.length > 1){
	    	city = "," + vs[1].substring(1, vs[1].length() - 1);
	    }
		nameValuePair.setValue("{\"limit\":\"" + pageSize + "\", \"prefix\":\"" + vs[0] + "\" " + city + "}");
		nameValuePairs[0] = nameValuePair;
		String queryString = EncodingUtil.formUrlEncode(nameValuePairs, "UTF-8");
		httpMethod.setQueryString(queryString);
		return httpMethod;
	}
	
	protected String getName(String collection){
		if("b5mp".equals(collection)) return "b5mo";
		if("ticket".equals(collection)) return "ticketp";
		if("tour".equals(collection)) return "tourp";
		if("tuan".equals(collection)) return "tuanm";
		return collection;
	}

}
