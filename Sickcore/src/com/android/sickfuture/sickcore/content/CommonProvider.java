package com.android.sickfuture.sickcore.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.android.sickfuture.sickcore.utils.ContractUtils;

public abstract class CommonProvider extends ContentProvider {

	public static final String LOG_TAG = CommonProvider.class.getSimpleName();
	private CommonDataBase mHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int result = mHelper.deleteItems(getContractClass(), selection,
				selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}

	@Override
	public String getType(Uri uri) {
		return ContractUtils.getConentTypeFromContract(getContractClass());
	}

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) throws SQLException {
		int numInserted = 0;
		numInserted = mHelper.addItems(getContractClass(), values);
		if (numInserted > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return numInserted;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) throws SQLException {
		long itemID = mHelper.addItem(getContractClass(), values);
		Uri itemUri = Uri.parse(uri + "/" + itemID);
		if (itemID > 0) {
			getContext().getContentResolver().notifyChange(itemUri, null);
		}
		return itemUri;
	}

	@Override
	public boolean onCreate() {
		mHelper = new CommonDataBase(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor items = mHelper.getItems(getContractClass(), sortOrder,
				selection, selectionArgs);
		items.setNotificationUri(getContext().getContentResolver(), uri);
		return items;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int valueId = 0;
		valueId = mHelper.update(getContractClass(), values, selection,
				selectionArgs);
		if (valueId > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return valueId;
	}

	protected Cursor rawQuery(Class<?> contract, Uri uri, String sql,
			String[] selectionArgs) {
		Cursor items = mHelper.rawQuery(contract, sql, selectionArgs);
		items.setNotificationUri(getContext().getContentResolver(), uri);
		return items;
	}

	protected abstract Class<?> getContractClass();

}
