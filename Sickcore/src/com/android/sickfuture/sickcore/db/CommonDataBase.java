package com.android.sickfuture.sickcore.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.sickfuture.sickcore.utils.DatabaseUtils;

public class CommonDataBase extends SQLiteOpenHelper {

	private final static String LOG_TAG = CommonDataBase.class.getSimpleName();

	private static Context mContext;
	private final static String DB_NAME = "sickcore.store.db";
	private final static int DB_VERSION = 1;

	private static volatile CommonDataBase instance;

	private SQLiteDatabase mDatabase;

	private Class<?> mContract;

	private boolean mInTransaction = false;

	private CommonDataBase() {
		super(mContext, DB_NAME, null, DB_VERSION);
	}

	/** Double Checked Locking Singleton & volatile */
	public static CommonDataBase getInstance(Context context) {
		mContext = context;
		if (context == null) {
			Log.e(LOG_TAG, "Context is null");
		}
		CommonDataBase localInstance = instance;
		if (localInstance == null) {
			synchronized (CommonDataBase.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new CommonDataBase();
				}
			}
		}
		return localInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		syncTransactions();
		try {
			setTransactionStarted();
			database.beginTransaction();
			Class<?> contracts = mContract.getDeclaringClass();
			DatabaseUtils.checkContractsClass(contracts);
			Class<?>[] subClasses = contracts.getClasses();
			for (Class<?> contract : subClasses) {
				database.execSQL(DatabaseUtils.creationTableString(contract));
			}
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
			setTransactionEnded();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (oldVersion < newVersion) {
			mContext.deleteDatabase(DB_NAME);
		}
		onCreate(mDatabase);
	}

	/**
	 * Inserting items from contentValues into database table which described by
	 * the contract
	 * 
	 * @param contract
	 *            contract class of the current table.
	 * @param contentValues
	 *            values which should be inserted into database
	 * @return the row ID of the newly inserted row OR the primary key of the
	 *         existing row if the input param 'conflictAlgorithm' =
	 *         CONFLICT_IGNORE OR -1 if any error
	 * */
	@SuppressLint("NewApi")
	public long addItem(Class<?> contract, ContentValues contentValues) {
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		long value;
		try {
			setTransactionStarted();
			mDatabase.beginTransaction();
			value = mDatabase.insertWithOnConflict(tableName, null,
					contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			if (value <= 0) {
				throw new SQLException("Failed to insert row into " + tableName);
			}
			mDatabase.setTransactionSuccessful();
		} finally {
			mDatabase.endTransaction();
			setTransactionEnded();
		}
		return value;
	}

	/**
	 * Returns items from database table which described by the contract.
	 * 
	 * @param contract
	 *            contract class of the current table.
	 * @param orderBy
	 *            How to order the rows, formatted as an SQL ORDER BY clause
	 *            (excluding the ORDER BY itself). Passing null will use the
	 *            default sort order, which may be unordered.
	 * @param selection
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause (excluding the WHERE itself). Passing null will
	 *            return all rows for the given table.
	 * @param selectionArgs
	 *            You may include ?s in selection, which will be replaced by the
	 *            values from selectionArgs, in order that they appear in the
	 *            selection. The values will be bound as Strings.
	 * @return A Cursor object, which is positioned before the first entry. Note
	 *         that Cursors are not synchronized, see the documentation for more
	 *         details.
	 * */
	public Cursor getItems(Class<?> contract, String orderBy, String selection,
			String[] selectionArgs) {
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		Cursor cursor = null;
		try {
			setTransactionStarted();
			mDatabase.beginTransaction();
			cursor = mDatabase.query(tableName, null, selection, selectionArgs,
					null, null, orderBy);
			if (cursor == null) {
				throw new SQLException("Failed to query row from " + tableName);
			}
			mDatabase.setTransactionSuccessful();
		} finally {
			mDatabase.endTransaction();
			setTransactionEnded();
		}
		return cursor;
	}

	/**
	 * Convenience method for deleting rows in the database.
	 * 
	 * @param contract
	 *            contract class of the current table.
	 * @param where
	 *            the optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows.
	 * */
	public int deleteItems(Class<?> contract, String where, String[] whereArgs) {
		syncTransactions();
		String tableName = null;
		int result = 0;
		try {
			setTransactionStarted();
			tableName = DatabaseUtils.getTableNameFromContract(contract);
			if (DatabaseUtils.isTableExists(mDatabase, tableName)
					&& tableName != null) {
				result = mDatabase.delete(tableName, where, whereArgs);
			}
			return result;
		} finally {
			setTransactionEnded();
		}
	}

	/**
	 * Delete an existing private SQLiteDatabase associated with this Context's
	 * application package.
	 */
	public void deleteDataBase() {
		syncTransactions();
		try {
			setTransactionStarted();
			mContext.deleteDatabase(DB_NAME);
		} finally {
			setTransactionEnded();
		}
	}

	private void syncTransactions() {
		while (mInTransaction) {
			DatabaseUtils.waitWhileTransaction();
		}
	}

	private void setTransactionStarted() {
		mInTransaction = true;
	}

	private void setTransactionEnded() {
		mInTransaction = false;
	}
}
