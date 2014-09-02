package com.b5m.sys;
/**
 * @description
 * 渠道
 * @author echo
 * @time 2014年6月6日
 * @mail wuming@b5m.com
 */
public class Channel {
	//渠道中文名称
	private String displayName;
	//渠道名称
	private String name;
	//集合 对应sf1这边的collection
	private String collection;
	//页面大小
	private Integer pageSize;
	//搜索结果页
	private String resultPage;
	//无搜索结果页
	private String noResultPage;
	//是否继续属性过滤 由后台控制的
	private boolean filterAttr;
	//sf1服务地址 不同的collection有可能不一样
	private String serverPath;
	//搜索不到，是否需要重新搜索
	private boolean needMoreResearch;
	
	private String detailPage;
	//是否category转化成数字
	private boolean categoryUrl;
	//有些渠道根据国家进行筛选
	private String country;
	
	private String domain;
	
	private String headerCss;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getResultPage() {
		return resultPage;
	}

	public void setResultPage(String resultPage) {
		this.resultPage = resultPage;
	}

	public String getNoResultPage() {
		return noResultPage;
	}

	public void setNoResultPage(String noResultPage) {
		this.noResultPage = noResultPage;
	}

	public boolean isFilterAttr() {
		return filterAttr;
	}

	public void setFilterAttr(boolean filterAttr) {
		this.filterAttr = filterAttr;
	}

	public String getServerPath() {
		return serverPath;
	}

	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	public boolean isNeedMoreResearch() {
		return needMoreResearch;
	}

	public void setNeedMoreResearch(boolean needMoreResearch) {
		this.needMoreResearch = needMoreResearch;
	}

	public String getDetailPage() {
		return detailPage;
	}

	public void setDetailPage(String detailPage) {
		this.detailPage = detailPage;
	}

	public boolean isCategoryUrl() {
		return categoryUrl;
	}

	public void setCategoryUrl(boolean categoryUrl) {
		this.categoryUrl = categoryUrl;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getHeaderCss() {
		return headerCss;
	}

	public void setHeaderCss(String headerCss) {
		this.headerCss = headerCss;
	}
	
}
