package com.android.sickfuture.sickcore.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentValues;

public class StringsUtils {

	public static <T> String join(List<T> items, String joiner) {
		String ids = "";
		for (int i = 0; i < items.size(); i++) {
			ids += String.valueOf(items.get(i)) + joiner;
		}
		return ids.substring(0, ids.length() - joiner.length());
	}
	
	public static <T> String join(HashSet<T> items, String joiner) {
		String ids = "";
		for (T t : items) {
			ids += String.valueOf(t) + joiner;
		}
		return ids.substring(0, ids.length() - joiner.length());
	}

	public static String join(HashMap<?, ?> items, String joiner) {
		String keys = "";
		String[] k = new String[items.size()];
		items.keySet().toArray(k);
		for (int i = 0; i < k.length; i++) {
			keys += k[i] + joiner;
		}
		return keys.substring(0, keys.length() - joiner.length());
	}

	public static String join(List<ContentValues> items, String itemKey,
			String joiner) {
		String keys = "";
		for (int i = 0; i < items.size(); i++) {
			keys += items.get(i).getAsString(itemKey) + joiner;
		}
		return keys.substring(0, keys.length() - joiner.length());
	}
}
