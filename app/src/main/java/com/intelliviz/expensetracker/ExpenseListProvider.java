package com.intelliviz.expensetracker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;


/**
 * Created by edm on 10/13/2015.
 */
public class ExpenseListProvider extends ContentProvider {
    private SqliteHelper mSqliteHelper;
    private static final String DBASE_NAME = "expenses";
    private static final int DBASE_VERSION = 6;
    private static final int DBASE_ADD_INCOME_TABLE = 6;

    private static final int CATEGORY_LIST = 101;
    private static final int CATEGORY_ID = 102;
    private static final int CATEGORY_VER_ID = 103;
    private static final int CATEGORY_VER_LIST = 104;
    private static final int MONTH_LIST = 201;
    private static final int MONTH_ID = 202;
    private static final int EXPENSE_ITEM_LIST = 301;
    private static final int EXPENSE_ITEM_ID = 302;
    private static final int EXPENSE_MONTH_ID = 303;
    private static final int INCOME_LIST = 401;
    private static final int INCOME_ID = 402;
    private static final int INCOME_MONTH_ID = 403;

    private static UriMatcher sUriMatcher;
    private static HashSet<String> sProjectionCategoryMap;
    private static HashSet<String> sProjectionCategoryVersionMap;
    private static HashSet<String> sProjectionMonthMap;
    private static HashMap<String, String> sProjectionExpenseItemMap;
    private static HashMap<String, String> sProjectionIncomeMap;

    //private static final SQLiteQueryBuilder sMonthlyExpensesQueryBuilder;

    // INNER JOIN
    // month INNER JOIN item ON month._id = expense_item.month_id
    private static final String QUERY_TABLES_FOR_EXPENSES =
            ExpenseListContract.MonthEntry.TABLE_NAME +
            " INNER JOIN " +
            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + " ON " +
            ExpenseListContract.MonthEntry.TABLE_NAME + "." +
            ExpenseListContract.MonthEntry._ID + " = " +
            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." +
            ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID +
            " INNER JOIN " +
            ExpenseListContract.CategoryEntry.TABLE_NAME + " ON " +
            ExpenseListContract.CategoryEntry.TABLE_NAME + "." +
            ExpenseListContract.CategoryEntry._ID + " = " +
            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." +
            ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID;

    private static final String QUERY_TABLES_FOR_INCOME =
            ExpenseListContract.MonthEntry.TABLE_NAME +
                    " INNER JOIN " +
                    ExpenseListContract.IncomeEntry.TABLE_NAME + " ON " +
                    ExpenseListContract.MonthEntry.TABLE_NAME + "." +
                    ExpenseListContract.MonthEntry._ID + " = " +
                    ExpenseListContract.IncomeEntry.TABLE_NAME + "." +
                    ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID;

    static {
        sUriMatcher = new UriMatcher((UriMatcher.NO_MATCH));

        // all categories
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_CATEGORY, CATEGORY_LIST);

        // a particular category
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_CATEGORY + "/#", CATEGORY_ID);

        // all category versions (should only be one)
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_CAT_VER, CATEGORY_VER_LIST);

        // a category version
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_CAT_VER + "/#", CATEGORY_VER_ID);

        // all months
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_MONTH, MONTH_LIST);

        // a particular month
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_MONTH + "/#", MONTH_ID);

        // all expense items
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_EXPENSE_ITEM, EXPENSE_ITEM_LIST);

        // a particular expense item
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_EXPENSE_ITEM + "/#", EXPENSE_ITEM_ID);

        // all expenses for particular month
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY,
                ExpenseListContract.PATH_EXPENSE_ITEM + "/" + ExpenseListContract.PATH_MONTH + "/#/", EXPENSE_MONTH_ID);

        // all incomes
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_INCOME, INCOME_LIST);

        // a particular month
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY, ExpenseListContract.PATH_INCOME + "/#", INCOME_ID);

        // income for particular month
        sUriMatcher.addURI(ExpenseListContract.CONTENT_AUTHORITY,
                ExpenseListContract.PATH_INCOME + "/" + ExpenseListContract.PATH_MONTH + "/#/", INCOME_MONTH_ID);

        sProjectionCategoryMap = new HashSet<String>();
        sProjectionCategoryMap.add(ExpenseListContract.CategoryEntry._ID);
        sProjectionCategoryMap.add(ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME);
        sProjectionCategoryVersionMap = new HashSet<String>();
        sProjectionCategoryVersionMap.add(ExpenseListContract.CategoryVersionEntry._ID);
        sProjectionCategoryVersionMap.add(ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION);
        sProjectionMonthMap = new HashSet<String>();
        sProjectionCategoryMap.add(ExpenseListContract.MonthEntry._ID);
        sProjectionCategoryMap.add(ExpenseListContract.MonthEntry.COLUMN_MONTH);
        sProjectionCategoryMap.add(ExpenseListContract.MonthEntry.COLUMN_YEAR);
        sProjectionExpenseItemMap = new HashMap<String, String>();
        sProjectionExpenseItemMap.put(ExpenseListContract.ExpenseItemEntry._ID, ExpenseListContract.ExpenseItemEntry.TABLE_NAME+"."+ExpenseListContract.ExpenseItemEntry._ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID, ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID, ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT, ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT);
        sProjectionIncomeMap = new HashMap<String, String>();
        sProjectionExpenseItemMap.put(ExpenseListContract.IncomeEntry._ID, ExpenseListContract.IncomeEntry.TABLE_NAME+"."+ExpenseListContract.IncomeEntry._ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.IncomeEntry.COLUMN_CAT_ID, ExpenseListContract.IncomeEntry.COLUMN_CAT_ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID, ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID);
        sProjectionExpenseItemMap.put(ExpenseListContract.IncomeEntry.COLUMN_AMOUNT, ExpenseListContract.IncomeEntry.COLUMN_AMOUNT);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mSqliteHelper = new SqliteHelper(context);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)) {
            case CATEGORY_LIST:
                return ExpenseListContract.CategoryEntry.CONTENT_TYPE;
            case CATEGORY_ID:
                return ExpenseListContract.CategoryEntry.CONTENT_ITEM_TYPE;
            case CATEGORY_VER_LIST:
                return ExpenseListContract.CategoryVersionEntry.CONTENT_TYPE;
            case CATEGORY_VER_ID:
                return ExpenseListContract.CategoryVersionEntry.CONTENT_ITEM_TYPE;
            case MONTH_LIST:
                return ExpenseListContract.MonthEntry.CONTENT_TYPE;
            case MONTH_ID:
                return ExpenseListContract.MonthEntry.CONTENT_ITEM_TYPE;
            case EXPENSE_ITEM_LIST:
                return ExpenseListContract.ExpenseItemEntry.CONTENT_TYPE;
            case EXPENSE_ITEM_ID:
                return ExpenseListContract.ExpenseItemEntry.CONTENT_ITEM_TYPE;
            case INCOME_LIST:
                return ExpenseListContract.IncomeEntry.CONTENT_TYPE;
            case INCOME_ID:
                return ExpenseListContract.IncomeEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        switch(sUriMatcher.match(uri)) {
            case CATEGORY_LIST:
                // get all categories: "category/"
                sqLiteQueryBuilder.setTables(ExpenseListContract.CategoryEntry.TABLE_NAME);
                break;
            case CATEGORY_ID:
                // get a particular category: "category/#"
                sqLiteQueryBuilder.setTables(ExpenseListContract.CategoryEntry.TABLE_NAME);
                sqLiteQueryBuilder.appendWhere(ExpenseListContract.CategoryEntry._ID +
                        "=" + uri.getLastPathSegment());
                break;
            case CATEGORY_VER_LIST:
                // get all categories: "category/"
                sqLiteQueryBuilder.setTables(ExpenseListContract.CategoryVersionEntry.TABLE_NAME);
                break;
            case CATEGORY_VER_ID:
                // get a particular category: "category/#"
                sqLiteQueryBuilder.setTables(ExpenseListContract.CategoryVersionEntry.TABLE_NAME);
                sqLiteQueryBuilder.appendWhere(ExpenseListContract.CategoryVersionEntry._ID +
                        "=" + uri.getLastPathSegment());
                break;
            case MONTH_LIST:
                // get all months: "month/"
                sqLiteQueryBuilder.setTables(ExpenseListContract.MonthEntry.TABLE_NAME);
                break;
            case MONTH_ID:
                // get a particular month: "month/#"
                sqLiteQueryBuilder.setTables(ExpenseListContract.MonthEntry.TABLE_NAME);
                sqLiteQueryBuilder.appendWhere(ExpenseListContract.MonthEntry._ID +
                        "=" + uri.getLastPathSegment());
                break;
            case EXPENSE_ITEM_LIST:
                // get all expenses for specified month: "expense/month_id"
                sqLiteQueryBuilder.setTables(ExpenseListContract.MonthEntry.TABLE_NAME);
                break;
            case EXPENSE_ITEM_ID:
                // get a particular expense: "expense/#"
                sqLiteQueryBuilder.setTables(ExpenseListContract.ExpenseItemEntry.TABLE_NAME);
                sqLiteQueryBuilder.appendWhere(ExpenseListContract.ExpenseItemEntry._ID +
                        "=" + uri.getLastPathSegment());
                break;
            case EXPENSE_MONTH_ID:
                // get all expenses for a particular month: expense/month/#
                return getMonthlyExpenseItems(uri, projection, selection, selectionArgs, sortOrder);
            case INCOME_LIST:
                // get all income for specified month: "income/"
                sqLiteQueryBuilder.setTables(ExpenseListContract.IncomeEntry.TABLE_NAME);
                break;
            case INCOME_ID:
                // get a particular expense: "income/#"
                sqLiteQueryBuilder.setTables(ExpenseListContract.IncomeEntry.TABLE_NAME);
                sqLiteQueryBuilder.appendWhere(ExpenseListContract.IncomeEntry._ID +
                        "=" + uri.getLastPathSegment());
                break;
            case INCOME_MONTH_ID:
                // get income for a particular month: income/month/#
                return getMonthlyIncome(uri, projection, selection, selectionArgs, sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = mSqliteHelper.getWritableDatabase();
        Cursor cursor = sqLiteQueryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId;
        SQLiteDatabase db;
        Uri returnUri;

        switch(sUriMatcher.match(uri)) {
            case CATEGORY_LIST:
                db = mSqliteHelper.getWritableDatabase();

                // The second parameter will allow an empty row to be inserted. If it was null, then no row
                // can be inserted if values is empty.
                rowId = db.insert(ExpenseListContract.CategoryEntry.TABLE_NAME, null, values);
                if (rowId > -1) {
                    returnUri = ContentUris.withAppendedId(uri, rowId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case CATEGORY_VER_LIST:
                db = mSqliteHelper.getWritableDatabase();

                // The second parameter will allow an empty row to be inserted. If it was null, then no row
                // can be inserted if values is empty.
                rowId = db.insert(ExpenseListContract.CategoryVersionEntry.TABLE_NAME, null, values);
                if (rowId > -1) {
                    returnUri = ContentUris.withAppendedId(uri, rowId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case MONTH_LIST:
                db = mSqliteHelper.getWritableDatabase();

                // The second parameter will allow an empty row to be inserted. If it was null, then no row
                // can be inserted if values is empty.
                rowId = db.insert(ExpenseListContract.MonthEntry.TABLE_NAME, null, values);
                if (rowId > -1) {
                    returnUri = ContentUris.withAppendedId(uri, rowId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case EXPENSE_ITEM_LIST:
                db = mSqliteHelper.getWritableDatabase();

                // The second parameter will allow an empty row to be inserted. If it was null, then no row
                // can be inserted if values is empty.
                rowId = db.insert(ExpenseListContract.ExpenseItemEntry.TABLE_NAME, null, values);
                if (rowId > -1) {
                    returnUri = ContentUris.withAppendedId(uri, rowId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case INCOME_LIST:
                db = mSqliteHelper.getWritableDatabase();

                // The second parameter will allow an empty row to be inserted. If it was null, then no row
                // can be inserted if values is empty.
                rowId = db.insert(ExpenseListContract.IncomeEntry.TABLE_NAME, null, values);
                if (rowId > -1) {
                    returnUri = ContentUris.withAppendedId(uri, rowId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mSqliteHelper.getWritableDatabase();
        int rowsDeleted = 0;
        String id;

        switch(sUriMatcher.match(uri)) {
            case CATEGORY_LIST:
                rowsDeleted = db.delete(ExpenseListContract.CategoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CATEGORY_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = db.delete(ExpenseListContract.CategoryEntry.TABLE_NAME,
                        ExpenseListContract.CategoryEntry._ID + "=" + id, null);
                break;
            case CATEGORY_VER_LIST:
                rowsDeleted = db.delete(ExpenseListContract.CategoryVersionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CATEGORY_VER_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = db.delete(ExpenseListContract.CategoryVersionEntry.TABLE_NAME,
                        ExpenseListContract.CategoryVersionEntry._ID + "=" + id, null);
                break;
            case MONTH_LIST:
                rowsDeleted = db.delete(ExpenseListContract.MonthEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case MONTH_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = db.delete(ExpenseListContract.MonthEntry.TABLE_NAME,
                        ExpenseListContract.MonthEntry._ID + "=" + id, null);
                break;
            case EXPENSE_ITEM_LIST:
                rowsDeleted = db.delete(ExpenseListContract.ExpenseItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case EXPENSE_ITEM_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = db.delete(ExpenseListContract.ExpenseItemEntry.TABLE_NAME,
                        ExpenseListContract.ExpenseItemEntry._ID + "=" + id, null);
                break;
            case INCOME_LIST:
                rowsDeleted = db.delete(ExpenseListContract.IncomeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INCOME_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = db.delete(ExpenseListContract.IncomeEntry.TABLE_NAME,
                        ExpenseListContract.IncomeEntry._ID + "=" + id, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mSqliteHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated = 0;
        String id;

        switch(sUriMatcher.match(uri)) {
            case CATEGORY_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(ExpenseListContract.CategoryEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.CategoryEntry._ID + "=?",
                            new String[]{id});
                } else {
                    rowsUpdated = db.update(ExpenseListContract.CategoryEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.CategoryEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case CATEGORY_VER_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(ExpenseListContract.CategoryVersionEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.CategoryVersionEntry._ID + "=?",
                            new String[]{id});
                } else {
                    rowsUpdated = db.update(ExpenseListContract.CategoryVersionEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.CategoryVersionEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case MONTH_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(ExpenseListContract.MonthEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.MonthEntry._ID + "=?",
                            new String[]{id});
                } else {
                    rowsUpdated = db.update(ExpenseListContract.MonthEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.MonthEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case EXPENSE_ITEM_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(ExpenseListContract.ExpenseItemEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.ExpenseItemEntry._ID + "=?",
                            new String[]{id});
                } else {
                    rowsUpdated = db.update(ExpenseListContract.ExpenseItemEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.ExpenseItemEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case INCOME_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(ExpenseListContract.IncomeEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.IncomeEntry._ID + "=?",
                            new String[]{id});
                } else {
                    rowsUpdated = db.update(ExpenseListContract.IncomeEntry.TABLE_NAME,
                            values,
                            ExpenseListContract.IncomeEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    private static class SqliteHelper extends SQLiteOpenHelper {

        private SqliteHelper(Context context) {
            super(context, DBASE_NAME, null, DBASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // create the category table
            String sql = "CREATE TABLE " + ExpenseListContract.CategoryEntry.TABLE_NAME +
                    " ( " + ExpenseListContract.CategoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME + " TEXT NOT NULL);";

            db.execSQL(sql);

            // create the category version table
            sql = "CREATE TABLE " + ExpenseListContract.CategoryVersionEntry.TABLE_NAME +
                    " ( " + ExpenseListContract.CategoryVersionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION + " INTEGER NOT NULL);";

            db.execSQL(sql);

            // create the month table
            sql = "CREATE TABLE " + ExpenseListContract.MonthEntry.TABLE_NAME +
                    " ( " + ExpenseListContract.MonthEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ExpenseListContract.MonthEntry.COLUMN_CAT_VERSION + " INTEGER, " +
                    ExpenseListContract.MonthEntry.COLUMN_MONTH + " INTEGER, " +
                    ExpenseListContract.MonthEntry.COLUMN_YEAR + " INTEGER);";

            db.execSQL(sql);

            // create the expense item table
            sql = "CREATE TABLE " + ExpenseListContract.ExpenseItemEntry.TABLE_NAME +
                    " ( " + ExpenseListContract.ExpenseItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID + " INTEGER, " +
                    ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID + " INTEGER, " +
                    ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT + " REAL);";

            db.execSQL(sql);

            // create the income table
            sql = "CREATE TABLE " + ExpenseListContract.IncomeEntry.TABLE_NAME +
                    " ( " + ExpenseListContract.IncomeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID + " INTEGER, " +
                    ExpenseListContract.IncomeEntry.COLUMN_CAT_ID + " INTEGER, " +
                    ExpenseListContract.IncomeEntry.COLUMN_AMOUNT + " REAL);";

            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            if(oldVersion < DBASE_ADD_INCOME_TABLE) {
                db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.IncomeEntry.TABLE_NAME);

                // need to add income table
                String sql = "CREATE TABLE " + ExpenseListContract.IncomeEntry.TABLE_NAME +
                        " ( " + ExpenseListContract.IncomeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID + " INTEGER, " +
                        ExpenseListContract.IncomeEntry.COLUMN_CAT_ID + " INTEGER, " +
                        ExpenseListContract.IncomeEntry.COLUMN_AMOUNT + " REAL);";

                db.execSQL(sql);
            }

            if(oldVersion == newVersion) {
                Log.d("EDM", "upgrading...");
            } else {
                Log.d("EDM", "testing...");
            }
            /*
            db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.CategoryEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.MonthEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.ExpenseItemEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.CategoryVersionEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ExpenseListContract.IncomeEntry.TABLE_NAME);
            onCreate(db);
            */
        }
    }

    private Cursor getMonthlyExpenseItems(
            Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String monthId = uri.getLastPathSegment();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(QUERY_TABLES_FOR_EXPENSES);
        //builder.setProjectionMap(sProjectionExpenseItemMap);

        builder.appendWhere(ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." +
                ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID + " = " + monthId);

        Cursor cursor = builder.query(mSqliteHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        return cursor;
    }

    private Cursor getMonthlyIncome(
            Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String monthId = uri.getLastPathSegment();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(QUERY_TABLES_FOR_INCOME);
        //builder.setProjectionMap(sProjectionExpenseItemMap);

        builder.appendWhere(ExpenseListContract.IncomeEntry.TABLE_NAME + "." +
                ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID + " = " + monthId);

        Cursor cursor = builder.query(mSqliteHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        return cursor;
    }
}
