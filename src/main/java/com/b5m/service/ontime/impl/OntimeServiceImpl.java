package com.b5m.service.ontime.impl;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.b5m.base.common.utils.cache.MemCachedUtils;
import com.b5m.service.client.ontimeprice.OntimeClient;
import com.b5m.service.ontime.OntimeLogUtils;
import com.b5m.service.ontime.OntimeService;
import com.b5m.service.ontime.bean.OntimePriceBean;
import com.b5m.service.ontime.bean.SkuBean;
import com.b5m.service.ontime.bean.SkuProp;
import com.b5m.service.ontime.bean.SkuRequest;

@Service(version = "0.0.1", filter = {"b5mcache"})
public class OntimeServiceImpl implements OntimeService {
	@Autowired
	private OntimeClient ontimeClient;

	@Override
	public JSONArray queryPrices(OntimePriceBean ontimePriceBean) {
		try {
			JSONArray jsonArray = ontimeClient.queryPrices(ontimePriceBean.toJsonString());
			return jsonArray;
		} catch (HttpException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return new JSONArray();
		} catch (IOException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return new JSONArray();
		} catch (Exception e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return new JSONArray();
		}
	}
	
	@Override
	public JSONObject queryDetail(String url){
		long start = System.currentTimeMillis();
		SkuRequest skuRequest = new SkuRequest("", url);
		skuRequest.addKeys("Source","NoSKU","OriginalPicture","Url");
		try {
			JSONObject result = ontimeClient.querySku(skuRequest.toJsonString());
			return result;
		} catch (HttpException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return null;
		} catch (IOException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return null;
		} catch (Exception e) {
			OntimeLogUtils.error("error --- > on time query sku time is["+(System.currentTimeMillis() - start)+"ms] for request[" + skuRequest.toJsonString() + "]", e);
			return null;
		}
	}
	
	@Override
	public SkuBean querySkuProp(SkuRequest skuRequest, String docId) {
		SkuBean skuBean = new SkuBean();
		long start = System.currentTimeMillis();
		try {
			JSONObject result = ontimeClient.querySku(skuRequest.toJsonString());
			if(result == null) return skuBean;
			JSONArray skus = result.getJSONArray("skus");
			if (CollectionUtils.isEmpty(skus) ) return skuBean;
			int length = skus.size();
			JSONObject _sku = new JSONObject();
			skuBean.setSku(_sku);
			for (int index = 0; index < length; index++) {
				JSONObject sku = skus.getJSONObject(index);
				String property = sku.getString("properties_name");
				if (property.endsWith(";"))
					property = property.substring(0, property.length() - 1);
				_sku.put(property, sku.getString("price"));
				addProperty(skuBean, property);
			}
			MemCachedUtils.setCache(docId + "_skuprice", _sku, 86400);
			return skuBean;
		} catch (HttpException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return skuBean;
		} catch (IOException e) {
			OntimeLogUtils.error(e.getMessage(), e);
			return skuBean;
		} catch (Exception e) {
			OntimeLogUtils.error("error --- > on time query sku time is["+(System.currentTimeMillis() - start)+"ms] for request[" + skuRequest.toJsonString() + "]", e);
			return skuBean;
		}
	}

	@Override
	public String getPriceFromSku(String docId, String goosSpec) {
		JSONObject sku = (JSONObject) MemCachedUtils.getCache(docId + "_skuprice");
		if (sku == null)
			return null;
		return sku.getString(goosSpec);
	}

	private void addProperty(SkuBean skuBean, String property) {
		String[] props = StringUtils.split(property, ";");
		for (String prop : props) {
			String[] p = StringUtils.split(prop, ":");
			SkuProp skuProp = skuBean.getByName(p[0]);
			if (skuProp == null) {
				skuProp = new SkuProp();
				skuProp.setName(p[0]);
				skuBean.addSkuProp(skuProp);
			}
			skuProp.addProp(p[1]);
		}
	}

}
