package com.intelliviz.expensetracker;

/**
 * Created by edm on 10/13/2015.
 */
public class ExpenseItem {
    private String mCategory;
    private String mAmount;

    public ExpenseItem(String category, String amount) {
        mCategory = category;
        mAmount = amount;
    }

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getAmount() {
        return mAmount;
    }

    public void setAmount(String amount) {
        mAmount = amount;
    }
}
