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
import java.util.List;
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
    private long mCategoryVersionId;
    private long mExpenseItemId;
    private Month mCurrentMonth;
    private CategoryVersion mCategoryVersion;
    private String mDialogName;
    private TextView mCurrentDate;
    private TextView mTotalExpenseText;
    private TextView mTotalIncomeText;
    private TextView mDifferenceText;
    private Map<Long, String> mCategoryMap;
    private GestureDetector mGestureDetector;


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

        int month = QueryPreferences.getCurrentMonth(getActivity());
        int year = QueryPreferences.getCurrentYear(getActivity());
        if(month == -1 || year == -1) {
            Calendar calendar = Calendar.getInstance();
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);
        }

        mCurrentMonth = ExpenseListProviderHelper.getMonth(getActivity(), month, year);
        if(mCurrentMonth == null) {
            mCurrentMonth = ExpenseListProviderHelper.addMonth(getActivity(), month, year);
        }


        mCategoryVersion = ExpenseListProviderHelper.getCategoryVersion(getActivity());
        checkCurrentMonth();

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
                switch (item.getItemId()) {
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
                                cursor = (Cursor) adapter.getItem(i);
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

        getLoaderManager().initLoader(EXPENSE_ITEM_LOADER, null, this);

        mGestureDetector = new GestureDetector(getActivity(), new SwipeGestureDetector());

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               if(mGestureDetector.onTouchEvent(event)) {
                   return true;
               }
                return false;
            }
        });

        updateUI();

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
        uri = Uri.withAppendedPath(uri, "" + mCurrentMonth.getId());
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
        values.put(ExpenseListContract.ExpenseItemEntry.COLUMN_MONTH_ID, mCurrentMonth.getId());
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
        uri = Uri.withAppendedPath(uri, "" + mCurrentMonth.getId());
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

    private void updateDate() {
        mCurrentDate.setText(getMonth(mCurrentMonth.getMonth()) + ", " + mCurrentMonth.getYear());
    }

    private void updateUI() {
        updateAmounts();
        updateDate();
        refreshList();
    }

    private float getTotalExpenses() {
        List<ExpenseItem> expenses = ExpenseListProviderHelper.getAllExpensesForMonth(getActivity(), mCurrentMonth.getId());

        float amount = 0;
        for(ExpenseItem item : expenses) {
            amount += item.getAmount();
        }
        /*
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
        */

        return amount;
    }

    private void checkCurrentMonth() {
        boolean monthUpdated = false;
        int version = mCurrentMonth.getCatVersion();
        int catVersion = mCategoryVersion.getVersion();
        if(version == catVersion) {
            return;
        }

        List<Category> cats = ExpenseListProviderHelper.getAllCategories(getActivity());
        List<ExpenseItem> expenses = ExpenseListProviderHelper.getAllExpensesForMonth(getActivity(), mCurrentMonth.getId());

        boolean addToList;
        for(Category cat : cats) {
            addToList = true;
            for(ExpenseItem item : expenses) {
                if(cat.getId() == item.getCategoryId()) {
                    addToList = false;
                    break;
                }
            }

            if(addToList) {
                ExpenseListProviderHelper.addExpenseItemToMonth(getActivity(), cat.getId(), mCurrentMonth.getId());
                monthUpdated = true;
            }
        }

        if(monthUpdated) {
            updateCurrentMonth(catVersion);
        }
    }

    private boolean isInCategoryList(List<Category> cats, long catId) {
        for(Category cat : cats) {
            if(cat.getId() == catId) {
                return true;
            }
        }

        return false;
    }

    private void onLeftSwipe() {
        mCurrentMonth = ExpenseListProviderHelper.incMonth(getActivity(), mCurrentMonth);
        checkCurrentMonth();
        QueryPreferences.setCurrentMonth(getActivity(), mCurrentMonth.getMonth());
        QueryPreferences.setCurrentYear(getActivity(), mCurrentMonth.getYear());
        updateUI();
    }

    private void onRightSwipe() {
        mCurrentMonth = ExpenseListProviderHelper.decMonth(getActivity(), mCurrentMonth);
        checkCurrentMonth();
        QueryPreferences.setCurrentMonth(getActivity(), mCurrentMonth.getMonth());
        QueryPreferences.setCurrentYear(getActivity(), mCurrentMonth.getYear());
        updateUI();
    }

    private class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MAX_OFF_PATH = 200;
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 150;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffAbs = Math.abs(e1.getY() - e2.getY());
            float diff = e1.getX() - e2.getX();

            if(diffAbs > SWIPE_MAX_OFF_PATH) {
                return false;
            }

            if(diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                onLeftSwipe();
            } else if(-diff > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                onRightSwipe();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
