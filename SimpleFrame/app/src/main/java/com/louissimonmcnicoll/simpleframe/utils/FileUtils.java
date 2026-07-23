package com.louissimonmcnicoll.simpleframe.utils;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.louissimonmcnicoll.simpleframe.settings.AppData;

public class FileUtils {
    private static final Set<String> ALLOWED_EXTS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif"));

    /**
     * Returns all supported images below {@code path}, including images in subfolders.
     *
     * <p>The scan is iterative so a deeply nested photo library cannot overflow the
     * stack. Callers should run this method off the main thread for large libraries.</p>
     */
    public static List<String> getFileList(Context context, String path) {
        return getFileList(path, AppData.getRandomize(context));
    }

    public static List<String> getFileList(String path, boolean randomize) {
        List<String> files = readDirectoryTree(path);
        if (randomize) {
            Collections.shuffle(files);
        } else {
            Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        }
        return files;
    }

    private static List<String> readDirectoryTree(String path) {
        List<String> imagePaths = new ArrayList<>();
        if (path == null || path.trim().isEmpty()) {
            return imagePaths;
        }

        ArrayDeque<File> pendingDirectories = new ArrayDeque<>();
        Set<String> visitedDirectories = new HashSet<>();
        pendingDirectories.add(new File(path));

        while (!pendingDirectories.isEmpty() && !Thread.currentThread().isInterrupted()) {
            File directory = pendingDirectories.removeFirst();
            String canonicalPath;
            try {
                canonicalPath = directory.getCanonicalPath();
            } catch (IOException ignored) {
                canonicalPath = directory.getAbsolutePath();
            }
            if (!visitedDirectories.add(canonicalPath)) {
                continue;
            }

            File[] children = directory.listFiles();
            if (children == null) {
                continue;
            }
            Arrays.sort(children, (left, right) ->
                    String.CASE_INSENSITIVE_ORDER.compare(left.getName(), right.getName()));

            for (File child : children) {
                if (Thread.currentThread().isInterrupted()) {
                    return imagePaths;
                }
                if (child.isDirectory()) {
                    if (!child.isHidden() && !child.getName().startsWith(".")) {
                        pendingDirectories.addLast(child);
                    }
                } else if (isSupportedImage(child)) {
                    imagePaths.add(child.getAbsolutePath());
                }
            }
        }
        return imagePaths;
    }

    private static boolean isSupportedImage(File file) {
        String name = file.getName();
        if (file.isHidden() || name.startsWith("._")) {
            return false;
        }
        int extensionSeparator = name.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == name.length() - 1) {
            return false;
        }
        return ALLOWED_EXTS.contains(name.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT));
    }
}
