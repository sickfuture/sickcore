package com.android.sickfuture.sickcore.utils;

import android.util.Log;

import com.android.sickfuture.sickcore.BuildConfig;

public class L {

	public static void d(String tag, String msg) {
		if (BuildConfig.DEBUG) {
			if (tag == null || msg == null) {
				return;
			}
			Log.d(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (tag == null || msg == null) {
			return;
		}
		Log.e(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (tag == null || msg == null) {
			return;
		}
		Log.w(tag, msg);
	}

	public static void i(String tag, String msg) {
		if (tag == null || msg == null) {
			return;
		}
		Log.i(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (tag == null || msg == null) {
			return;
		}
		Log.v(tag, msg);
	}
}
