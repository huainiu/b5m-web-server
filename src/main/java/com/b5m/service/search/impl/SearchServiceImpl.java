package com.b5m.service.search.impl;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.b5m.base.common.utils.StringTools;
import com.b5m.bean.dto.SuiSearchDto;
import com.b5m.bean.entity.filterattr.Attibute;
import com.b5m.service.search.SearchResultService;
import com.b5m.service.search.SearchService;
import com.b5m.service.sf1.SF1QueryService;
import com.b5m.service.sf1.helper.SearchHelper;
import com.b5m.sf1api.dto.res.GroupTree;
import com.b5m.sf1api.dto.res.SearchDTO;
import com.b5m.sf1api.utils.Sf1DataHelper;

@Service(version = "0.0.1")
public class SearchServiceImpl implements SearchService{
	
	@Resource(name = "searchResultService")
	private SearchResultService searchResultService;
	
	@Autowired
	private SF1QueryService sf1Search;
	
	/**
	 *<font style="font-weight:bold">Description: </font> <br/>
	 * 
	 * @author echo
	 * @email wuming@b5m.cn
	 * @since 2014年8月20日 下午1:52:49
	 *
	 * @param dto
	 * @param col collection
	 * @return
	 */
	public SearchDTO searchList(SuiSearchDto dto, String col){
		if(dto.getFilterAttr() != null && dto.getFilterAttr()){
			queryFilterAttr(dto);
		}
		dto.setCollectionName(col);
		SearchDTO searchDTO = sf1Search.search(dto);
		searchDTO.setResultJsonObject(null);
		searchDTO.setDocResourceDtos(null);
		if(dto.getNoSource() == null || !dto.getNoSource()){
			searchDTO.setSourceTree(searchDTO.getGroups().get("Source"));
			Sf1DataHelper.sortGroupTreeByDesc(searchDTO.getSourceTree());
			GroupTree groupTree = searchDTO.getSourceTree();
			GroupTree taosha = null;
			for(GroupTree sub : groupTree.getGroupTree()){
				if("淘沙商城".equals(sub.getGroup().getGroupName())){
					taosha = sub;break;
				}
			}
			if(taosha != null){
				groupTree.getGroupTree().remove(taosha);
				groupTree.getGroupTree().add(0, taosha);
			}
		}
		if(dto.getNoCategory() != null && dto.getNoCategory()){
			searchDTO.setCategoryTree(null);
		}else{
			Sf1DataHelper.sortCategoryTree(searchDTO.getTopGroups(), searchDTO.getCategoryTree());
		}
		if(dto.getNoattr() != null && dto.getNoattr()){
			searchDTO.setAttributeTree(null);
		}
		searchDTO.setTopGroups(null);
		searchDTO.getGroups().clear();
		// 过滤掉已经选择了的属性
		if(dto.getFilterAttr() != null && dto.getFilterAttr()){
			needRemoveAttr(dto.getFilterMap(), dto.getAttrs());
			//过滤属性，属性排序，属性名显示
			SearchHelper.filterAttr(dto.getFilterMap(), searchDTO);
		}
		return searchDTO;
	}
	
	// 过滤掉已经选择了的属性
	protected void needRemoveAttr(Map<String, Attibute> filterMap, String attrs){
		if(StringTools.isEmpty(attrs)) return;
		String[] strs = StringTools.split(attrs, ",");
		for(String str : strs){
			String name = StringTools.split(str, ":")[0];
			Attibute value = new Attibute();
			value.setStatus(0);
			value.setName(name);
			filterMap.put(name, value);
		}
	}
	
	private void queryFilterAttr(SuiSearchDto dto){
		//先不进行过滤
		Map<String, Attibute> filterMap = searchResultService.queryAttrFilterList(dto.getKeyword());
		dto.setFilterMap(filterMap);
	}
	
}
