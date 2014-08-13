package com.b5m.service.sf1.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.b5m.base.common.utils.ThreadTools;
import com.b5m.service.client.autofill.SF1AutoFillClient;
import com.b5m.service.sf1.SF1QueryService;
import com.b5m.service.sf1.bean.AutoFillInfo;
import com.b5m.service.sf1.bean.SuiSearchDto;
import com.b5m.service.sf1.helper.SearchHelper;
import com.b5m.sf1api.dto.req.SF1SearchBean;
import com.b5m.sf1api.dto.res.SearchDTO;
import com.b5m.sf1api.service.Sf1Query;

@Service(version = "0.0.1")
public class SF1QueryServiceImpl implements SF1QueryService{
	@Autowired
	private Sf1Query sf1Query;
	@Autowired
	private SF1AutoFillClient sf1AutoFillClient;
	private ExecutorService threadPool;
	
	public SF1QueryServiceImpl(){
		threadPool = new ThreadPoolExecutor(2, 5,  0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public SF1QueryServiceImpl(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	public SearchDTO search(SuiSearchDto dto) {
		dto.setCollectionName(dto.getCollectionName());
		dto.setRequireRelated(true);
		SF1SearchBean searchBean = SearchHelper.convertTo4Search(dto);
		SearchDTO searchDTO = sf1Query.doSearch(searchBean);
		return searchDTO;
	}

	@Override
	public String queryCorrect(String keywords) {
		List<String> refinedQuery = sf1Query.getRefined_query("b5mp", keywords);
		if (refinedQuery.isEmpty()) return keywords;
		String refine = refinedQuery.get(0);
		if (StringUtils.isEmpty(refine)) return keywords;
		return refine;
	}

	@Override
	public JSONObject doGet(String collection, String docId) {
		SF1SearchBean searchBean = SearchHelper.converTo4Get(collection, docId);
		return sf1Query.doGetForJson(searchBean);
	}

	@Override
	public SearchDTO[] multiSearch(String[] keywords) {
		List<String> keywordList = needSearchKeywords(keywords);
		List<Callable<SearchDTO>> queryTasks = new ArrayList<Callable<SearchDTO>>();
		for (final String keyword : keywordList) {
			queryTasks.add(new Callable<SearchDTO>() {

				@Override
				public SearchDTO call() throws Exception {
					SuiSearchDto dto = new SuiSearchDto();
					dto.setKeyword(keyword);
					dto.setPageSize(5);
					dto.setCurrPageNo(1);
					SearchDTO searchDto = search(dto);
					searchDto.setKeywords(keyword);
					return searchDto;
				}

			});
		}
		return ThreadTools.executor(queryTasks, SearchDTO.class, threadPool);
	}
	
	/**
	 * @description 最多用三个 keywords 进行查询
	 * @param keywords
	 * @return
	 * @author echo
	 * @since 2013-9-6
	 * @email echo.weng@b5m.com
	 */
	protected List<String> needSearchKeywords(String[] keywords) {
		List<String> keywordList = new ArrayList<String>(3);
		if (keywords.length < 3) {
			for (String word : keywords) {
				keywordList.add(word);
			}
		} else {
			for (int i = 0; i <= 2; i++) {
				keywordList.add(keywords[i]);
			}
		}
		return keywordList;
	}
	
	@Override
	public Map<String, List<AutoFillInfo>> autoFillSearch(String prefix, int limit, String city) throws Exception{
		String k = prefix;
		if (!StringUtils.isEmpty(city)) {
			k = prefix + "|" + city;
		}
		return sf1AutoFillClient.allAutoFillSearch(k, limit);
	}

}
