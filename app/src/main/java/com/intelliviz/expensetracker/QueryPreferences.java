package com.intelliviz.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;


/**
 * Created by edm on 10/24/2015.
 */
public class QueryPreferences {
    private static final String PREF_CURRENT_MONTH = "currentMonth";
    private static final String PREF_CURRENT_YEAR = "currentYear";

    public static int getCurrentMonth(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_CURRENT_MONTH, -1);
    }

    public static void setCurrentMonth(Context context, int month) {

        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putInt(PREF_CURRENT_MONTH, month);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        }
        else {
            editor.commit();
        }
    }

    public static int getCurrentYear(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_CURRENT_YEAR, -1);
    }

    public static void setCurrentYear(Context context, int year) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putInt(PREF_CURRENT_YEAR, year);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        }
        else {
            editor.commit();
        }
    }
}
