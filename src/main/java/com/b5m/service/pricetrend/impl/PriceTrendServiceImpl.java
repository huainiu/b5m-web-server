package com.b5m.service.pricetrend.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.alibaba.dubbo.config.annotation.Service;
import com.b5m.base.common.utils.DateTools;
import com.b5m.service.hbase.bean.PricePerDay;
import com.b5m.service.hbase.bean.PriceTrend;
import com.b5m.service.pricetrend.PriceTrendService;
import com.b5m.service.pricetrend.PriceTrendUtils;
/**
 * @description
 * 价格趋势服务 
 * @author echo
 * @time 2014年7月31日
 * @mail wuming@b5m.com
 */
@Service(version = "0.0.1", filter = {"b5mcache"})
public class PriceTrendServiceImpl implements PriceTrendService{

	@Override
	public String priceTrendTyp(String[] docIds, Integer range) {
		return PriceTrendUtils.getPriceTrendType(docIds, range);
	}

	@Override
	public PriceTrend singlePriceTrend(Integer range, String docId, boolean fill, String price) {
		List<PriceTrend> priceHistoryDTOs = PriceTrendUtils.getPriceHistory(range, false, docId);
		PriceTrend priceTrend = null;
		if (!CollectionUtils.isEmpty(priceHistoryDTOs)) {
			priceTrend = priceHistoryDTOs.get(0);
		}
		if (priceTrend == null) {
			if (!StringUtils.isEmpty(price)) {
				priceTrend = new PriceTrend(docId, price, DateTools.now());
			} else {
				return null;
			}
		}
		List<PricePerDay> pricePerDays = priceTrend.getPricePerDays();
		if (fill) {
			PriceTrendUtils.fillEveryDatePrice(priceTrend, range);
		}
		Collections.sort(pricePerDays, new Comparator<PricePerDay>() {
			@Override
			public int compare(PricePerDay o1, PricePerDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		if (!StringUtils.isEmpty(price)) {
			int size = pricePerDays.size();
			if (DateTools.toMidnightDate(pricePerDays.get(size - 1).getDate()).compareTo(DateTools.getMidnightToday()) >= 0) {
				pricePerDays.set(size - 1, new PricePerDay(DateTools.now(), price));
			} else {
				pricePerDays.add(new PricePerDay(DateTools.now(), price));
			}
		}
		return priceTrend;
	}

}
