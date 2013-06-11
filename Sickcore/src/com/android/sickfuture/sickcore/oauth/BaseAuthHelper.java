package com.android.sickfuture.sickcore.oauth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.android.sickfuture.sickcore.preference.PreferencesHelper;

public class BaseAuthHelper {

	public static final String PREF_KEY_TOKEN = "token";
	public static final int PREF_MODE = Context.MODE_PRIVATE;
	public static final String KEY_ACCESS_TOKEN = "access_token";
	private static final String LOG_TAG = "BaseAuthHelper";

	private String mName;

	protected BaseAuthHelper(String name) {
		mName = name;
	}

	public void saveToken(Context context, String access_token) {
		PreferencesHelper.putString(context, PREF_MODE, mName, PREF_KEY_TOKEN,
				access_token);
	}

	public String getToken(Context context) {
		return PreferencesHelper.getString(context, PREF_MODE, mName,
				PREF_KEY_TOKEN);
	}

	public void deleteToken(Context context) {
		PreferencesHelper.clear(context, PREF_MODE, mName);
	}

	public void exit(Context context) {
		try {
			CookieSyncManager cookieSyncMngr = CookieSyncManager
					.createInstance(context);
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookie();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
		deleteToken(context);
	}

	public boolean isRedirect(String url, String redirectUrl) {
		return url.startsWith(redirectUrl);
	}

	public String processUrl(String url, String redirectUrl) {
		if (isRedirect(url, redirectUrl)) {
			Uri parseUrl = Uri.parse(url.replace("#", "?"));
			String token = parseUrl.getQueryParameter(KEY_ACCESS_TOKEN);
			if (token == null) {
				return null;
			}
			return token;
		}
		return null;
	}

	public WebViewClient getWebClient(WebView webView, ProgressBar progressBar,
			Context context, Intent intent, final WebViewClientCallback callback) {
		return new BaseWebWiewClient(webView, progressBar, context, intent) {

			@Override
			void pageStarted(WebView view, String url, Bitmap favicon) {
				callback.onPageStarted(view, url, favicon);
			}

			@Override
			void pageFinished(WebView view, String url, Intent intent) {
				callback.onPageFinished(view, url, intent);
			}

		};
	}

	public static void toogleView() {
		BaseWebWiewClient.toogleView();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private abstract static class BaseWebWiewClient extends WebViewClient {

		private static WebView mWebView;
		private static ProgressBar mProgressBar;

		// private Context mContext;
		private static Intent mIntent;

		public BaseWebWiewClient(WebView webView, ProgressBar progressBar,
				Context context, Intent intent) {
			mWebView = webView;
			mProgressBar = progressBar;
			// mContext = context;
			mIntent = intent;

			mWebView.getSettings().setJavaScriptEnabled(true);
			mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(
					true);

			mWebView.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.GONE);
		}

		private static void toogleView() {
			if (mProgressBar.getVisibility() == View.VISIBLE) {
				mProgressBar.setVisibility(View.GONE);
				mWebView.setVisibility(View.VISIBLE);
			} else {
				mProgressBar.setVisibility(View.VISIBLE);
				mWebView.setVisibility(View.GONE);
			}
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			// toogleView();
			pageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			pageFinished(view, url, mIntent);
			// toogleView();
			// String token = processUrl(url);
			// if (token != null) {
			// toogleView();
			// saveToken(mContext, token);
			// mContext.startActivity(mIntent
			// .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			// }
		}

		abstract void pageStarted(WebView view, String url, Bitmap favicon);

		abstract void pageFinished(WebView view, String url, Intent intent);
	}

}
