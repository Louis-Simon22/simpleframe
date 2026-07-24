package com.louissimonmcnicoll.simpleframe.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import com.louissimonmcnicoll.simpleframe.R;

/** Single source of truth for preference defaults and reset behavior. */
public final class SettingsDefaults {

    private static final Map<Integer, Object> DEFAULT_VALUES = new HashMap<>();

    static {
        DEFAULT_VALUES.put(R.string.sett_key_scaling, false);
        DEFAULT_VALUES.put(R.string.sett_key_randomize, false);
        DEFAULT_VALUES.put(R.string.sett_key_displaytime, "5");
        DEFAULT_VALUES.put(R.string.sett_key_srcpath_sd, "");
        DEFAULT_VALUES.put(R.string.sett_key_transition, "10");
        DEFAULT_VALUES.put(R.string.sett_key_brightness_mode, AppData.BRIGHTNESS_MODE_SYSTEM);
        DEFAULT_VALUES.put(R.string.sett_key_brightness_percent, 80);
        DEFAULT_VALUES.put(R.string.sett_key_night_mode, false);
        DEFAULT_VALUES.put(R.string.sett_key_night_start, 22 * 60);
        DEFAULT_VALUES.put(R.string.sett_key_night_end, 7 * 60);
    }

    private SettingsDefaults() {
    }

    public static Object getDefaultValueForKey(int key) {
        return DEFAULT_VALUES.get(key);
    }

    /** Writes every default in one atomic preference edit. */
    public static void resetSettings(Context context) {
        SharedPreferences.Editor prefEditor = AppData.getSharedPreferences(context).edit();
        for (Map.Entry<Integer, Object> prefSet : DEFAULT_VALUES.entrySet()) {
            String key = context.getString(prefSet.getKey());
            if (prefSet.getValue() instanceof String) {
                prefEditor.putString(key, (String) prefSet.getValue());
            } else if (prefSet.getValue() instanceof Boolean) {
                prefEditor.putBoolean(key, (Boolean) prefSet.getValue());
            } else if (prefSet.getValue() instanceof Integer) {
                prefEditor.putInt(key, (Integer) prefSet.getValue());
            }
        }
        prefEditor.apply();
    }
}
