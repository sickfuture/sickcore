package com.android.sickfuture.sickcore.app;

import android.app.Application;

import com.android.sickfuture.sickcore.app.AppHelper.IAppServiceKey;
import com.android.sickfuture.sickcore.context.ContextHolder;

public abstract class SickApp extends Application {

	private AppHelper mHelper;

	public static final String IMAGE_LOADER_SERVICE = "sickcore:imageloader";
	public static final String HTTP_INPUT_STREAM_SERVICE_KEY = "sickcore:httpinputstreamdatasource";

	@Override
	public void onCreate() {
		mHelper = new AppHelper();
		ContextHolder.getInstance().setContext(this);
		register();
		super.onCreate();
	}

	public void registerAppService(IAppServiceKey appService) {
		mHelper.registerAppService(appService);
	}

	@Override
	public Object getSystemService(String key) {
		Object systemService = mHelper.getSystemService(key);
		if (systemService != null) {
			return systemService;
		}
		return super.getSystemService(key);
	}

	public abstract void register();

}
