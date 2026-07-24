package com.louissimonmcnicoll.simpleframe.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.louissimonmcnicoll.simpleframe.R;

/** Typed access to SimpleFrame's private preferences. */
public final class AppData {
    public static final String SETTINGS_FILENAME = "SimpleFrameSettings";
    public static final String BRIGHTNESS_MODE_SYSTEM = "system";
    public static final String BRIGHTNESS_MODE_MANUAL = "manual";
    public static final String BRIGHTNESS_MODE_AUTO = "auto";

    private AppData() {
    }

    /** Returns the number of seconds each photo remains visible. */
    public static int getDisplayTime(Context context) {
        return Integer.parseInt(getString(context, R.string.sett_key_displaytime));
    }

    /** Returns the selected index in {@code transitionTypeValues}. */
    public static int getTransitionStyle(Context context) {
        return Integer.parseInt(getString(context, R.string.sett_key_transition));
    }

    public static boolean getRandomize(Context context) {
        return getBoolean(context, R.string.sett_key_randomize);
    }

    public static boolean getScaling(Context context) {
        return getBoolean(context, R.string.sett_key_scaling);
    }

    /** Returns the selected root directory; its readable descendants are scanned recursively. */
    public static String getImagePath(Context context) {
        return getString(context, R.string.sett_key_srcpath_sd);
    }

    public static void setSdSourcePath(Context context, String path) {
        getSharedPreferences(context).edit()
                .putString(key(context, R.string.sett_key_srcpath_sd), path)
                .apply();
    }

    public static boolean getFirstAppStart(Context context) {
        return getSharedPreferences(context).getBoolean(key(context, R.string.sett_key_firstStart), false);
    }

    public static void setFirstAppStart(Context context, boolean firstAppStart) {
        getSharedPreferences(context).edit()
                .putBoolean(key(context, R.string.sett_key_firstStart), firstAppStart)
                .apply();
    }

    public static int getCurrentPage(Context context) {
        return getSharedPreferences(context).getInt(key(context, R.string.sett_key_currentPage), 1);
    }

    public static void setCurrentPage(Context context, int page) {
        getSharedPreferences(context).edit()
                .putInt(key(context, R.string.sett_key_currentPage), page)
                .apply();
    }

    public static String getBrightnessMode(Context context) {
        return getString(context, R.string.sett_key_brightness_mode);
    }

    public static int getBrightnessPercent(Context context) {
        return getInt(context, R.string.sett_key_brightness_percent);
    }

    public static boolean getNightModeEnabled(Context context) {
        return getBoolean(context, R.string.sett_key_night_mode);
    }

    public static int getNightStartMinutes(Context context) {
        return getInt(context, R.string.sett_key_night_start);
    }

    public static int getNightEndMinutes(Context context) {
        return getInt(context, R.string.sett_key_night_end);
    }

    /** Returns the timeout captured before night mode, or {@code -1} when none is pending. */
    public static int getSavedScreenOffTimeout(Context context) {
        return getSharedPreferences(context).getInt(key(context, R.string.sett_key_saved_screen_timeout), -1);
    }

    public static void setSavedScreenOffTimeout(Context context, int timeout) {
        getSharedPreferences(context).edit()
                .putInt(key(context, R.string.sett_key_saved_screen_timeout), timeout)
                .apply();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SETTINGS_FILENAME, Context.MODE_PRIVATE);
    }

    private static String getString(Context context, int keyResource) {
        return getSharedPreferences(context).getString(
                key(context, keyResource),
                (String) SettingsDefaults.getDefaultValueForKey(keyResource));
    }

    private static boolean getBoolean(Context context, int keyResource) {
        return getSharedPreferences(context).getBoolean(
                key(context, keyResource),
                (Boolean) SettingsDefaults.getDefaultValueForKey(keyResource));
    }

    private static int getInt(Context context, int keyResource) {
        return getSharedPreferences(context).getInt(
                key(context, keyResource),
                (Integer) SettingsDefaults.getDefaultValueForKey(keyResource));
    }

    private static String key(Context context, int keyResource) {
        return context.getString(keyResource);
    }
}
