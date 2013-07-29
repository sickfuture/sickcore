package com.android.sickfuture.sickcore.source;

import android.content.Context;

import com.android.sickfuture.sickcore.app.AppHelper.IAppServiceKey;

public interface IProcessor<DataSource, Result> extends IAppServiceKey {

	Result process(DataSource data);

	boolean cache(Result result, Context context);

}
