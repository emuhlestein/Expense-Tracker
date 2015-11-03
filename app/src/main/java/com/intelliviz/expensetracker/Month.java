package com.intelliviz.expensetracker;

/**
 * Created by edm on 11/3/2015.
 */
public class Month {
    private long _id;
    private int _month;
    private int _year;
    private int _catVersion;

    public Month(long id, int month, int year, int catVersion) {
        _id = id;
        _month = month;
        _year = year;
        _catVersion = catVersion;
    }

    public long getId() {
        return _id;
    }

    public int getMonth() {
        return _month;
    }

    public int getYear() {
        return _year;
    }

    public int getCatVersion() {
        return _catVersion;
    }
}
