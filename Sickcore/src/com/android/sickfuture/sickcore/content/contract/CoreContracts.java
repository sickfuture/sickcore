package com.android.sickfuture.sickcore.content.contract;

import android.content.ContentValues;

import com.android.sickfuture.sickcore.annotations.ContentInfo;
import com.android.sickfuture.sickcore.annotations.db.DBTableName;
import com.android.sickfuture.sickcore.annotations.db.DBUnique;
import com.android.sickfuture.sickcore.annotations.db.contract.DBContract;
import com.android.sickfuture.sickcore.annotations.db.types.DBBooleanType;
import com.android.sickfuture.sickcore.annotations.db.types.DBLongType;
import com.android.sickfuture.sickcore.annotations.db.types.DBVarcharType;
import com.android.sickfuture.sickcore.service.DataSourceRequest;

@DBContract
public class CoreContracts {

	@DBTableName(tableName = "SICK_APP_REQUESTS")
	@ContentInfo(contentType = "vnd.android.cursor.dir/SICK_APP_REQUESTS", contentUri = "content://provider.AppRequestsProvider/SICK_APP_REQUESTS")
	public static final class Request implements CoreBaseColumns {

		// @DBLongType
		// public static final String REQUEST_ID = "request_id";

		@DBLongType
		public static final String LAST_UPDATE = "last_update";

		@DBLongType
		public static final String EXPIRATION = "expiration";

		@DBUnique
		@DBVarcharType
		public static final String URI = "uri";

		@DBBooleanType
		public static final String IS_CACHEBLE = "is_cacheble";

		public static ContentValues prepareRequest(DataSourceRequest request) {
			ContentValues values = new ContentValues();
			// values.put(REQUEST_ID, HashUtils.generateId(request.getUri()));
			values.put(URI, request.getUri());
			values.put(LAST_UPDATE, System.currentTimeMillis());
			values.put(EXPIRATION, request.getExpiration());
			values.put(IS_CACHEBLE, request.isCacheable());
			return values;
		}

	}

}
