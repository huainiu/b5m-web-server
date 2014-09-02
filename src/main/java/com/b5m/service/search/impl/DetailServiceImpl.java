package com.b5m.service.search.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.b5m.service.search.DetailService;
import com.b5m.service.sf1.SF1QueryService;

/**
 * @description
 * 详情
 * @author echo
 * @time 2014年8月21日
 * @mail wuming@b5m.com
 */
@Service(version = "0.0.1")
public class DetailServiceImpl implements DetailService{
	
	@Autowired
	private SF1QueryService sf1QueryService;
	
	@Override
	public JSONObject getItem(String docId){
		return sf1QueryService.doGet("b5mp", docId);
	}
	
	@Override
	public JSONObject getItem(String collection, String docId){
		if(StringUtils.isEmpty(collection)) collection = "b5mp";
		return sf1QueryService.doGet(collection, docId);
	}
}
