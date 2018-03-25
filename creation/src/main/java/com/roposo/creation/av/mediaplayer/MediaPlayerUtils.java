package com.roposo.creation.av.mediaplayer;

import android.view.Surface;

import com.roposo.creation.av.MediaExtractorCallback;

import java.io.IOException;

/**
 * @author bajaj on 14/02/18.
 */

public class MediaPlayerUtils {
    /**
     * Unspecified media player error.
     *
     * @see OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    /**
     * Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     *
     * @see OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    /**
     * The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     *
     * @see OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    /**
     * File or network related operation errors.
     */
    public static final int MEDIA_ERROR_IO = -1004;
    /**
     * Bitstream is not conforming to the related coding standard or file spec.
     */
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    /**
     * Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature.
     */
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    /**
     * Some operation takes too long to complete, usually more than 3-5 seconds.
     */
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    /**
     * The player just pushed the very first video frame for rendering.
     *
     * @see OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    /**
     * The video is too complex for the decoder: it can't decode frames fast
     * enough. Possibly only the audio plays fine at this stage.
     *
     * @see OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    /**
     * MediaPlayer is temporarily pausing playback internally in order to
     * buffer more data.
     *
     * @see OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    /**
     * MediaPlayer is resuming playback after filling buffers.
     *
     * @see OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    public static final long BUFFER_LOW_WATER_MARK_US = 2000000; // 2 seconds; NOTE: make sure this is below DashMediaExtractor's mMinBufferTimeUs
    public static final int MEDIA_PREPARED = 1;
    public static final int MEDIA_PLAYBACK_COMPLETE = 2;
    public static final int MEDIA_BUFFERING_UPDATE = 3;
    public static final int MEDIA_SEEK_COMPLETE = 4;
    public static final int MEDIA_SET_VIDEO_SIZE = 5;
    public static final int MEDIA_PROGRESS = 6;
    public static final int MEDIA_ERROR = 100;
    public static final int MEDIA_INFO = 200;

    /**
     * Interface definition for a callback to be invoked when the media
     * source is ready for playback.
     */
    public interface OnPreparedListener {
        /**
         * Called when the media file is ready for playback.
         *
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(MediaPlayerImpl mp);
    }

    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(MediaPlayerImpl mp);
    }

    /**
     * Interface definition of a callback to be invoked when a seek
     * is issued.
     */
    public interface OnSeekListener {
        /**
         * Called to indicate that a seek operation has been started.
         *
         * @param mp the mediaPlayer that the seek was called on
         */
        public void onSeek(MediaPlayerImpl mp);
    }

    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnSeekCompleteListener {
        /**
         * Called to indicate the completion of a seek operation.
         *
         * @param mp the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(MediaPlayerImpl mp);
    }

    /**
     * Interface definition of a callback to be invoked
     * for every change in progress
     */
    public interface OnProgressListener {
        /**
         * Called to indicate the progress of media player.
         *
         * @param mp       the MediaPlayer that issued the seek operation
         * @param timeInUs the current time in micro seconds
         */
        void onProgress(MediaPlayerImpl mp, long timeInUs);
    }

    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnVideoSizeChangedListener {
        /**
         * Called to indicate the video size
         * <p>
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param mp     the MediaPlayer associated with this callback
         * @param width  the width of the video
         * @param height the height of the video
         */
        public void onVideoSizeChanged(MediaPlayerImpl mp, int width, int height);
    }

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener {
        /**
         * Called to update status in buffering a media stream received through
         * progressive HTTP download. The received buffering percentage
         * indicates how much of the content has been buffered or played.
         * For example a buffering update of 80 percent when half the content
         * has already been played indicates that the next 30 percent of the
         * content to play has been buffered.
         *
         * @param mp      the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the content
         *                that has been buffered or played thus far
         */
        void onBufferingUpdate(MediaPlayerImpl mp, int percent);
    }

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnErrorListener {
        /**
         * Called to indicate an error.
         *
         * @param mp    the MediaPlayer the error pertains to
         * @param what  the type of error that has occurred:
         *              <ul>
         *              <li>{@link #MEDIA_ERROR_UNKNOWN}
         *              <li>{@link #MEDIA_ERROR_SERVER_DIED}
         *              </ul>
         * @param extra an extra code, specific to the error. Typically
         *              implementation dependent.
         *              <ul>
         *              <li>{@link #MEDIA_ERROR_IO}
         *              <li>{@link #MEDIA_ERROR_MALFORMED}
         *              <li>{@link #MEDIA_ERROR_UNSUPPORTED}
         *              <li>{@link #MEDIA_ERROR_TIMED_OUT}
         *              </ul>
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(MediaPlayerImpl mp, int what, int extra);
    }

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the media or its playback.
     */
    public interface OnInfoListener {
        /**
         * Called to indicate an info or a warning.
         *
         * @param mp    the MediaPlayer the info pertains to.
         * @param what  the type of info or warning.
         *              <ul>
         *              <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         *              <li>{@link #MEDIA_INFO_VIDEO_RENDERING_START}
         *              <li>{@link #MEDIA_INFO_BUFFERING_START}
         *              <li>{@link #MEDIA_INFO_BUFFERING_END}
         *              </ul>
         * @param extra an extra code, specific to the info. Typically
         *              implementation dependent.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(MediaPlayerImpl mp, int what, int extra);
    }

    public interface MediaPlayerImpl {
        void prepareAsync();

        void prepare() throws IOException, IllegalStateException;

        void setDataSource(MediaSource mediaSource) throws IOException, IllegalStateException;

        void start();

        void resume();

        void pause();

        void stop();

        void seekTo(long time);

        void setSurface(Surface surface);

        void setClipSize(long startTime, long endTime);

        void setLooping(boolean loop);

        boolean isLooping();

        void setPlayAudio(boolean playAudio);

        void setVolume(float volume);

        void setOnPreparedListener(OnPreparedListener listener);

        void setOnCompletionListener(OnCompletionListener listener);

        void setOnSeekListener(OnSeekListener listener);

        void setOnSeekCompleteListener(OnSeekCompleteListener listener);

        void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener);

        void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);

        void setOnProgressListener(OnProgressListener listener);

        void setOnErrorListener(OnErrorListener listener);

        void setOnInfoListener(OnInfoListener listener);

        void setExtractionMode(int extractionMode);

        void setId(String id);

        String getId();

        void setMediaExtractionListener(MediaExtractorCallback listener);

        void setSeekMode(MediaPlayer.SeekMode seekMode);

        void setPlaybackSpeed(float playbackSpeed);

        boolean isPrepared();

        void release();

        void reset();

        long getCurrentPosition();

        void setPrevVideoFrameTs(long videoRenderTimeStamp);

        void setPrevAudioFrameTs(long audioRenderTimeStamp);

        void setRealTimeExtract(boolean realTimeExtract);
    }
}
