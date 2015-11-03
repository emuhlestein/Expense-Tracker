package com.intelliviz.expensetracker;

/**
 * Created by edm on 10/13/2015.
 */
public class ExpenseItem {
    private long mExpenseId;
    private long mCategoryId;
    private long mMonthId;
    private float mAmount;

    public ExpenseItem(long expenseId, long categoryId, long monthId, float amount) {
        mExpenseId = expenseId;
        mCategoryId = categoryId;
        mMonthId = monthId;
        mAmount = amount;
    }

    public long getExpenseId() {
        return mExpenseId;
    }

    public long getMonthId() {
        return mMonthId;
    }

    public long getCategoryId() {
        return mCategoryId;
    }

    public float getAmount() {
        return mAmount;
    }
}
