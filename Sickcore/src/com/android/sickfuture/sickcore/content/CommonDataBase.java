package com.android.sickfuture.sickcore.content;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.sickfuture.sickcore.utils.AndroidVersionsUtils;
import com.android.sickfuture.sickcore.utils.DatabaseUtils;

public class CommonDataBase extends SQLiteOpenHelper {

	// private final static String LOG_TAG = "CommonDataBase";

	private Context mContext;

	private final static String DB_NAME = "sickcore.store.db";
	private final static int DB_VERSION = 1;

	private SQLiteDatabase mDatabase;

	private Class<?> mContract;

	private boolean mInTransaction = false;
	private final static Object mDBInTransactionObjectLock = new Object();

	protected CommonDataBase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		// TODO Auto-generated method stub
		return super.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		syncTransactions();
		try {
			setInTransaction(true);
			// database.beginTransaction();
			beginTransaction(database);
			Class<?> contracts = mContract.getDeclaringClass();
			DatabaseUtils.checkContractsClass(contracts);
			Class<?>[] subClasses = contracts.getClasses();
			for (Class<?> contract : subClasses) {
				database.execSQL(DatabaseUtils.creationTableString(contract));
			}
			// database.setTransactionSuccessful();
			setTransactionSuccessful(database);
		} finally {
			// database.endTransaction();
			endTransaction(database);
			setInTransaction(false);
		}
	}

	// TODO needs be configurable. We needs support old version of apps and
	// provide correct update
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (oldVersion < newVersion) {
			deleteDataBase();
			database.setVersion(newVersion);
		}
		onCreate(mDatabase);
	}

	@SuppressLint("NewApi")
	private void beginTransaction(SQLiteDatabase database) {
		if (AndroidVersionsUtils.hasHoneycomb()) {
			database.beginTransactionNonExclusive();
		} else {
			database.beginTransaction();
		}
	}

	private void setTransactionSuccessful(SQLiteDatabase database) {
		database.setTransactionSuccessful();
	}

	private void endTransaction(SQLiteDatabase database) {
		database.endTransaction();
	}

	/**
	 * Inserting item from contentValues into database table which described by
	 * the contract.
	 * 
	 * @param contract
	 *            Contract class of the current table.
	 * @param contentValues
	 *            Values which should be inserted into database.
	 * @return The row ID of the newly inserted row OR the primary key of the
	 *         existing row if the input param 'conflictAlgorithm' =
	 *         CONFLICT_IGNORE OR -1 if any error.
	 * @throws SQLException
	 * */
	protected long addItem(Class<?> contract, ContentValues contentValues)
			throws SQLException {
		if (contract == null || contentValues == null) {
			return 0;
		}
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		createTableIfNotExist(tableName);
		long value;
		try {
			setInTransaction(true);
			// mDatabase.beginTransaction();
			beginTransaction(mDatabase);
			value = mDatabase.insertWithOnConflict(tableName, null,
					contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			if (value <= 0) {
				throw new SQLException("Failed to insert row into " + tableName);
			}
			// mDatabase.setTransactionSuccessful();
			setTransactionSuccessful(mDatabase);
		} finally {
			// mDatabase.endTransaction();
			endTransaction(mDatabase);
			setInTransaction(false);
		}
		return value;
	}

	/**
	 * Inserting item from ContentValues array into database table which
	 * described by the contract.
	 * 
	 * @param contract
	 *            Contract class of the current table.
	 * @param contentValues
	 *            Array of ContentValues which should be inserted into database.
	 * @return Count of inserted rows.
	 * @throws SQLException
	 * */
	protected int addItems(Class<?> contract, ContentValues[] contentValues)
			throws SQLException {
		if (contract == null || contentValues == null) {
			return 0;
		}
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		createTableIfNotExist(tableName);
		long value;
		int numInserted = 0;
		try {
			setInTransaction(true);
			// mDatabase.beginTransaction();
			beginTransaction(mDatabase);
			for (ContentValues cv : contentValues) {
				value = mDatabase.insertWithOnConflict(tableName, null, cv,
						SQLiteDatabase.CONFLICT_REPLACE);
				if (value <= 0) {
					throw new SQLException("Failed to insert row into "
							+ tableName);
				} else {
					numInserted++;
				}
			}
			// mDatabase.setTransactionSuccessful();
			setTransactionSuccessful(mDatabase);
		} finally {
			// mDatabase.endTransaction();
			endTransaction(mDatabase);
			setInTransaction(false);
		}
		return numInserted;
	}

	/**
	 * Returns items from database table which described by the contract.
	 * 
	 * @param contract
	 *            Contract class of the current table.
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
	protected Cursor getItems(Class<?> contract, String orderBy,
			String selection, String[] selectionArgs) {
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		createTableIfNotExist(tableName);
		Cursor cursor = null;
		try {
			setInTransaction(true);
			// mDatabase.beginTransaction();
			beginTransaction(mDatabase);
			cursor = mDatabase.query(tableName, null, selection, selectionArgs,
					null, null, orderBy);
			if (cursor == null) {
				throw new SQLException("Failed to query row from " + tableName);
			}
			// mDatabase.setTransactionSuccessful();
			setTransactionSuccessful(mDatabase);
		} finally {
			// mDatabase.endTransaction();
			endTransaction(mDatabase);
			setInTransaction(false);
		}
		return cursor;
	}

	/**
	 * Runs the provided SQL and returns a Cursor over the result set.
	 * 
	 * @param contract
	 *            Contract class of the current table.
	 * @param sql
	 *            the SQL query. The SQL string must not be ; terminated
	 * @param selectionArgs
	 *            You may include ?s in where clause in the query, which will be
	 *            replaced by the values from selectionArgs. The values will be
	 *            bound as Strings.
	 * @return A Cursor object, which is positioned before the first entry. Note
	 *         that Cursors are not synchronized, see the documentation for more
	 *         details.
	 * @throws SQLException
	 * */
	protected Cursor rawQuery(Class<?> contract, String sql,
			String[] selectionArgs) throws SQLException {
		syncTransactions();
		mContract = contract;
		mDatabase = getWritableDatabase();
		String tableName = DatabaseUtils.getTableNameFromContract(contract);
		createTableIfNotExist(tableName);
		Cursor cursor = null;
		try {
			setInTransaction(true);
			// mDatabase.beginTransaction();
			beginTransaction(mDatabase);
			cursor = mDatabase.rawQuery(sql, selectionArgs);
			if (cursor == null) {
				throw new SQLException("Failed to query: " + sql);
			}
			// mDatabase.setTransactionSuccessful();
			setTransactionSuccessful(mDatabase);
		} finally {
			// mDatabase.endTransaction();
			endTransaction(mDatabase);
			setInTransaction(false);
		}
		return cursor;
	}

	/**
	 * Convenience method for deleting rows in the database.
	 * 
	 * @param contract
	 *            Contract class of the current table.
	 * @param where
	 *            The optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows.
	 * @return The number of rows affected if a whereClause is passed in, 0
	 *         otherwise. To remove all rows and get a count pass "1" as the
	 *         whereClause.
	 * */
	protected int deleteItems(Class<?> contract, String where,
			String[] whereArgs) {
		if (mDatabase == null) {
			return 0;
		}
		syncTransactions();
		mDatabase = getWritableDatabase();
		String tableName = null;
		int result = 0;
		try {
			setInTransaction(true);
			beginTransaction(mDatabase);
			tableName = DatabaseUtils.getTableNameFromContract(contract);
			if (DatabaseUtils.isTableExists(mDatabase, tableName)) {
				result = mDatabase.delete(tableName, where, whereArgs);
			}
			setTransactionSuccessful(mDatabase);
			return result;
		} finally {
			endTransaction(mDatabase);
			setInTransaction(false);
		}
	}

	/**
	 * Delete an existing private SQLiteDatabase associated with this Context's
	 * application package.
	 */
	protected void deleteDataBase() {
		syncTransactions();
		try {
			setInTransaction(true);
			mContext.deleteDatabase(DB_NAME);
		} finally {
			setInTransaction(false);
		}
	}

	private void createTableIfNotExist(String tableName) {
		if (DatabaseUtils.isTableExists(mDatabase, tableName)) {
		} else {
			// table not exist, so create it
			onCreate(mDatabase);
		}
	}

	// TODO needs testing, maybe will eat battery
	private void syncTransactions() {
		while (mInTransaction) {
			waitWhileTransaction();
		}
	}

	/**
	 * Pause any ongoing background work.
	 **/
	private static void waitWhileTransaction() {
		synchronized (mDBInTransactionObjectLock) {
			try {
				mDBInTransactionObjectLock.wait();
			} catch (InterruptedException e) {
				// can be ignore
			}
		}
	}

	private void setInTransaction(boolean pauseWork) {
		synchronized (mDBInTransactionObjectLock) {
			mInTransaction = pauseWork;
			if (!mInTransaction) {
				mDBInTransactionObjectLock.notifyAll();
			}
		}
	}
}
