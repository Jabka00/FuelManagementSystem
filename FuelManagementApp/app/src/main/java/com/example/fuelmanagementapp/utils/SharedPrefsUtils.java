package com.example.fuelmanagementapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtils {

    public static void saveDriverInfo(Context context, Integer driverId, String driverName) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.KEY_SELECTED_DRIVER_ID, driverId != null ? driverId : -1);
        editor.putString(Constants.KEY_SELECTED_DRIVER_NAME, driverName);
        editor.apply();
    }

    public static Integer getSelectedDriverId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int driverId = prefs.getInt(Constants.KEY_SELECTED_DRIVER_ID, -1);
        return driverId != -1 ? driverId : null;
    }

    public static String getSelectedDriverName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Constants.KEY_SELECTED_DRIVER_NAME, null);
    }

    public static boolean hasSelectedDriver(Context context) {
        return getSelectedDriverId(context) != null;
    }

    public static void clearDriverInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.KEY_SELECTED_DRIVER_ID);
        editor.remove(Constants.KEY_SELECTED_DRIVER_NAME);
        editor.apply();
    }
}
