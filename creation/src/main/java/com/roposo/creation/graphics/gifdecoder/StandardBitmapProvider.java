package com.roposo.creation.graphics.gifdecoder;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

/**
 * @author Mayank on 19/03/18.
 */

public class StandardBitmapProvider implements GifDecoder.BitmapProvider {
    @NonNull
    @Override
    public Bitmap obtain(int width, int height, @NonNull Bitmap.Config config) {
        return Bitmap.createBitmap(width, height, config);
    }

    @Override
    public void release(@NonNull Bitmap bitmap) {
        bitmap.recycle();
    }

    @NonNull
    @Override
    public byte[] obtainByteArray(int size) {
        return new byte[size];
    }

    @Override
    public void release(@NonNull byte[] bytes) {

    }

    @NonNull
    @Override
    public int[] obtainIntArray(int size) {
        return new int[size];
    }

    @Override
    public void release(@NonNull int[] array) {

    }
}
