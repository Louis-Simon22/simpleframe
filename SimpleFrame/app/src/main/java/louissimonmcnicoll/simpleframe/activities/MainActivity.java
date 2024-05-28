package louissimonmcnicoll.simpleframe.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import louissimonmcnicoll.simpleframe.R;
import louissimonmcnicoll.simpleframe.utils.FileUtils;
import louissimonmcnicoll.simpleframe.utils.transformers.AccordionTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.BackgroundToForegroundTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.CubeOutTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.CustomViewPager;
import louissimonmcnicoll.simpleframe.utils.transformers.DrawFromBackTransformer;
import louissimonmcnicoll.simpleframe.utils.EXIFUtils;
import louissimonmcnicoll.simpleframe.utils.transformers.FadeInFadeOutTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.FlipVerticalTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.ForegroundToBackgroundTransformer;
import louissimonmcnicoll.simpleframe.utils.Gestures;
import louissimonmcnicoll.simpleframe.utils.transformers.RotateDownTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.StackTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.ZoomInTransformer;
import louissimonmcnicoll.simpleframe.utils.transformers.ZoomOutPageTransformer;
import louissimonmcnicoll.simpleframe.settings.AppData;

// TODO handle first time instructions
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
        private int localPage;

        public ImagePagerAdapter(Activity activity) {
            this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            updateSettings();
        }

        @Override
        public int getCount() {
            return pictureCount;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            View viewLayout = inflater.inflate(R.layout.fullscreen_layout, container, false);

            ImageView imgDisplay = viewLayout.findViewById(R.id.photocontainer);

            if (AppData.getScaling(getApplicationContext())) {
                imgDisplay.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imgDisplay.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
            this.localPage = position;
            imgDisplay.setImageBitmap(EXIFUtils.decodeFile(mFilePaths.get(this.localPage), getApplicationContext()));
            imgDisplay.setOnTouchListener(new Gestures(getApplicationContext()) {
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
            });
            container.addView(viewLayout);
            return viewLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            container.removeView((RelativeLayout) object);
        }

        private void updateSettings() {
            mFilePaths = FileUtils.getFileList(getApplicationContext(), AppData.getImagePath(getApplicationContext()));
        }

        public int getPage() {
            return this.localPage;
        }
    }

    public final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501;
    public final static int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 501;
    private final static boolean DEBUG = true;
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int ACTION_BAR_SHOW_DURATION = 4000;

    private ImagePagerAdapter imagePagerAdapter;
    private TextView noFileFoundTextView;
    private CustomViewPager pager;
    private Timer slideshowTimer;

    private String mOldPath;
    private boolean paused;

    private final PageTransformer[] TRANSFORMERS = new PageTransformer[]{new ZoomOutPageTransformer(), new AccordionTransformer(), new BackgroundToForegroundTransformer(), new CubeOutTransformer(), new DrawFromBackTransformer(), new FadeInFadeOutTransformer(), new FlipVerticalTransformer(), new ForegroundToBackgroundTransformer(), new RotateDownTransformer(), new StackTransformer(), new ZoomInTransformer(), new ZoomOutPageTransformer(),};
    private List<String> mFilePaths;
    private int pictureCount;
    private int currentPage;
    private Handler actionbarHideHandler;
    public boolean mDoubleBackToExitPressedOnce;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paused = false;
        pager = findViewById(R.id.pager);
        noFileFoundTextView = findViewById(R.id.no_file_found);
        loadAdapter();

        mOldPath = AppData.getImagePath(getApplicationContext());

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

        currentPage = AppData.getCurrentPage(getApplicationContext());
        if (Objects.requireNonNull(pager.getAdapter()).getCount() < currentPage) {
            currentPage = 1;
        }
        AppData.getSharedPreferences(this).registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key != null) {
                if (key.equals(getString(R.string.sett_key_transition))
                        || key.equals(getString(R.string.sett_key_randomize))
                        || key.equals(getString(R.string.sett_key_srcpath_sd))
                        || key.equals(getString(R.string.sett_key_scaling))
                ) {
                    startSlideshow();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupTimer();

        // refresh toolbar options (hide/show downloadNow)
        supportInvalidateOptionsMenu();
        if (AppData.getFirstAppStart(getApplicationContext())) {
            AppData.setFirstAppStart(getApplicationContext(), false);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
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

        slideshowTimer.cancel();
        slideshowTimer = null;

        mOldPath = AppData.getImagePath(getApplicationContext());

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
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            actionBar.show();
        }
        if (actionbarHideHandler != null) {
            actionbarHideHandler.removeCallbacksAndMessages(null);
            actionbarHideHandler = null;
        }
        actionbarHideHandler = new Handler(Looper.getMainLooper());
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
        if (imagePagerAdapter.getCount() > 0 && !paused) {
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
        super.onStop();
        // Save the current Page to resume after next start
        // maybe not right here will test
        AppData.setCurrentPage(getApplicationContext(), currentPage);
        debug("SAVING PAGE  " + currentPage);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_STORAGE_ACCESS_REQUEST_CODE) {
            startSlideshow();
        }
    }

    private void startSlideshow() {
        if (!FileUtils.getFileList(getApplicationContext(), AppData.getImagePath(getApplicationContext())).isEmpty()) {
            if (!AppData.getImagePath(getApplicationContext()).equals(mOldPath)) {
                loadAdapter();
            }
        }

        mFilePaths = FileUtils.getFileList(getApplicationContext(), AppData.getImagePath(getApplicationContext()));
        pictureCount = mFilePaths.size();
        imagePagerAdapter.notifyDataSetChanged();

        // start on the page we left in onPause, unless it was the first or last picture (as this freezes the slideshow)
        if (currentPage < Objects.requireNonNull(pager.getAdapter()).getCount() - 1 && currentPage > 0) {
            pager.setCurrentItem(currentPage);
        }
        pager.setScrollDurationFactor(8);

        hideActionBar();

        noFileFoundTextView.setVisibility(pictureCount == 0 ? View.INVISIBLE : View.VISIBLE);
    }

    private void loadAdapter() {
        imagePagerAdapter = new ImagePagerAdapter(this);
        pager.setAdapter(imagePagerAdapter);
        currentPage = imagePagerAdapter.getPage();
    }

    public void selectTransformer() {
        int[] transitionTypeValues = getResources().getIntArray(R.array.transitionTypeValues);
        int transitionStyleIndex = AppData.getTransitionStyle(getApplicationContext());
        // If the style is the random style, we cycle randomly select another style
        if (transitionStyleIndex == transitionTypeValues[transitionTypeValues.length - 1]) {
            transitionStyleIndex = transitionTypeValues[(int) (Math.random() * (transitionTypeValues.length - 1))];
        }
        pager.setPageTransformer(true, TRANSFORMERS[transitionStyleIndex]);
    }

    private void debug(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}

