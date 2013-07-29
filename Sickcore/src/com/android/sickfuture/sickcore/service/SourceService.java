package com.android.sickfuture.sickcore.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.ResultReceiver;

import com.android.sickfuture.sickcore.context.ContextHolder;
import com.android.sickfuture.sickcore.utils.L;

public class SourceService extends IntentService {

	private static final String IS_FORCE_UPDATE = "isForceUpdate";
	private static final String RESULT_RECEIVER = "resultReceiver";
	private static final String DATA_SOURCE_KEY = "dataSourceKey";
	private static final String PROCESSOR_KEY = "processorKey";
	private static final String LOG_TAG = SourceService.class.getSimpleName();

	public SourceService(String name) {
		super(name);
	}

	public SourceService() {
		super(LOG_TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		L.i(LOG_TAG, "SourceService started");
		final DataSourceRequest request = DataSourceRequest.fromIntent(intent);
		final String dataSourceKey = intent.getStringExtra(DATA_SOURCE_KEY);
		final String processorKey = intent.getStringExtra(PROCESSOR_KEY);
		final ResultReceiver resultReceiver = intent
				.getParcelableExtra(RESULT_RECEIVER);
		if (request != null) {
			request.execute(getApplicationContext(), dataSourceKey,
					processorKey, resultReceiver);
		}
	}

	/**
	 * Executing and processing current DataSourceRequest
	 * 
	 * @param context
	 *            Current context.
	 * @param dataSourceRequest
	 *            Request to execute.
	 * @param dataSourceKey
	 *            String key of system service dataSource.
	 * @param processorKey
	 *            String key of system service processor.
	 * */
	public static void execute(Context context,
			DataSourceRequest dataSourceRequest, String dataSourceKey,
			String processorKey) {
		execute(context, dataSourceRequest, dataSourceKey, processorKey, null);
	}

	/**
	 * Executing and processing current DataSourceRequest
	 * 
	 * @param context
	 *            Current context.
	 * @param dataSourceRequest
	 *            Request to execute.
	 * @param dataSourceKey
	 *            String key of system service dataSource.
	 * @param processorKey
	 *            String key of system service processor.
	 * @param resultReceiver
	 *            {@link SourceResultReceiver} which reports about the progress
	 *            of executing.
	 * */
	public static void execute(Context context,
			DataSourceRequest dataSourceRequest, String dataSourceKey,
			String processorKey, ResultReceiver resultReceiver) {
		if (context == null) {
			context = ContextHolder.getInstance().getContext()
					.getApplicationContext();
		}
		Intent intent = buildIntent(context, dataSourceRequest, dataSourceKey,
				processorKey, resultReceiver);
		if (intent == null) {
			return;
		}
		context.startService(intent);
	}

	private static Intent buildIntent(Context context,
			DataSourceRequest dataSourceRequest, String dataSourceKey,
			String processorKey, ResultReceiver resultReceiver) {
		if (dataSourceRequest == null) {
			return null;
		}
		Intent intent = new Intent(context, SourceService.class);
		dataSourceRequest.toIntent(intent);
		intent.putExtra(PROCESSOR_KEY, processorKey);
		intent.putExtra(DATA_SOURCE_KEY, dataSourceKey);
		intent.putExtra(RESULT_RECEIVER, resultReceiver);
		intent.putExtra(IS_FORCE_UPDATE, dataSourceRequest.isForceUpdate());
		return intent;
	}
}
