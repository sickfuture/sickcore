package com.android.sickfuture.sickcore.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.res.Resources;
import android.util.TypedValue;

public class Converter {

	private static final String LOG_TAG = Converter.class.getSimpleName();

	private Converter() {
	}

	public static long dateTimeToUnix(String dateTime) {
		try {
			String format = "yyyy-MM-dd'T'HH:mm:ssZ";
			SimpleDateFormat sdf = new SimpleDateFormat(format,
					Locale.getDefault());
			long unix = sdf.parse(dateTime).getTime();
			unix /= 1000;
			return unix;
		} catch (ParseException e) {
			L.e(LOG_TAG, "dateTimeToUnix: " + e.toString());
		}
		return -1;
	}

	public static String unixTimeToDateTimeString(long unixTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm",
				Locale.getDefault());
		GregorianCalendar calendar = new GregorianCalendar(
				TimeZone.getDefault());
		calendar.setTimeInMillis(unixTime * 1000);
		return sdf.format(calendar.getTime());
	}

	public static String stringToMD5(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	public static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	public static float dpToPx(Resources r, int dp) {
		if (r == null) {
			return 0f;
		}
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				r.getDisplayMetrics());
	}

}
