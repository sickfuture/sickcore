package com.android.sickfuture.sickcore.app;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.android.sickfuture.sickcore.utils.AppUtils;

public class AppHelper {

	public static final String SYSTEM_SERVICE_KEY = "sickcore:apphelper";

	private Map<String, IAppServiceKey> mAppService = new HashMap<String, IAppServiceKey>();

	public static AppHelper get(Context context) {
		return (AppHelper) AppUtils.get(context, SYSTEM_SERVICE_KEY);
	}

	public static interface IAppServiceKey {

		String getKey();

	}

	protected Object getSystemService(String key) {
		if (key.equals(SYSTEM_SERVICE_KEY)) {
			return this;
		}
		if (mAppService.containsKey(key)) {
			return mAppService.get(key);
		}
		return null;
	}

	public void registerAppService(IAppServiceKey appService) {
		if (mAppService.containsKey(appService.getKey())) {
			return;
		}
		mAppService.put(appService.getKey(), appService);
	}

}
