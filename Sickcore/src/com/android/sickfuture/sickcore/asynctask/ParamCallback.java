package com.android.sickfuture.sickcore.asynctask;

public interface ParamCallback<C> {

	void onSuccess(C c);

	void onError(Throwable e);

}
