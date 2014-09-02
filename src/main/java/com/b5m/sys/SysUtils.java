package com.b5m.sys;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class SysUtils {
	private static Map<String, Class<?>> PARAMS_MAP = new HashMap<String, Class<?>>();
	static{
		PARAMS_MAP.put("Long", Long.class);
		PARAMS_MAP.put("long", Long.class);
		PARAMS_MAP.put("int", Integer.class);
		PARAMS_MAP.put("Integer", Integer.class);
		PARAMS_MAP.put("String", String.class);
		PARAMS_MAP.put("boolean", Boolean.class);
		PARAMS_MAP.put("Boolean", Boolean.class);
		PARAMS_MAP.put("class", Class.class);
		PARAMS_MAP.put("Object[]", Object[].class);
	}
	
	public static Method getMethod(Class<?> cls, String methodName, String paramTypes){
		Class<?>[] paramsCls = null; 
		if(StringUtils.isEmpty(paramTypes)){
			paramsCls = new Class<?>[0];
		}else{
			String[] types = StringUtils.split(paramTypes, ",");
			paramsCls = new Class<?>[types.length];
			for(int index = 0; index < types.length; index++){
				Class<?> _cls = PARAMS_MAP.get(types[index]);
				if(_cls == null) return null;
				paramsCls[index] = _cls;
			}
		}
		try {
			return cls.getMethod(methodName, paramsCls);
		} catch (SecurityException e) {
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
	
	public static Object[] getValues(String params, String paramTypes){
		if(StringUtils.isEmpty(params) && StringUtils.isEmpty(paramTypes)) return new Object[]{};
		if(StringUtils.isEmpty(params)) return null;
		if(StringUtils.isEmpty(paramTypes)) return null;
		String[] values = params.split(",");
		String[] keys = paramTypes.split(",");
		if(values.length != keys.length) return null;
		Object[] _values = new Object[values.length];
		for (int i = 0; i < keys.length; i++) {
			if(StringUtils.isEmpty(values[i])){
				_values[i] = null;
				continue;
			}
			if("Long".equals(keys[i]) || "long".equals(keys[i])){
				_values[i] = Long.valueOf(values[i]);
			}else if("int".equals(keys[i]) || "Integer".equals(keys[i])){
				_values[i] = Integer.valueOf(values[i]);
			}else if("String".equals(keys[i])){
				_values[i] = values[i];
			}else if("Boolean".equals(keys[i]) || "boolean".equals(keys[i])){
				_values[i] = Long.valueOf(values[i]);
			}else if("class".equals(keys[i])){
				try {
					_values[i] = Class.forName(values[i]);
				} catch (ClassNotFoundException e) {
					return null;
				}
			}else if("Object[]".equals(keys[i])){
				_values[i] = StringUtils.split(values[i], "@");
			}else{
				return null;
			}
		}
		return _values;
	}
	
	public static void main(String[] args) {
		System.out.println("ajfkv||dkjvklfd".split("\\|").length);
	}
}
