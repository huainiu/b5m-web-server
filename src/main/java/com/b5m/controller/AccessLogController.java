package com.b5m.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.b5m.base.common.utils.DateTools;
import com.b5m.bean.entity.AccessLog;
import com.b5m.dao.Dao;
import com.b5m.dao.domain.page.PageCnd;
import com.b5m.dao.domain.page.PageView;

@Controller
public class AccessLogController {
	
	@Autowired
	@Qualifier("logDao")
	private Dao dao;
	
	@RequestMapping("/accesslog")
	public String log(String server, String time, Integer currentPage, Integer pageSize, String path, String ref, String _ref, String params, HttpServletRequest req){
		if(currentPage == null) currentPage = 1;
		if(pageSize == null) pageSize = 100;
		StringBuilder where = new StringBuilder(" where 1=1 ");
		if(!StringUtils.isEmpty(server)){
			where.append(" and server = '").append(server).append("'");
		}
		if(!StringUtils.isEmpty(path)){
			where.append(" and path like ").append("'%" + path + "%'");
		}
		if(!StringUtils.isEmpty(ref)){
			where.append(" and ref like ").append("'%" + ref + "%'");
		}
		if(!StringUtils.isEmpty(_ref)){
			where.append(" and ref not like ").append("'%" + _ref + "%'");
		}
		if(!StringUtils.isEmpty(params)){
			where.append(" and params like ").append("'%" + params + "%'");
		}
		if(StringUtils.isEmpty(time)){
			time = DateTools.formate(new Date(), "yyyyMMdd");
		}
		List<Map> mapList = dao.queryBySql("select server, count(*) as count, max(respone_time) as max, avg(respone_time) as avg from t_access_log_" + time + " group by server", new Object[]{}, Map.class);
		PageView<AccessLog> page = dao.queryPageBySql("select * from t_access_log_" + time + " " + where.toString() , new Object[]{}, PageCnd.newInstance(currentPage, pageSize), AccessLog.class);
//		PageView<AccessLog> page = dao.queryPage(AccessLog.class, cnd, PageCnd.newInstance(currentPage, pageSize));
		req.setAttribute("page", page);
		req.setAttribute("mapList", mapList);
		req.setAttribute("currentPage", currentPage);
		req.setAttribute("pageSize", pageSize);
		req.setAttribute("page", page);
		req.setAttribute("path", path);
		req.setAttribute("server", server);
		req.setAttribute("params", params);
		return "log";
	}
	
	
}
