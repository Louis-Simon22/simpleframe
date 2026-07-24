package com.louissimonmcnicoll.simpleframe.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import java.io.IOException;

/**
 * Created by ClemensH on 06.04.2015.
 */
public class EXIFUtils {

    public static Bitmap decodeFile(String filePath, Context myContext) {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);

        // The new size we want to scale to ( max display resolution)
        WindowManager wm = (WindowManager) myContext.getSystemService(Context.WINDOW_SERVICE);
        int width;
        int height;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            width = wm.getCurrentWindowMetrics().getBounds().width();
            height = wm.getCurrentWindowMetrics().getBounds().height();
        } else {
            width = Resources.getSystem().getDisplayMetrics().widthPixels;
            height = Resources.getSystem().getDisplayMetrics().heightPixels;
        }
        int requiredSize = Math.max(width, height);

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth;
        int height_tmp = o.outHeight;
        int scale = 1;
        while (width_tmp > requiredSize || height_tmp > requiredSize) {
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with correct scale (inSampleSize)
        o = new BitmapFactory.Options();
        o.inSampleSize = scale;
        Bitmap b1 = BitmapFactory.decodeFile(filePath, o);
        // Rotate scaled image according to EXIF Information stored in the file
        return EXIFUtils.rotateBitmap(filePath, b1);
    }

    // @see http://sylvana.net/jpegcrop/exif_orientation.html
    public static Bitmap rotateBitmap(String src, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        try {
            int orientation = getExifOrientation(src);
            if (orientation == 1) {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                default:
                    return bitmap;
            }

            try {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return oriented;
            } catch (OutOfMemoryError e) {
                Log.e(EXIFUtils.class.getCanonicalName(), Log.getStackTraceString(e));
                return bitmap;
            }
        } catch (IOException e) {
            Log.e(EXIFUtils.class.getCanonicalName(), Log.getStackTraceString(e));
        }

        return bitmap;
    }

    private static int getExifOrientation(String src) throws IOException {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(src);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(EXIFUtils.class.getCanonicalName(), Log.getStackTraceString(e));
        }
        return orientation;
    }
}
