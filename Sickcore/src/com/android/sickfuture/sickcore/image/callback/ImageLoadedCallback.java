package com.android.sickfuture.sickcore.image.callback;

public interface ImageLoadedCallback {

	void onLoadStarted(String uri);

	void onLoadError(Throwable e);

	void onLoadFinished(Object result);

}
