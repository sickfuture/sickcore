package com.android.sickfuture.sickcore.utils;

import java.util.HashMap;
import java.util.List;

public class JoinStringsUtils {

	public static <T> String getJoinToString(List<T> items, String joiner) {
		String ids = "";
		for (int i = 0; i < items.size(); i++) {
			ids += String.valueOf(items.get(i)) + joiner;
		}
		return ids.substring(0, ids.length() - 1);
	}
	
	public static String getJoinToString(HashMap<?, ?> items, String joiner) {
		String keys = "";
		String[] k = new String[items.size()];
		items.keySet().toArray(k);
		for (int i = 0; i < k.length; i++) {
			keys += k[i] + joiner;
		}
		return keys.substring(0, keys.length() - 1);
	}
	
}
