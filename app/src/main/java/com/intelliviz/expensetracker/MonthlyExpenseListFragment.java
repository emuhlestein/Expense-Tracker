package com.intelliviz.expensetracker;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Created by edm on 10/12/2015.
 */
public class MonthlyExpenseListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, GestureDetector.OnGestureListener {
    private static final String DIALOG_AMOUNT = "amount";
    private static final String DIALOG_CATEGORY = "category";
    private static final String MONTH_KEY = "month";
    public static final int REQUEST_ITEM = 0;
    private static final int EXPENSE_ITEM_LOADER = 0;
    private ExpenseListCursorAdapter mAdapter;
    private ListView mExpenseList;
    private int mCurrentMonth;
    private int mCurrentYear;
    private long mCurrentMonthId = -1;
    private long mCategoryVersionId;
    private long mExpenseItemId;
    private String mDialogName;
    private TextView mCurrentDate;
    private TextView mTotalExpenseText;
    private TextView mTotalIncomeText;
    private TextView mDifferenceText;
    private Map<Long, String> mCategoryMap;


    public static MonthlyExpenseListFragment newInstance() {
        Bundle args = new Bundle();

        //args.putLong(MONTH_KEY, month);
        MonthlyExpenseListFragment fragment = new MonthlyExpenseListFragment();
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        buildCategoryMap();

        // NOTE: do not access UI here; onCreateVIew has not been called
        //mCurrentMonthId = getArguments().getLong(MONTH_KEY);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.expense_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_add_category:
                SimpleTextDialog dialog = SimpleTextDialog.newInstance("Add a category", "", "Category");
                dialog.setTargetFragment(this, REQUEST_ITEM);
                dialog.show(getFragmentManager(), "Dial1");
                mDialogName = DIALOG_CATEGORY;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_monthly_expense_list, container, false);

        mTotalExpenseText = (TextView)view.findViewById(R.id.totalExpenses);
        mTotalIncomeText = (TextView)view.findViewById(R.id.totalIncome);
        mDifferenceText = (TextView)view.findViewById(R.id.difference);

        mCurrentDate = (TextView)view.findViewById(R.id.currentMonth);

        mCurrentMonth = QueryPreferences.getCurrentMonth(getActivity());
        mCurrentYear = QueryPreferences.getCurrentYear(getActivity());
        if(mCurrentMonth == -1 || mCurrentYear == -1) {
            Calendar calendar = Calendar.getInstance();
            mCurrentMonth = calendar.get(Calendar.MONTH);
            mCurrentYear = calendar.get(Calendar.YEAR);
        }

        mCurrentDate.setText(getMonth(mCurrentMonth) + ", " + mCurrentYear);

        mExpenseList = (ListView)view.findViewById(R.id.expenseList);
        mAdapter = new ExpenseListCursorAdapter(getActivity(), null);
        mExpenseList.setAdapter(mAdapter);

        mExpenseList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mExpenseList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.expense_list_item_context, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Adapter adapter = mExpenseList.getAdapter();
                switch(item.getItemId()) {
                    case R.id.menu_item_delete:
                        long id;
                        for (int i = adapter.getCount() - 1; i >= 0; i--) {
                            if (mExpenseList.isItemChecked(i)) {
                                id = adapter.getItemId(i);
                                deleteCategoryFromList(id);
                            }
                        }
                        mode.finish();
                        getLoaderManager().initLoader(EXPENSE_ITEM_LOADER, null, MonthlyExpenseListFragment.this);
                        return true;
                    case R.id.menu_item_edit:
                        Cursor cursor = null;
                        for (int i = adapter.getCount() - 1; i >= 0; i--) {
                            if (mExpenseList.isItemChecked(i)) {
                                cursor = (Cursor)adapter.getItem(i);
                                break;
                            }
                        }

                        int amountIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT);
                        int catIdx = cursor.getColumnIndex(ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME);
                        int expenseItemIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry._ID);
                        mExpenseItemId = cursor.getLong(expenseItemIdx);
                        float amount = cursor.getFloat(amountIdx);
                        String cat = cursor.getString(catIdx);

                        //String cat = MonthlyExpenseListFragment.this.mCategoryMap.get(labelId);

                        SimpleTextDialog dialog = SimpleTextDialog.newInstance("Update the amount",
                                formatCurrency(amount), cat);
                        dialog.setTargetFragment(MonthlyExpenseListFragment.this, REQUEST_ITEM);
                        dialog.show(getFragmentManager(), "Dial1");
                        mDialogName = DIALOG_AMOUNT;
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

            }
        });

        mCurrentMonthId = getCurrentMonthId();
        if(mCurrentMonthId == -1) {
            addCurrentMonth();
        }

        getLoaderManager().initLoader(EXPENSE_ITEM_LOADER, null, this);

        updateAmounts();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ITEM) {
            if(resultCode == Activity.RESULT_OK) {
                if(mDialogName.equals(DIALOG_AMOUNT)) {
                    String amount = data.getStringExtra(SimpleTextDialog.EXTRA_TEXT);
                    if(amount.charAt(0) == '$') {
                        amount = amount.substring(1);
                    }
                    float amt = Float.parseFloat(amount);
                    updateAmount(amt);
                    updateAmounts();
                } else if(mDialogName.equals(DIALOG_CATEGORY)) {
                    String category = data.getStringExtra(SimpleTextDialog.EXTRA_TEXT);
                    Toast.makeText(getActivity(), "Got it: " + category, Toast.LENGTH_SHORT).show();
                    addCategory(formatText(category));
                }
            }
        }
    }

    @Override
    public Loader onCreateLoader(int loaderId, Bundle args) {
        Loader<Cursor> loader;
        Uri uri = Uri.withAppendedPath(ExpenseListContract.ExpenseItemEntry.CONTENT_URI, ExpenseListContract.PATH_MONTH);
        uri = Uri.withAppendedPath(uri, "" + mCurrentMonthId);
        switch (loaderId) {
            case EXPENSE_ITEM_LOADER:
                loader = new CursorLoader(getActivity(),
                        uri,
                        new String[]
                        {
                            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." + ExpenseListContract.ExpenseItemEntry._ID,
                            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." + ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID,
                            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." + ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID,
                            ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." + ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT,
                            ExpenseListContract.CategoryEntry.TABLE_NAME + "." + ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME,
                        },
                        null,
                        null,
                        ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME + " ASC");
                break;
            default:
                loader = null;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private class ExpenseListCursorAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public ExpenseListCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.expense_list_item, parent, false);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView catView = (TextView) view.findViewById(R.id.textCategory);
            TextView amtView = (TextView) view.findViewById(R.id.textAmount);


            int amountIdx = cursor.getColumnIndex(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT);
            int catIdx = cursor.getColumnIndex(ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME);
            String cat = "???";
            String amount = "$-666.00";
            if(amountIdx != -1) {
                amount = formatCurrency(cursor.getFloat(amountIdx));
            }
            if(catIdx != -1) {
                cat = cursor.getString(catIdx);
            }



            //String cat = mCategoryMap.get(cursor.getLong(1));
            //String amount = cursor.getString(3);
            //amount = formatCurrency(amount);
            //amount = amount + " (" + cursor.getString(2) + ")";

            catView.setText(cat);

            amtView.setText(amount);
        }
    }

    private void buildCategoryMap() {
        Cursor cursor;
        mCategoryMap = new HashMap<>();

        cursor = getActivity().getContentResolver().query(
                ExpenseListContract.CategoryEntry.CONTENT_URI,
                new String[]{ExpenseListContract.CategoryEntry._ID,
                        ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME},
                null, null, null);

        while (cursor.moveToNext()) {
            mCategoryMap.put(cursor.getLong(0), cursor.getString(1));
        }

        cursor.close();
    }

    private long getCurrentMonthId() {
        long id = -1;
        String[] projection = new String[]{
                ExpenseListContract.MonthEntry.TABLE_NAME + "." + ExpenseListContract.MonthEntry._ID,
                ExpenseListContract.MonthEntry.COLUMN_MONTH,
                ExpenseListContract.MonthEntry.COLUMN_YEAR};
        String selection = ExpenseListContract.MonthEntry.TABLE_NAME + "." +
                ExpenseListContract.MonthEntry.COLUMN_MONTH + " = ? AND " +
                ExpenseListContract.MonthEntry.COLUMN_YEAR + " = ?";
        String[] selectionArgs = new String[]{Long.toString(mCurrentMonth), Long.toString(mCurrentYear)};

        Uri uri = ExpenseListContract.MonthEntry.CONTENT_URI;

        Cursor cursor = getActivity().getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if(cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }

        return id;
    }

    private void addCurrentMonth() {
        ContentValues values;
        Uri uri;

        // month does not exist; need to add it.
        values = new ContentValues();
        values.put(ExpenseListContract.MonthEntry.COLUMN_MONTH, mCurrentMonth);
        values.put(ExpenseListContract.MonthEntry.COLUMN_YEAR, mCurrentYear);
        uri = getActivity().getContentResolver().insert(ExpenseListContract.MonthEntry.CONTENT_URI, values);
        mCurrentMonthId = Long.parseLong(uri.getLastPathSegment());
    }

    private Cursor getCategory(String cat) {
        String[] projection = new String[]{
                ExpenseListContract.CategoryEntry.TABLE_NAME + "." + ExpenseListContract.CategoryEntry._ID,
                ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME};
        String selection = ExpenseListContract.CategoryEntry.TABLE_NAME + "." +
                ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME + " = ?";
        String[] selectionArgs = new String[]{cat};

        Uri uri = ExpenseListContract.CategoryEntry.CONTENT_URI;

        Cursor cursor = getActivity().getContentResolver().query(uri, projection, selection, selectionArgs, null);

        return cursor;
    }

    private Cursor getCategoryVersion() {
        String[] projection = new String[]{
                ExpenseListContract.CategoryVersionEntry._ID,
                ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION};

        Uri uri = ExpenseListContract.CategoryVersionEntry.CONTENT_URI;

        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

        return cursor;
    }

    private void addCategory(String cat) {
        ContentValues values;
        Uri uri;

        Cursor cursor = getCategory(cat);
        if(cursor.moveToFirst()) {
            return; // category already exists
        }

        // create new category
        values = new ContentValues();
        values.put(ExpenseListContract.CategoryEntry.COLUMN_CATEGORY_NAME, cat);
        uri = getActivity().getContentResolver().insert(ExpenseListContract.CategoryEntry.CONTENT_URI, values);
        long catId = Long.parseLong(uri.getLastPathSegment());
        mCategoryMap.put(catId, cat);

        // create new expense item for new category
        values = new ContentValues();
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID, catId);
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID, mCurrentMonthId);
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT, 0);

        uri = getActivity().getContentResolver().insert(ExpenseListContract.ExpenseItemEntry.CONTENT_URI, values);
        long expenseId = Long.parseLong(uri.getLastPathSegment());

        int version;
        cursor = getCategoryVersion();
        if(cursor.moveToFirst()) {
            mCategoryVersionId = cursor.getLong(0);
            version = cursor.getInt(1);
            version++;

            updateCategoryVersion(version);
            updateCurrentMonth(version);
        } else {
            version = 1;
            uri = createCatgoryVersion(version);
            String id = uri.getLastPathSegment();
            mCategoryVersionId = Long.parseLong(id);
            updateCurrentMonth(version);
        }

        getLoaderManager().restartLoader(EXPENSE_ITEM_LOADER, null, this);
    }

    private String getMonth(int month) {
        return new DateFormatSymbols().getMonths()[month];
    }

    private String formatText(String input) {
        String output = "";
        String space = " ";
        String[] str = input.split(" ");

        for(int i = 0; i < str.length; i++) {
            if(i == (str.length-1)) {
                space = "";
            }
            output = output + str[i].substring(0, 1).toUpperCase() + str[i].substring(1) + space;
        }

        return output;
    }

    private void deleteCategoryFromList(long id) {
        Uri uri = ExpenseListContract.ExpenseItemEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, "" + id);
        getActivity().getContentResolver().delete(uri, null, null);
        refreshList();
    }

    private void updateCategoryVersion(int version) {
        ContentValues values = new ContentValues();
        values.put(ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION, version);

        Uri uri = ExpenseListContract.CategoryVersionEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, "" + mCategoryVersionId);
        getActivity().getContentResolver().update(uri, values, null, null);

        getLoaderManager().restartLoader(EXPENSE_ITEM_LOADER, null, this);
    }

    private void updateCurrentMonth(int version) {
        ContentValues values = new ContentValues();
        values.put(ExpenseListContract.MonthEntry.COLUMN_CAT_VERSION, version);

        Uri uri = ExpenseListContract.MonthEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, "" + mCurrentMonthId);
        getActivity().getContentResolver().update(uri, values, null, null);

        getLoaderManager().restartLoader(EXPENSE_ITEM_LOADER, null, this);
    }

    private void updateAmount(float amount) {
        ContentValues values = new ContentValues();
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT, amount);

        Uri uri = ExpenseListContract.ExpenseItemEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, "" + mExpenseItemId);
        getActivity().getContentResolver().update(uri, values, null, null);

        getLoaderManager().restartLoader(EXPENSE_ITEM_LOADER, null, this);
    }

    private Uri createCatgoryVersion(int version) {
        ContentValues values = new ContentValues();
        values.put(ExpenseListContract.CategoryVersionEntry.COLUMN_VERSION, version);
        Uri uri = getActivity().getContentResolver().insert(ExpenseListContract.CategoryVersionEntry.CONTENT_URI, values);
        return uri;
    }

    public void refreshList() {
        getLoaderManager().restartLoader(EXPENSE_ITEM_LOADER, null, this);
        buildCategoryMap();
    }

    private String formatCurrency(String amount) {
        Locale locale = new Locale("en", "US");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        float amt = Float.parseFloat(amount);
        return currencyFormatter.format(amt);
    }

    private String formatCurrency(float amount) {
        Locale locale = new Locale("en", "US");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        return currencyFormatter.format(amount);
    }

    private void updateAmounts() {

        mTotalExpenseText.setText(formatCurrency(getTotalExpenses()));

    }

    private float getTotalExpenses() {
        String[] projection = new String[]{
                ExpenseListContract.ExpenseItemEntry.TABLE_NAME+"."+ExpenseListContract.ExpenseItemEntry._ID, // TODO try a projection map for this
                ExpenseListContract.ExpenseItemEntry.COLUMN_CAT_ID,
                ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID,
                ExpenseListContract.ExpenseItemEntry.COLUMN_AMOUNT};
        //String selection = ExpenseListContract.ExpenseItemEntry.TABLE_NAME + "." +
        //        ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID + " = ?";
        //String[] selectionArgs = new String[]{"" + mCurrentMonthId};

        Uri uri = ExpenseListContract.ExpenseItemEntry.CONTENT_URI;
        uri = Uri.withAppendedPath(uri, ExpenseListContract.PATH_MONTH);
        uri = Uri.withAppendedPath(uri, "" + mCurrentMonthId);

        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

        float amount = 0;
        cursor.moveToPosition(-1);
        while(cursor.moveToNext()) {
            amount += cursor.getFloat(3);
        }

        cursor.close();

        return amount;
    }
}
