package com.louissimonmcnicoll.simpleframe.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.louissimonmcnicoll.simpleframe.R;
import com.louissimonmcnicoll.simpleframe.display.DisplayController;
import com.louissimonmcnicoll.simpleframe.display.NightModeScheduler;
import com.louissimonmcnicoll.simpleframe.display.NightSchedule;
import com.louissimonmcnicoll.simpleframe.utils.FileUtils;
import com.louissimonmcnicoll.simpleframe.transformers.AccordionTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.BackgroundToForegroundTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.CubeOutTransformer;
import com.louissimonmcnicoll.simpleframe.custom_views.CustomViewPager;
import com.louissimonmcnicoll.simpleframe.transformers.DrawFromBackTransformer;
import com.louissimonmcnicoll.simpleframe.utils.EXIFUtils;
import com.louissimonmcnicoll.simpleframe.transformers.FadeInFadeOutTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.FlipVerticalTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ForegroundToBackgroundTransformer;
import com.louissimonmcnicoll.simpleframe.utils.Gestures;
import com.louissimonmcnicoll.simpleframe.transformers.RotateDownTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.StackTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ZoomInTransformer;
import com.louissimonmcnicoll.simpleframe.transformers.ZoomOutPageTransformer;
import com.louissimonmcnicoll.simpleframe.settings.AppData;

public class MainActivity extends AppCompatActivity {
    private static class SlideShowTimerTask extends TimerTask {
        private final WeakReference<MainActivity> mainActivityWeakReference;
        private final Handler mainThreadHandler;

        private SlideShowTimerTask(WeakReference<MainActivity> mainActivityWeakReference) {
            this.mainActivityWeakReference = mainActivityWeakReference;
            this.mainThreadHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void run() {
            MainActivity mainActivity = mainActivityWeakReference.get();
            if (mainActivity == null) {
                cancel();
            } else {
                mainThreadHandler.post(mainActivity::nextSlideshowPage);
            }
        }
    }

    private class ImagePagerAdapter extends PagerAdapter {
        private final LayoutInflater inflater;
        private final List<String> imagePaths;

        public ImagePagerAdapter(Activity activity, List<String> imagePaths) {
            this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            View viewLayout = inflater.inflate(R.layout.photo_container, container, false);
            ImageView imgDisplay = viewLayout.findViewById(R.id.photocontainer);
            imgDisplay.setScaleType(AppData.getScaling(getApplicationContext()) ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            imgDisplay.setImageBitmap(EXIFUtils.decodeFile(imagePaths.get(position), getApplicationContext()));
            imgDisplay.setOnTouchListener(showActionBarGestures);
            container.addView(viewLayout);
            return viewLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            container.removeView((RelativeLayout) object);
        }
    }

    public final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
    public final static int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 501;
    private final static boolean DEBUG = true;
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int ACTION_BAR_SHOW_DURATION = 4000;
    private static final int EMPTY_LIBRARY_RETRY_DELAY = 10_000;

    private ImagePagerAdapter imagePagerAdapter;
    private View tutorial;
    private TextView noFileFoundTextView;
    private TextView loadingSlideshowTextView;
    private TextView permissionsDeniedTextView;
    private LinearLayout permissionsExplanationLayout;
    private Button grantPermissionsButton;
    private CustomViewPager pager;
    private Timer slideshowTimer;
    private Gestures showActionBarGestures;
    private List<String> loadedImagePaths;
    private boolean paused;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private final ExecutorService fileScanExecutor = Executors.newSingleThreadExecutor();
    private Future<?> activeFileScan;
    private int fileScanGeneration;
    private View nightOverlay;
    private Handler displayScheduleHandler;
    private Handler fileScanRetryHandler;
    private boolean nightActive;
    private boolean storageReceiverRegistered;

    private final PageTransformer[] TRANSFORMERS = new PageTransformer[]{
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
    private int currentPage;
    private Handler actionbarHideHandler;
    private Handler slideshowStartHandler;
    private final Runnable displayScheduleCheck = new Runnable() {
        @Override
        public void run() {
            applyScheduledDisplayState(true);
            displayScheduleHandler.postDelayed(this, 30_000);
        }
    };
    private final Runnable emptyLibraryRetry = () -> {
        if (!nightActive && (loadedImagePaths == null || loadedImagePaths.isEmpty())) {
            startSlideshowWithPermissionsCheck();
        }
    };
    private final BroadcastReceiver storageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!nightActive) {
                startSlideshowWithPermissionsCheck();
            }
        }
    };
    public boolean mDoubleBackToExitPressedOnce;
    private boolean askedForPermissionOnce;

    // TODO add message when the permissions are not granted
    // TODO add explanation message for why permissions are required
    // TODO test on vm and on physical devices
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleScheduledWakeIntent(getIntent());
        setContentView(R.layout.main_activity);

        paused = false;
        askedForPermissionOnce = false;
        pager = findViewById(R.id.pager);
        noFileFoundTextView = findViewById(R.id.no_files_found);
        loadingSlideshowTextView = findViewById(R.id.loading_slideshow);
        permissionsDeniedTextView = findViewById(R.id.permissions_denied);
        permissionsExplanationLayout = findViewById(R.id.permissions_explanation);
        grantPermissionsButton = findViewById(R.id.grant_permissions);
        tutorial = findViewById(R.id.tutorial);
        nightOverlay = findViewById(R.id.night_overlay);

        actionbarHideHandler = new Handler(Looper.getMainLooper());
        slideshowStartHandler = new Handler(Looper.getMainLooper());
        displayScheduleHandler = new Handler(Looper.getMainLooper());
        fileScanRetryHandler = new Handler(Looper.getMainLooper());

        showActionBarGestures = new Gestures(getApplicationContext()) {
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
        View mainActivity = findViewById(R.id.main_activity);
        mainActivity.setOnTouchListener(showActionBarGestures);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                selectTransformer();
            }
        });
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    startSlideshowWithPermissionsCheck();
                });
        grantPermissionsButton.setOnClickListener(view -> requestPermissionLauncher.launch(getPermissionCompat()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        NightModeScheduler.update(this);
        displayScheduleHandler.removeCallbacks(displayScheduleCheck);
        displayScheduleHandler.postDelayed(displayScheduleCheck, 30_000);
        boolean currentlyNight = applyScheduledDisplayState(false);
        if (!currentlyNight) {
            setupTimer();
        }

        // refresh toolbar options (hide/show downloadNow)
        supportInvalidateOptionsMenu();
        if (AppData.getFirstAppStart(getApplicationContext())) {
            AppData.setFirstAppStart(getApplicationContext(), false);
        } else {
            tutorial.setVisibility(View.INVISIBLE);
        }

        pager.setVisibility(View.INVISIBLE);
        noFileFoundTextView.setVisibility(View.INVISIBLE);

        if (!currentlyNight) {
            startSlideshowWithPermissionsCheck();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter storageFilter = new IntentFilter();
        storageFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        storageFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        storageFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        storageFilter.addDataScheme("file");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(storageReceiver, storageFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(storageReceiver, storageFilter);
        }
        storageReceiverRegistered = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleScheduledWakeIntent(intent);
        applyScheduledDisplayState(true);
    }

    private void startSlideshowWithPermissionsCheck() {
        String permission = getPermissionCompat();
        permissionsExplanationLayout.setVisibility(View.INVISIBLE);
        permissionsDeniedTextView.setVisibility(View.INVISIBLE);
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startSlideshowWithDelay();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected, and what
            // features are disabled if it's declined. In this UI, include a
            // "cancel" or "no thanks" button that lets the user continue
            // using your app without granting the permission.
            permissionsExplanationLayout.setVisibility(View.VISIBLE);
        } else if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            if (askedForPermissionOnce) {
                // If we ask for the permissions here too, it results in a loop as
                // we launch the permission request activity, but it returns to this one immediately
                // if the permissions is denied.
                permissionsDeniedTextView.setVisibility(View.VISIBLE);
            } else {
                askedForPermissionOnce = true;
                requestPermissionLauncher.launch(getPermissionCompat());
            }
        }
    }

    private String getPermissionCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    private void setupTimer() {
        if (slideshowTimer == null) {
            slideshowTimer = new Timer();
        }
        SlideShowTimerTask slideShowTimerTask = new SlideShowTimerTask(new WeakReference<>(this));
        int displayTimeInMillis = AppData.getDisplayTime(this) * 1000;
        debug("Display time: " + displayTimeInMillis);
        slideshowTimer.schedule(slideShowTimerTask, displayTimeInMillis, displayTimeInMillis);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        startActivity(new Intent(this, SettingsActivity.class));
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else {
                Toast.makeText(this, R.string.main_toast_noSDReadRights, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent;
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            myIntent = new Intent(this, SettingsActivity.class);
        } else {
            return super.onOptionsItemSelected(item);
        }
        startActivity(myIntent);
        return true;
    }

    protected void onPause() {
        super.onPause();

        cancelSlideshowTimer();
        displayScheduleHandler.removeCallbacks(displayScheduleCheck);
        fileScanRetryHandler.removeCallbacks(emptyLibraryRetry);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        currentPage = pager.getCurrentItem();
    }

    public void onBackPressed() {
        if (mDoubleBackToExitPressedOnce) {
            super.onBackPressed();
        } else {
            this.mDoubleBackToExitPressedOnce = true;
            Toast.makeText(this, R.string.main_toast_exitmsg, Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> mDoubleBackToExitPressedOnce = false, 2000);
        }
    }

    public void showActionBar() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
        actionbarHideHandler.removeCallbacksAndMessages(null);
        actionbarHideHandler.postDelayed(this::hideActionBar, ACTION_BAR_SHOW_DURATION);
    }

    private void hideActionBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    private void nextSlideshowPage() {
        if (!paused && imagePagerAdapter != null && imagePagerAdapter.getCount() > 0) {
            int localpage = pager.getCurrentItem();
            localpage++;
            // We loop when reaching the end
            if (localpage == imagePagerAdapter.getCount()) {
                localpage = 0;
            }
            pager.setCurrentItem(localpage, true);
            debug("localpage " + localpage);
        }
    }

    @Override
    protected void onStop() {
        if (storageReceiverRegistered) {
            unregisterReceiver(storageReceiver);
            storageReceiverRegistered = false;
        }
        // Save the current Page to resume after next start
        AppData.setCurrentPage(getApplicationContext(), currentPage);
        debug("SAVING PAGE  " + currentPage);
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_STORAGE_ACCESS_REQUEST_CODE) {
            startSlideshow();
        }
    }

    private void startSlideshowWithDelay() {
        loadingSlideshowTextView.setVisibility(View.VISIBLE);
        // Start slideshow with a very short delay so we don't freeze on the previous activity
        slideshowStartHandler.removeCallbacksAndMessages(null);
        slideshowStartHandler.postDelayed(this::startSlideshow, 1);
    }

    private void startSlideshow() {
        if (nightActive) {
            return;
        }
        fileScanRetryHandler.removeCallbacks(emptyLibraryRetry);
        hideActionBar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        noFileFoundTextView.setVisibility(View.INVISIBLE);
        pager.setVisibility(View.INVISIBLE);
        loadingSlideshowTextView.setVisibility(View.VISIBLE);

        String imagePath = AppData.getImagePath(getApplicationContext());
        int generation = ++fileScanGeneration;
        if (activeFileScan != null) {
            activeFileScan.cancel(true);
        }
        activeFileScan = fileScanExecutor.submit(() -> {
            List<String> imagePaths = FileUtils.getFileList(getApplicationContext(), imagePath);
            runOnUiThread(() -> applyScannedImages(generation, imagePaths));
        });
    }

    private void applyScannedImages(int generation, List<String> imagePaths) {
        if (generation != fileScanGeneration || isFinishing() || isDestroyed() || nightActive) {
            return;
        }

        loadedImagePaths = imagePaths;
        imagePagerAdapter = new ImagePagerAdapter(this, loadedImagePaths);
        pager.setAdapter(imagePagerAdapter);

        currentPage = AppData.getCurrentPage(getApplicationContext());
        if (imagePagerAdapter.getCount() <= currentPage) {
            currentPage = 0;
        }
        if (currentPage > 0) {
            pager.setCurrentItem(currentPage, false);
        }
        pager.setScrollDurationFactor(8);

        loadingSlideshowTextView.setVisibility(View.INVISIBLE);
        if (imagePagerAdapter.getCount() == 0) {
            noFileFoundTextView.setVisibility(View.VISIBLE);
            pager.setVisibility(View.INVISIBLE);
            fileScanRetryHandler.postDelayed(emptyLibraryRetry, EMPTY_LIBRARY_RETRY_DELAY);
        } else {
            fileScanRetryHandler.removeCallbacks(emptyLibraryRetry);
            noFileFoundTextView.setVisibility(View.INVISIBLE);
            pager.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        fileScanGeneration++;
        if (activeFileScan != null) {
            activeFileScan.cancel(true);
        }
        fileScanExecutor.shutdownNow();
        actionbarHideHandler.removeCallbacksAndMessages(null);
        slideshowStartHandler.removeCallbacksAndMessages(null);
        displayScheduleHandler.removeCallbacksAndMessages(null);
        fileScanRetryHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private boolean applyScheduledDisplayState(boolean reactToTransition) {
        boolean shouldBeNight = NightSchedule.isNight(
                AppData.getNightModeEnabled(this),
                NightSchedule.currentMinuteOfDay(),
                AppData.getNightStartMinutes(this),
                AppData.getNightEndMinutes(this));
        boolean wasNight = nightActive;
        nightActive = shouldBeNight;
        paused = shouldBeNight;

        if (shouldBeNight) {
            cancelSlideshowTimer();
            fileScanGeneration++;
            if (activeFileScan != null) {
                activeFileScan.cancel(true);
            }
            loadingSlideshowTextView.setVisibility(View.INVISIBLE);
            DisplayController.enterNight(this, nightOverlay);
        } else {
            DisplayController.leaveNight(this, nightOverlay);
            if (reactToTransition && wasNight) {
                setupTimer();
                startSlideshowWithPermissionsCheck();
            }
        }
        return shouldBeNight;
    }

    private void cancelSlideshowTimer() {
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
            slideshowTimer = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void handleScheduledWakeIntent(Intent intent) {
        if (intent == null
                || !NightModeScheduler.ACTION_WAKE.equals(
                intent.getStringExtra(NightModeScheduler.EXTRA_DISPLAY_ACTION))) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    public void selectTransformer() {
        int[] transitionTypeValues = getResources().getIntArray(R.array.transitionTypeValues);
        int transitionStyleIndex = AppData.getTransitionStyle(getApplicationContext());
        // If the style is the random style, we randomly select another style
        if (transitionStyleIndex == transitionTypeValues.length - 1) {
            transitionStyleIndex = (int) (Math.random() * TRANSFORMERS.length);
        }
        pager.setPageTransformer(true, TRANSFORMERS[transitionStyleIndex]);
    }

    private void debug(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
