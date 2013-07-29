package com.android.sickfuture.sickcore.source;

import com.android.sickfuture.sickcore.app.AppHelper.IAppServiceKey;
import com.android.sickfuture.sickcore.exceptions.BadRequestException;

public interface IDataSource<DataSource> extends IAppServiceKey {

	DataSource getSource(String source) throws BadRequestException;

}
