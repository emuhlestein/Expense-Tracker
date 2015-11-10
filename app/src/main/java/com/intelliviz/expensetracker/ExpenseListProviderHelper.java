package com.intelliviz.expensetracker;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edm on 11/2/2015.
 */
public class ExpenseListProviderHelper {
    public static final int INIT_CAT_VERSION = 1;
    public static List<Category> getAllCategories(Activity activity) {

        List<Category> list = new ArrayList<Category>();
        String[] projection = new String[]{
                ExpenseListContract.CategoryEntry.TABLE_NAME + "." + ExpenseListContract.CategoryEntry._ID,
                ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME};

        Uri uri = ExpenseListContract.CategoryEntry.CONTENT_URI;

        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.CategoryEntry._ID);
            int nameIdx = cursor.getColumnIndex(ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME);

            Category cat = new Category(cursor.getLong(idIdx), cursor.getString(nameIdx));
            list.add(cat);
        }

        cursor.close();

        return list;
    }

    public static List<ExpenseItem> getAllExpensesForMonth(Activity activity, long monthId) {

        List<ExpenseItem> list = new ArrayList<ExpenseItem>();
        String[] projection = new String[]{
                ExpenseListContract.ExpenseItemEntry.TABLE_NAME+"."+ExpenseListContract.ExpenseItemEntry._ID,
                ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID,
                ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID,
                ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT};

        Uri uri = ExpenseListContract.ExpenseItemEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, ExpenseListContract.PATH_MONTH);
        uri = Uri.withAppendedPath(uri, "" + monthId);

        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry._ID);
            int catIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID);
            int monthIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID);
            int amountIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT);

            ExpenseItem expenseItem = new ExpenseItem(cursor.getLong(idIdx),
                    cursor.getLong(catIdx), cursor.getLong(monthIdx), cursor.getFloat(amountIdx));

            list.add(expenseItem);
        }

        cursor.close();

        return list;
    }

    public static long addExpenseItemToMonth(Activity activity, long catId, long monthId) {
        return addExpenseItemToMonth(activity, catId, monthId, 0);
    }

    public static long addExpenseItemToMonth(Activity activity, long catId, long monthId, float amount) {
        ContentValues values = new ContentValues();
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID, catId);
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID, monthId);
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT, amount);

        Uri uri = activity.getContentResolver().insert(ExpenseListContract.ExpenseItemEntry.CONTENT_URI, values);
        long expenseId = Long.parseLong(uri.getLastPathSegment());
        return expenseId;
    }

        public static Month getMonth(Activity activity, int month, int year) {
        Month newMonth = null;
        String[] projection = new String[]{
                ExpenseListContract.MonthEntry.TABLE_NAME + "." + ExpenseListContract.MonthEntry._ID,
                ExpenseListContract.MonthEntry.COLUMN_MONTH,
                ExpenseListContract.MonthEntry.COLUMN_YEAR,
                ExpenseListContract.MonthEntry.COLUMN_CAT_VERSION};
        String selection = ExpenseListContract.MonthEntry.TABLE_NAME + "." +
                ExpenseListContract.MonthEntry.COLUMN_MONTH + " = ? AND " +
                ExpenseListContract.MonthEntry.COLUMN_YEAR + " = ?";
        String[] selectionArgs = new String[]{Long.toString(month), Long.toString(year)};

        Uri uri = ExpenseListContract.MonthEntry.CONTENT_URI;

        Cursor cursor = activity.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if(cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.MonthEntry._ID);
            int monthIdx = cursor.getColumnIndex(ExpenseListContract.MonthEntry.COLUMN_MONTH);
            int yearIdx = cursor.getColumnIndex(ExpenseListContract.MonthEntry.COLUMN_YEAR);
            int catIdx = cursor.getColumnIndex(ExpenseListContract.MonthEntry.COLUMN_CAT_VERSION);

            newMonth = new Month(cursor.getLong(idIdx), cursor.getInt(monthIdx),
                    cursor.getInt(yearIdx), cursor.getInt(catIdx));
        }

        return newMonth;
    }

    public static Month addMonth(Activity activity, int month, int year) {
        int catVersion = -1;
        ContentValues values;
        Uri uri;

        // month does not exist; need to add it.
        values = new ContentValues();
        values.put(ExpenseListContract.MonthEntry.COLUMN_MONTH, month);
        values.put(ExpenseListContract.MonthEntry.COLUMN_YEAR, year);
        values.put(ExpenseListContract.MonthEntry.COLUMN_CAT_VERSION, catVersion);
        uri = activity.getContentResolver().insert(ExpenseListContract.MonthEntry.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        Month newMonth = new Month(id, month, year, catVersion);

        return newMonth;
    }

    public static Month addPrevMonth(Activity activity, Month month) {
        int monthNum = month.getMonth();
        int year = month.getYear();
        monthNum--;
        if(monthNum < 0) {
            monthNum = 11;
            year = year - 1; // TODO should we look at a earliest year????
        }

        addMonth(activity, monthNum, year);

        Month nextMonth = ExpenseListProviderHelper.getMonth(activity, monthNum, year);
        if(nextMonth != null) {
            return nextMonth;
        } else {
            return month;
        }
    }

    public static Month incMonth(Activity activity, Month month) {
        int monthNum = month.getMonth();
        int year = month.getYear();
        monthNum++;
        if(monthNum >= 12) {
            monthNum = 0;
            year = year + 1;
        }

        Month nextMonth = ExpenseListProviderHelper.getMonth(activity, monthNum, year);
        if(nextMonth != null) {
            return nextMonth;
        } else {
            return month;
        }
    }

    public static Month decMonth(Month month) { // TODO refactor
        int monthNum = month.getMonth();
        int year = month.getYear();
        monthNum--;
        if (monthNum < 0) {
            monthNum = 11;
            year = year - 1; // TODO should we look at a earliest year????
        }

        Month newMonth = new Month(-1, monthNum, year, -1);
        return newMonth;
    }

    /**
     * Decrement the month and return the new month from the database.
     * @param activity The activity.
     * @param month The month to decrement.
     * @return The new month.
     */
    public static Month decMonth(Activity activity, Month month) {
        Month newMonth = decMonth(month);

        Month prevMonth = ExpenseListProviderHelper.getMonth(activity, newMonth.getMonth(), newMonth.getYear());
        if(prevMonth != null) {
            return prevMonth;
        } else {
            return month;
        }
    }

    public static CategoryVersion getCategoryVersion(Activity activity) {
        CategoryVersion catVersion;
        String[] projection = new String[]{
                ExpenseListContract.CategoryVersionEntry.TABLE_NAME + "." + ExpenseListContract.CategoryVersionEntry._ID,
                ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION};

        Uri uri = ExpenseListContract.CategoryVersionEntry.CONTENT_URI;

        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);
        if(cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.CategoryVersionEntry._ID);
            int versionIdx = cursor.getColumnIndex(ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION);

            catVersion = new CategoryVersion(cursor.getLong(idIdx), cursor.getInt(versionIdx));
        } else {
            ContentValues values = new ContentValues();
            values.put(ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION, INIT_CAT_VERSION);

            uri = activity.getContentResolver().insert(ExpenseListContract.CategoryVersionEntry.CONTENT_URI, values);
            long id = Long.parseLong(uri.getLastPathSegment());

            catVersion = new CategoryVersion(id, INIT_CAT_VERSION);
        }

        return catVersion;
    }

    public static Income getIncomeForMonth(Activity activity, long monthId) {
        Income income = null;
        String[] projection = new String[]{
                ExpenseListContract.IncomeEntry.TABLE_NAME + "." + ExpenseListContract.IncomeEntry._ID,
                ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID,
                ExpenseListContract.IncomeEntry.COLUMN_CAT_ID,
                ExpenseListContract.IncomeEntry.COLUMN_AMOUNT};
        String selection = ExpenseListContract.IncomeEntry.TABLE_NAME + "." +
                ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID + " = ?";
        String[] selectionArgs = new String[]{Long.toString(monthId)};

        Uri uri = ExpenseListContract.IncomeEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, ExpenseListContract.PATH_MONTH);
        uri = Uri.withAppendedPath(uri, "" + monthId);

        Cursor cursor = activity.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if(cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry._ID);
            int monthIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID);
            int amountIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_AMOUNT);
            int catIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_CAT_ID);

            income = new Income(cursor.getLong(idIdx), cursor.getLong(catIdx),
                    cursor.getLong(monthIdx), cursor.getFloat(amountIdx));
        }

        return income;
    }

    public static Income addIncome(Activity activity, long monthId, float amount) {
        int catVersion = -1;
        ContentValues values;
        Uri uri;

        values = new ContentValues();
        values.put(ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID, monthId);
        values.put(ExpenseListContract.IncomeEntry.COLUMN_AMOUNT, amount);
        values.put(ExpenseListContract.IncomeEntry.COLUMN_CAT_ID, catVersion);
        uri = activity.getContentResolver().insert(ExpenseListContract.IncomeEntry.CONTENT_URI, values);
        long id = Long.parseLong(uri.getLastPathSegment());
        Income income = new Income(id, catVersion, monthId, amount);

        return income;
    }

    public static List<Income> getAllIncomes(Activity activity) {

        List<Income> list = new ArrayList<Income>();
        String[] projection = new String[]{
                ExpenseListContract.IncomeEntry.TABLE_NAME + "." + ExpenseListContract.IncomeEntry._ID,
                ExpenseListContract.IncomeEntry.COLUMN_CAT_ID,
                ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID,
                ExpenseListContract.IncomeEntry.COLUMN_AMOUNT};

        Uri uri = ExpenseListContract.IncomeEntry.CONTENT_URI;

        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            int idIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry._ID);
            int catIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_CAT_ID);
            int monthIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_MONTH_ID);
            int amountIdx = cursor.getColumnIndex(ExpenseListContract.IncomeEntry.COLUMN_AMOUNT);

            Income cat = new Income(cursor.getLong(idIdx), cursor.getLong(catIdx), cursor.getLong(monthIdx), cursor.getFloat(amountIdx));
            list.add(cat);
        }

        cursor.close();

        return list;
    }

}
