package com.louissimonmcnicoll.simpleframe.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.PageTransformer;

import com.louissimonmcnicoll.simpleframe.R;
import com.louissimonmcnicoll.simpleframe.custom_views.CustomViewPager;
import com.louissimonmcnicoll.simpleframe.display.DisplayController;
import com.louissimonmcnicoll.simpleframe.display.NightModeScheduler;
import com.louissimonmcnicoll.simpleframe.display.NightSchedule;
import com.louissimonmcnicoll.simpleframe.settings.AppData;
import com.louissimonmcnicoll.simpleframe.system.FrameControlsService;
import com.louissimonmcnicoll.simpleframe.transformers.AccordionTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.BackgroundToForegroundTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.CubeOutTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.DrawFromBackTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.FadeInFadeOutTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.FlipVerticalTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ForegroundToBackgroundTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.RotateDownTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.StackTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ZoomInTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ZoomOutPageTransformer;
import com.louissimonmcnicoll.simpleframe.utils.EXIFUtils;
import com.louissimonmcnicoll.simpleframe.utils.FileUtils;
import com.louissimonmcnicoll.simpleframe.utils.Gestures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Displays the photo library and coordinates the foreground photo-frame lifecycle.
 *
 * <p>Directory scanning runs on a worker thread. All view updates, slide changes,
 * empty-library retries, and schedule checks share one main-thread handler so they
 * can be stopped together when the activity leaves the foreground.</p>
 */
public class MainActivity extends AppCompatActivity {
    private static final long ACTION_BAR_SHOW_DURATION_MS = 4_000;
    private static final long DISPLAY_SCHEDULE_CHECK_INTERVAL_MS = 30_000;
    private static final long EMPTY_LIBRARY_RETRY_DELAY_MS = 10_000;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService fileScanExecutor = Executors.newSingleThreadExecutor();
    private final Runnable hideActionBarTask = this::hideActionBar;

    private final PageTransformer[] transformers = new PageTransformer[]{
            new AccordionTransformer(),
            new BackgroundToForegroundTransformer(),
            new CubeOutTransformer(),
            new DrawFromBackTransformer(),
            new FadeInFadeOutTransformer(),
            new FlipVerticalTransformer(),
            new ForegroundToBackgroundTransformer(),
            new RotateDownTransformer(),
            new StackTransformer(),
            new ZoomInTransformer(),
            new ZoomOutPageTransformer(),
    };

    private final Runnable advanceSlideTask = this::advanceSlideAndReschedule;
    private final Runnable displayScheduleTask = this::checkDisplaySchedule;
    private final Runnable emptyLibraryRetryTask = this::retryEmptyLibrary;

    /**
     * Removable media can mount after this HOME activity starts. The broadcast
     * triggers an immediate rescan; the timed empty-library retry covers firmware
     * that sends the mount event before the receiver is registered.
     */
    private final BroadcastReceiver storageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!nightActive) {
                startSlideshowWithPermissionCheck();
            }
        }
    };

    private ImagePagerAdapter imagePagerAdapter;
    private CustomViewPager pager;
    private View tutorial;
    private View nightOverlay;
    private TextView noFileFoundTextView;
    private TextView loadingSlideshowTextView;
    private TextView permissionsDeniedTextView;
    private LinearLayout permissionsExplanationLayout;
    private Button grantPermissionsButton;
    private Gestures actionBarGestures;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private Future<?> activeFileScan;
    private int fileScanGeneration;
    private int currentPage;
    private boolean nightActive;
    private boolean askedForPermissionOnce;
    private boolean doubleBackToExitPressed;

    /**
     * Holds only the paths needed by ViewPager. Bitmap decoding remains lazy, so
     * large libraries do not keep every image in memory.
     */
    private final class ImagePagerAdapter extends PagerAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        private final List<String> imagePaths;

        private ImagePagerAdapter(List<String> imagePaths) {
            this.imagePaths = new ArrayList<>(imagePaths);
        }

        @Override
        public int getCount() {
            return imagePaths.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View photoView = inflater.inflate(R.layout.photo_container, container, false);
            ImageView imageView = photoView.findViewById(R.id.photocontainer);
            imageView.setScaleType(AppData.getScaling(MainActivity.this)
                    ? ImageView.ScaleType.CENTER_CROP
                    : ImageView.ScaleType.FIT_CENTER);
            imageView.setImageBitmap(
                    EXIFUtils.decodeFile(imagePaths.get(position), MainActivity.this));
            imageView.setOnTouchListener(actionBarGestures);
            container.addView(photoView);
            return photoView;
        }

        @Override
        public void destroyItem(
                @NonNull ViewGroup container,
                int position,
                @NonNull Object object) {
            container.removeView((RelativeLayout) object);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleScheduledWakeIntent(getIntent());
        setContentView(R.layout.main_activity);

        bindViews();
        configureGestures();
        configurePager();
        configurePermissionRequest();
    }

    private void bindViews() {
        pager = findViewById(R.id.pager);
        tutorial = findViewById(R.id.tutorial);
        nightOverlay = findViewById(R.id.night_overlay);
        noFileFoundTextView = findViewById(R.id.no_files_found);
        loadingSlideshowTextView = findViewById(R.id.loading_slideshow);
        permissionsDeniedTextView = findViewById(R.id.permissions_denied);
        permissionsExplanationLayout = findViewById(R.id.permissions_explanation);
        grantPermissionsButton = findViewById(R.id.grant_permissions);
    }

    private void configureGestures() {
        actionBarGestures = new Gestures(this) {
            @Override
            public void onSwipeBottom() {
                showActionBar();
            }

            @Override
            public void onSwipeTop() {
                hideActionBar();
            }

            @Override
            public void onTap() {
                showActionBar();
            }
        };
        findViewById(R.id.main_activity).setOnTouchListener(actionBarGestures);
    }

    private void configurePager() {
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                selectTransformer();
            }
        });
    }

    private void configurePermissionRequest() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                ignored -> startSlideshowWithPermissionCheck());
        grantPermissionsButton.setOnClickListener(
                ignored -> requestPermissionLauncher.launch(requiredImagePermission()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerStorageReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // The return control is useful only while another app covers SimpleFrame.
        FrameControlsService.dismissReturnOverlay(this);
        NightModeScheduler.update(this);
        applyDisplayState(true);
        startDisplayScheduleChecks();

        supportInvalidateOptionsMenu();
        if (AppData.getFirstAppStart(this)) {
            AppData.setFirstAppStart(this, false);
        } else {
            tutorial.setVisibility(View.INVISIBLE);
        }

        pager.setVisibility(View.INVISIBLE);
        noFileFoundTextView.setVisibility(View.INVISIBLE);
        if (!nightActive) {
            startDaytimePlayback();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleScheduledWakeIntent(intent);
        if (applyDisplayState(false) && !nightActive) {
            startDaytimePlayback();
        }
    }

    @Override
    protected void onPause() {
        stopPlayback();
        uiHandler.removeCallbacks(displayScheduleTask);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        currentPage = pager.getCurrentItem();
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(storageReceiver);
        AppData.setCurrentPage(this, currentPage);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelFileScan();
        fileScanExecutor.shutdownNow();
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void registerStorageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(storageReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(storageReceiver, filter);
        }
    }

    private void startDaytimePlayback() {
        scheduleNextSlide();
        startSlideshowWithPermissionCheck();
    }

    private void stopPlayback() {
        uiHandler.removeCallbacks(advanceSlideTask);
        uiHandler.removeCallbacks(emptyLibraryRetryTask);
        cancelFileScan();
    }

    private void startSlideshowWithPermissionCheck() {
        String permission = requiredImagePermission();
        permissionsExplanationLayout.setVisibility(View.INVISIBLE);
        permissionsDeniedTextView.setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            scanPhotoLibrary();
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            permissionsExplanationLayout.setVisibility(View.VISIBLE);
        } else if (askedForPermissionOnce) {
            // Avoid reopening a permission dialog that Android has already denied.
            permissionsDeniedTextView.setVisibility(View.VISIBLE);
        } else {
            askedForPermissionOnce = true;
            requestPermissionLauncher.launch(permission);
        }
    }

    private String requiredImagePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /**
     * Starts a cancellable recursive scan. The generation check prevents a
     * canceled or slower scan from replacing a newer result.
     */
    private void scanPhotoLibrary() {
        if (nightActive) {
            return;
        }

        uiHandler.removeCallbacks(emptyLibraryRetryTask);
        hideActionBar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        noFileFoundTextView.setVisibility(View.INVISIBLE);
        pager.setVisibility(View.INVISIBLE);
        loadingSlideshowTextView.setVisibility(View.VISIBLE);

        cancelFileScan();
        int generation = fileScanGeneration;
        String imagePath = AppData.getImagePath(this);
        boolean randomize = AppData.getRandomize(this);
        activeFileScan = fileScanExecutor.submit(() -> {
            List<String> imagePaths = FileUtils.getFileList(imagePath, randomize);
            runOnUiThread(() -> applyScannedImages(generation, imagePaths));
        });
    }

    private void cancelFileScan() {
        fileScanGeneration++;
        if (activeFileScan != null) {
            activeFileScan.cancel(true);
            activeFileScan = null;
        }
    }

    private void applyScannedImages(int generation, List<String> imagePaths) {
        if (generation != fileScanGeneration || isFinishing() || isDestroyed() || nightActive) {
            return;
        }
        activeFileScan = null;

        imagePagerAdapter = new ImagePagerAdapter(imagePaths);
        pager.setAdapter(imagePagerAdapter);
        restoreCurrentPage(imagePagerAdapter.getCount());
        pager.setScrollDurationFactor(8);

        loadingSlideshowTextView.setVisibility(View.INVISIBLE);
        boolean libraryIsEmpty = imagePagerAdapter.getCount() == 0;
        noFileFoundTextView.setVisibility(libraryIsEmpty ? View.VISIBLE : View.INVISIBLE);
        pager.setVisibility(libraryIsEmpty ? View.INVISIBLE : View.VISIBLE);
        if (libraryIsEmpty) {
            uiHandler.postDelayed(emptyLibraryRetryTask, EMPTY_LIBRARY_RETRY_DELAY_MS);
        }
    }

    private void restoreCurrentPage(int imageCount) {
        currentPage = AppData.getCurrentPage(this);
        if (currentPage < 0 || currentPage >= imageCount) {
            currentPage = 0;
        }
        if (currentPage > 0) {
            pager.setCurrentItem(currentPage, false);
        }
    }

    private void retryEmptyLibrary() {
        if (!nightActive && (imagePagerAdapter == null || imagePagerAdapter.getCount() == 0)) {
            startSlideshowWithPermissionCheck();
        }
    }

    private void scheduleNextSlide() {
        uiHandler.removeCallbacks(advanceSlideTask);
        if (!nightActive) {
            long delayMillis = Math.max(1, AppData.getDisplayTime(this)) * 1_000L;
            uiHandler.postDelayed(advanceSlideTask, delayMillis);
        }
    }

    private void advanceSlideAndReschedule() {
        nextSlideshowPage();
        scheduleNextSlide();
    }

    private void nextSlideshowPage() {
        if (nightActive || imagePagerAdapter == null || imagePagerAdapter.getCount() == 0) {
            return;
        }
        int nextPage = (pager.getCurrentItem() + 1) % imagePagerAdapter.getCount();
        pager.setCurrentItem(nextPage, true);
    }

    private void startDisplayScheduleChecks() {
        uiHandler.removeCallbacks(displayScheduleTask);
        uiHandler.postDelayed(displayScheduleTask, DISPLAY_SCHEDULE_CHECK_INTERVAL_MS);
    }

    private void checkDisplaySchedule() {
        if (applyDisplayState(false) && !nightActive) {
            startDaytimePlayback();
        }
        uiHandler.postDelayed(displayScheduleTask, DISPLAY_SCHEDULE_CHECK_INTERVAL_MS);
    }

    /**
     * Applies the current day/night state and reports whether it changed.
     * Expensive playback work only happens when a transition actually occurs.
     */
    private boolean applyDisplayState(boolean force) {
        boolean shouldBeNight = NightSchedule.isNight(
                AppData.getNightModeEnabled(this),
                NightSchedule.currentMinuteOfDay(),
                AppData.getNightStartMinutes(this),
                AppData.getNightEndMinutes(this));
        boolean changed = shouldBeNight != nightActive;
        if (!force && !changed) {
            return false;
        }

        nightActive = shouldBeNight;
        if (nightActive) {
            stopPlayback();
            loadingSlideshowTextView.setVisibility(View.INVISIBLE);
            DisplayController.enterNight(this, nightOverlay);
        } else {
            DisplayController.enterDay(this, nightOverlay);
        }
        return changed;
    }

    @SuppressWarnings("deprecation")
    private void handleScheduledWakeIntent(Intent intent) {
        if (intent == null || !NightModeScheduler.ACTION_WAKE.equals(intent.getAction())) {
            return;
        }

        // Alarm receivers may run while the panel is asleep. Set the flag before
        // drawing so the resumed activity can make the display visible immediately.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    private void selectTransformer() {
        int[] transitionValues = getResources().getIntArray(R.array.transitionTypeValues);
        int transitionIndex = AppData.getTransitionStyle(this);
        if (transitionIndex == transitionValues.length - 1) {
            transitionIndex = (int) (Math.random() * transformers.length);
        }
        pager.setPageTransformer(true, transformers[transitionIndex]);
    }

    private void showActionBar() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
        uiHandler.removeCallbacks(hideActionBarTask);
        uiHandler.postDelayed(hideActionBarTask, ACTION_BAR_SHOW_DURATION_MS);
    }

    private void hideActionBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        openSettings();
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            openSettings();
            return true;
        }
        if (itemId == R.id.action_android_settings) {
            openAndroidSettings();
            return true;
        }
        if (itemId == R.id.action_power) {
            openPowerDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /**
     * Opens Android Settings only after the trusted return overlay is ready.
     * This prevents hardware-button-free frames from becoming trapped in the
     * external Settings app.
     */
    private void openAndroidSettings() {
        if (!FrameControlsService.isConnected()) {
            showFrameControlsSetupDialog();
            return;
        }
        if (!FrameControlsService.requestReturnOverlay(this)) {
            Toast.makeText(this, R.string.frame_controls_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    /** Opens the firmware power/restart dialog through the accessibility service. */
    private void openPowerDialog() {
        if (!FrameControlsService.isConnected()) {
            showFrameControlsSetupDialog();
            return;
        }
        if (!FrameControlsService.showPowerDialog()) {
            Toast.makeText(this, R.string.frame_controls_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Accessibility is required for global power actions and for an overlay the
     * Android Settings app cannot suppress. If enabled while that screen is open,
     * the pending return button appears immediately and brings the user back.
     */
    private void showFrameControlsSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.frame_controls_setup_title)
                .setMessage(R.string.frame_controls_setup_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.frame_controls_setup_open, (dialog, which) -> {
                    FrameControlsService.requestReturnOverlay(this);
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressed) {
            super.onBackPressed();
            return;
        }
        doubleBackToExitPressed = true;
        Toast.makeText(this, R.string.main_toast_exitmsg, Toast.LENGTH_SHORT).show();
        uiHandler.postDelayed(() -> doubleBackToExitPressed = false, 2_000);
    }
}
