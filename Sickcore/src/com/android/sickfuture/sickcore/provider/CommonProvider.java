package com.android.sickfuture.sickcore.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.android.sickfuture.sickcore.db.CommonDataBase;
import com.android.sickfuture.sickcore.utils.ContractUtils;

public abstract class CommonProvider extends ContentProvider {

	private CommonDataBase mHelper;

	protected CommonProvider() {
	}

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
	public int bulkInsert(Uri uri, ContentValues[] values) {
		int numInserted = 0;
		mHelper = CommonDataBase.getInstance(getContext());
		for (ContentValues value : values) {
			long itemID = mHelper.addItem(getContractClass(), value);
			if (itemID < 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			numInserted++;
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return numInserted;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		mHelper = CommonDataBase.getInstance(getContext());
		long itemID = mHelper.addItem(getContractClass(), values);
		Uri itemUri = Uri.parse(uri + "/" + itemID);
		getContext().getContentResolver().notifyChange(itemUri, null);
		return itemUri;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		mHelper = CommonDataBase.getInstance(getContext());
		Cursor items = mHelper.getItems(getContractClass(), sortOrder,
				selection, selectionArgs);
		items.setNotificationUri(getContext().getContentResolver(), uri);
		return items;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	protected abstract Class<?> getContractClass();

}
