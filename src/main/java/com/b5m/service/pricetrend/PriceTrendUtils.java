package com.b5m.service.pricetrend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.b5m.base.common.utils.CollectionTools;
import com.b5m.base.common.utils.DateTools;
import com.b5m.service.hbase.HbaseDataService;
import com.b5m.service.hbase.bean.PricePerDay;
import com.b5m.service.hbase.bean.PriceTrend;

public class PriceTrendUtils {
	public static final int DOWN = -1; // 价格下降
	public static final int NO_CHANGE = 0; // 价格不变
	public static final int UP = 1; // 价格上升
	public static final int ERROR = -99; // 价格趋势非法
	public static Integer DATA_SOURCE;
	
	private static HbaseDataService hbaseDataService;

	/**
	 * @description 获取历史价格
	 * @param range
	 * @param getFulHistory
	 * @param docids
	 * @return
	 * @author echo
	 * @since 2013-9-24
	 * @email echo.weng@b5m.com
	 */
	public static List<PriceTrend> getPriceHistory(int range, boolean getFulHistory, String... docids) {
		Date beginTime = DateTools.addDay(DateTools.now(), -range);
		List<PriceTrend> priceHistoryDTOs = hbaseDataService.getPriceTrends(beginTime, null, docids);
		dealWithPrice(range, priceHistoryDTOs);
		return priceHistoryDTOs;
	}

	/**
	 * @description 获取价格趋势类型
	 * @param docIds
	 * @param range
	 * @return
	 * @author echo
	 * @since 2013-9-24
	 * @email echo.weng@b5m.com
	 */
	public static String getPriceTrendType(String[] docIds, int range) {
		return getPriceTrendType(getPriceHistory(range, false, docIds));
	}

	

	public static Map<String, PricePerDay> convertDatePriceMap(List<PricePerDay> pricePerDays) {
		Map<String, PricePerDay> map = CollectionTools.newMap();
		for (PricePerDay pricePerDay : pricePerDays) {
			map.put(DateTools.formate(pricePerDay.getDate()), pricePerDay);
		}
		return map;
	}

	/**
	 * @description 获取价格趋势类型
	 * @param priceHistoryDtos
	 * @return
	 * @author echo
	 * @since 2013-9-24
	 * @email echo.weng@b5m.com
	 */
	public static String getPriceTrendType(List<PriceTrend> priceHistoryDtos) {
		StringBuffer type = new StringBuffer();
		for (int i = 0; i < priceHistoryDtos.size(); i++) {
			PriceTrend priceHistoryDTO = priceHistoryDtos.get(i);
			if (priceHistoryDTO == null)
				continue;
			int priceType = getPriceType(priceHistoryDTO);
			consistPriceType(priceHistoryDTO, priceType, type);
		}
		return type.toString();
	}

	public static void fillEveryDatePrice(PriceTrend priceTrend, Integer range) {
		List<PricePerDay> pricePerDays = priceTrend.getPricePerDays();
		Map<String, PricePerDay> pricePerDayMap = convertDatePriceMap(pricePerDays);
		Date now = DateTools.now();
		PricePerDay beforePrice = priceTrend.getPricePerDays().get(0);
		for (int i = range; i >= 0; i--) {
			Date indexDate = DateTools.addDay(now, -i);
			PricePerDay pricePerDay = getPricePerDay(pricePerDayMap, indexDate);
			if (pricePerDay == null) {
				pricePerDay = createPricePerDay(indexDate, beforePrice.getPrice());
				pricePerDays.add(pricePerDay);
			} else {
				beforePrice = pricePerDay;
			}
		}
	}

	protected static PricePerDay getPricePerDay(Map<String, PricePerDay> pricePerDayMap, Date date) {
		return pricePerDayMap.get(DateTools.formate(date));
	}

	/**
	 * @description 获取单个价格趋势类型
	 * @param priceHistoryDTO
	 * @return
	 * @author echo
	 * @since 2013-9-24
	 * @email echo.weng@b5m.com
	 */
	public static int getPriceType(PriceTrend priceHistoryDTO) {
		if (priceHistoryDTO == null)
			return NO_CHANGE;
		Collections.sort(priceHistoryDTO.getPricePerDays(), new Comparator<PricePerDay>() {

			@Override
			public int compare(PricePerDay o1, PricePerDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}

		});
		List<PricePerDay> pricePerDays = priceHistoryDTO.getPricePerDays();
		int length = pricePerDays.size();
		if (length <= 1)
			return NO_CHANGE;
		PricePerDay last = pricePerDays.get(length - 1);
		// 去除小于一块的升降
		BigDecimal lastPrice = last.getPrice().setScale(0, RoundingMode.DOWN);
		BigDecimal p2Price = getPrePrice(pricePerDays, lastPrice).setScale(0, RoundingMode.DOWN);
		if (lastPrice.compareTo(p2Price) > 0)
			return UP;
		if (lastPrice.compareTo(p2Price) < 0)
			return DOWN;
		return NO_CHANGE;
	}

	public static PricePerDay getPrePrice(PriceTrend priceHistoryDTO, PricePerDay today) {
		List<PricePerDay> pricePerDays = priceHistoryDTO.getPricePerDays();
		for (int i = pricePerDays.size() - 1; i >= 0; i--) {
			PricePerDay p = pricePerDays.get(i);
			if (!(p.getPrice().intValue() == today.getPrice().intValue()))
				return p;
		}
		return null;
	}

	/**
	 * @description 计算平均价格
	 * @param priceHistoryDTOs
	 * @param range
	 * @return
	 * @author echo
	 * @since 2013-9-26
	 * @email echo.weng@b5m.com
	 */
	public static PriceTrend createAveriage(List<PriceTrend> priceHistoryDTOs, int range) {

		int count = 0;
		PriceTrend totalPrice = new PriceTrend();
		Map<String, PricePerDay> totalPricePerDayMap = convertDatePriceMap(totalPrice.getPricePerDays());
		Date now = DateTools.now();
		for (PriceTrend priceTrend : priceHistoryDTOs) {
			Map<String, PricePerDay> pricePerDayMap = convertDatePriceMap(priceTrend.getPricePerDays());
			count++;
			PricePerDay beforePrice = priceTrend.getPricePerDays().get(0);
			for (int i = range; i >= 0; i--) {
				Date indexDate = DateTools.addDay(now, -i);
				PricePerDay totalPerDay = getPricePerDay(totalPricePerDayMap, indexDate);
				if (totalPerDay == null) {
					totalPerDay = createPricePerDay(indexDate, BigDecimal.ZERO);
					totalPrice.addPricePerDay(totalPerDay);
					totalPricePerDayMap.put(DateTools.formate(indexDate), totalPerDay);
				}
				PricePerDay perDay = getPricePerDay(pricePerDayMap, indexDate);
				if (perDay == null) {
					perDay = beforePrice;
				} else {
					beforePrice = perDay;
				}
				totalPerDay.setPrice(totalPerDay.getPrice().add(perDay.getPrice()));
			}
		}
		for (PricePerDay pricePerDay : totalPrice.getPricePerDays()) {
			if (count != 0) {
				BigDecimal price = pricePerDay.getPrice().divide(new BigDecimal(count), 2, RoundingMode.UP);
				pricePerDay.setPrice(price);
			}
		}
		return totalPrice;
	}

	protected static PriceTrend getLowest(PriceTrend first, PriceTrend sencod) {
		List<PricePerDay> pricePerDaysF = first.getPricePerDays();
		List<PricePerDay> pricePerDaysS = sencod.getPricePerDays();
		PricePerDay flast = pricePerDaysF.get(pricePerDaysF.size() - 1);
		PricePerDay slast = pricePerDaysS.get(pricePerDaysS.size() - 1);
		if (flast.getPrice().compareTo(slast.getPrice()) > 0)
			return sencod;
		return first;
	}

	/**
	 * @description 获得历史最低价格
	 * @param priceHistoryDTOs
	 * @param range
	 * @return
	 * @author dadian
	 * @since 2014-1-7
	 * @email dadian@b5m.cn
	 */
	public static PriceTrend createPriceTrendLow(List<PriceTrend> priceHistoryDTOs, int range) {
		PriceTrend lowestPrice = new PriceTrend();
		Map<String, PricePerDay> lowestPricePerDayMap = convertDatePriceMap(lowestPrice.getPricePerDays());
		Date now = DateTools.now();
		for (PriceTrend priceTrend : priceHistoryDTOs) {
			Map<String, PricePerDay> pricePerDayMap = convertDatePriceMap(priceTrend.getPricePerDays());
			PricePerDay beforePrice = priceTrend.getPricePerDays().get(0);
			for (int i = range; i >= 0; i--) {
				Date indexDate = DateTools.addDay(now, -i);
				PricePerDay perDay = getPricePerDay(pricePerDayMap, indexDate);
				if (perDay == null) {
					perDay = new PricePerDay();
					perDay.setPrice(beforePrice.getPrice());
					perDay.setDate(indexDate);
					pricePerDayMap.put(DateTools.formate(indexDate), perDay);
				} else {
					beforePrice = perDay;
				}
				PricePerDay lowestPerDay = getPricePerDay(lowestPricePerDayMap, indexDate);
				if (lowestPerDay == null) {
					lowestPerDay = perDay;
					lowestPrice.addPricePerDay(lowestPerDay);
				} else {
					if (lowestPerDay.getPrice().compareTo(perDay.getPrice()) == 1) {
						lowestPrice.removePricePerDay(lowestPerDay);
						lowestPerDay = perDay;
						lowestPrice.addPricePerDay(lowestPerDay);
					}
				}
				lowestPricePerDayMap.put(DateTools.formate(indexDate), lowestPerDay);
			}
		}
		Collections.sort(lowestPrice.getPricePerDays(), new Comparator<PricePerDay>() {

			@Override
			public int compare(PricePerDay o1, PricePerDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}

		});
		return lowestPrice;
	}

	public static JSONObject convert(PriceTrend priceHistoryDTO, String source) {
		return convert(priceHistoryDTO, source, false);
	}

	public static JSONObject convert(PriceTrend priceHistoryDTO, String source, boolean isFormate) {
		JSONObject jsonObject = new JSONObject();
		JSONArray priceArray = new JSONArray();
		if (priceHistoryDTO == null) {
			return jsonObject;
		}
		for (PricePerDay pricePerDay : priceHistoryDTO.getPricePerDays()) {
			JSONObject price = new JSONObject();
			price.put("price", pricePerDay.getPrice());
			if (isFormate) {
				price.put("time", DateTools.formate(pricePerDay.getDate()));
			} else {
				price.put("time", pricePerDay.getDate().getTime());
			}
			priceArray.add(price);
		}
		jsonObject.put("prices", priceArray);
		jsonObject.put("site", source);
		return jsonObject;
	}

	public static JSONArray convert(JSONArray jsonArray, String formate) {
		JSONArray priceArray = new JSONArray();
		if (formate == null)
			formate = "MM";
		if (!CollectionUtils.isEmpty(jsonArray)) {
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject obj = jsonArray.getJSONObject(i);
				JSONObject rs = new JSONObject();
				rs.put("price", obj.getString("price"));
				rs.put("sales", 0);
				rs.put("month", DateTools.formate(new Date(obj.getLong("time")), formate));
				priceArray.add(rs);
			}
		}
		return priceArray;
	}

	/**
	 * @description 获取前一个不同的价格。为了判断最后一个价格跟前一个不同的价格进行比较 得出价格趋势类型
	 * @param pricePerDays
	 * @param p1Price
	 * @return
	 * @author echo
	 * @since 2013-9-24
	 * @email echo.weng@b5m.com
	 */
	protected static BigDecimal getPrePrice(List<PricePerDay> pricePerDays, BigDecimal p1Price) {
		int length = pricePerDays.size();
		for (int i = length - 1; i >= 0; i--) {
			BigDecimal p2Price = pricePerDays.get(i).getPrice().setScale(0, RoundingMode.DOWN);
			if (p1Price.compareTo(p2Price) != 0)
				return p2Price;
		}
		return p1Price;
	}

	public static void consistPriceType(PriceTrend priceHistoryDTO, Integer priceType, StringBuffer type) {
		if (priceType.equals(ERROR))
			return;
		if (type.length() > 0) {
			type.append(";");
		}
		type.append(priceHistoryDTO.getDocId()).append(",").append(priceType);
	}

	/**
	 * @description 填充首尾价格
	 * @param range
	 * @param priceHistoryDTOs
	 * @author echo
	 * @since 2013-9-25
	 * @email echo.weng@b5m.com
	 */
	public static void dealWithPrice(Integer range, List<PriceTrend> priceHistoryDTOs) {
		int length = priceHistoryDTOs.size();
		for (int index = 0; index < length; index++) {
			if (priceHistoryDTOs.get(index) == null) {
				priceHistoryDTOs.remove(index);
				continue;
			}
			dealWithPrice(range, priceHistoryDTOs.get(index));
		}
	}

	/**
	 * @description 价格处理
	 * @param range
	 * @param priceHistoryDTO
	 * @author echo
	 * @since 2013-9-23
	 * @email echo.weng@b5m.com
	 */
	public static void dealWithPrice(Integer range, PriceTrend priceHistoryDTO) {
		List<PricePerDay> pricePerDays = priceHistoryDTO.getPricePerDays();
		if (pricePerDays.isEmpty())
			return;
		Collections.sort(pricePerDays, new Comparator<PricePerDay>() {

			@Override
			public int compare(PricePerDay o1, PricePerDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}

		});
		Date firstDate = DateTools.addDay(DateTools.now(), -range);
		// 将日期改成查询的第一天
		PricePerDay first = pricePerDays.get(0);
		PricePerDay last = pricePerDays.get(pricePerDays.size() - 1);
		if (!DateTools.formate(first.getDate()).equals(DateTools.formate(firstDate))) {
			pricePerDays.add(0, createPricePerDay(firstDate, first.getPrice()));
		}
		if (!DateTools.formate(last.getDate()).equals(DateTools.formate(DateTools.now()))) {
			pricePerDays.add(createPricePerDay(DateTools.now(), last.getPrice()));
		}
	}

	private static PricePerDay createPricePerDay(Date date, BigDecimal price) {
		PricePerDay perDay = new PricePerDay();
		perDay.setDate(date);
		perDay.setPrice(price);
		return perDay;
	}

	public Integer getDATA_SOURCE() {
		return DATA_SOURCE;
	}

	public void setDATA_SOURCE(Integer dATA_SOURCE) {
		DATA_SOURCE = dATA_SOURCE;
	}

	public void setHbaseDataService(HbaseDataService hbaseDataService) {
		PriceTrendUtils.hbaseDataService = hbaseDataService;
	}
}
