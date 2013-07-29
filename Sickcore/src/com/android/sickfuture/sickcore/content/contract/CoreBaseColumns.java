package com.android.sickfuture.sickcore.content.contract;

import com.android.sickfuture.sickcore.annotations.db.DBAutoincrement;
import com.android.sickfuture.sickcore.annotations.db.DBPrimaryKey;
import com.android.sickfuture.sickcore.annotations.db.types.DBLongType;

/** public interface with default database primary key filed */
public interface CoreBaseColumns {
	@DBAutoincrement
	@DBLongType
	@DBPrimaryKey
	public static final String _ID = "_id";
}
