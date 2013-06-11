package com.android.sickfuture.sickcore.oauth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.util.Log;

public class AuthHelperFactory {

	private static final String LOG_TAG = "AuthHelperFactory";

	private Map<String, BaseAuthHelper> mHelpers;

	private static volatile AuthHelperFactory instance;

	private AuthHelperFactory() {
		mHelpers = Collections
				.synchronizedMap(new HashMap<String, BaseAuthHelper>());
	}

	/** Double Checked Locking Singleton & volatile */
	public static AuthHelperFactory getInstance() {
		AuthHelperFactory localInstance = instance;
		if (localInstance == null) {
			synchronized (AuthHelperFactory.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new AuthHelperFactory();
				}
			}
		}
		return localInstance;
	}

	@SuppressLint("DefaultLocale")
	public BaseAuthHelper getHelper(String key) {
		String lowerCaseKey = key.toLowerCase(Locale.getDefault());
		Log.d(LOG_TAG, lowerCaseKey);
		BaseAuthHelper helper = mHelpers.get(lowerCaseKey);
		if (helper == null) {
			helper = createHelper(lowerCaseKey);
		}
		return helper;
	}

	private BaseAuthHelper createHelper(String key) {
		BaseAuthHelper helper = new BaseAuthHelper(key);
		mHelpers.put(key, helper);
		return helper;
	}

}
