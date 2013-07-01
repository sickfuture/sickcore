package com.android.sickfuture.sickcore.asynctask;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

public abstract class CommonTask<D,T> extends AsyncTask<D, Void, T> {

	private ParamCallback<T> mParamCallback;

	public CommonTask(ParamCallback<T> paramCallback) {
		super();
		this.mParamCallback = paramCallback;
	}

	public abstract Object load(D d);

	private Exception e = null;

	@Override
	protected T doInBackground(D... params) {
		try {
			Object source = load(params[0]);
		} catch (Exception e) {
			this.e = e;
		}
		return null;
	}

	@Override
	protected void onPostExecute(T result) {
		if (this.e != null) {
			mParamCallback.onError(e);
		} else {
			mParamCallback.onSuccess(result);
		}
	}

	@SuppressLint({ "NewApi" })
	public void start(D... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			execute(params);
		}
	}

	public void setException(Exception e) {
		this.e = e;
	}
}
