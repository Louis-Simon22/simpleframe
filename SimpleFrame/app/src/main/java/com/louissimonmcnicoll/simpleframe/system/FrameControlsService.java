package com.louissimonmcnicoll.simpleframe.system;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageButton;

import com.louissimonmcnicoll.simpleframe.R;
import com.louissimonmcnicoll.simpleframe.activities.MainActivity;

/**
 * Provides the two system-level controls a hardware-button-free photo frame needs.
 *
 * <p>The power dialog is an accessibility global action. The return button uses
 * {@link WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY}, which Android
 * treats as a trusted accessibility window rather than a normal “draw over other
 * apps” window. That distinction keeps the button usable over Android Settings.</p>
 *
 * <p>The requested-overlay flag is persisted because Android may reconnect an
 * accessibility service while Settings is already in front. Reconnection then
 * recreates the return button without relying on a live activity instance.</p>
 */
public final class FrameControlsService extends AccessibilityService {
    private static final String STATE_PREFERENCES = "frame_controls_state";
    private static final String KEY_RETURN_OVERLAY_REQUESTED =
            "return_overlay_requested";

    // Android owns this service's lifecycle. Both disconnect callbacks clear the
    // reference, so it cannot outlive the system-managed service instance.
    @SuppressLint("StaticFieldLeak")
    private static FrameControlsService connectedService;

    private WindowManager windowManager;
    private ImageButton returnButton;

    /** Returns whether Android currently has the accessibility service connected. */
    public static boolean isConnected() {
        return connectedService != null;
    }

    /** Requests Android's standard Power off/Restart dialog. */
    public static boolean showPowerDialog() {
        FrameControlsService service = connectedService;
        return service != null
                && service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }

    /**
     * Persists and displays the return control before Android Settings is opened.
     *
     * @return {@code true} when the service is connected and the overlay exists
     */
    public static boolean requestReturnOverlay(Context context) {
        setReturnOverlayRequested(context, true);
        FrameControlsService service = connectedService;
        return service != null && service.showReturnButton();
    }

    /** Clears the pending state and removes the button when SimpleFrame resumes. */
    public static void dismissReturnOverlay(Context context) {
        setReturnOverlayRequested(context, false);
        FrameControlsService service = connectedService;
        if (service != null) {
            service.hideReturnButton();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        connectedService = this;
        if (isReturnOverlayRequested(this)) {
            showReturnButton();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No screen content is inspected; the service only performs explicit controls.
    }

    @Override
    public void onInterrupt() {
        // There is no continuous feedback to interrupt.
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    /** Adds one compact, orientation-independent button at the right screen edge. */
    private boolean showReturnButton() {
        if (returnButton != null) {
            return true;
        }
        ImageButton button = new ImageButton(this);
        button.setImageResource(R.drawable.ic_return_to_frame);
        button.setBackgroundResource(R.drawable.frame_return_button_background);
        button.setContentDescription(getString(R.string.frame_controls_return));
        int padding = dp(14);
        button.setPadding(padding, padding, padding, padding);
        button.setElevation(dp(8));
        button.setOnClickListener(ignored -> returnToSimpleFrame());

        WindowManager.LayoutParams parameters = new WindowManager.LayoutParams(
                dp(64),
                dp(64),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        parameters.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        parameters.x = dp(12);
        parameters.setTitle(getString(R.string.frame_controls_return));

        try {
            windowManager.addView(button, parameters);
            returnButton = button;
            return true;
        } catch (RuntimeException ignored) {
            // A disconnected WindowManager can race a service shutdown. The caller
            // reports the failure and Android may reconnect the service later.
            return false;
        }
    }

    private void returnToSimpleFrame() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        dismissReturnOverlay(this);
    }

    private void hideReturnButton() {
        if (returnButton == null) {
            return;
        }
        try {
            windowManager.removeView(returnButton);
        } catch (IllegalArgumentException ignored) {
            // The system may already have detached the view during service shutdown.
        }
        returnButton = null;
    }

    private void disconnect() {
        hideReturnButton();
        if (connectedService == this) {
            connectedService = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean isReturnOverlayRequested(Context context) {
        return preferences(context).getBoolean(KEY_RETURN_OVERLAY_REQUESTED, false);
    }

    private static void setReturnOverlayRequested(Context context, boolean requested) {
        preferences(context).edit()
                .putBoolean(KEY_RETURN_OVERLAY_REQUESTED, requested)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE);
    }
}
