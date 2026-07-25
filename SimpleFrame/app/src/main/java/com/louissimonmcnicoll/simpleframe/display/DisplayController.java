package com.louissimonmcnicoll.simpleframe.display;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.louissimonmcnicoll.simpleframe.settings.AppData;

/**
 * Applies the frame's day and night display policies.
 *
 * <p>Window brightness is sufficient while SimpleFrame is visible. System brightness and the
 * screen-off timeout are also updated when Android grants {@code WRITE_SETTINGS}, allowing the
 * physical panel to turn off at night and restore its prior state in the morning.</p>
 */
public final class DisplayController {
    private static final int NIGHT_SCREEN_OFF_TIMEOUT_MS = 15_000;
    private static final long MAX_NIGHT_WAKE_LOCK_MS = 26 * 60 * 60 * 1_000L;
    private static PowerManager.WakeLock nightWakeLock;

    private DisplayController() {
    }

    /** Returns whether SimpleFrame may change global brightness and timeout settings. */
    public static boolean canWriteSystemSettings(Context context) {
        return Settings.System.canWrite(context);
    }

    /** Dims the app immediately and lets Android switch the panel off shortly afterwards. */
    public static void enterNight(Activity activity, View nightOverlay) {
        acquireNightWakeLock(activity);
        nightOverlay.setVisibility(View.VISIBLE);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setWindowBrightness(activity, 0f);

        if (canWriteSystemSettings(activity)) {
            if (AppData.getSavedScreenOffTimeout(activity) < 0) {
                int currentTimeout = Settings.System.getInt(
                        activity.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT,
                        30 * 60 * 1000);
                AppData.setSavedScreenOffTimeout(activity, currentTimeout);
            }
            Settings.System.putInt(
                    activity.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    NIGHT_SCREEN_OFF_TIMEOUT_MS);
        }
    }

    /** Restores the normal daytime window, brightness, and keep-awake behavior. */
    public static void enterDay(Activity activity, View nightOverlay) {
        restoreScreenOffTimeout(activity);
        nightOverlay.setVisibility(View.GONE);
        applyDayBrightness(activity);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        releaseNightWakeLock();
    }

    /** Applies the configured brightness to both the app window and, when allowed, the system. */
    public static void applyDayBrightness(Activity activity) {
        String mode = AppData.getBrightnessMode(activity);
        if (AppData.BRIGHTNESS_MODE_MANUAL.equals(mode)) {
            setWindowBrightness(activity, AppData.getBrightnessPercent(activity) / 100f);
        } else {
            setWindowBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
        }
        applySystemBrightness(activity);
    }

    /** Previews a manual brightness value on a window without changing saved settings. */
    public static void previewBrightness(Window window, int percent) {
        int boundedPercent = Math.max(1, Math.min(100, percent));
        setWindowBrightness(window, boundedPercent / 100f);
    }

    /** Applies automatic or manual brightness to Android's global display setting. */
    public static void applySystemBrightness(Context context) {
        if (!canWriteSystemSettings(context)) {
            return;
        }

        String mode = AppData.getBrightnessMode(context);
        if (AppData.BRIGHTNESS_MODE_AUTO.equals(mode)) {
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        } else if (AppData.BRIGHTNESS_MODE_MANUAL.equals(mode)) {
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            int brightness = Math.round(255 * AppData.getBrightnessPercent(context) / 100f);
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    Math.max(1, Math.min(255, brightness)));
        }
    }

    /** Restores the timeout saved when night mode started, if one is pending. */
    public static void restoreScreenOffTimeout(Context context) {
        int savedTimeout = AppData.getSavedScreenOffTimeout(context);
        if (savedTimeout >= 0 && canWriteSystemSettings(context)) {
            Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    savedTimeout);
            AppData.setSavedScreenOffTimeout(context, -1);
        }
    }

    /**
     * Wakes the display for a scheduled morning transition.
     *
     * <p>The deprecated screen wake-lock flags are deliberately used for compatibility with the
     * old Android version found on the target photo frame.</p>
     */
    @SuppressWarnings("deprecation")
    public static void wakeScreen(Context context) {
        releaseNightWakeLock();
        restoreScreenOffTimeout(context);
        applySystemBrightness(context);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE,
                "SimpleFrame:NightScheduleWake");
        wakeLock.acquire(10_000);
    }

    /**
     * Prevents this frame's USB host from powering down while the LCD is off.
     *
     * <p>The target firmware removes mounted USB storage during full system suspend. A partial
     * wake lock keeps the USB host alive without keeping the display on. The timeout is only a
     * leak safeguard; the normal morning transition releases the lock.</p>
     */
    private static synchronized void acquireNightWakeLock(Context context) {
        if (nightWakeLock != null && nightWakeLock.isHeld()) {
            return;
        }

        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return;
        }

        nightWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SimpleFrame:KeepUsbMountedAtNight");
        nightWakeLock.setReferenceCounted(false);
        nightWakeLock.acquire(MAX_NIGHT_WAKE_LOCK_MS);
    }

    /** Releases the overnight CPU/USB protection when night mode ends or is disabled. */
    public static synchronized void releaseNightWakeLock() {
        if (nightWakeLock != null && nightWakeLock.isHeld()) {
            nightWakeLock.release();
        }
        nightWakeLock = null;
    }

    private static void setWindowBrightness(Activity activity, float brightness) {
        setWindowBrightness(activity.getWindow(), brightness);
    }

    private static void setWindowBrightness(Window window, float brightness) {
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.screenBrightness = brightness;
        window.setAttributes(attributes);
    }
}
