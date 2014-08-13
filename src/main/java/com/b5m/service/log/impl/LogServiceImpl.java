package com.b5m.service.log.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.alibaba.dubbo.config.annotation.Service;
import com.b5m.base.common.utils.DateTools;
import com.b5m.bean.entity.AccessLog;
import com.b5m.dao.Dao;
import com.b5m.service.log.LogService;

@Service(version = "0.0.1")
public class LogServiceImpl implements LogService{
	
	private String date;
	
	@Autowired
	@Qualifier("logDao")
	private Dao logDao;

	@Override
	public void accessLog(AccessLog accessLog) {
		String now = DateTools.formate(new Date(), "yyyyMMdd");
		if(date == null || !now.equals(date)){
			logDao.create(AccessLog.class, false);
		}
		logDao.insert(accessLog);
	}

}
