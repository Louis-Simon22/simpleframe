package com.louissimonmcnicoll.simpleframe.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.louissimonmcnicoll.simpleframe.activities.MainActivity;

public class NightModeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!NightModeScheduler.ACTION_SLEEP.equals(action)
                && !NightModeScheduler.ACTION_WAKE.equals(action)) {
            return;
        }

        if (NightModeScheduler.ACTION_WAKE.equals(action)) {
            DisplayController.wakeScreen(context);
        }

        Intent activityIntent = new Intent(context, MainActivity.class)
                .putExtra(NightModeScheduler.EXTRA_DISPLAY_ACTION, action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(activityIntent);
        NightModeScheduler.update(context);
    }
}
