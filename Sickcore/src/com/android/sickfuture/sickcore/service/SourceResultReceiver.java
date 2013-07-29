package com.android.sickfuture.sickcore.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public abstract class SourceResultReceiver extends ResultReceiver {

	public static final String ERROR_KEY = "framework:errorKey";
	public static final String RESULT_KEY = "framework:resultKey";

	public static enum Status {
		UPDATING, ERROR, DONE, CACHED
	}

	public SourceResultReceiver(Handler handler) {
		super(handler);
	}

	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		Status status = Status.values()[resultCode];
		switch (status) {
		case UPDATING:
			onStart(resultData);
			break;
		case ERROR:
			onError((Exception) resultData.getSerializable(ERROR_KEY));
			break;
		case DONE:
			onDone(resultData);
			break;
		case CACHED:
			onCached(resultData);
			break;
		default:
			break;
		}
		super.onReceiveResult(resultCode, resultData);
	}

	protected void onCached(Bundle result) {

	}

	/**
	 * Invoking when process starting.
	 * 
	 * @param result
	 *            Result of started work.
	 * */
	public abstract void onStart(Bundle result);

	/**
	 * Invoking when process finished with errors.
	 * 
	 * @param exception
	 *            Current exception.
	 * */
	public abstract void onError(Exception exception);

	/**
	 * Invoking when process finished successfully.
	 * 
	 * @param result
	 *            Result of finished work.
	 * */
	public abstract void onDone(Bundle result);

}
