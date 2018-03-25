package com.roposo.creation.util.frameExtractor;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.BitmapFactoryUtils;
import com.roposo.core.util.FileUtilities;
import com.roposo.creation.graphics.gles.LruBitmapCache;
import com.roposo.creation.util.BitmapExtensionsKt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mayank on 08/11/17.
 */

public class VideoFrameExtractor {

    private AsyncTask<Void, Void, Void> frameExtractingTask;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private int noOfFramesReceived = 0;
    private FrameExtractorAsyncTask.OnFrameObtainedListener onFrameObtainedListener;
    private List<MediaInfo> mediaInfos;
    private MediaInfo currentMediaInfo;
    private int currentMediaInfoIndex = 0;
    private int accumulativeIndexTillCurrentFrame = 0;

    private FrameExtractorAsyncTask.OnFrameObtainedListener onFrameExtractedListener = new FrameExtractorAsyncTask.OnFrameObtainedListener() {
        @Override
        public void onFrameReceived(int index, Bitmap bitmap) {

            onFrameObtainedListener.onFrameReceived(accumulativeIndexTillCurrentFrame + index, bitmap);

            noOfFramesReceived++;
            if (noOfFramesReceived >= currentMediaInfo.numberOfFrames) {
                startExtractingFramesForNextMedia();
            } else {
                startExtractingFrame(++index);
            }
        }
    };

    public VideoFrameExtractor(@NonNull MediaInfo mediaInfo,
                               @NonNull FrameExtractorAsyncTask.OnFrameObtainedListener onFrameObtainedListener) {
        mediaMetadataRetriever = new MediaMetadataRetriever();
        this.onFrameObtainedListener = onFrameObtainedListener;
        mediaInfos = new ArrayList<>(1);
        mediaInfos.add(mediaInfo);
    }


    public VideoFrameExtractor(@NonNull List<MediaInfo> mediaInfos,
                               @NonNull FrameExtractorAsyncTask.OnFrameObtainedListener onFrameObtainedListener) {
        if (mediaInfos.size() == 0) {
            return;
        }
        mediaMetadataRetriever = new MediaMetadataRetriever();
        this.onFrameObtainedListener = onFrameObtainedListener;
        this.mediaInfos = mediaInfos;
    }

    public void start() {

        cancelFrameExtractorAsyncTask();

        if (!canStart()) {
            onDestroy();
            return;
        }

        if (currentMediaInfoIndex != 0) {
            accumulativeIndexTillCurrentFrame += currentMediaInfo.numberOfFrames;
        }

        currentMediaInfo = mediaInfos.get(currentMediaInfoIndex);

        if (FileUtilities.isFileTypeImage(currentMediaInfo.mediaDurationInfo.filePath)) {

            String key = currentMediaInfo.mediaDurationInfo.filePath
                    + "_" + currentMediaInfo.frameWidths[0] + "x" + currentMediaInfo.frameHeight;
            Bitmap cachedBitmap = LruBitmapCache.INSTANCE.get(key);
            if (cachedBitmap == null) {

                int[] size = new int[2];
                AndroidUtilities.getImageSize(currentMediaInfo.mediaDurationInfo.filePath, size);
                Rect srcRect = new Rect(0, 0, size[0], size[1]);
                Rect finalRect = new Rect(0, 0, currentMediaInfo.frameWidths[0], currentMediaInfo.frameHeight);
                Rect rect = new Rect();
                AndroidUtilities.scaleCenter(srcRect, finalRect, rect, true);

                int maxDimen = Math.max(rect.width(), rect.height());
                cachedBitmap = BitmapExtensionsKt.getScaledBitmap(BitmapFactoryUtils.decodeFile(currentMediaInfo.mediaDurationInfo.filePath, maxDimen), rect.width(), rect.height());

                LruBitmapCache.INSTANCE.put(key, cachedBitmap);

            }

            onFrameObtainedListener.onFrameReceived(accumulativeIndexTillCurrentFrame, cachedBitmap);

            startExtractingFramesForNextMedia();
        } else {
            mediaMetadataRetriever.setDataSource(currentMediaInfo.mediaDurationInfo.filePath);
            startExtractingFrame(0);
        }
    }

    private boolean canStart() {
        return mediaMetadataRetriever != null &&
                mediaInfos != null &&
                !mediaInfos.isEmpty() &&
                currentMediaInfoIndex < mediaInfos.size() &&
                noOfFramesReceived == 0 &&
                frameExtractingTask == null;
    }

    private void startExtractingFrame(int frameIndex) {

        if (frameIndex >= currentMediaInfo.numberOfFrames) {
            onDestroy();
            return;
        }

        frameExtractingTask = new FrameExtractorAsyncTask(mediaMetadataRetriever,
                currentMediaInfo.mediaDurationInfo.filePath,
                currentMediaInfo.mediaDurationInfo.playStart,
                frameIndex,
                currentMediaInfo.frameTimeOffsets[frameIndex],
                currentMediaInfo.frameWidths[frameIndex],
                currentMediaInfo.frameHeight,
                onFrameExtractedListener);
        frameExtractingTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void startExtractingFramesForNextMedia() {
        noOfFramesReceived = 0;
        currentMediaInfoIndex++;
        start();
    }

    public void onDestroy() {
        if (mediaMetadataRetriever != null) {
            mediaMetadataRetriever.release();
        }
        cancelFrameExtractorAsyncTask();
    }

    private void cancelFrameExtractorAsyncTask() {
        if (frameExtractingTask != null) {
            frameExtractingTask.cancel(true);
            frameExtractingTask = null;
        }
    }

    public static class MediaInfo {
        public int numberOfFrames;
        public int frameWidths[];
        public int frameHeight;
        private MediaDurationInfo mediaDurationInfo;
        private int frameTimeOffsets[];

        MediaInfo(MediaDurationInfo mediaDurationInfo, int numberOfFrames, int frameWidths[], int frameTimeOffsets[], int frameHeight) {
            this.mediaDurationInfo = mediaDurationInfo;
            this.numberOfFrames = numberOfFrames;
            this.frameTimeOffsets = frameTimeOffsets;
            this.frameWidths = frameWidths;
            this.frameHeight = frameHeight;
        }
    }

    public static class MediaDurationInfo {
        public String filePath;
        public long playStart = -1, playEnd = -1;

        public MediaDurationInfo(String filePath, long playStart, long playEnd) {
            this.filePath = filePath;
            this.playStart = playStart;
            this.playEnd = playEnd;
        }
    }
}
