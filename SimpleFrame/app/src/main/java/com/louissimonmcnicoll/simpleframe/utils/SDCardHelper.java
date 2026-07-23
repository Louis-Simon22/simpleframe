package com.louissimonmcnicoll.simpleframe.utils;

import android.os.Environment;

import java.io.File;

/**
 * Created by linda on 31.12.2015.
 */
public class SDCardHelper {
    File mnt;
    public SDCardHelper() {
        mnt = new File("/storage");
        if (!mnt.exists()) {
            mnt = new File("/mnt");
        }
    }

    public String getExteralStoragePath() {
        // Start at the storage root so the folder chooser exposes internal storage,
        // SD cards and USB drives instead of guessing that the last writable volume
        // is the one the user wants.
        if (mnt.exists() && mnt.isDirectory() && mnt.canRead()) {
            return mnt.getAbsolutePath();
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
