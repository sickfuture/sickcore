package com.android.sickfuture.sickcore.db;

import com.android.sickfuture.sickcore.annotations.db.DBAutoincrement;
import com.android.sickfuture.sickcore.annotations.db.DBPrimaryKey;
import com.android.sickfuture.sickcore.annotations.db.types.DBIntegerType;

/** public interface with default database primary key filed */
public interface BaseColumns{
	@DBAutoincrement
	@DBIntegerType
	@DBPrimaryKey
	public static final String _ID = "_id";
}
