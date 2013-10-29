package com.android.sickfuture.sickcore.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferencesHelper {

	public static final String DEFVALUE_STRING = null;
	public static final int DEFVALUE_INT = -1;
	public static final boolean DEFVALUE_BOOLEAN = false;

	public static void putString(Context context, int mode, String name,
			String key, String value) {
		Editor editor = context.getSharedPreferences(name, mode).edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static String getString(Context context, int mode, String name,
			String key) {
		SharedPreferences pref = context.getSharedPreferences(name, mode);
		return pref.getString(key, DEFVALUE_STRING);
	}

	public static void putInt(Context context, int mode, String name,
			String key, int value) {
		Editor editor = context.getSharedPreferences(name, mode).edit();
		editor.putInt(key, value);
		editor.commit();
	}
	
	/**
	 * Preferences with MODE_PRIVATE
	 */
	public static void putString(Context context, String name, String key,
			String value) {
		putString(context, Context.MODE_PRIVATE, name, key, value);
	}

	/**
	 * Preferences with MODE_PRIVATE
	 */
	public static String getString(Context context, String name, String key) {
		return getString(context, Context.MODE_PRIVATE, name, key);
	}

	/**
	 * Preferences with MODE_PRIVATE
	 * 
	 * @param context
	 * @param name
	 * @param key
	 * @param value
	 */
	public static void putInt(Context context, String name, String key,
			int value) {
		putInt(context, Context.MODE_PRIVATE, name, key, value);
	}

	public static int getInt(Context context, int mode, String name, String key) {
		SharedPreferences pref = context.getSharedPreferences(name, mode);
		return pref.getInt(key, DEFVALUE_INT);
	}

	/**
	 * Preferences with MODE_PRIVATE
	 * 
	 * @param context
	 * @param name
	 * @param key
	 * @return
	 */
	public static int getInt(Context context, String name, String key) {
		return getInt(context, Context.MODE_PRIVATE, name, key);
	}

	public static void putBoolean(Context context, int mode, String name,
			String key, boolean value) {
		Editor editor = context.getSharedPreferences(name, mode).edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public static boolean getBoolean(Context context, int mode, String name,
			String key) {
		SharedPreferences pref = context.getSharedPreferences(name, mode);
		return pref.getBoolean(key, DEFVALUE_BOOLEAN);
	}

	/**
	 * Preferences with MODE_PRIVATE
	 */
	public static void putBoolean(Context context, String name, String key,
			boolean value) {
		putBoolean(context, Context.MODE_PRIVATE, name, key, value);
	}

	/**
	 * Preferences with MODE_PRIVATE
	 */
	public static boolean getBoolean(Context context, String name, String key) {
		return getBoolean(context, Context.MODE_PRIVATE, name, key);
	}

	public static void clear(Context context, int mode, String name) {
		Editor editor = context.getSharedPreferences(name, mode).edit();
		editor.clear();
		editor.commit();
	}

}
