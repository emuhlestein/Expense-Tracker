package com.intelliviz.expensetracker;

/**
 * Created by edm on 11/2/2015.
 */
public class Category {
    private long _id;
    private String _name;

    public Category(long id, String name) {
        _name = name;
        _id = id;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }
}
