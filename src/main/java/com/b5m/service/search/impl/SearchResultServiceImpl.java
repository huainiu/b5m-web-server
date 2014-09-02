package com.b5m.service.search.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.b5m.base.common.Lang;
import com.b5m.bean.entity.KeywordSort;
import com.b5m.bean.entity.filterattr.Attibute;
import com.b5m.bean.entity.filterattr.AttibuteRel;
import com.b5m.bean.entity.filterattr.AttibuteValue;
import com.b5m.bean.entity.filterattr.Keywords;
import com.b5m.dao.Dao;
import com.b5m.dao.domain.cnd.Cnd;
import com.b5m.dao.domain.cnd.Op;
import com.b5m.service.search.SearchResultService;
import com.b5m.sf1api.dto.res.DocResourceDto;
import com.b5m.sf1api.dto.res.SearchDTO;

/**
 * @Company B5M.com
 * @description
 * 
 * @author echo
 * @since 2013-9-5
 * @email wuming@b5m.com
 */
@Service("searchResultService")
public class SearchResultServiceImpl implements SearchResultService {
	@Autowired
	private Dao dao;

	@Override
	public Map<String, Attibute> queryAttrFilterList(String keyword) {
		Map<String, Attibute> returnMap = queryAttrFilterList(keyword, false);
		Map<String, Attibute> globalMap = queryAttrFilterList(keyword, true);
		merge(returnMap, globalMap);
		return returnMap;
	}

	// 合并
	protected void merge(Map<String, Attibute> returnMap, Map<String, Attibute> globalMap) {
		for (String key : globalMap.keySet()) {
			Attibute attr = returnMap.get(key);
			Attibute global = globalMap.get(key);
			if (attr == null) {// 如果不存在，则添加
				returnMap.put(key, global);
				continue;
			}
			if (attr.getStatus() == 1 || global.getStatus() == 1) {// 如果已经设置了整个过滤，就直接跳过;如果全局设置了整个过滤，按照优先级的要求
																	// 也进行跳过
				continue;
			}
			merge(attr, global);
		}
	}

	// 合并属性
	protected void merge(Attibute attr, Attibute global) {
		Set<String> values = attr.getValues();
		for (String v : global.getValues()) {
			values.add(v);
		}
	}

	// 获取全局过滤属性
	protected Map<String, Attibute> queryAttrFilterList(String keyword, boolean isGlobal) {
		Cnd cnd = Cnd.where("status", Op.EQ, 1).add("type", Op.EQ, 1);
		if (isGlobal) {
			keyword = "ALL_GLOBAL_FILER_ATTR";
		}
		cnd.add("name", Op.EQ, keyword);
		Keywords keyRes = dao.queryUnique(Keywords.class, cnd);
		Map<String, Attibute> returnMap = new HashMap<String, Attibute>();
		if (keyRes != null) {
			Cnd keywordsCnd = Cnd.where("keywordsId", Op.EQ, keyRes.getId());
			if (!isGlobal) {// 如果查询的是全局的过滤属性，则不需要再进行如下设置
				Attibute allAttr = new Attibute();
				allAttr.setKeyRes(keyRes);
				returnMap.put("ALL", allAttr);
				// 查询attribute 表
				// 设置属性关系
				List<AttibuteRel> attibuteRels = dao.query(AttibuteRel.class, keywordsCnd);
				allAttr.setAttibuteRels(attibuteRels);
			}
			List<Attibute> attrList = dao.query(Attibute.class, keywordsCnd);
			List<Long> attrIds = new ArrayList<Long>();
			Map<Long, Attibute> map = new HashMap<Long, Attibute>();
			for (Attibute attibute : attrList) {
				if (attibute.getStatus() == 0) {
					attrIds.add(attibute.getId());
				}
				map.put(attibute.getId(), attibute);
				attibute.setKeyRes(keyRes);
			}
			// 查询attribute_value 表
			if (!CollectionUtils.isEmpty(attrIds)) {
				Cnd cnd3 = Cnd.where("attibuteId", Op.IN, attrIds.toArray(new Long[attrIds.size()]));
				List<AttibuteValue> valueList = dao.query(AttibuteValue.class, cnd3);
				for (AttibuteValue attibuteValue : valueList) {
					Attibute attr = map.get(attibuteValue.getAttibuteId());
					Set<String> subSet = attr.getValues();
					if (subSet == null) {
						subSet = new HashSet<String>();
						attr.setValues(subSet);
					}
					subSet.add(attibuteValue.getName());
				}
			}
			// 组装数据
			for (Iterator<Attibute> iterator = map.values().iterator(); iterator.hasNext();) {
				Attibute attr = iterator.next();
				returnMap.put(attr.getName(), attr);
			}
		}
		return returnMap;
	}

	@Override
	public void sort(SearchDTO searchDTO, String name) {
		KeywordSort ks = queryKeywordSort(name);
		if (ks == null || Lang.isEmpty(ks.getDocid())) return;
		String[] docids = ks.getDocid().split(";");
		List<DocResourceDto> list = searchDTO.getDocResourceDtos();
		for (int i = 0; i < docids.length; i++) {
			String id = docids[i];
			for (DocResourceDto docResourceDto : list) {
				if (docResourceDto.getRes().get("DOCID").equals(id)) {
					list.remove(docResourceDto);
					list.add(i, docResourceDto);
					break;
				}
			}
		}
		searchDTO.setDocResourceDtos(list);
	}

	protected KeywordSort queryKeywordSort(String name) {
		KeywordSort bean = dao.getByName(KeywordSort.class, name);
		return bean;
	}

}
