package com.roposo.core.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.TypedValue;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


public class BitmapFactoryUtils {
    private BitmapFactoryUtils() {
    }

    @Nullable
    public static Bitmap decodeFile(final String filename, final int minSize, final boolean square) {
        return decodeFile(filename, minSize, square, true);
    }

    @Nullable
    public static Bitmap decodeFile(final String filename, final int minSize, final boolean square, boolean fixRotation) {
        final int angle = ExifUtils.getAngle(filename);

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, opts);

        final int size = Math.max(opts.outWidth, opts.outHeight);
        if (size > minSize && minSize > 0) {
            opts.inSampleSize = size / minSize;
        } else {
            opts.inSampleSize = 1;
        }

        Bitmap bitmap = decodeFile(filename, opts.inSampleSize, 0, 2);
        if(null == bitmap) {
            HashMap<String, String> map = new HashMap<>();
            map.put("path", filename);
            map.put("sampleSize", String.valueOf(opts.inSampleSize));
            EventTrackUtil.logDebug("decodeFile", "decodeFile", "BitmapFactoryUtils", map, 4);
            return null;
        }
        if (angle != 0 && fixRotation) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            final Bitmap _bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            bitmap.recycle();
            bitmap = _bitmap;
        }
        if (square && bitmap.getWidth() != bitmap.getHeight()) {
            if (bitmap.getWidth() > bitmap.getHeight()) {
                final Bitmap _bitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - bitmap.getHeight()) / 2, 0, bitmap.getHeight(), bitmap.getHeight());
                bitmap.recycle();
                bitmap = _bitmap;
            } else if (bitmap.getWidth() < bitmap.getHeight()) {
                final Bitmap _bitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight() - bitmap.getWidth()) / 2, bitmap.getWidth(), bitmap.getWidth());
                bitmap.recycle();
                bitmap = _bitmap;
            }
        }
        return bitmap;
    }

    public static int getImageRotation(final String filename) {
        return ExifUtils.getAngle(filename);
    }


    public static Bitmap
    decodeResource(final Resources res, @DrawableRes @RawRes final int resId, final int minSize) {

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, opts);

        final int size = Math.max(opts.outWidth, opts.outHeight);

        if (size > minSize) {
            opts.inSampleSize = size / minSize;
        } else {
            opts.inSampleSize = 1;
        }

        opts.inJustDecodeBounds = false;

        return BitmapFactory.decodeResource(res, resId, opts);
    }

    public static Bitmap decodeStream(final InputStream is) {
        return decodeStream(is, 1, 0, 2);
    }

    public static Bitmap decodeStream(final InputStream is, final int startInSampleSize, final int add, final int multi) {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        int inSampleSize = startInSampleSize;
        while (true) {
            opts.inSampleSize = inSampleSize;
            opts.inDither = true;
            try {
                return BitmapFactory.decodeStream(is, null, opts);
            } catch (final OutOfMemoryError e) {
                inSampleSize = (inSampleSize + add) * multi;
            }
        }
    }

    public static Bitmap decodeResource(final Resources res, @DrawableRes @RawRes final int resId) {
        return decodeResource(res, resId, 1, 0, 2);
    }

    public static Bitmap decodeResource(final Resources res, @DrawableRes @RawRes final int resId, final int startInSampleSize, final int add, final int multi) {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        int inSampleSize = startInSampleSize;
        while (true) {
            opts.inSampleSize = inSampleSize;
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                return BitmapFactory.decodeResource(res, resId, opts);
            } catch (final OutOfMemoryError e) {
                inSampleSize = (inSampleSize + add) * multi;
            }
        }
    }

    public static Bitmap decodeFile(final String pathName) {
        return decodeFile(pathName, 1, 0, 2);
    }

    public static Bitmap decodeFile(final String pathName, int maxSize) {
        return decodeFile(pathName, maxSize, false);
    }

    @Nullable
    public static Bitmap decodeFile(final String pathName, final int startInSampleSize, final int add, final int multi) {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        int inSampleSize = startInSampleSize;
        while (true) {
            opts.inSampleSize = inSampleSize;
            opts.inDither = false;
            opts.inMutable = true;
            try {
                return BitmapFactory.decodeFile(pathName, opts);
            } catch (final OutOfMemoryError e) {
                // TODO sahil , add a condition to break this loop
                inSampleSize = (inSampleSize + add) * multi;
            }
        }
    }

    public static Bitmap decodeStream(final Context context, final String name, final BitmapFactory.Options opts) throws FileNotFoundException {
        final InputStream in = new BufferedInputStream(context.openFileInput(name));
        try {
            return BitmapFactory.decodeStream(in, null, opts);
        } finally {
            try {
                in.close();
            } catch (final IOException e) {
            }
        }
    }


    public static Bitmap decodeByteArray(final byte[] data, final Bitmap.Config config) {
        return decodeByteArray(data, 0, data.length, config);
    }

    public static Bitmap decodeByteArray(final byte[] data, final int offset, final int length, final Bitmap.Config config) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, offset, length);
        if (bitmap.getConfig().compareTo(config) == 0) {
            return bitmap;
        }
        final int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        bitmap.recycle();

        return Bitmap.createBitmap(pixels, bitmap.getWidth(), bitmap.getHeight(), config);
    }

    public static boolean checkIsSvgResource(@DrawableRes @RawRes int resourceID) {
        if(null == ContextHelper.getContext()) {
            return false;
        }
        // TODO :: someting getContext is null here , how ?

        final Resources resources = ContextHelper.getContext().getResources();
        boolean isSvg;
        try {
            final String resourceTypeName = resources.getResourceTypeName(resourceID);

            if (resourceTypeName.contains("raw")) {
                final TypedValue value = new TypedValue();
                resources.getValue(resourceID, value, true); //Get file Name
                isSvg = value.string.toString().toLowerCase().endsWith(".svg"); //Is file suffix .SVG
            } else {
                isSvg = false; //SVG must be RAW
            }

        } catch (Resources.NotFoundException notFound) {
            isSvg = false;
        }

        return isSvg;
    }
}
