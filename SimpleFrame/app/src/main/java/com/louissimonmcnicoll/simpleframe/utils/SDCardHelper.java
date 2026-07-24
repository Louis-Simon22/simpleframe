package com.louissimonmcnicoll.simpleframe.utils;

import android.os.Environment;

import java.io.File;

/** Finds a useful root for the folder picker across Android storage layouts. */
public final class SDCardHelper {
    private SDCardHelper() {
    }

    public static String getStorageRootPath() {
        // Start at the storage root so the folder chooser exposes internal storage,
        // SD cards and USB drives instead of guessing that the last writable volume
        // is the one the user wants.
        File storageRoot = new File("/storage");
        if (!storageRoot.exists()) {
            storageRoot = new File("/mnt");
        }
        if (storageRoot.isDirectory() && storageRoot.canRead()) {
            return storageRoot.getAbsolutePath();
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
