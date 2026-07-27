package com.louissimonmcnicoll.simpleframe.activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.louissimonmcnicoll.simpleframe.R;
import com.louissimonmcnicoll.simpleframe.display.DisplayController;
import com.louissimonmcnicoll.simpleframe.display.NightModeScheduler;
import com.louissimonmcnicoll.simpleframe.settings.AppData;
import com.louissimonmcnicoll.simpleframe.settings.SettingsDefaults;
import com.louissimonmcnicoll.simpleframe.settings.SimpleFileDialog;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Presents the app's small set of photo, slideshow, and display preferences.
 *
 * <p>The project still uses the platform {@link PreferenceActivity} for
 * compatibility with the existing UI. Dynamic preferences are limited to
 * controls that need custom dialogs: folder, brightness, and schedule times.</p>
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private ListPreference brightnessModePreference;
    private Preference brightnessPreference;
    private Preference nightStartPreference;
    private Preference nightEndPreference;
    private boolean permissionExplanationShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(AppData.SETTINGS_FILENAME);
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        addPreferencesFromResource(R.xml.settings);

        sharedPreferences = AppData.getSharedPreferences(this);
        setupFolderPicker();
        setupDisplayPreferences();
        refreshPreferenceSummaries();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root =
                (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar =
                (Toolbar) LayoutInflater.from(this)
                        .inflate(R.layout.settings_toolbar, root, false);
        root.addView(toolbar, 0);
        toolbar.setNavigationOnClickListener(ignored -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPreferenceSummaries();
        DisplayController.applyDayBrightness(this);
        NightModeScheduler.update(this);
    }

    @Override
    protected void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key == null) {
            return;
        }

        if (key.equals(getString(R.string.sett_key_srcpath_sd))) {
            AppData.setCurrentPage(this, 0);
        }

        refreshPreferenceSummaries();
        NightModeScheduler.update(this);

        if (isBrightnessPreference(key)) {
            DisplayController.applyDayBrightness(this);
        }
        if (needsSystemSettingsPermission(key)) {
            offerSystemSettingsPermission();
        }
    }

    private void setupFolderPicker() {
        Preference folderPreference = new Preference(this);
        folderPreference.setKey(getString(R.string.sett_key_srcpath_sd));
        folderPreference.setSummary(R.string.sett_picture_folder_summary);
        folderPreference.setOnPreferenceClickListener(ignored -> {
            SimpleFileDialog dialog = new SimpleFileDialog(
                    this,
                    chosenDirectory -> AppData.setSdSourcePath(this, chosenDirectory));
            dialog.chooseFileOrDir(AppData.getImagePath(this));
            return true;
        });

        PreferenceCategory sourceCategory =
                (PreferenceCategory) findPreference("sett_key_cat1");
        sourceCategory.addPreference(folderPreference);
    }

    private void setupDisplayPreferences() {
        brightnessModePreference =
                (ListPreference) findPreference(getString(R.string.sett_key_brightness_mode));
        brightnessPreference =
                findPreference(getString(R.string.sett_key_brightness_percent));
        nightStartPreference =
                findPreference(getString(R.string.sett_key_night_start));
        nightEndPreference =
                findPreference(getString(R.string.sett_key_night_end));

        brightnessPreference.setOnPreferenceClickListener(ignored -> {
            showBrightnessDialog();
            return true;
        });
        setupTimePicker(nightStartPreference, true);
        setupTimePicker(nightEndPreference, false);
    }

    private void refreshPreferenceSummaries() {
        updateFolderPreference();
        updateListPreferenceTitle(
                R.string.sett_key_displaytime,
                R.string.sett_displayTime);
        updateListPreferenceTitle(
                R.string.sett_key_transition,
                R.string.sett_transition);
        updateDisplayPreferenceSummaries();
    }

    private void updateFolderPreference() {
        Preference folderPreference =
                findPreference(getString(R.string.sett_key_srcpath_sd));
        if (folderPreference == null) {
            return;
        }

        String imagePath = AppData.getImagePath(this);
        String shortPath = imagePath.replace("/storage/emulated/0/", "");
        folderPreference.setTitle(shortPath.isEmpty()
                ? getString(R.string.sett_choose_picture_folder)
                : shortPath);
    }

    private void updateListPreferenceTitle(int keyResource, int titleResource) {
        String key = getString(keyResource);
        ListPreference preference = (ListPreference) findPreference(key);
        if (preference == null) {
            return;
        }

        String defaultValue =
                (String) SettingsDefaults.getDefaultValueForKey(keyResource);
        String value = sharedPreferences.getString(key, defaultValue);
        if (!value.equals(preference.getValue())) {
            preference.setValue(value);
        }

        int entryIndex = preference.findIndexOfValue(value);
        CharSequence displayValue = entryIndex >= 0
                ? preference.getEntries()[entryIndex]
                : value;
        preference.setTitle(getString(titleResource) + ": " + displayValue);
    }

    private void updateDisplayPreferenceSummaries() {
        if (brightnessModePreference == null) {
            return;
        }

        String brightnessMode = AppData.getBrightnessMode(this);
        if (!brightnessMode.equals(brightnessModePreference.getValue())) {
            brightnessModePreference.setValue(brightnessMode);
        }

        int modeIndex = brightnessModePreference.findIndexOfValue(brightnessMode);
        if (modeIndex >= 0) {
            brightnessModePreference.setSummary(
                    brightnessModePreference.getEntries()[modeIndex]);
        }
        if (AppData.BRIGHTNESS_MODE_AUTO.equals(brightnessMode)
                && !hasAmbientLightSensor()) {
            brightnessModePreference.setSummary(R.string.sett_auto_brightness_no_sensor);
        }

        brightnessPreference.setEnabled(
                AppData.BRIGHTNESS_MODE_MANUAL.equals(brightnessMode));
        brightnessPreference.setSummary(
                getString(
                        R.string.sett_brightness_value,
                        AppData.getBrightnessPercent(this)));

        boolean nightEnabled = AppData.getNightModeEnabled(this);
        nightStartPreference.setEnabled(nightEnabled);
        nightEndPreference.setEnabled(nightEnabled);
        nightStartPreference.setSummary(formatTime(AppData.getNightStartMinutes(this)));
        nightEndPreference.setSummary(formatTime(AppData.getNightEndMinutes(this)));
    }

    private void showBrightnessDialog() {
        int initialBrightness = AppData.getBrightnessPercent(this);
        TextView valueLabel = new TextView(this);
        valueLabel.setText(
                getString(R.string.sett_brightness_dialog_title, initialBrightness));
        valueLabel.setTextSize(18);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(99);
        seekBar.setProgress(initialBrightness - 1);
        int seekBarHeight =
                Math.round(64 * getResources().getDisplayMetrics().density);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                seekBarHeight));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);
        content.addView(valueLabel);
        content.addView(seekBar);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.sett_brightness_percent)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (ignoredDialog, which) ->
                        sharedPreferences.edit()
                                .putInt(
                                        getString(R.string.sett_key_brightness_percent),
                                        seekBar.getProgress() + 1)
                                .apply())
                .create();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                    SeekBar changedSeekBar,
                    int progress,
                    boolean fromUser) {
                int percent = progress + 1;
                valueLabel.setText(
                        getString(R.string.sett_brightness_dialog_title, percent));
                DisplayController.previewBrightness(dialog.getWindow(), percent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar changedSeekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar changedSeekBar) {
            }
        });

        dialog.setOnShowListener(
                ignored -> DisplayController.previewBrightness(
                        dialog.getWindow(),
                        initialBrightness));
        dialog.show();
    }

    private void setupTimePicker(Preference preference, boolean startTime) {
        preference.setOnPreferenceClickListener(ignored -> {
            int initialMinutes = startTime
                    ? AppData.getNightStartMinutes(this)
                    : AppData.getNightEndMinutes(this);
            int keyResource = startTime
                    ? R.string.sett_key_night_start
                    : R.string.sett_key_night_end;

            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) ->
                            sharedPreferences.edit()
                                    .putInt(
                                            getString(keyResource),
                                            hourOfDay * 60 + minute)
                                    .apply(),
                    initialMinutes / 60,
                    initialMinutes % 60,
                    android.text.format.DateFormat.is24HourFormat(this))
                    .show();
            return true;
        });
    }

    private String formatTime(int minutes) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, minutes / 60);
        time.set(Calendar.MINUTE, minutes % 60);
        DateFormat formatter = android.text.format.DateFormat.getTimeFormat(this);
        return formatter.format(time.getTime());
    }

    private boolean hasAmbientLightSensor() {
        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        return sensorManager != null
                && sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null;
    }

    private boolean isBrightnessPreference(String key) {
        return key.equals(getString(R.string.sett_key_brightness_mode))
                || key.equals(getString(R.string.sett_key_brightness_percent));
    }

    private boolean needsSystemSettingsPermission(String changedKey) {
        boolean automaticBrightnessSelected =
                changedKey.equals(getString(R.string.sett_key_brightness_mode))
                        && AppData.BRIGHTNESS_MODE_AUTO.equals(
                        AppData.getBrightnessMode(this));
        boolean nightModeEnabled =
                changedKey.equals(getString(R.string.sett_key_night_mode))
                        && AppData.getNightModeEnabled(this);
        return automaticBrightnessSelected || nightModeEnabled;
    }

    /**
     * Manual window brightness works without special access. Automatic system
     * brightness and a real screen timeout require Android's WRITE_SETTINGS grant.
     */
    private void offerSystemSettingsPermission() {
        if (DisplayController.canWriteSystemSettings(this)
                || permissionExplanationShown) {
            return;
        }

        permissionExplanationShown = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.sett_system_settings_permission_title)
                .setMessage(R.string.sett_system_settings_permission_message)
                .setNegativeButton(R.string.sett_system_settings_permission_later, null)
                .setPositiveButton(
                        R.string.sett_system_settings_permission_allow,
                        (dialog, which) -> startActivity(new Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + getPackageName()))))
                .show();
    }
}
