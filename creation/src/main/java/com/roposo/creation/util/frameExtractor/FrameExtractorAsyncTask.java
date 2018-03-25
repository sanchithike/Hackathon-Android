package com.roposo.creation.util.frameExtractor;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.MyLogger;
import com.roposo.creation.graphics.gles.LruBitmapCache;
import com.roposo.creation.util.BitmapExtensionsKt;

/**
 * @author Mayank on 17/11/17.
 */

public class FrameExtractorAsyncTask extends AsyncTask<Void, Void, Void> {

    private int frameTimeOffset, frameWidth, frameHeight;
    private OnFrameObtainedListener onFrameObtainedListener;
    private int frameNum = 0;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private Bitmap bitmap = null;
    private long startDurationInMillis;
    private String filePath;


    FrameExtractorAsyncTask(MediaMetadataRetriever mediaMetadataRetriever,
                            String filePath,
                            long startDurationInMillis,
                            int frameNum,
                            int frameTimeOffset,
                            int frameWidth,
                            int frameHeight,
                            @NonNull OnFrameObtainedListener onFrameObtainedListener) {
        this.mediaMetadataRetriever = mediaMetadataRetriever;
        this.filePath = filePath;
        if (startDurationInMillis < 0) startDurationInMillis = 0;
        this.startDurationInMillis = startDurationInMillis;
        this.frameNum = frameNum;
        this.frameTimeOffset = frameTimeOffset;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.onFrameObtainedListener = onFrameObtainedListener;
    }

    @Override
    protected Void doInBackground(Void... objects) {
        if (isCancelled() || mediaMetadataRetriever == null) {
            return null;
        }
        try {
            long timeUs = startDurationInMillis + (frameTimeOffset * 1000);
            String key = filePath + "_" + frameWidth + "x" + frameHeight + "_" + timeUs;
            bitmap = LruBitmapCache.INSTANCE.get(key);
            if (bitmap == null) {
                bitmap = mediaMetadataRetriever.getFrameAtTime(timeUs);
                if (isCancelled() || bitmap == null) {
                    return null;
                }
                Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                Rect finalRect = new Rect(0, 0, frameWidth, frameHeight);
                Rect rect = new Rect();
                AndroidUtilities.scaleCenter(srcRect, finalRect, rect, true);
                bitmap = BitmapExtensionsKt.getScaledBitmap(bitmap, rect.width(), rect.height());
                LruBitmapCache.INSTANCE.put(key, bitmap);
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
            MyLogger.e("rps_msg", e.toString());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        if (!isCancelled() && bitmap != null && !bitmap.isRecycled()) {
            if (onFrameObtainedListener != null) {
                onFrameObtainedListener.onFrameReceived(frameNum, bitmap);
            }
        }
    }

    public interface OnFrameObtainedListener {
        void onFrameReceived(int index, Bitmap bitmap);
    }
}

