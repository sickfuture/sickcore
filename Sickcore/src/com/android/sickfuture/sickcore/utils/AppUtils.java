package com.android.sickfuture.sickcore.utils;

import android.content.Context;

public class AppUtils {

	public static Object get(Context context, String key) {
		Object service = context.getSystemService(key);
		if (service == null) {
			context = context.getApplicationContext();
			service = context.getSystemService(key);
		}
		if (service == null) {
			throw new IllegalArgumentException("Service " + key
					+ " is not availible!");
		}
		return service;
	}

}
