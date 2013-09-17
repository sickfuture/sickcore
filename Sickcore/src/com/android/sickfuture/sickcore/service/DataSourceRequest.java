package com.android.sickfuture.sickcore.service;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.TextUtils;

import com.android.sickfuture.sickcore.content.contract.CoreContracts;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;
import com.android.sickfuture.sickcore.service.SourceResultReceiver.Status;
import com.android.sickfuture.sickcore.source.IDataSource;
import com.android.sickfuture.sickcore.source.IProcessor;
import com.android.sickfuture.sickcore.utils.AppUtils;
import com.android.sickfuture.sickcore.utils.ContractUtils;
import com.android.sickfuture.sickcore.utils.L;

public class DataSourceRequest<DataSource, Result> {

	private static final String LOG_TAG = DataSourceRequest.class
			.getSimpleName();

	private static final String REQUEST_URI = "request_uri";
	private static final String DATA_SOURCE_REQUEST_PARAMS = "DataSourceRequestParams";
	private static final String REQUEST_CACHEABLE = "requestCacheble";

	private static final long NO_VALUE = -1l;

	private static final String CACHE_EXPIRATION = "cacheExpiration";

	private static final String FORCE_UPDATE = "forceUpdate";

	private Bundle mBundle;

	/**
	 * Creating new instance of DataSourceRequest and sets default params
	 * (isCacheable = false && isForceUpdate = true).
	 * 
	 * @param uri
	 *            Request uri string.
	 */
	public DataSourceRequest(String uri) {
		mBundle = new Bundle();
		mBundle.putString(REQUEST_URI, uri);
		setForceUpdate(true); // Default value for force update data
		setIsCacheable(true); // default value for isCacheble value
	}

	protected DataSourceRequest() {
		mBundle = new Bundle();
	}

	@SuppressWarnings("unchecked")
	protected void execute(Context context, String dataSourceKey,
			String processorKey, ResultReceiver resultReceiver) {
		if (isCacheable() && !isForceUpdate()) {
			Uri uri = ContractUtils
					.getProviderUriFromContract(CoreContracts.Request.class);
			Cursor c = null;
			try {
				c = context.getContentResolver().query(uri, null,
						CoreContracts.Request.URI + " = ?",
						new String[] { getUri() }, null);
				if (c == null || !c.moveToFirst()) {
					context.getContentResolver().insert(uri,
							CoreContracts.Request.prepareRequest(this));
				} else {
					Long lastUpdate = c
							.getLong(c
									.getColumnIndex(CoreContracts.Request.LAST_UPDATE));
					if (System.currentTimeMillis() - getExpiration() < lastUpdate) {
						L.d(LOG_TAG,
								"expiration time is not past yet! Not refresing"
										+ System.currentTimeMillis());
						sendStatus(Status.CACHED, resultReceiver, mBundle);
						sendStatus(Status.DONE, resultReceiver, mBundle);
						return;
					} else {
						L.d(LOG_TAG,
								"expiration time past! REFRESING"
										+ System.currentTimeMillis());
						context.getContentResolver()
								.insert(uri,
										CoreContracts.Request
												.prepareRequest(this));
					}
				}
			} finally {
				if (c != null && !c.isClosed()) {
					c.close();
				}
			}
		}
		try {
			sendStatus(Status.UPDATING, resultReceiver, mBundle);
			IDataSource<DataSource> dataSource = (IDataSource<DataSource>) AppUtils
					.get(context, dataSourceKey);
			DataSource sourceResult = null;
			try {
				sourceResult = dataSource.getSource(getUri());
			} catch (BadRequestException e) {
				mBundle.putSerializable(SourceResultReceiver.ERROR_KEY, e);
				sendStatus(Status.ERROR, resultReceiver, mBundle);
				context.getContentResolver()
						.delete(ContractUtils
								.getProviderUriFromContract(CoreContracts.Request.class),
								CoreContracts.Request.URI + " IN (?)",
								new String[] { getUri() });
			}
			if (sourceResult == null) {
				mBundle.putSerializable(SourceResultReceiver.ERROR_KEY,
						new NullPointerException(
								"loaded data form dataSource is null"));
				sendStatus(Status.ERROR, resultReceiver, mBundle);
				return;
			}
			IProcessor<DataSource, Result> processor = (IProcessor<DataSource, Result>) AppUtils
					.get(context, processorKey);
			Result result = processor.process(sourceResult);
			if (result == null) {
				mBundle.putSerializable(SourceResultReceiver.ERROR_KEY,
						new NullPointerException(
								"processed data form dataSource is null"));
				sendStatus(Status.ERROR, resultReceiver, mBundle);
				return;
			}
			if (isCacheable()) {
				boolean cached = false;
				cached = processor.cache(result, context);
				if (cached) {
					sendStatus(Status.CACHED, resultReceiver, mBundle);
				} else {
					L.w(LOG_TAG, "cant't cache entity!");
				}
			} else {
				if (result instanceof Parcelable) {
					mBundle.putParcelable(SourceResultReceiver.RESULT_KEY,
							(Parcelable) result);
				} else if (result instanceof Parcelable[]) {
					mBundle.putParcelableArray(SourceResultReceiver.RESULT_KEY,
							(Parcelable[]) result);
				} else if (result instanceof Serializable) {
					mBundle.putSerializable(SourceResultReceiver.RESULT_KEY,
							(Serializable) result);
				} else if (result instanceof ArrayList<?>) {
					mBundle.putParcelableArrayList(
							SourceResultReceiver.RESULT_KEY,
							(ArrayList<? extends Parcelable>) result);
				} else if (result instanceof String) {
					mBundle.putString(SourceResultReceiver.RESULT_KEY,
							(String) result);
				}
			}
			sendStatus(Status.DONE, resultReceiver, mBundle);
		} catch (Exception e) {
			e.printStackTrace();
			mBundle.putSerializable(SourceResultReceiver.ERROR_KEY, e);
			sendStatus(Status.ERROR, resultReceiver, mBundle);
		}
	}

	/**
	 * Sets is the request should be cached.
	 * 
	 * @param isCacheable
	 *            If 'true' then request will be cacheable, and will not be
	 *            cacheable otherwise. (default value is 'false').
	 * */
	public void setIsCacheable(boolean isCacheable) {
		mBundle.putString(REQUEST_CACHEABLE, String.valueOf(isCacheable));
	}

	/**
	 * @return 'true' if request is cacheable and 'false' otherwise.
	 * */
	public boolean isCacheable() {
		String value = mBundle.getString(REQUEST_CACHEABLE);
		return Boolean.valueOf(value);
	}

	/**
	 * Returns request uri.
	 * 
	 * @return Request uri string.
	 * */
	public String getUri() {
		return mBundle.getString(REQUEST_URI);
	}

	/**
	 * Sets is the request should be forcibly updated.
	 * 
	 * @param isForceUpdate
	 *            If 'true' then request will be update forcibly, if 'false'
	 *            then otherwise.
	 * */
	public void setForceUpdate(boolean isForceUpdate) {
		mBundle.putString(FORCE_UPDATE, String.valueOf(isForceUpdate));
	}

	/**
	 * @return 'true' if request updating forcibly and 'false' otherwise
	 * */
	public boolean isForceUpdate() {
		String value = mBundle.getString(FORCE_UPDATE);
		return Boolean.valueOf(value);
	}

	/**
	 * Sets the delay between updates in millis.
	 * 
	 * @param expirationInMillis
	 *            The delay value in millis.
	 * */
	public void setExpiration(long expirationInMillis) {
		mBundle.putString(CACHE_EXPIRATION, String.valueOf(expirationInMillis));
	}

	/**
	 * @return The delay value in millis.
	 * */
	public long getExpiration() {
		String value = mBundle.getString(CACHE_EXPIRATION);
		if (TextUtils.isEmpty(value)) {
			return NO_VALUE;
		} else {
			return Long.parseLong(value);
		}
	}

	/**
	 * Puts request into intent.
	 * 
	 * @param intent
	 *            The intent which should contains current request.
	 * */
	public void toIntent(Intent intent) {
		intent.putExtra(DATA_SOURCE_REQUEST_PARAMS, mBundle);
	}

	/**
	 * @return Request from current intent.
	 * */
	public static DataSourceRequest fromIntent(Intent intent) {
		DataSourceRequest dataSourceRequest = new DataSourceRequest();
		dataSourceRequest.mBundle = intent
				.getParcelableExtra(DATA_SOURCE_REQUEST_PARAMS);
		return dataSourceRequest;
	}

	private void sendStatus(SourceResultReceiver.Status status,
			ResultReceiver resultReceiver, Bundle resultData) {
		if (resultReceiver != null) {
			resultReceiver.send(status.ordinal(), resultData);
		}
	}

}
