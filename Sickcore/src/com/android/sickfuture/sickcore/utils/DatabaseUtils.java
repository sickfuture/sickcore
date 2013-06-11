package com.android.sickfuture.sickcore.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.android.sickfuture.sickcore.annotations.db.DBAutoincrement;
import com.android.sickfuture.sickcore.annotations.db.DBPrimaryKey;
import com.android.sickfuture.sickcore.annotations.db.DBTableName;
import com.android.sickfuture.sickcore.annotations.db.DBUnique;
import com.android.sickfuture.sickcore.annotations.db.contract.DBContract;
import com.android.sickfuture.sickcore.annotations.db.types.DBBooleanType;
import com.android.sickfuture.sickcore.annotations.db.types.DBByteArrayType;
import com.android.sickfuture.sickcore.annotations.db.types.DBByteType;
import com.android.sickfuture.sickcore.annotations.db.types.DBDoubleType;
import com.android.sickfuture.sickcore.annotations.db.types.DBIntegerType;
import com.android.sickfuture.sickcore.annotations.db.types.DBLongType;
import com.android.sickfuture.sickcore.annotations.db.types.DBVarcharType;

public class DatabaseUtils {

	private static final String WRONG_CONTRACTS_CLASS = "Wrong contracts class! Contracts class should be marked with DBContract annotation";
	private static final String DATABASE_UTILS = "DatabaseUtils";
	private static final String BAD_CONTRACT_FIELD_VALUE = "Bad contract field value";
	private static final String DATABASE_PRIMARY_KEY_EXCEPTION_MESSAGE = "Wrong contract class! Annotation @DBPrimaryKey does not exist at any field. Please implement the contract class by FrameWorkBaseColumns interface to get default primary key field";
	private static final String INTEGER_PRIMARY_KEY = " INTEGER PRIMARY KEY";
	private static final String AUTOINCREMENT = " AUTOINCREMENT ";
	private static final String DOUBLE = "DOUBLE";
	private static final String BYTE = "BYTE";
	private static final String BLOB = "BLOB";
	private static final String LONG = "LONG";
	private static final String BOOLEAN = "BOOLEAN";
	private static final String INTEGER = "INTEGER";
	private static final String VARCHAR = "VARCHAR";

	private static final String SELECT_DISTINCT_TBL = "select DISTINCT tbl_name from sqlite_master where tbl_name = '";

	private static final String CREATE_TABLE = "CREATE TABLE";

	private final static Object mDBInTransactionObjectLock = new Object();

	private DatabaseUtils() {
	}

	/**
	 * Checks if such database table already exists.
	 * 
	 * @param db
	 *            current database object
	 * @param tableName
	 *            current table name
	 * @return 'true' if table already exists and 'false' otherwise.
	 * */
	public static boolean isTableExists(SQLiteDatabase database,
			String tableName) {
		Cursor cursor;
		cursor = database.rawQuery(SELECT_DISTINCT_TBL + tableName + "'", null);
		if (cursor.getCount() > 0) {
			cursor.close();
			return true;
		} else {
			cursor.close();
			return false;
		}
	}

	/**
	 * Stopping current thread while database in transaction
	 * 
	 * @param db
	 *            current database object
	 * */
	public static void waitWhileTransaction() {
		synchronized (mDBInTransactionObjectLock) {
			try {
				mDBInTransactionObjectLock.wait();
			} catch (InterruptedException e) {
				// just ignore
			}
		}
	}

	/**
	 * Creates database creation script by fields from contract class.
	 * 
	 * @param tableName
	 *            current table name.
	 * @param contract
	 *            contract class of the current table.
	 * @return database creation script string.
	 * */
	public static String creationTableString(Class<?> contract) {
		StringBuilder sb = new StringBuilder();
		String tableName = getTableNameFromContract(contract);
		sb.append(CREATE_TABLE + " " + tableName + " (");
		Field[] fields = contract.getFields();
		String primaryKey = getPrimaryKey(fields);
		if (primaryKey != null) {
			sb.append(primaryKey);
		}
		for (Field field : fields) {
			String currentString = appendString(field);
			if (currentString != null) {
				sb.append(currentString);
			}
		}
		String unique = getSQLUniqueString(fields);
		if (unique != null) {
			sb.append(unique);
		}
		String result = sb.substring(0, sb.length() - 2) + ")";
		Log.d(DATABASE_UTILS, result);
		return result;
	}

	/**
	 * Returns string with database table name from current contract class
	 * 
	 * @param contract
	 *            contract class of the current table.
	 * @return database table name string.
	 * */
	public static String getTableNameFromContract(Class<?> contract) {
		Annotation annotation = contract.getAnnotation(DBTableName.class);
		return ((DBTableName) annotation).tableName();
	}

	/**
	 * Checking if class is database Contracts(marked with
	 * {@link annotations.database.contract.DBContract}
	 * annotations.database.contract.DBContract ).
	 * 
	 * @param clazz
	 *            class to check.
	 * @throws IllegalArgumentsException
	 * */
	public static void checkContractsClass(Class<?> clazz) {
		if (!isContractsClass(clazz)) {
			throw new IllegalArgumentException(WRONG_CONTRACTS_CLASS);
		}
	}

	private static boolean isContractsClass(Class<?> clazz) {
		if (clazz.getAnnotation(DBContract.class) != null) {
			return true;
		}
		return false;
	}

	private static String getPrimaryKey(Field[] fields) {
		String result = null;
		for (Field field : fields) {
			if (isPrimaryKeyField(field)) {
				String columnValue = getColumnValue(field);
				result = columnValue + INTEGER_PRIMARY_KEY;
				if (isAutoincrementField(field)) {
					result += AUTOINCREMENT + ", ";
				} else {
					result += ", ";
				}
			}
		}
		if (result == null) {
			throw new IllegalArgumentException(
					DATABASE_PRIMARY_KEY_EXCEPTION_MESSAGE);
		}
		return result;
	}

	private static boolean isPrimaryKeyField(Field field) {
		Annotation annotation = field.getAnnotation(DBPrimaryKey.class);
		if (annotation != null) {
			return true;
		}
		return false;
	}

	private static boolean isAutoincrementField(Field field) {
		Annotation annotation = field.getAnnotation(DBAutoincrement.class);
		if (annotation != null) {
			return true;
		}
		return false;
	}

	private static String getSQLUniqueString(Field[] fields) {
		ArrayList<Field> uniqueFields = new ArrayList<Field>();
		for (Field field : fields) {
			if (field.getAnnotation(DBUnique.class) != null) {
				uniqueFields.add(field);
			}
		}
		StringBuilder sb = null;
		String result = null;
		if (uniqueFields.size() > 0) {
			sb = new StringBuilder();
			for (int i = 0; i < uniqueFields.size(); i++) {
				sb.append("UNIQUE (" + getColumnValue(uniqueFields.get(0))
						+ ", ");
			}
			result = sb.substring(0, sb.length() - 2);
			result = result + "), ";
		}
		return result;
	}

	private static String appendString(Field field) {
		Annotation[] annotations = field.getAnnotations();
		if (annotations.length == 0) {
			throw new IllegalArgumentException("The contract field:"
					+ field.getName() + " in "
					+ field.getClass().getSimpleName()
					+ " unmarked with database types annotations");
		}
		for (Annotation annotation : annotations) {
			if (annotation instanceof DBPrimaryKey
					|| annotation instanceof DBAutoincrement) {
				return null;
			}
		}
		String columnValue = getColumnValue(field);
		// TODO check SQLite data types
		for (Annotation annotation : annotations) {
			if (annotation instanceof DBIntegerType) {
				return columnValue + " " + INTEGER + ", ";
			}
			if (annotation instanceof DBVarcharType) {
				return columnValue + " " + VARCHAR + ", ";
			}
			if (annotation instanceof DBBooleanType) {
				return columnValue + " " + BOOLEAN + ", ";
			}
			if (annotation instanceof DBLongType) {
				return columnValue + " " + LONG + ", ";
			}
			if (annotation instanceof DBByteArrayType) {
				return columnValue + " " + BLOB + ", ";
			}
			if (annotation instanceof DBByteType) {
				return columnValue + " " + BYTE + ", ";
			}
			if (annotation instanceof DBDoubleType) {
				return columnValue + " " + DOUBLE + ", ";
			}
		}
		return null;
	}

	private static String getColumnValue(Field field) {
		String columnValue = null;
		try {
			columnValue = (String) field.get(columnValue);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(BAD_CONTRACT_FIELD_VALUE);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(BAD_CONTRACT_FIELD_VALUE);
		}
		return columnValue;
	}
}
