package com.android.sickfuture.sickcore.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.android.sickfuture.sickcore.asynctask.ParamCallback;

//TODO fullrefactoring after discussion
public abstract class CommonService<T> extends Service implements
		ParamCallback<T> {

	protected static final String PROVIDER_INSERT_ERROR_MESSAGE = "Can't insert items into provider with uri";

	public static String DATA = "data";

	public static String ACTION_ON_SUCCESS = "com.epam.android.framework.ACTION_ON_SUCCESS";

	public static String ACTION_ON_ERROR = "com.epam.android.framework.ACTION_ON_ERROR";

	public static final String EXTRA_KEY_MESSAGE = "error message";

	public static final String LOG_TAG = "CommonService";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		task(intent);
		return getStartMode();
	}

	@Override
	public void onSuccess(final T t) {
		if (t != null) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					callbackOnSuccess(t);
				}

			}).start();
		}
	}

	@Override
	public void onError(Throwable e) {
		callbackOnError(e);
	}

	protected abstract int getStartMode();
	
	protected abstract void task(Intent intent);

	protected abstract Uri getProviderUri();

	protected abstract void callbackOnSuccess(final T t);

	protected abstract void callbackOnError(Throwable e);

}
