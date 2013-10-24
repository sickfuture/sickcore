package com.android.sickfuture.sickcore.source;

import android.content.Context;
import android.os.Bundle;

import com.android.sickfuture.sickcore.app.AppHelper.IAppServiceKey;

public interface IProcessor<DataSource, Result> extends IAppServiceKey {

	public static final String EXTRA_PROCESSING_DATA = "extra_processing_data";
	
	Result process(DataSource data);

	boolean cache(Result result, Context context);
	
	Bundle extraProcessingData();

}
