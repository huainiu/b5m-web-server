package com.b5m.sys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.b5m.base.common.utils.BeanTools;
import com.b5m.base.common.utils.DateTools;

/**
 * @description
 * 初始化上下文
 * @author echo
 * @time 2014年6月9日
 * @mail wuming@b5m.com
 */
public class ContextUtils {
	private static Properties properties;
	private static String[] collections = new String[]{"b5mo", "hotel", "ticketp", "tourguide", "tourp", "tuanm", "zdm", "she", "haiwaip", "haiwaiinfo", "guang", "doctor", "usa", "korea", "koreainfo", "taosha"};
	
	public static void init(String xmlConfig, Properties properties){
		SearchContext.getInstance();
		setVersion();
		setChannels(xmlConfig);
		ContextUtils.properties = properties;
	}
	
	public static void setChannels(String xmlConfig){
		List<Channel> channels = BeanTools.xmlToObject(xmlConfig, Channel.class);
		Map<String, Channel> channelsMap = new HashMap<String, Channel>();
		for(Channel channel : channels){
			channelsMap.put(channel.getName(), channel);
		}
		SearchContext.getInstance().setChannels(channelsMap);
	}
	
	public static void setVersion(){
		SearchContext.getInstance().setVersion(DateTools.formate(DateTools.now(), "yyyyMMddHHmmss"));
	}
	
	public static String getVersion(){
		return SearchContext.getInstance().getVersion();
	}
	
	public static Channel getChannel(String channel){
		return SearchContext.getInstance().getChannel(channel);
	}
	
	public static boolean isTaoshaChannel(String channel){
		return "taosha".equals(channel);
	}
	
	public static String getProp(String name){
		return properties.getProperty(name);
	}
	
	public static Integer getIntProp(String key){
        Object o = properties.get(key);
        if(null == o) return null;
        return Integer.parseInt(o.toString());
    }
    
    public static Long getLongProp(String key){
    	Object o = properties.get(key);
        if(null == o) return null;
        return Long.parseLong(o.toString());
    }
    
}
