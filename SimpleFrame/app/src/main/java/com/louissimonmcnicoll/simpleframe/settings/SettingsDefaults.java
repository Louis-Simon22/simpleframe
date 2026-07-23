package com.louissimonmcnicoll.simpleframe.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import com.louissimonmcnicoll.simpleframe.R;

public final class SettingsDefaults {

    private static final Map<Integer, Object> defValues = new HashMap<>();
    static {
        defValues.put(R.string.sett_key_scaling, false);
        defValues.put(R.string.sett_key_randomize, false);
        defValues.put(R.string.sett_key_displaytime, "5");
        defValues.put(R.string.sett_key_srcpath_sd, "");
        defValues.put(R.string.sett_key_transition, "10");
        defValues.put(R.string.sett_key_brightness_mode, AppData.BRIGHTNESS_MODE_SYSTEM);
        defValues.put(R.string.sett_key_brightness_percent, 80);
        defValues.put(R.string.sett_key_night_mode, false);
        defValues.put(R.string.sett_key_night_start, 22 * 60);
        defValues.put(R.string.sett_key_night_end, 7 * 60);
    }

    public static Object getDefaultValueForKey(int key) {
        return defValues.get(key);
    }

    public static void resetSettings(Context context) {
        SharedPreferences.Editor prefEditor = AppData.getSharedPreferences(context).edit();
        for (Map.Entry<Integer, Object> prefSet : defValues.entrySet()) {
            if (prefSet.getValue() instanceof String) {
                prefEditor.putString(context.getString(prefSet.getKey()), (String) prefSet.getValue()).apply();
            } else if (prefSet.getValue() instanceof Boolean) {
                prefEditor.putBoolean(context.getString(prefSet.getKey()), (Boolean) prefSet.getValue()).apply();
            } else if (prefSet.getValue() instanceof Integer) {
                prefEditor.putInt(context.getString(prefSet.getKey()), (Integer) prefSet.getValue()).apply();
            }
        }
    }
}
