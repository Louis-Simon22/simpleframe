package com.louissimonmcnicoll.simpleframe.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import com.louissimonmcnicoll.simpleframe.R;
import com.louissimonmcnicoll.simpleframe.display.DisplayController;
import com.louissimonmcnicoll.simpleframe.display.NightModeScheduler;
import com.louissimonmcnicoll.simpleframe.settings.AppData;
import com.louissimonmcnicoll.simpleframe.settings.SettingsDefaults;
import com.louissimonmcnicoll.simpleframe.settings.SimpleFileDialog;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final int[] EDITABLE_FIELD_IDS = new int[]{
            R.string.sett_key_displaytime,
            R.string.sett_key_transition,
            R.string.sett_key_srcpath_sd,
    };

    private PreferenceCategory sourceSettingsPreferenceCategory;
    private SharedPreferences sharedPreferences;
    private Preference brightnessPreference;
    private Preference nightStartPreference;
    private Preference nightEndPreference;
    private boolean permissionExplanationShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(AppData.mySettingsFilename);
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);
        sharedPreferences = AppData.getSharedPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (DEBUG) printAllPreferences();
        addPreferencesFromResource(R.xml.settings);
        sourceSettingsPreferenceCategory = (PreferenceCategory) findPreference("sett_key_cat1");

        updateAllTextFields();
        setupFolderPicker();
        setupBrightnessPicker();
        setupTimePicker(R.string.sett_key_night_start, true);
        setupTimePicker(R.string.sett_key_night_end, false);
        updateDisplayPreferences();
    }

    private void printAllPreferences() {
        Map<String, ?> keyMap = sharedPreferences.getAll();
        for (String e : keyMap.keySet()) {
            debug("DUMP| Key: " + e + " ++ Value: " + keyMap.get(e));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupFolderPicker() {
        Preference folderPicker;
        folderPicker = new Preference(this);
        setFolderPickerTitle(folderPicker);
        folderPicker.setSummary(getString(R.string.sett_picture_folder_summary));
        folderPicker.setDefaultValue(SettingsDefaults.getDefaultValueForKey(R.string.sett_key_srcpath_sd));
        folderPicker.setKey(getString(R.string.sett_key_srcpath_sd));
        Context self = this;
        folderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            String _chosenDir = AppData.getSourcePath(self);

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // The code in this function will be executed when the dialog OK button is pushed
                SimpleFileDialog FolderChooseDialog =
                        new SimpleFileDialog(
                                self,
                                chosenDir -> {
                                    _chosenDir = chosenDir;
                                    AppData.setSdSourcePath(self, _chosenDir);
                                });
                FolderChooseDialog.chooseFileOrDir(_chosenDir);
                return true;
            }
        });
        sourceSettingsPreferenceCategory.addPreference(folderPicker);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences != null && key != null) {
            // If we change the image path, we also want to reset the current page to start over
            if (key.equals(getString(R.string.sett_key_srcpath_sd))) {
                // We use commit() and not apply() because we want the update to be immediate
                sharedPreferences.edit().putInt(getString(R.string.sett_key_currentPage), 1).commit();
            }
            updateAllTextFields();
            updateDisplayPreferences();
            NightModeScheduler.update(this);
            if (key.equals(getString(R.string.sett_key_brightness_mode))
                    || key.equals(getString(R.string.sett_key_brightness_percent))) {
                DisplayController.applySystemBrightness(this);
            }
            if ((key.equals(getString(R.string.sett_key_brightness_mode))
                    && AppData.BRIGHTNESS_MODE_AUTO.equals(AppData.getBrightnessMode(this)))
                    || (key.equals(getString(R.string.sett_key_night_mode))
                    && AppData.getNightModeEnabled(this))) {
                offerSystemSettingsPermission();
            }
            debug("CHANGED| Key:" + key + " ++ Value: " + sharedPreferences.getAll().get(key));
        }
    }

    public void updateAllTextFields() {
        for (int fieldId : EDITABLE_FIELD_IDS) {
            updateTextField(fieldId);
        }
    }

    public void updateTextField(int fieldId) {
        String key = getString(fieldId);
        Preference pref = findPreference(key);
        if (pref != null) {
            String defaultValue = (String) SettingsDefaults.getDefaultValueForKey(fieldId);
            pref.setDefaultValue(defaultValue);
            String prefValue = "";
            if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                prefValue = (String) sharedPreferences.getAll().get(key);
                if (prefValue == null) {
                    prefValue = defaultValue;
                }
                if (!Objects.equals(prefValue, listPref.getValue())) {
                    listPref.setValue(prefValue);
                }
                int index = listPref.findIndexOfValue(prefValue);
                prefValue = (String) listPref.getEntries()[index];
            } else if (pref instanceof EditTextPreference) {
                prefValue = (String) sharedPreferences.getAll().get(key);
                if (prefValue == null) {
                    prefValue = defaultValue;
                }
            }
            if (getString(R.string.sett_key_displaytime).equals(key)) {
                String prefTitle = getString(R.string.sett_displayTime);
                pref.setTitle(prefTitle + ": " + prefValue);
            } else if (getString(R.string.sett_key_transition).equals(key)) {
                String prefTitle = getString(R.string.sett_transition);
                pref.setTitle(prefTitle + ": " + prefValue);
            } else if (getString(R.string.sett_key_srcpath_sd).equals(key)) {
                setFolderPickerTitle(pref);
            }
        }
    }

    private void setFolderPickerTitle(Preference folderPickerPref) {
        String imagePath = AppData.getImagePath(getApplicationContext());
        // Make the path a bit mo
        imagePath = imagePath.replace("/storage/emulated/0/", "");
        folderPickerPref.setTitle(imagePath.isEmpty()
                ? getString(R.string.sett_choose_picture_folder)
                : imagePath);
    }

    private void setupBrightnessPicker() {
        brightnessPreference = findPreference(getString(R.string.sett_key_brightness_percent));
        brightnessPreference.setOnPreferenceClickListener(preference -> {
            int initialBrightness = AppData.getBrightnessPercent(this);
            int horizontalPadding = Math.round(24 * getResources().getDisplayMetrics().density);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(horizontalPadding, 0, horizontalPadding, 0);

            TextView value = new TextView(this);
            value.setText(getString(R.string.sett_brightness_dialog_title, initialBrightness));
            value.setTextSize(18);
            content.addView(value);

            SeekBar seekBar = new SeekBar(this);
            seekBar.setMax(99);
            seekBar.setProgress(initialBrightness - 1);
            content.addView(seekBar);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    value.setText(getString(R.string.sett_brightness_dialog_title, progress + 1));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            new AlertDialog.Builder(this)
                    .setTitle(R.string.sett_brightness_percent)
                    .setView(content)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            sharedPreferences.edit()
                                    .putInt(
                                            getString(R.string.sett_key_brightness_percent),
                                            seekBar.getProgress() + 1)
                                    .apply())
                    .show();
            return true;
        });
    }

    private void setupTimePicker(int keyResource, boolean startTime) {
        Preference preference = findPreference(getString(keyResource));
        if (startTime) {
            nightStartPreference = preference;
        } else {
            nightEndPreference = preference;
        }
        preference.setOnPreferenceClickListener(clickedPreference -> {
            int initialMinutes = startTime
                    ? AppData.getNightStartMinutes(this)
                    : AppData.getNightEndMinutes(this);
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) ->
                            sharedPreferences.edit()
                                    .putInt(getString(keyResource), hourOfDay * 60 + minute)
                                    .apply(),
                    initialMinutes / 60,
                    initialMinutes % 60,
                    android.text.format.DateFormat.is24HourFormat(this))
                    .show();
            return true;
        });
    }

    private void updateDisplayPreferences() {
        if (brightnessPreference == null || nightStartPreference == null || nightEndPreference == null) {
            return;
        }

        ListPreference brightnessMode =
                (ListPreference) findPreference(getString(R.string.sett_key_brightness_mode));
        String mode = AppData.getBrightnessMode(this);
        if (!Objects.equals(mode, brightnessMode.getValue())) {
            brightnessMode.setValue(mode);
        }
        int modeIndex = brightnessMode.findIndexOfValue(mode);
        if (modeIndex >= 0) {
            brightnessMode.setSummary(brightnessMode.getEntries()[modeIndex]);
        }
        if (AppData.BRIGHTNESS_MODE_AUTO.equals(mode) && !hasAmbientLightSensor()) {
            brightnessMode.setSummary(R.string.sett_auto_brightness_no_sensor);
        }

        brightnessPreference.setEnabled(AppData.BRIGHTNESS_MODE_MANUAL.equals(mode));
        brightnessPreference.setSummary(
                getString(R.string.sett_brightness_value, AppData.getBrightnessPercent(this)));

        boolean nightEnabled = AppData.getNightModeEnabled(this);
        nightStartPreference.setEnabled(nightEnabled);
        nightEndPreference.setEnabled(nightEnabled);
        nightStartPreference.setSummary(formatTime(AppData.getNightStartMinutes(this)));
        nightEndPreference.setSummary(formatTime(AppData.getNightEndMinutes(this)));
    }

    private boolean hasAmbientLightSensor() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        return sensorManager != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null;
    }

    private String formatTime(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60);
        calendar.set(Calendar.MINUTE, minutes % 60);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        return timeFormat.format(new Date(calendar.getTimeInMillis()));
    }

    private void offerSystemSettingsPermission() {
        if (DisplayController.canWriteSystemSettings(this) || permissionExplanationShown) {
            return;
        }
        permissionExplanationShown = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.sett_system_settings_permission_title)
                .setMessage(R.string.sett_system_settings_permission_message)
                .setNegativeButton(R.string.sett_system_settings_permission_later, null)
                .setPositiveButton(R.string.sett_system_settings_permission_allow, (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (brightnessPreference != null) {
            updateDisplayPreferences();
            DisplayController.applySystemBrightness(this);
            NightModeScheduler.update(this);
        }
    }

    @Override
    protected void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        NightModeScheduler.update(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void debug(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
