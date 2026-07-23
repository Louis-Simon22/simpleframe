package com.louissimonmcnicoll.simpleframe.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.louissimonmcnicoll.simpleframe.R;

/**
 * Stores App Settings, to get and load easily
 * Created by ClemensH on 04.04.2015.
 */
public class AppData {
    public static final String mySettingsFilename = "SimpleFrameSettings";
    public static final String BRIGHTNESS_MODE_SYSTEM = "system";
    public static final String BRIGHTNESS_MODE_MANUAL = "manual";
    public static final String BRIGHTNESS_MODE_AUTO = "auto";

    // holds the time to display each picture in seconds
    public static int getDisplayTime(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.sett_key_displaytime),
                (String) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_displaytime)));
    }

    // holds the int of the transitionStyle - @res.values.arrays.transitionTypeValues
    public static int getTransitionStyle(Context context) {
        return Integer.parseInt(
                getSharedPreferences(context).getString(context.getString(R.string.sett_key_transition),
                        (String) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_transition)));
    }

    // flag whether to randomize the order of the displayed images (on=true)
    public static boolean getRandomize(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.sett_key_randomize),
                (Boolean) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_randomize));
    }

    // flag whether to scale the displayed image (on=true)
    public static boolean getScaling(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.sett_key_scaling),
                (Boolean) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_scaling));
    }

    // Returns selected SD-Card directory, or URL to owncloud or samba server
    // holds the path to the image source (from where to (down)-load them
    public static String getSourcePath(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.sett_key_srcpath_sd),
                (String) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_srcpath_sd));
    }

    // Always returns the path to the img folder of current src type
    // holds the root-path to the displayed images
    public static String getImagePath(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.sett_key_srcpath_sd),
                (String) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_srcpath_sd));
    }

    public static void setSdSourcePath(Context context, String path) {
        getSharedPreferences(context).edit().putString(context.getString(R.string.sett_key_srcpath_sd), path).apply();
    }

    // flag whether this is the first app start
    public static boolean getFirstAppStart(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.sett_key_firstStart), false);
    }

    public static void setFirstAppStart(Context context, boolean firstAppStart) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.sett_key_firstStart), firstAppStart).apply();
    }

    public static int getCurrentPage(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.sett_key_currentPage), 1);
    }

    public static void setCurrentPage(Context context, int page) {
        getSharedPreferences(context).edit().putInt(context.getString(R.string.sett_key_currentPage), page).apply();
    }

    public static String getBrightnessMode(Context context) {
        return getSharedPreferences(context).getString(
                context.getString(R.string.sett_key_brightness_mode),
                (String) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_brightness_mode));
    }

    public static int getBrightnessPercent(Context context) {
        return getSharedPreferences(context).getInt(
                context.getString(R.string.sett_key_brightness_percent),
                (Integer) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_brightness_percent));
    }

    public static boolean getNightModeEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(
                context.getString(R.string.sett_key_night_mode),
                (Boolean) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_night_mode));
    }

    public static int getNightStartMinutes(Context context) {
        return getSharedPreferences(context).getInt(
                context.getString(R.string.sett_key_night_start),
                (Integer) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_night_start));
    }

    public static int getNightEndMinutes(Context context) {
        return getSharedPreferences(context).getInt(
                context.getString(R.string.sett_key_night_end),
                (Integer) SettingsDefaults.getDefaultValueForKey(R.string.sett_key_night_end));
    }

    public static int getSavedScreenOffTimeout(Context context) {
        return getSharedPreferences(context).getInt(
                context.getString(R.string.sett_key_saved_screen_timeout), -1);
    }

    public static void setSavedScreenOffTimeout(Context context, int timeout) {
        getSharedPreferences(context).edit()
                .putInt(context.getString(R.string.sett_key_saved_screen_timeout), timeout)
                .apply();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(mySettingsFilename, Context.MODE_PRIVATE);
    }
}
