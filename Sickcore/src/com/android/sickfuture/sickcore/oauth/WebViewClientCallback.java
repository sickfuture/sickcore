package com.android.sickfuture.sickcore.oauth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.webkit.WebView;

public interface WebViewClientCallback {

	void onPageStarted(WebView view, String url, Bitmap favicon);

	void onPageFinished(WebView view, String url, Intent intent);

}
