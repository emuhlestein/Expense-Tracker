package com.intelliviz.expensetracker;

/**
 * Created by edm on 11/3/2015.
 */
public class CategoryVersion {
    private long _id;
    private int _version;

    public CategoryVersion(long id, int version) {
        _id = id;
        _version = version;
    }

    public long getId() {
        return _id;
    }

    public int getVersion() {
        return _version;
    }
}
