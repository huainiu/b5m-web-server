package com.b5m.service.hbase.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.ColumnPaginationFilter;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.filter.ValueFilter;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.b5m.base.common.spring.aop.Cache;
import com.b5m.base.common.utils.CollectionTools;
import com.b5m.base.common.utils.DateTools;
import com.b5m.base.common.utils.StringTools;
import com.b5m.dao.domain.page.PageView;
import com.b5m.service.hbase.Connection;
import com.b5m.service.hbase.HbaseDataService;
import com.b5m.service.hbase.bean.CommentPageView;
import com.b5m.service.hbase.bean.CommentType;
import com.b5m.service.hbase.bean.HComment;
import com.b5m.service.hbase.bean.PricePerDay;
import com.b5m.service.hbase.bean.PriceTrend;
/**
 * @description
 * hbase 数据查询服务
 * @author echo
 * @time 2014年7月28日
 * @mail wuming@b5m.com
 */
@Service(version = "0.0.1", filter = {"b5mcache"})
public class HbaseDataServiceImpl implements HbaseDataService {
	private static final String CHAR_SET = "UTF-8";
	@Resource(name = "config")
	private Properties properties;
	private Connection hBaseConnection;
	private HTable productDetailsTable;
	private HTable commentTable;
	private HTable priceTrendTable;
	private long currenttime;
	private static final Log LOG = LogFactory.getLog(HbaseDataServiceImpl.class);

	private Connection getHBaseConnection() {
		if (hBaseConnection == null) {
			String hbaseZookeeperQuorum = properties.getProperty("zookeeper");
			String hbaseZookeeperClientPort = properties.getProperty("zookeeperPort");
			hBaseConnection = Connection.initConnection(hbaseZookeeperQuorum, hbaseZookeeperClientPort);
		}
		return hBaseConnection;
	}

	private HTable getProductDetailsTable() {
		if (productDetailsTable == null) {
			synchronized (this) {// 同步创建
				if (productDetailsTable == null) {
					if(currenttime == 0 || System.currentTimeMillis() - currenttime > 60000){//如果获取表失败 则等待60秒后重新获取
						String tableName = properties.getProperty("product.detail.tableName");
						String columnFamily = properties.getProperty("product.detail.columnFamily");
						productDetailsTable = getHBaseConnection().createHTable(tableName, columnFamily);
						currenttime = System.currentTimeMillis();
					}
				}
			}
		}
		return productDetailsTable;
	}

	private HTable getCommentTable() {
		if (commentTable == null) {
			synchronized (this) {// 同步创建
				if (commentTable == null) {
					if(currenttime == 0 || System.currentTimeMillis() - currenttime > 60000){
						String tableName = properties.getProperty("product.comment.tableName");
						commentTable = getHBaseConnection().createHTable(tableName, "COMM");
						currenttime = System.currentTimeMillis();
					}
				}
			}
		}
		return commentTable;
	}
	
	private HTable getPriceTrendTable() {
		if (priceTrendTable == null) {
			synchronized (this) {// 同步创建
				if (priceTrendTable == null) {
					if(currenttime == 0 || System.currentTimeMillis() - currenttime > 60000){
						String tableName = properties.getProperty("tableName");
						priceTrendTable = getHBaseConnection().createHTable(tableName, "PH");
						currenttime = System.currentTimeMillis();
					}
				}
			}
		}
		return priceTrendTable;
	}

	@Override
	@Cache
	public String getProductDetail(String docId) throws Exception {
		if (StringTools.isEmpty(docId))
			return "";
		Get get = new Get(docId.getBytes(CHAR_SET));
		String columnFamily = properties.getProperty("product.detail.columnFamily");
		// 添加获取的列
		String qualifier = "detail";
		get.addColumn(columnFamily.getBytes(CHAR_SET), qualifier.getBytes(CHAR_SET));
		HTable htable = getProductDetailsTable();
		if(htable == null) return "";
		Result result = htable.get(get);
		// 判断结果是否为空
		if (result == null)
			return "";
		// 获取结果
		byte[] detailBytes = result.getValue(columnFamily.getBytes(CHAR_SET), "detail".getBytes(CHAR_SET));
		// 判断结果是否为空
		if (detailBytes == null)
			return "";
		String detail = new String(detailBytes, CHAR_SET);
		return detail;
	}

	@Override
	@Cache
	public PageView<HComment> queryCommentPage(String docId, CommentType type, Integer pageSize, Integer pageCode) throws Exception {
		if (pageSize == null)
			pageSize = 10;
		if (pageCode == null)
			pageCode = 1;
		CommentPageView<HComment> pageView = new CommentPageView<HComment>(pageSize, pageCode);
		if (StringTools.isEmpty(docId))
			return pageView;
		// 查询总量
		long size = getSize(docId, type, pageView);
		// 设置总量
		pageView.setTotalRecord(size);
		if (size > 0) {
			Long offset = size - pageSize * pageCode;
			if(offset < 0) {
				offset = 0l;
				pageSize = (int) (size % pageSize);
			}
			List<HComment> list = queryPageComment(docId, type, pageSize, offset.intValue());
			pageView.setRecords(list);
		}
		return pageView;
	}

	@Cache
	public long getCommentSize(String docId, CommentType type) throws Exception {
		long totalCount = 0;
		if (CommentType.ALL.equals(type)) {
			long goodNum = getTypeCount(docId, CommentType.GOOD);
			long badNum = getTypeCount(docId, CommentType.BAD);
			long midNum = getTypeCount(docId, CommentType.NORMAL);
			totalCount = goodNum + badNum + midNum;// 算总数
		} else {
			totalCount = getTypeCount(docId, type);
		}
		return totalCount;
	}

	@Cache
	protected long getSize(String docId, CommentType type, CommentPageView<HComment> pageView) throws Exception {
		long totalCount = 0;
		if (CommentType.ALL.equals(type)) {
			long goodNum = getTypeCount(docId, CommentType.GOOD);
			long badNum = getTypeCount(docId, CommentType.BAD);
			long midNum = getTypeCount(docId, CommentType.NORMAL);
			pageView.setGoodNum(goodNum);
			pageView.setMidNum(midNum);
			pageView.setBadNum(badNum);
			totalCount = goodNum + badNum + midNum;// 算总数
		} else {
			totalCount = getTypeCount(docId, type);
		}
		return totalCount;
	}

	@Cache
	protected int getTypeCount(String docId, CommentType type) throws Exception {
		String columnFamily = properties.getProperty("product.comment.columnFamily");
		Get get = new Get(docId.getBytes());
		get.addColumn(columnFamily.getBytes(), type.toString().getBytes());
		HTable htable = getCommentTable();
		if(htable == null) return 0;
		Result result = htable.get(get);

		KeyValue keyValue = result.getColumnLatest(columnFamily.getBytes(), type.toString().getBytes());
		if (keyValue != null) {
			String value = new String(keyValue.getValue(), "UTF-8");
			if (!StringTools.isEmpty(value))
				return Integer.valueOf(value);
		}
		return 0;
	}

	@Override
	@Cache
	public List<String> getTag(String docId) throws Exception {
		if (StringTools.isEmpty(docId))
			return new ArrayList<String>(0);
		Get get = new Get(docId.getBytes(CHAR_SET));
		String columnFamily = properties.getProperty("product.comment.columnFamily");
		// 添加获取的列
		String qualifier = "TAG";
		get.addColumn(columnFamily.getBytes(CHAR_SET), qualifier.getBytes(CHAR_SET));
		
		HTable htable = getCommentTable();
		if(htable == null) return new ArrayList<String>(0);
		
		Result result = htable.get(get);
		// 判断结果是否为空
		if (result == null)
			return new ArrayList<String>(0);
		// 获取结果
		byte[] tagBytes = result.getValue(columnFamily.getBytes(CHAR_SET), qualifier.getBytes(CHAR_SET));
		// 判断结果是否为空
		if (tagBytes == null)
			return new ArrayList<String>(0);
		String tagStr = new String(tagBytes, CHAR_SET);
		if (StringUtils.isEmpty(tagStr))
			return new ArrayList<String>(0);
		if ((tagStr.indexOf("@") == -1) && (tagStr.indexOf("good") != -1)) {
			List<String> list = new ArrayList<String>();
			setTagList(list, tagStr);
			return list;
		} else {
			return Arrays.asList(StringUtils.split(tagStr, "@"));
		}
	}
	
	@Override
	@Cache
	public PriceTrend getPriceTrend(Date start, Date end, String docId){
		PriceTrend priceTrend = new PriceTrend();
		try {
			Get product = new Get(docId.getBytes(CHAR_SET));
			product.addFamily(properties.getProperty("columnFamily").getBytes(CHAR_SET));
			product.setFilter(createFilter(start, end));
			priceTrend.setDocId(docId);
			List<KeyValue> kvs = getPriceTrendTable().get(product).list();
			if (null == kvs) return null;
			for (int i = 0; i < kvs.size(); ++i) {
				KeyValue kv = kvs.get(i);
				PricePerDay pricePerDay = new PricePerDay();
				pricePerDay.setDate(DateTools.toDate(kv.getTimestamp()));
				BigDecimal p = new BigDecimal(new String(kv.getValue(), CHAR_SET)).setScale(2, RoundingMode.HALF_EVEN);
				pricePerDay.setPrice(p);
				priceTrend.addPricePerDay(pricePerDay);
			}
		} catch (Exception e) {
			return priceTrend;
		}
		return priceTrend;
	}
	
	public List<PriceTrend> getPriceTrends(Date start, Date end, String... docIds) {
		return getPriceTrends(start, end, Arrays.asList(docIds));
	}

	public List<PriceTrend> getPriceTrends(Date start, Date end, List<String> docIds) {
		List<PriceTrend> priceTrends = new ArrayList<PriceTrend>(docIds.size());
		for(String docId : docIds){
			priceTrends.add(getPriceTrend(start, end, docId));
		}
		return priceTrends;
	}
	
	protected void setTagList(List<String> list, String tagStr) {
		String goodStr = StringUtils.substringBetween(tagStr, "\"good\":\"", "\",");
		String badStr = StringUtils.substringBetween(tagStr, "\"bad\":\"", "\"");
		if (!StringUtils.isEmpty(goodStr)) {
			if (goodStr.indexOf(",") != -1) {
				String[] goods = StringUtils.split(goodStr, ",");
				for (String good : goods) {
					if (!"".equals(good.trim())) {
						list.add(good);
					}
				}
			} else {
				list.add(goodStr);
			}
		}
		if (!StringUtils.isEmpty(badStr)) {
			if (badStr.indexOf(",") != -1) {
				String[] bads = StringUtils.split(badStr, ",");
				for (String bad : bads) {
					if (!"".equals(bad.trim())) {
						list.add(bad);
					}
				}
			} else {
				list.add(badStr);
			}
		}
	}

	@Cache
	protected List<HComment> queryPageComment(String docId, CommentType type, int pageSize, int offset) throws Exception {
		Get get = new Get(docId.getBytes(CHAR_SET));
		get.setFilter(generatorFilter(type, pageSize, offset));

		HTable htable = getCommentTable();
		if(htable == null) return CollectionTools.newListWithSize(0);
		Result result = htable.get(get);
		if (result == null)
			return CollectionTools.newListWithSize(0);
		String columnFamily = properties.getProperty("product.comment.columnFamily");
		// 根据rowkey获取所有内容
		NavigableMap<byte[], byte[]> nav = result.getFamilyMap(columnFamily.getBytes());
		if (nav == null)
			return CollectionTools.newListWithSize(0);
		List<HComment> list = CollectionTools.newListWithSize(20);

		for (byte[] k : nav.keySet()) {
			String key = new String(k, CHAR_SET);
			if (StringTools.isEmpty(key))
				continue;
			int lastSepIndex = key.lastIndexOf(":");
			if (lastSepIndex < 0)
				continue;
			// 获取一条的key
			String commentType = key.substring(0, lastSepIndex);
			// 获取评论哪一部分的标记
			String time = key.substring(lastSepIndex + 1);
			HComment hComment = new HComment();
			hComment.setType(getCommentType(commentType));
			setCreateTime(time, hComment);
			String value = new String(nav.get(k), CHAR_SET);
			JSONObject jsonObject = JSON.parseObject(value);
			hComment.setAuthor(jsonObject.getString("E"));
			hComment.setContent(jsonObject.getString("F"));
			hComment.setCommentAttr(jsonObject.getString("H"));
			list.add(hComment);
		}
		Collections.sort(list, new Comparator<HComment>() {

			@Override
			public int compare(HComment o1, HComment o2) {
				return o2.getCreateTime().compareTo(o1.getCreateTime());
			}

		});
		return list;
	}

	protected void setCreateTime(String time, HComment hComment) {
		if (StringTools.isEmpty(time)) return;
		hComment.setCreateTime(DateTools.parse(time, "yyyyMMddHHmmss".substring(0, time.length())));
	}

	protected Filter generatorFilter(CommentType type, int pageSize, int offset) {
		FilterList filterList = new FilterList();
		if (!type.equals(CommentType.ALL)) {
			Filter filter = new ColumnPrefixFilter((type.toString() + ":").getBytes());
			filterList.addFilter(filter);
		} else {
			filterList.addFilter(new ValueFilter(CompareOp.EQUAL, new SubstringComparator("{")));
			filterList.addFilter(new ValueFilter(CompareOp.EQUAL, new SubstringComparator("\"F\"")));
		}
//		Filter filter = new ColumnPaginationFilter(pageSize, (pageCode - 1) * pageSize);
		Filter filter = new ColumnPaginationFilter(pageSize, offset);
		filterList.addFilter(filter);
		return filterList;
	}

	protected int getCommentType(String value) {
		if ("好评".equals(value)) return 1;
		if ("中评".equals(value)) return 0;
		return -1;
	}
	
	private FilterList createFilter(Date start, Date end) throws Exception {
		List<Filter> filters = new ArrayList<Filter>();
		Filter filter = null;
		if(start != null){
			filter = new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(DateTools.formate(start, "yyyy-MM-dd HH:mm:ss").getBytes(CHAR_SET)));
			filters.add(filter);
		}
		if(end != null){
			filter = new QualifierFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, new BinaryComparator(DateTools.formate(start, "yyyy-MM-dd HH:mm:ss").getBytes(CHAR_SET)));
			filters.add(filter);
		}
		return new FilterList(FilterList.Operator.MUST_PASS_ALL, filters);
	}
}
