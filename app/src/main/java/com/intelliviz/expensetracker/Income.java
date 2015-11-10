package com.intelliviz.expensetracker;

/**
 * Created by edm on 11/5/2015.
 */
public class Income {
    private long mId;
    private long mCatId;
    private long mMonthId;
    private float mAmount;

    public Income() {
        this(0, 0, 0, 0);
    }

    public Income(long id, long catId, long monthId, float amount) {
        mId = id;
        mCatId = catId;
        mMonthId = monthId;
        mAmount = amount;
    }

    public long getId() {
        return mId;
    }

    public long getCatId() {
        return mCatId;
    }

    public long getMonthId() {
        return mMonthId;
    }

    public float getAmount() {
        return mAmount;
    }

    @Override
    public String toString() {
        String s = "" + mId + " " + mCatId + " " + mMonthId + " " + mAmount;
        return s;
    }
}
