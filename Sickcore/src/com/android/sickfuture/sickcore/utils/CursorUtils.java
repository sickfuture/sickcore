package com.android.sickfuture.sickcore.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class CursorUtils {

	private static final String LOG_TAG = CursorUtils.class.getSimpleName();

	private static final String ILLEGAL_ACCESS_TO_CONTRACT_S_FIELD = "Illegal access to Contract's field";
	private static final String CONTRACT_FIELD_IS_NOT_INSTANCE_OF_STRING = "Contract field is not instance of String.";
	private static final String CONTRACT_CLASS_IS_NULL = "Contract class is null.";
	private static final String KEYS_ARRAY_LENGTH_UNEQUAL_TO_VALUES_ARRAY_LENGTH = "Keys array length unequal to values array length.";

	public static void logCursor(Cursor cursor, Class<?> contract) {

		if (cursor == null)
			return;
		if (contract == null)
			L.d(LOG_TAG, CONTRACT_CLASS_IS_NULL);
		Field[] fields = contract.getFields();
		if (cursor.getCount() > 0 && cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				for (Field field : fields) {
					String contractItem;
					try {
						contractItem = (String) field.get(null);
						String value = cursor.getString(cursor
								.getColumnIndex(contractItem));
						L.d(LOG_TAG, contractItem + " --- " + value);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(
								CONTRACT_FIELD_IS_NOT_INSTANCE_OF_STRING);
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException(
								ILLEGAL_ACCESS_TO_CONTRACT_S_FIELD);
					}

				}
				cursor.moveToNext();
			}
		}
	}

}
