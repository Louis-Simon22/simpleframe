package com.louissimonmcnicoll.simpleframe.display;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.louissimonmcnicoll.simpleframe.settings.AppData;

public final class NightModeScheduler {
    public static final String ACTION_SLEEP =
            "com.louissimonmcnicoll.simpleframe.action.NIGHT_SLEEP";
    public static final String ACTION_WAKE =
            "com.louissimonmcnicoll.simpleframe.action.NIGHT_WAKE";
    public static final String EXTRA_DISPLAY_ACTION = "display_action";

    private static final int REQUEST_SLEEP = 7001;
    private static final int REQUEST_WAKE = 7002;

    private NightModeScheduler() {
    }

    public static void update(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager =
                (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent sleepIntent = pendingIntent(appContext, ACTION_SLEEP, REQUEST_SLEEP);
        PendingIntent wakeIntent = pendingIntent(appContext, ACTION_WAKE, REQUEST_WAKE);
        alarmManager.cancel(sleepIntent);
        alarmManager.cancel(wakeIntent);

        if (!AppData.getNightModeEnabled(appContext)) {
            DisplayController.restoreScreenOffTimeout(appContext);
            return;
        }

        schedule(
                alarmManager,
                NightSchedule.nextOccurrenceMillis(AppData.getNightStartMinutes(appContext)),
                sleepIntent);
        schedule(
                alarmManager,
                NightSchedule.nextOccurrenceMillis(AppData.getNightEndMinutes(appContext)),
                wakeIntent);
    }

    private static PendingIntent pendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, NightModeReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressLint("ScheduleExactAlarm")
    private static void schedule(AlarmManager alarmManager, long triggerAtMillis, PendingIntent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent);
        } else {
            // Android 12+ may deny exact-alarm access. The inexact fallback still
            // guarantees the transition without forcing another setup permission.
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent);
        }
    }
}
