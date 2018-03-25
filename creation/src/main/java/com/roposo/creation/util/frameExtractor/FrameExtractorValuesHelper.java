package com.roposo.creation.util.frameExtractor;

import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.FileUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mayank on 24/01/18.
 */

public class FrameExtractorValuesHelper {

    public static VideoFrameExtractor.MediaInfo obtainValues(@NonNull String filePath,
                                                             int initialFrameWidth,
                                                             int initialFrameHeight,
                                                             float pixelPerMilliSecond,
                                                             float scaleDownFactor) {

        int frameWidth = initialFrameWidth;
        int frameHeight = initialFrameHeight;

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);
        int w = 0, h = 0;
        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        if (width != null) {
            w = Integer.parseInt(width);
        }
        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if (height != null) {
            h = Integer.parseInt(height);
        }

        String orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (null != orientation) {
            int angle = Integer.parseInt(orientation);
            if (angle == 90 || angle == 270) {
                int temp = h;
                h = w;
                w = temp;
            }
        }


        if (w != 0 && h != 0) {
            frameWidth = (int) ((w * scaleDownFactor * frameHeight) / h);
        }


        String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long videoDuration = Long.parseLong(duration);

        int numFrames = (int) ((pixelPerMilliSecond * videoDuration) / frameWidth);
        int lastFrameWidth = ((int) (pixelPerMilliSecond * videoDuration)) % frameWidth;
        if (lastFrameWidth > 0) {
            numFrames++;
        } else {
            lastFrameWidth = frameWidth;
        }

        int frameTimeOffset = (int) (videoDuration / numFrames);

        int frameWidths[] =  new int[numFrames];
        int frameTimeOffsets[] = new int[numFrames];

        for (int i = 0; i < numFrames - 1; i++) {
            frameWidths[i] = frameWidth;
            frameTimeOffsets[i] = i * frameTimeOffset;
        }

        frameWidths[numFrames - 1] = lastFrameWidth;
        frameTimeOffsets[numFrames - 1] = (numFrames - 1) * frameTimeOffset;

        return new VideoFrameExtractor.MediaInfo(
                new VideoFrameExtractor.MediaDurationInfo(filePath, 0, videoDuration),
                numFrames,
                frameWidths,
                frameTimeOffsets,
                frameHeight
        );
    }


    public static List<VideoFrameExtractor.MediaInfo> extractValues(List<VideoFrameExtractor.MediaDurationInfo> mediaDurationInfos,
                                                                     int frameDuration,
                                                                     int frameWidth,
                                                                     int frameHeight) {
        ArrayList<VideoFrameExtractor.MediaInfo> mediaInfos = new ArrayList<>(mediaDurationInfos.size());

        for (int i = 0; i < mediaDurationInfos.size(); i++) {
            VideoFrameExtractor.MediaDurationInfo mediaDurationInfo = mediaDurationInfos.get(i);
            String filePath = mediaDurationInfo.filePath;
            int numFrames, lastFrameWidth;
            if (FileUtilities.isFileTypeImage(filePath)) {
                mediaInfos.add(new VideoFrameExtractor.MediaInfo(mediaDurationInfo,
                        1,
                        new int[]{ frameWidth },
                        new int[]{ 0 },
                        frameHeight));
            } else {

                if (mediaDurationInfo.playStart < 0) mediaDurationInfo.playStart = 0;
                if (mediaDurationInfo.playEnd < 0) mediaDurationInfo.playEnd = 0;

                long videoDuration = AndroidUtilities.getAVDuration(filePath);

                if (mediaDurationInfo.playStart >= videoDuration) continue;
                if (mediaDurationInfo.playEnd > videoDuration)
                    mediaDurationInfo.playEnd = videoDuration;

                if (mediaDurationInfo.playEnd == 0) mediaDurationInfo.playEnd = videoDuration;

                long obtainedVideoDuration = mediaDurationInfo.playEnd - mediaDurationInfo.playStart;

                if (obtainedVideoDuration > 0) {
                    videoDuration = obtainedVideoDuration;
                }

                numFrames = (int) Math.ceil((float)videoDuration / frameDuration);
                float newFrameDuration = (float) videoDuration / numFrames;
                float delta = 0F;
                int[] frameWidths = new int[numFrames];
                int[] frameTimeOffsets = new int[numFrames];
                float newFrameWidthF = frameWidth * (newFrameDuration / frameDuration);

                for (int j = 0; j < numFrames; j++) {
                    int newFrameWidth = (int) (newFrameWidthF + delta);
                    delta = newFrameWidthF - newFrameWidth;
                    frameWidths[j] = newFrameWidth;

                    frameTimeOffsets[j] = (int) (j * newFrameDuration);
                }
                mediaInfos.add(new VideoFrameExtractor.MediaInfo(mediaDurationInfo,
                        numFrames,
                        frameWidths,
                        frameTimeOffsets,
                        frameHeight));
            }
        }

        return mediaInfos;
    }
}
