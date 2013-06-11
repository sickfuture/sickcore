package com.android.sickfuture.sickcore.utils;

import android.app.Activity;
import android.widget.Toast;

import com.android.sickfuture.sickcore.R;
import com.android.sickfuture.sickcore.http.HttpManager;

public class InetChecker {

	private InetChecker() {
	}

	public static boolean checkInetConnection(Activity activity,
			boolean... showToast) {
		if (!HttpManager.getInstance(activity).isAvalibleInetConnection()) {
			if (showToast.length != 0) {
				if (showToast[0])
					Toast.makeText(activity,
							R.string.internet_connection_is_not_avalible,
							Toast.LENGTH_LONG).show();
			}
			return false;
		} else
			return true;
	}
}
