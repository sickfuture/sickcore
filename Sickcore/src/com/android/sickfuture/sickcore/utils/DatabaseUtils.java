package com.android.sickfuture.sickcore.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.android.sickfuture.sickcore.annotations.db.*;
import com.android.sickfuture.sickcore.annotations.db.contract.DBContract;
import com.android.sickfuture.sickcore.annotations.db.types.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class DatabaseUtils {

    private static final String LOG_TAG = "DatabaseUtils";
    private static final String WRONG_CONTRACTS_CLASS = "Wrong contracts class! Contracts class should be marked with DBContract annotation";
    private static final String BAD_CONTRACT_FIELD_VALUE = "Bad contract field value";
    private static final String DATABASE_PRIMARY_KEY_EXCEPTION_MESSAGE = "Wrong contract class! Annotation @DBPrimaryKey does not exist at any field. Please implement the contract class by FrameWorkBaseColumns interface to get default primary key field";
    private static final String INTEGER_PRIMARY_KEY = " INTEGER PRIMARY KEY";
    private static final String AUTOINCREMENT = " AUTOINCREMENT";
    private static final String DOUBLE = "DOUBLE";
    private static final String BLOB = "BLOB";
    private static final String BIGINT = "BIGINT";
    private static final String BOOLEAN = "BOOLEAN";
    private static final String INTEGER = "INTEGER";
    private static final String VARCHAR = "VARCHAR";

    private static final String SELECT_DISTINCT_TBL = "select DISTINCT tbl_name from sqlite_master where tbl_name = '";

    private static String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS %1$s (%2$s)";

    private DatabaseUtils() {
    }

    /**
     * Checks if such database table already exists.
     *
     * @param database  current database object
     * @param tableName current table name
     * @return 'true' if table already exists and 'false' otherwise.
     */
    public static boolean isTableExists(SQLiteDatabase database,
                                        String tableName) {
        if (database == null || tableName == null) {
            return false;
        }
        Cursor cursor = database.rawQuery(
                SELECT_DISTINCT_TBL + tableName + "'", null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    /**
     * Creates database creation script by fields from contract class.
     *
     * @param contract contract class of the current table.
     * @return database creation script string.
     */
    public static String creationTableString(Class<?> contract) {
        StringBuilder sb = new StringBuilder();
        String tableName = getTableNameFromContract(contract);
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
        String tables = sb.substring(0, sb.length() - 2);
        String result = String.format(CREATE_TABLE_TEMPLATE, tableName, tables);
        L.d(LOG_TAG, result);
        return result;
    }

    /**
     * Returns string with database table name from current contract class
     *
     * @param contract contract class of the current table.
     * @return database table name string.
     */
    public static String getTableNameFromContract(Class<?> contract) {
        return ((DBTableName) contract.getAnnotation(DBTableName.class))
                .tableName();
    }

    /**
     * Checking if class is database Contracts(marked with
     * {@link com.android.sickfuture.sickcore.annotations.db.contract.DBContract}
     * annotations.database.contract.DBContract ).
     *
     * @param clazz class to check.
     */
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
                return columnValue + " " + BIGINT + ", ";
            }
            if (annotation instanceof DBByteArrayType) {
                return columnValue + " " + BLOB + ", ";
            }
            if (annotation instanceof DBByteType) {
                return columnValue + " " + INTEGER + ", ";
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
