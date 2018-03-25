package com.roposo.creation.av;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.EventTrackUtil;
import com.roposo.creation.av.mediaplayer.FileSource;
import com.roposo.creation.av.mediaplayer.MediaPlayer;
import com.roposo.creation.av.mediaplayer.MediaPlayerUtils;
import com.roposo.creation.av.mediaplayer.MediaSource;
import com.roposo.creation.av.mediaplayer.UriSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Amud on 29/07/16.
 */


public class AVMediaExtractor implements MediaPlayerUtils.MediaPlayerImpl {
    private static final boolean DEBUG = true || AVUtils.VERBOSE;
    private final String TAG = "AVMediaExtractor:";
    private static final boolean VERBOSE = true || AVUtils.VERBOSE;

    private MediaExtractorCallback mCallback;
    private boolean mAudioEnabled;

    private int mExtractionMode;

    public static final int EXTRACT_TYPE_AUDIO = 0X01; // Extracts and feeds raw audio frame
    public static final int EXTRACT_TYPE_VIDEO = 0X02; // Extracts and feeds raw audio frame
    public static final int EXTRACT_TYPE_AUDIO_DECODED = 0X04; // Extracts, decodes and feeds
    public static final int EXTRACT_TYPE_VIDEO_DECODED = 0X08; // Extracts, decodes and feeds

    boolean mRealTimeExtract = true;

    /**
     * if true, the audio/video will not be decoded,
     * rather will simply feed raw encoded frames to the registered listeners.
     */
    private boolean mJustExtract = true;

    /**
     * if true, the audio will be played using audiotrack
     * (this is useful especially when using OpenGL to start the video)
     */
    private boolean mPlayAudio = false;
    private boolean mDoLoop = false;
    private boolean mShouldLoop = false;

    private final Object mReadyForNextFrame = new Object();
    private VideoEncoderConfig mVideoEncoderConfig;
    private AudioEncoderConfig mAudioEncoderConfig;
    private boolean mVideoFrameRendered, mAudioFrameRendered;
    private boolean mAudioPlaying;
    private volatile boolean mIsReadyForNextFrame;
    private volatile boolean seekVideo, seekAudio;
    private long mSeekTime;
    private long mJumpTime;
    private volatile float mAudioVolume = 1.0f;
    private MediaPlayerUtils.OnPreparedListener mOnPreparedListener;
    private SessionConfig mSessionConfig;
    private MediaPlayerUtils.OnCompletionListener mOnCompletionListener;
    private MediaPlayerUtils.OnSeekListener mOnSeekListener;
    private MediaPlayerUtils.OnSeekCompleteListener mOnSeekCompleteListener;
    private MediaPlayerUtils.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private MediaPlayerUtils.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private MediaPlayerUtils.OnProgressListener mOnProgressListener;
    private MediaPlayerUtils.OnErrorListener mOnErrorListener;
    private MediaPlayerUtils.OnInfoListener mOnInfoListener;
    private String mId;
    private long mVideoPresentationTime, mAudioPresentationTime;

    @Override
    public void setId(String id) {
        mId = id;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public void setSeekMode(MediaPlayer.SeekMode seekMode) {

    }

    @Override
    public void setPlaybackSpeed(float playbackSpeed) {

    }

    @Override
    public boolean isPrepared() {
        return mState >= STATE_PREPARED;
    }

    @Override
    public void reset() {

    }

    @Override
    public long getCurrentPosition() {
        return mVideoPresentationTime;
    }

    public AVMediaExtractor() throws NullPointerException {
        if (DEBUG) Log.v(TAG, "Constructor:");

        mAudioEnabled = true;
        new Thread(mMoviePlayerTask, TAG).start();
        synchronized (mSync) {
            try {
                if (!mIsRunning)
                    mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    @Override
    public final void prepareAsync() {
        prepare();
    }

    @Override
    public final void prepare() {
        if (DEBUG) Log.v(TAG, "prepare:");
        synchronized (mSync) {
            mPlayerHandler.sendEmptyMessage(MSG_PREPARE);
        }
    }


    /**
     * Well actually it's just a request to start...
     * Extractor will start in another thread based on certain conditions (related to system state).
     */
    public final void start() {
        if (DEBUG) Log.v(TAG, "start:");
        synchronized (mSync) {
            if (mState == STATE_PLAYING) {
                Log.w(TAG, "Already playing. Ignoring!");
                return;
            }
            mPlayerHandler.sendEmptyMessage(MSG_START);
        }
    }

    /**
     * request to seek to specifc timed frame<br>
     * if the frame is not a key frame, frame image will be broken
     *
     * @param newTime seek to new time[usec]
     */
    @Override
    public final void seekTo(final long newTime) {
        if (DEBUG) Log.v(TAG, "request seek: " + newTime);
        synchronized (mSync) {
            mRequestTime = newTime;
            mPlayerHandler.sendMessage(mPlayerHandler.obtainMessage(MSG_SEEK, (int) (newTime >> 32), (int) newTime));
        }
    }

    /**
     * request stop playing
     */
    public final void stop() {
        if (DEBUG) Log.v(TAG, "stop:");
        synchronized (mSync) {
            if (mState != STATE_STOP) {
                mPlayerHandler.sendEmptyMessage(MSG_STOP);
            }
        }
    }

    /**
     * request pause playing<br>
     * this function is un-implemented yet
     */
    public final void pause() {
        if (DEBUG) Log.v(TAG, "pause:");
        synchronized (mSync) {
            mPlayerHandler.sendEmptyMessage(MSG_PAUSE);
        }
    }

    /**
     * request resume from pausing<br>
     * this function is un-implemented yet
     */
    public final void resume() {
        if (DEBUG) Log.v(TAG, "resume:");
        synchronized (mSync) {
            if (!mVideoInputDone && !mVideoOutputDone && !mAudioInputDone && !mAudioOutputDone) {
                mPlayerHandler.sendEmptyMessage(MSG_RESUME);
            }
        }
    }

    /**
     * release releated resources
     */
    public final void release() {
        if (DEBUG) Log.v(TAG, "release:");
        stop();
        synchronized (mSync) {
           mPlayerHandler.sendEmptyMessage(MSG_QUIT);
        }
    }

    //================================================================================
    private static final int TIMEOUT_USEC = 2000;    // milliseconds

    /*
     * STATE_CLOSED => [preapre] => STATE_PREPARED [start]
     * 	=> STATE_PLAYING => [seek] => STATE_PLAYING
     * 		=> [pause] => STATE_PAUSED => [resume] => STATE_PLAYING
     * 		=> [stop] => STATE_CLOSED
     */
    private static final int STATE_STOP = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;

    private static final int MSG_PREPARE = 1;
    private static final int MSG_START = 2;
    private static final int MSG_RESUME = 3;
    private static final int MSG_SEEK = 4;
    private static final int MSG_PAUSE = 5;
    private static final int MSG_STOP = 6;

    private static final int MSG_QUIT = 10;

    protected MediaMetadataRetriever mMetadata;
    private final Object mSync = new Object();
    private final Object mVideoPause = new Object();
    private final Object mAudioPause = new Object();
    private volatile boolean mIsRunning;
    private int mState;
    private String mSourcePath;
    private long mDuration;
    private int mRequest;
    private long mRequestTime;
    // for video playback
    private final Object mVideoSync = new Object();
    private Surface mOutputSurface;
    protected MediaExtractor mVideoMediaExtractor;
    private MediaCodec mVideoMediaCodec;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private ByteBuffer[] mVideoInputBuffers;
    private ByteBuffer[] mVideoOutputBuffers;
    private volatile long mVideoStartTime;
    @SuppressWarnings("unused")
    /**
     * Could be fake timestamp coming from the Renderer just in case we're not extracting
     * the data to be rendered to the video from Video source
     */
    private volatile int mVideoTrackIndex;
    private volatile boolean mVideoInputDone;
    private volatile boolean mVideoOutputDone;
    private int mVideoWidth, mVideoHeight;
    private int mVideoBitRate;
    private int mBitrate;
    private float mFrameRate;
    private int mRotation;
    // for audio playback
    private final Object mAudioSync = new Object();
    private MediaExtractor mAudioMediaExtractor;
    private MediaCodec mAudioMediaCodec;
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private ByteBuffer[] mAudioInputBuffers;
    private ByteBuffer[] mAudioOutputBuffers;
    private long mAudioStartTime;
    @SuppressWarnings("unused")
    private volatile long previousAudioPresentationTimeUs = -1, previousVideoPresentationTimeUs = -1;
    private volatile int mAudioTrackIndex;
    private volatile boolean mAudioInputDone;
    private volatile boolean mAudioOutputDone;
    private int mAudioChannels;
    private int mAudioSampleRate;
    private int mAudioBitRate;
    private int mAudioPCMEncoding;
    private int mAudioInputBufSize;
    private byte[] mAudioOutTempBuf;
    private AudioTrack mAudioTrack;
    Thread videoThread = null, audioThread = null;
    private boolean isAudioPausedRequested = false, isVideoPausedRequested = false;
    //--------------------------------------------------------------------------------
    private Handler mPlayerHandler;

    private TimeBase mTimeBase = new TimeBase();
    private long mWaitTime;

    /**
     * playback control task
     */
    private final Runnable mMoviePlayerTask = new Runnable() {
        @Override
        public final void run() {
            Looper.prepare();
            mPlayerHandler = new Handler(Looper.myLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    int message = msg.what;

                    switch (message) {
                        case MSG_PREPARE:
                            handlePrepare(mSourcePath);
                            break;
                        case MSG_START:
                            handleStart();
                            break;
                        case MSG_RESUME:
                            if (mState == STATE_PAUSED) {
                                handleResume();
                            } else {
                                handleStart();
                            }
                            break;
                        case MSG_SEEK:
                            long timeStampNs = (((long) msg.arg1) << 32) | ((long) msg.arg2 & 0xffffffffL);
                            handleSeek(timeStampNs);
                            break;
                        case MSG_PAUSE:
                            handlePause();
                            break;
                        case MSG_STOP:
                            handleStop();
                            break;
                        case MSG_QUIT:
                            handleRelease();
                            Looper.myLooper().quit();
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown msg");
                    }
                    return true;
                }
            });

            synchronized (mSync) {
                mIsRunning = true;
                mState = STATE_STOP;
                mRequestTime = -1;
                mSync.notifyAll();
            }
            Looper.loop();
            Log.d(TAG, "AVMediaExtractor exiting!");
        }
    };

//--------------------------------------------------------------------------------
/*    */
    /**
     * video playback task
     *//*
    private final Runnable mVideoTask = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "VideoTask:start");
            for (; mIsRunning && !mVideoInputDone; ) {
                try {
                    if (!mVideoInputDone) {
                        handleInputMuxing(mVideoMediaExtractor, mVideoTrackIndex);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "VideoTask:", e);
                    break;
                }


            } // end of for
            if (DEBUG) Log.v(TAG, "VideoTask:finished");
            synchronized (mSync) {
                mVideoInputDone = mVideoOutputDone = true;
                mSync.notifyAll();
            }
        }
    };*/

    private final Runnable mVideoTask = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "VideoTask:start");
            mVideoFrameRendered = false;

            for (; mIsRunning && !mVideoInputDone && !mVideoOutputDone; ) {
                synchronized (mVideoPause) {
                    if (mSeekTime >= 0 && seekVideo) {
                        previousVideoPresentationTimeUs = mSeekTime;
                        mVideoStartTime -= mJumpTime;
                        if (mVideoTrackIndex >= 0) {
                            mVideoMediaExtractor.seekTo(mSeekTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            mVideoMediaCodec.flush();
                            mSeekTime = -1;
                        }
                    }
                    if (!isVideoPausedRequested || seekVideo) {
                        if (DEBUG)
                            Log.d(TAG + "pause", "isVideoPausedRequested " + isVideoPausedRequested);
                        if (!mVideoFrameRendered) {
                            synchronized (mReadyForNextFrame) {
                                // Spurious wake ups shouldn't be an issue here... so no need of an additional bool check
                                try {
                                    if (DEBUG) Log.d(TAG, "video: waiting for next frame signal short");
                                    mReadyForNextFrame.wait(5);
                                    if (DEBUG) Log.d(TAG, "video: got the next frame signal short");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (!mIsReadyForNextFrame) {
                            // Don't wait at all if we're ready for next frame i.e. we've got a signal from the client.
                            if (previousVideoPresentationTimeUs > 0) {
                                synchronized (mReadyForNextFrame) {
                                    // Spurious wake ups shouldn't be an issue here... so no need of an additional bool check
                                    try {
                                        if (DEBUG) {
                                            Log.d(TAG, "video: waiting for next frame signal");
                                        }
                                        mReadyForNextFrame.wait(100);
                                        if (previousVideoPresentationTimeUs - previousAudioPresentationTimeUs > AVUtils.MAX_FRAME_INTERVAL / 2) {
                                            continue;
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (DEBUG) Log.d(TAG, "video: got the next frame signal");
                            } else {
                                synchronized (mReadyForNextFrame) {
                                    try {
                                        mReadyForNextFrame.wait(32);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        try {
                            if (!mVideoInputDone) {
                                handleInputVideo();
                            }
                            if (!mVideoOutputDone) {
                                handleOutputVideo(mCallback);
                            }

                        } catch (final Exception e) {
                            Log.e(TAG, "VideoTask:", e);
                            break;
                        }
                        if (mVideoFrameRendered) {
                            seekVideo = false;
                            handleSeeked();
                        }
                    } else {
                        long pauseTime = System.currentTimeMillis();
                        try {
                            Log.d(TAG + "pause", "mPause " + "wait");
                            Log.d(TAG + "pause", "isVideoPausedRequested " + isVideoPausedRequested);
                            mVideoPause.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            mWaitTime += System.currentTimeMillis() - pauseTime;
                        }
                        if (!isVideoPausedRequested) {
                            mTimeBase.offset(mWaitTime * 1000L);
                            mWaitTime = 0;
                        }
                    }
                }
            } // end of for
            if (DEBUG) Log.v(TAG, "VideoTask:finished");
            synchronized (mSync) {
                mShouldLoop = mDoLoop;
                mVideoInputDone = mVideoOutputDone = true;
                mSync.notifyAll();
                onExtractionFinished();
            }
        }
    };

    private void onExtractionFinished() {
        if (((mVideoInputDone && mVideoOutputDone) || !extractVideo()) && ((mAudioInputDone && mAudioOutputDone) || !extractAudio())) {
            stop();
        }
    }

    private void handleSeeked() {
        if (!seekVideo && !seekAudio) {
            mSeekTime = -1;
        }
    }

    /**
     * audio playback task
     */
    private final Runnable mAudioTask = new Runnable() {
        @Override
        public void run() {

            if (DEBUG) Log.v(TAG, "AudioTask:start");
            mAudioFrameRendered = false;
            if (decodeAudio()) {
                for (; mIsRunning && !mAudioInputDone && !mAudioOutputDone; ) {
                    try {
                        if (mSeekTime >= 0 && seekAudio) {
                            previousAudioPresentationTimeUs = mSeekTime;

                            if (mAudioTrackIndex >= 0) {
                                Log.d(TAG, "seeking audio extractor to: " + mSeekTime);
                                mAudioMediaExtractor.seekTo(mSeekTime, MediaExtractor
                                        .SEEK_TO_CLOSEST_SYNC);
                                mAudioMediaCodec.flush();
                                mSeekTime = -1;
                            }
                        }
                        synchronized (mAudioPause) {
                            if (!isAudioPausedRequested || seekAudio) {
                                // TODO with the new queues mechanism, these timestamps should not be required anymore.
                                if ((previousAudioPresentationTimeUs > previousVideoPresentationTimeUs) && ((previousAudioPresentationTimeUs - previousVideoPresentationTimeUs) < AVUtils.MAX_FRAME_INTERVAL)) {
                                    synchronized (mReadyForNextFrame) {
                                        // Spurious wake ups shouldn't be an issue here... so no need of an additional bool check
                                        try {
                                            if (VERBOSE) {
                                                Log.d(TAG, "audio: waiting for next frame signal short");
                                            }
                                            mReadyForNextFrame.wait(5);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (VERBOSE) {
                                            Log.d(TAG, "audio: got the next frame signal");
                                        }
                                    }
                                } else if (previousAudioPresentationTimeUs - previousVideoPresentationTimeUs > 0) {
                                    synchronized (mReadyForNextFrame) {
                                        if (VERBOSE) {
                                            Log.d(TAG, "audio: waiting for next frame signal");
                                        }
                                        mReadyForNextFrame.wait();
                                    }
                                    if (VERBOSE) {
                                        Log.d(TAG, "audio: got the next frame signal");
                                    }
                                    continue;
                                }
                                if (VERBOSE) {
                                    Log.d(TAG, "prevAudioTs: " + previousAudioPresentationTimeUs + "\tprevVideoTs: " + previousVideoPresentationTimeUs);
                                }
                                if (!mAudioInputDone) {
                                    handleInputAudio();
                                }
                                if (!mAudioOutputDone) {
                                    handleOutputAudio(/*mCallback*/);
                                }
                                if (mAudioFrameRendered) {
                                    seekAudio = false;
                                    handleSeeked();
                                }
                            } else {
                                mAudioPause.wait();
                            }
                        }
                        if (VERBOSE) {
                            Log.d(TAG, "prevAudioTs: " + previousAudioPresentationTimeUs + "\tprevVideoTs: " + previousVideoPresentationTimeUs);
                        }
                    } catch (final Exception e) {
                        Log.e(TAG, "VideoTask:", e);
                        break;
                    }
                } // end of for
            } else {
                for (; mIsRunning && !mAudioInputDone; ) {
                    try {
//                        Log.d(TAG, "prev audio time: " + previousAudioPresentationTimeUs + " prev video time: " + previousVideoPresentationTimeUs);
                        if (previousAudioPresentationTimeUs > previousVideoPresentationTimeUs) {
                            synchronized (mReadyForNextFrame) {
                                // Spurious wake ups shouldn't be an issue here... so no need of an additional bool check
                                try {
                                    Log.d(TAG, "Waiting for video frame with higher timestamp to be rendered");
                                    mReadyForNextFrame.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.d(TAG, "Got the signal video frame with higher timestamp to be rendered");
                            }
                        }
                        if (!mAudioInputDone) {
                            handleInputMuxing(mAudioMediaExtractor, mAudioTrackIndex);
                        }
                    } catch (final Exception e) {
                        Log.e(TAG, "VideoTask:", e);
                        break;
                    }
                } // end of for
            }
            if (DEBUG) Log.v(TAG, "AudioTask:finished");
            synchronized (mSync) {
                mShouldLoop = mDoLoop;
                mAudioInputDone = mAudioOutputDone = true;
                mSync.notifyAll();
                onExtractionFinished();
            }
        }
    };

//--------------------------------------------------------------------------------
//
//--------------------------------------------------------------------------------

    /**
     * @param source_file source file path to be decoded
     */
    private void handlePrepare(final String source_file) {
        if (DEBUG) Log.v(TAG, "handlePrepare:" + source_file);
        synchronized (mSync) {
            if (mState != STATE_STOP) {
                throw new RuntimeException("invalid state:" + mState);
            }
        }
        final File src = new File(source_file);
        // TODO critical sahilbajaj handle these exceptions and communicate to the UI layer somehow
        if (TextUtils.isEmpty(source_file) || !src.canRead()) {
            CrashlyticsWrapper.logException(new FileNotFoundException("Unable to read " + source_file));
        }
        mVideoTrackIndex = mAudioTrackIndex = -1;
        mMetadata = new MediaMetadataRetriever();
        mMetadata.setDataSource(source_file);
        if (extractVideo()) {
            updateMovieInfo();
            // preparation for video playback
            mVideoTrackIndex = internalPrepareVideo(source_file);
        }
        // preparation for audio playback
        if (extractAudio())
            mAudioTrackIndex = internalPrepareAudio(source_file);

        if ((mVideoTrackIndex < 0) && (mAudioTrackIndex < 0)) {
            throw new RuntimeException("No video and audio track found in " + source_file);
        }
        synchronized (mSync) {
            mState = STATE_PREPARED;
        }
        mSessionConfig = new SessionConfig(null, mVideoEncoderConfig, mAudioEncoderConfig);

        mOnPreparedListener.onPrepared(this);
    }

    private boolean extractAudio() {
        return (mExtractionMode & (EXTRACT_TYPE_AUDIO_DECODED | EXTRACT_TYPE_AUDIO)) > 0;
    }

    private boolean extractVideo() {
        return (mExtractionMode & (EXTRACT_TYPE_VIDEO_DECODED | EXTRACT_TYPE_VIDEO)) > 0;
    }

    private boolean decodeAudio() {
        return (mExtractionMode & EXTRACT_TYPE_AUDIO_DECODED) > 0;
    }

    private boolean decodeVideo() {
        return (mExtractionMode & EXTRACT_TYPE_VIDEO_DECODED) > 0;
    }

    private long mBeginTime = 0, mEndTime = 0, mVideoLength = 0; // us

    /**
     * @param source_path source file path to be decoded
     * @return first video track index, -1 if not found
     */
    private int internalPrepareVideo(final String source_path) {
        int trackindex = -1;
        mVideoMediaExtractor = new MediaExtractor();
        try {
            mVideoMediaExtractor.setDataSource(source_path);
            trackindex = selectTrack(mVideoMediaExtractor, "video/");
            if (trackindex >= 0) {
                mVideoMediaExtractor.selectTrack(trackindex);

                mVideoMediaExtractor.seekTo(mBeginTime, MediaExtractor.SEEK_TO_NEXT_SYNC);

                final MediaFormat format = mVideoMediaExtractor.getTrackFormat(trackindex);
                if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                    mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                } else {
                    Log.d(TAG, "WTF, no video width");
                }
                if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                    mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                } else {
                    Log.d(TAG, "WTF, no video height");
                }
                if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                    int rotation = format.getInteger(MediaFormat.KEY_ROTATION);
                    if (rotation == 90 || rotation == 270 || rotation == -90) {
                        int temp = mVideoHeight;
                        mVideoHeight = mVideoWidth;
                        mVideoWidth = temp;
                    }
                }
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    mDuration = format.getLong(MediaFormat.KEY_DURATION);
                } else {
                    Log.d(TAG, "WTF, no video duration");
                }
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    mVideoBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                } else {
                    Log.d(TAG, "WTF, no video bit rate");
                }
                if (DEBUG)
                    Log.v(TAG, String.format("format:size(%d,%d),duration=%d,bps=%d,framerate=%f,rotation=%d",
                            mVideoWidth, mVideoHeight, mDuration, mBitrate, mFrameRate, mRotation));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        mVideoEncoderConfig = new VideoEncoderConfig(mVideoWidth, mVideoHeight, mVideoBitRate);
        return trackindex;
    }

    /**
     * @param source_file source file path to be decoded
     * @return first audio track index, -1 if not found
     */
    private int internalPrepareAudio(final String source_file) {
        int trackindex = -1;
        mAudioMediaExtractor = new MediaExtractor();
        try {
            mAudioMediaExtractor.setDataSource(source_file);
            trackindex = selectTrack(mAudioMediaExtractor, "audio/");
            if (trackindex >= 0) {
                mAudioMediaExtractor.selectTrack(trackindex);

                mAudioMediaExtractor.seekTo(mBeginTime, MediaExtractor.SEEK_TO_NEXT_SYNC);

                final MediaFormat format = mAudioMediaExtractor.getTrackFormat(trackindex);
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    mAudioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                } else {
                    Log.w(TAG, "WTF, no audio channels");
                }
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    mAudioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                } else {
                    Log.w(TAG, "WTF, no audio sample rate");
                }
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    mAudioBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                } else {
                    mAudioBitRate = 96000;
                }
                final int min_buf_size = AudioTrack.getMinBufferSize(mAudioSampleRate,
                        (mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                        AudioFormat.ENCODING_PCM_16BIT);
                int max_input_size = 0;
                if (!format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    max_input_size = 21112; //sahil.bajaj random
                } else {
                    max_input_size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                }
                mAudioInputBufSize = min_buf_size > 0 ? min_buf_size * 4 : max_input_size;
                if (mAudioInputBufSize > max_input_size) mAudioInputBufSize = max_input_size;
                final int frameSizeInBytes = mAudioChannels * 2;
                mAudioInputBufSize = (mAudioInputBufSize / frameSizeInBytes) * frameSizeInBytes;
                if (DEBUG)
                    Log.v(TAG, String.format("getMinBufferSize=%d,max_input_size=%d,mAudioInputBufSize=%d", min_buf_size, max_input_size, mAudioInputBufSize));

//                configureAudioTrack();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        mAudioEncoderConfig = new AudioEncoderConfig(mAudioChannels, mAudioSampleRate, mAudioBitRate);
        return trackindex;
    }

    private void configureAudioTrack() {
        if (!mPlayAudio) return;

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mAudioSampleRate,
                (mAudioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                mAudioPCMEncoding,
                mAudioInputBufSize,
                AudioTrack.MODE_STREAM);
        setVolume(mAudioVolume);
    }

    private void startAudioPlayback() {
        if (!mPlayAudio) return;
        if ((previousAudioPresentationTimeUs < 0) || (extractVideo() && (previousVideoPresentationTimeUs < 0)))
            return;
        Log.d(TAG, "startAudioPlayback");
        try {
            mAudioTrack.play();
            mAudioPlaying = true;
        } catch (final Exception e) {
            Log.e(TAG, "failed to start audio track playing", e);
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    private void updateMovieInfo() {
        mVideoWidth = mVideoHeight = mRotation = mBitrate = 0;
        mDuration = 0;
        mFrameRate = 0;
        String value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        if (!TextUtils.isEmpty(value)) {
            mVideoWidth = Integer.parseInt(value);
        }
        value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if (!TextUtils.isEmpty(value)) {
            mVideoHeight = Integer.parseInt(value);
        }
        value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (!TextUtils.isEmpty(value)) {
            mRotation = Integer.parseInt(value);
        }
        value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        if (!TextUtils.isEmpty(value)) {
            mBitrate = Integer.parseInt(value);
        }
        value = mMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (!TextUtils.isEmpty(value)) {
            mDuration = Long.parseLong(value) * 1000;
        }
    }

    private void handleStartRender() {
        mShouldLoop = false;

        handleResume();
        prepareForLoopback();

        mWaitTime = 0;
        mRequestTime = mBeginTime;
//        handleSeek(mBeginTime);

        if (mVideoTrackIndex >= 0) {
            mVideoInputDone = mVideoOutputDone = false;
            videoThread = new Thread(mVideoTask, "VideoTask");
        }
        if (mAudioTrackIndex >= 0) {
            mAudioInputDone = mAudioOutputDone = false;
            audioThread = new Thread(mAudioTask, "AudioTask");
        }
        if (videoThread != null) videoThread.start();
        if (audioThread != null) audioThread.start();
    }

    private void prepareForLoopback() {
        resetPlaybackState();

/*        try {
            if (mVideoMediaCodec != null) {
                mVideoMediaCodec.flush();
            }
            if (mAudioMediaCodec != null) {
                mAudioMediaCodec.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private void resetPlaybackState() {
        previousVideoPresentationTimeUs = -1;
        previousAudioPresentationTimeUs = -1;

        mAudioStartTime = 0;
        mVideoStartTime = 0;
    }

    private void handleStart() {
        if (DEBUG) Log.v(TAG, "handleStart:");
        synchronized (mSync) {
            if (mState < STATE_PREPARED)
                throw new RuntimeException("invalid state:" + mState);
        }

        if (mState == STATE_PREPARED) {
            mState = STATE_PLAYING;
            if (mRequestTime > 0) {
                handleSeek(mRequestTime);
            }
            mAudioPlaying = false;
            mVideoInputDone = mVideoOutputDone = true;

            if (mVideoTrackIndex >= 0) {
                final MediaCodec codec = internalStartVideo(mVideoMediaExtractor, mVideoTrackIndex);
                if (codec != null) {
                    mVideoMediaCodec = codec;
                    mVideoBufferInfo = new MediaCodec.BufferInfo();
                    mVideoInputBuffers = codec.getInputBuffers();
                    mVideoOutputBuffers = codec.getOutputBuffers();
                }
            }
            mAudioInputDone = mAudioOutputDone = true;
            if (mAudioTrackIndex >= 0) {
                final MediaCodec codec = internalStartAudio(mAudioMediaExtractor, mAudioTrackIndex);
                if (codec != null) {
                    mAudioMediaCodec = codec;
                    mAudioBufferInfo = new MediaCodec.BufferInfo();
                    mAudioInputBuffers = codec.getInputBuffers();
                    mAudioOutputBuffers = codec.getOutputBuffers();
                }
            }
        }

        handleStartRender();
    }

    /**
     * @param media_extractor
     * @param trackIndex
     * @return
     */
    private MediaCodec internalStartVideo(final MediaExtractor media_extractor, final int trackIndex) {
        if (DEBUG) Log.v(TAG, "internalStartVideo:");
        MediaCodec codec = null;
        if (trackIndex >= 0) {
            final MediaFormat format = media_extractor.getTrackFormat(trackIndex);
            final String mime = format.getString(MediaFormat.KEY_MIME);
            try {
                codec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                // TODO critical fix it.
                format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            }
            Log.d(TAG, "mOutputSurface = " + mOutputSurface);
            mTimeBase.start();
            codec.configure(format, mOutputSurface, null, 0);
            codec.start();
            if (DEBUG) Log.v(TAG, "internalStartVideo:codec started");
        }
        return codec;
    }

    /**
     * @param media_extractor
     * @param trackIndex
     * @return
     */
    private MediaCodec internalStartAudio(final MediaExtractor media_extractor, final int trackIndex) {
        if (DEBUG) Log.v(TAG, "internalStartAudio:");
        MediaCodec codec = null;
        if (trackIndex >= 0) {
            final MediaFormat format = media_extractor.getTrackFormat(trackIndex);
            Log.d(TAG, "audio format: " + format);
            final String mime = format.getString(MediaFormat.KEY_MIME);
            try {
                codec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            codec.configure(format, null, null, 0);
            codec.start();
            if (DEBUG) Log.v(TAG, "internalStartAudio:codec started");
            //
            final ByteBuffer[] buffers = codec.getOutputBuffers();
            int sz = buffers[0].capacity();
            if (sz <= 0)
                sz = mAudioInputBufSize;
            if (DEBUG) Log.v(TAG, "AudioOutputBufSize:" + sz);
            mAudioOutTempBuf = new byte[sz];
        }
        return codec;
    }

    private void handleSeek(long newTime) {
        if (seekAudio || seekVideo) return;
        if (DEBUG) Log.d(TAG, "handleSeek: " + newTime);
        if (newTime < mBeginTime) return;
        if (newTime > mEndTime) newTime = mEndTime - AVUtils.MAX_FRAME_INTERVAL;

        mSeekTime = newTime;
        mVideoOutputDone = mVideoInputDone = mAudioOutputDone = mAudioInputDone = false;

        seekVideo = seekAudio = true;
        mJumpTime = mSeekTime - previousVideoPresentationTimeUs;
        mVideoPresentationTime = mSeekTime;
        mRequestTime = -1;
        synchronized (mVideoPause) {
            mVideoFrameRendered = false;
            mVideoPause.notify();
        }
        synchronized (mAudioPause) {
            mAudioFrameRendered = false;
            mAudioPause.notify();
        }
    }

    private void handleLoop(final MediaExtractorCallback frameCallback) {
        Log.d(TAG, "handleLoop");
        synchronized (mSync) {
            try {
                Log.d(TAG, "handleLoop: Waiting for sync");
                mSync.wait();
                Log.d(TAG, "handleLoop: Got the sync");
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (((mVideoInputDone && mVideoOutputDone)
                || !extractVideo())
                && ((mAudioInputDone && mAudioOutputDone)
                || !extractAudio())) {
            if (DEBUG) Log.d(TAG, "Reached EOS, looping check");
            if (mShouldLoop) {
                startRenderAV();
            } else {
                handleStop();
            }
        }
    }

    private void startRenderAV() {
        if (DEBUG) Log.v(TAG, "startRender");
        synchronized (mSync) {
            mSync.notifyAll();
        }
    }

    /**
     * @param codec
     * @param extractor
     * @param inputBuffers
     * @param presentationTimeUs
     * @param isAudio
     */
    private boolean internalProcessInput(final MediaCodec codec, final MediaExtractor extractor, final ByteBuffer[] inputBuffers, final long presentationTimeUs, final boolean isAudio) {
//		if (DEBUG) Log.v(TAG, "internalProcessInput:presentationTimeUs=" + presentationTimeUs);
        boolean result = true;
        while (mIsRunning) {
            final int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                break;
            if (inputBufIndex >= 0) {
                final int size = extractor.readSampleData(inputBuffers[inputBufIndex], 0);
                if (VERBOSE)
                    Log.d(TAG, (isAudio ? "audio" : "video") + "\tqueueInputBuffer: " + inputBufIndex);
                if (size > 0) {
                    codec.queueInputBuffer(inputBufIndex, 0, size, presentationTimeUs, 0);
                }
                result = extractor.advance();    // return false if no data is available
                break;
            }
        }
        return result;
    }

    private void handleInputVideo() {
        long sampleTime = mVideoMediaExtractor.getSampleTime();
        final long presentationTimeUs = sampleTime - mBeginTime;
        if (DEBUG) Log.d(TAG, "video: sampleTime: " + presentationTimeUs);

        long timeBase = mRealTimeExtract ? mTimeBase.getCurrentTime() : previousVideoPresentationTimeUs;
        if (DEBUG) Log.d(TAG, "timeBase: " + timeBase + " \tpresentationTime: " + presentationTimeUs);
        if (presentationTimeUs - timeBase > 100 * 1000L) {
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        boolean b;
        try {
            b = internalProcessInput(mVideoMediaCodec, mVideoMediaExtractor, mVideoInputBuffers,
                    presentationTimeUs, false);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }
        if (VERBOSE) Log.d(TAG, "handleInputVideo: " + "status: " + b);
        if (!b) {
            if (DEBUG) Log.i(TAG, "video track input reached EOS");
            int count = 10;
            while (mIsRunning && count > 0) {
                count--;
                int inputBufIndex = 0;
                try {
                    inputBufIndex = mVideoMediaCodec.dequeueInputBuffer(TIMEOUT_USEC * 2);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                if (VERBOSE) Log.d(TAG, "video dequeue status: " + inputBufIndex);
                if (inputBufIndex >= 0) {
                    mVideoMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (DEBUG) Log.v(TAG, "sent input EOS:" + mVideoMediaCodec);
                    break;
                }
            }
            synchronized (mSync) {
                mVideoInputDone = true;
                mSync.notifyAll();
            }
        }
    }

    @Override
    public void setOnPreparedListener(MediaPlayerUtils.OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(MediaPlayerUtils.OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @Override
    public void setOnSeekListener(MediaPlayerUtils.OnSeekListener listener) {
        mOnSeekListener = listener;
    }

    @Override
    public void setOnSeekCompleteListener(MediaPlayerUtils.OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    @Override
    public void setOnVideoSizeChangedListener(MediaPlayerUtils.OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(MediaPlayerUtils.OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    @Override
    public void setOnProgressListener(MediaPlayerUtils.OnProgressListener listener) {
        mOnProgressListener = listener;
    }

    @Override
    public void setOnErrorListener(MediaPlayerUtils.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    @Override
    public void setOnInfoListener(MediaPlayerUtils.OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    @Override
    public void setMediaExtractionListener(MediaExtractorCallback listener) {
        mCallback = listener;
    }

    /**
     * @param frameCallback
     */
    private final void handleOutputVideo(final MediaExtractorCallback frameCallback) {
        if (DEBUG) Log.v(TAG, "handleOutputVideo");
        mVideoFrameRendered = false;
        while (mIsRunning && !mVideoOutputDone) {
            int decoderStatus = 0;
            try {
                decoderStatus = mVideoMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }
            if (VERBOSE) Log.d(TAG, "handleOutputVideo: " + "decoderStatus: " + decoderStatus);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) {
                    Log.w(TAG, "Video try again later");
                }
                break;
            } else {
                mVideoFrameRendered = true;
                mIsReadyForNextFrame = false;
                if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mVideoOutputBuffers = mVideoMediaCodec.getOutputBuffers();
                    if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    final MediaFormat newFormat = mVideoMediaCodec.getOutputFormat();
                    if (DEBUG) Log.d(TAG, "video decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from video decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    boolean doRender = false;
                    if (mVideoBufferInfo.size > 0) {
                        doRender = (mVideoBufferInfo.size != 0)
                                && !internalWriteVideo(mVideoOutputBuffers[decoderStatus],
                                0, mVideoBufferInfo.size, mVideoBufferInfo.presentationTimeUs);

                        if (VERBOSE) {
                            Log.d(TAG, "video releaseOutputBuffer: " + "decoderstatus: " + String.format("%2d", decoderStatus) + "\tdoRender: " + doRender + "\tvideots: " + mVideoBufferInfo.presentationTimeUs + "\tsize: " + mVideoBufferInfo.size + "\t flags: " + mVideoBufferInfo.flags);
                        }

                        if (doRender) {
                            mVideoPresentationTime = mVideoBufferInfo.presentationTimeUs;
                            if (!frameCallback.onFrameAvailable(mVideoBufferInfo.presentationTimeUs))
                                mVideoStartTime = adjustPresentationTime(mVideoSync, mVideoStartTime, mVideoBufferInfo.presentationTimeUs);
                        }
                    }
                    boolean eos = false;
                    long videoTs = mVideoBufferInfo.presentationTimeUs;
                    if (VERBOSE) {
                        Log.d(TAG, "videoTs: " + videoTs + " expected Length: " + mVideoLength);
                    }
                    if (videoTs > mVideoLength) {
                        mShouldLoop = mDoLoop;
                        mVideoInputDone = mVideoOutputDone = true;
                        // Just being overly cautious and setting audio flags to done, JIC, someone is using them.
                        if (!extractAudio()) {
                            mAudioInputDone = mAudioOutputDone = true;
                        }
                        eos = true;
                    }
                    if (eos) {
                        mVideoBufferInfo.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    }
                    try {
                        if (doRender) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mVideoMediaCodec.releaseOutputBuffer(decoderStatus, mVideoBufferInfo.presentationTimeUs * 1000L);
                            } else {
                                mVideoMediaCodec.releaseOutputBuffer(decoderStatus, true);
                            }
                        } else {
                            mVideoMediaCodec.releaseOutputBuffer(decoderStatus, false);
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (DEBUG) Log.d(TAG, "video:output EOS");
                        synchronized (mSync) {
                            mVideoOutputDone = true;
                            mSync.notifyAll();
                        }
                    }
                }
            }
        }
    }

    /**
     * @param buffer
     * @param offset
     * @param size
     * @param presentationTimeUs
     */
    private boolean internalWriteVideo(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
//		if (DEBUG) Log.v(TAG, "internalWriteVideo");
        // Nothing to be done here... Video is rendered using OpenGL (If at all)
        return false;
    }

    private void handleInputAudio() {
        long sampleTime = mAudioMediaExtractor.getSampleTime();
        long presentationTimeUs = sampleTime - mBeginTime;

        boolean b;
        try {
            b = internalProcessInput(mAudioMediaCodec, mAudioMediaExtractor, mAudioInputBuffers,
                    presentationTimeUs, true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }
        if (!b) {
            if (DEBUG) Log.i(TAG, "audio track input reached EOS");
            int count = 10;
            while (mIsRunning && count > 0) {
                count--;
                int inputBufIndex = 0;
                try {
                    inputBufIndex = mAudioMediaCodec.dequeueInputBuffer(TIMEOUT_USEC * 2);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                if (inputBufIndex >= 0) {
                    mAudioMediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    if (DEBUG) Log.v(TAG, "sent input EOS:" + mAudioMediaCodec);
                    break;
                }
            }
            synchronized (mSync) {
                mAudioInputDone = true;
                mSync.notifyAll();
            }
        }
    }

    private void handleOutputAudio(/*final IFrameCallback frameCallback*/) {
//		if (DEBUG) Log.v(TAG, "handleDrainAudio:");
        mAudioFrameRendered = false;
        while (mIsRunning && !mAudioOutputDone) {
            int decoderStatus = 0;
            try {
                decoderStatus = mAudioMediaCodec.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "Audio try again later");
                return;
            } else {
                mAudioFrameRendered = true;
                if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mAudioOutputBuffers = mAudioMediaCodec.getOutputBuffers();
                    if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED:");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    final MediaFormat format = mAudioMediaCodec.getOutputFormat();
                    if (DEBUG) Log.d(TAG, "audio decoder output format changed: " + format);
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        mAudioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    } else {
                        Log.w(TAG, "WTF, no audio channels");
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        mAudioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    } else {
                        Log.w(TAG, "WTF, no audio sample rate");
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        mAudioBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                    } else {
                        mAudioBitRate = 96000;
                    }
                    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        mAudioPCMEncoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
                    } else {
                        mAudioPCMEncoding = AudioFormat.ENCODING_PCM_16BIT;
                    }
                    configureAudioTrack();
                } else if (decoderStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from audio decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if (mAudioBufferInfo.size > 0) {
                        if (VERBOSE)
                            Log.d(TAG, "extracted audio timestamp: " + String.valueOf(mAudioBufferInfo.presentationTimeUs) + "\toffset: " + mAudioBufferInfo.offset + "\tsize: " + mAudioBufferInfo.size);

                        boolean eos = false;
                        long audioTs = mAudioBufferInfo.presentationTimeUs;

                        if (VERBOSE)
                            Log.d(TAG, "audioTs: " + audioTs + " expected Length: " + mVideoLength);
                        if (audioTs > mVideoLength) {
                            eos = true;
                        }

                        // Unlike video, where releasing output buffer renders to
                        // the bound SurfaceTexture which is then fed to encoder,
                        // we have to do it manually in case of audio.

                        mCallback.sendAudioFrame(mAudioOutputBuffers[decoderStatus], mAudioBufferInfo, eos);
                        internalWriteAudio(mAudioOutputBuffers[decoderStatus],
                                mAudioBufferInfo.offset, mAudioBufferInfo.size, mAudioBufferInfo.presentationTimeUs);

                        if (eos) {
                            ByteBuffer eosBuffer = ByteBuffer.allocate(1);
                            MediaCodec.BufferInfo eosInfo = cloneBufferInfo(mAudioBufferInfo);
                            eosInfo.offset = 0;
                            eosInfo.size = 0;
                            eosInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                            mCallback.sendAudioFrame(eosBuffer, eosInfo, eos);
                            internalWriteAudio(eosBuffer,
                                    eosInfo.offset, eosInfo.size, eosInfo.presentationTimeUs);
                        }

/*                    if (!frameCallback.onFrameAvailable(mAudioBufferInfo.presentationTimeUs))
                        mAudioStartTime = adjustPresentationTime(mAudioSync, mAudioStartTime, mAudioBufferInfo.presentationTimeUs);*/
                        if (eos) {
                            mAudioInputDone = mAudioOutputDone = true;
                            // Just being overly cautious and setting video flags to done.
                            if (!extractVideo()) {
                                mVideoInputDone = mVideoOutputDone = true;
                            }
                        }
                    }
                    try {
                        if (VERBOSE) {
                            Log.d(TAG, "audio releaseOutputBuffer: " + "decoderstatus: " + String.format("%2d", decoderStatus) + "\taudiots: " + mAudioBufferInfo.presentationTimeUs + "\tsize: " + mAudioBufferInfo.size + "\t flags: " + mAudioBufferInfo.flags);
                        }
                        mAudioMediaCodec.releaseOutputBuffer(decoderStatus, false);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (DEBUG) Log.d(TAG, "audio:output EOS");
                        synchronized (mSync) {
                            mAudioOutputDone = true;
                            mSync.notifyAll();
                        }
                    }
                }
            }
        }
    }

    MediaCodec.BufferInfo cloneBufferInfo(MediaCodec.BufferInfo bufferInfo) {
        MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
        outBufferInfo.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
        return outBufferInfo;
    }

    /**
     * @param buffer
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return ignored
     */
    protected boolean internalWriteAudio(final ByteBuffer buffer, final int offset, final int size, final long presentationTimeUs) {
//		if (DEBUG) Log.d(TAG, "internalWriteAudio");
        if (mPlayAudio && mAudioTrack != null) {
            if (mAudioOutTempBuf.length < size) {
                mAudioOutTempBuf = new byte[size];
            }
            buffer.position(offset);
            buffer.get(mAudioOutTempBuf, 0, size);
            buffer.clear();

            mAudioTrack.write(mAudioOutTempBuf, 0, size);

            if (!mAudioPlaying) {
                startAudioPlayback();
            }
        }
        return true;
    }

    /**
     * adjusting frame rate
     *
     * @param sync
     * @param startTime
     * @param presentationTimeUs
     * @return startTime
     */
    private long adjustPresentationTime(final Object sync, final long startTime, final long presentationTimeUs) {
        if (startTime > 0) {
            for (long t = presentationTimeUs - (System.nanoTime() / 1000 - startTime);
                 t > 0; t = presentationTimeUs - (System.nanoTime() / 1000 - startTime)) {
                synchronized (sync) {
                    try {
                        sync.wait(t / 1000, (int) ((t % 1000) * 1000));
                    } catch (final InterruptedException e) {
                    }
                }
            }
            return startTime;
        } else {
            return System.nanoTime() / 1000;
        }
    }

    int inputChunk = 0;
    ByteBuffer inputBuf;

    private void handleInputMuxing(MediaExtractor extractor, int trackIndex) {

        int bufferInfoSize = 0;
        int bufferInfoOffset = 0;
        long bufferInfoPTS = 0;
        int bufferInfoFlag = 0;
        if (inputBuf == null) {
            inputBuf = ByteBuffer.allocate(512 * 512);
        }
        if (!mAudioInputDone) {
            int chunkSize = extractor.readSampleData(inputBuf, 0);
            int flag = extractor.getSampleFlags();
            long presentationTimeUs = extractor.getSampleTime();
            Log.d(TAG, "time : " + presentationTimeUs + " track " + trackIndex);
            previousAudioPresentationTimeUs = presentationTimeUs; //TODO do the same for video as well
            if (presentationTimeUs > mEndTime) {
                mAudioInputDone = true;
            } else {
                if (flag == MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                else if (flag == MediaCodec.BUFFER_FLAG_SYNC_FRAME)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                else if (flag == MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }

            Log.d(TAG, "flag : " + flag + " track " + trackIndex);


            if (chunkSize < 0) {
                mAudioInputDone = mAudioOutputDone = true;
                Log.d(TAG, "sent input EOS");
            } else {
                if (extractor.getSampleTrackIndex() != trackIndex) {
                    Log.w(TAG, "WEIRD: got sample from track " +
                            extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                }
                bufferInfoSize = chunkSize;
                bufferInfoPTS = extractor.getSampleTime();
                Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                        chunkSize);

                inputChunk++;
                extractor.advance();
            }


            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer encodedData;
            if (mAudioInputDone) {
                bufferInfo.set(0, 1, bufferInfoPTS, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                byte[] data = new byte[1];
                Log.d(TAG, "Sent EOS at time: " + bufferInfoPTS);
                encodedData = ByteBuffer.wrap(data);
            } else {
                bufferInfo.set(bufferInfoOffset, bufferInfoSize, bufferInfoPTS, bufferInfoFlag);
                inputBuf.position(bufferInfo.offset);
                inputBuf.limit(bufferInfo.offset + bufferInfo.size);
                encodedData = ByteBuffer.allocate(bufferInfo.size);
                encodedData.put(inputBuf.array(), bufferInfo.offset, bufferInfo.size);
            }
            Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer, ts=" +
                    bufferInfo.presentationTimeUs);
            Muxer.MediaFrame mediaFrame = new Muxer.MediaFrame(encodedData, bufferInfo, -1, Muxer.MediaFrame.FRAME_TYPE_AUDIO_EOS, -1);
            mCallback.sendDirectAudioFrame(mediaFrame);
        }
        if (mAudioInputDone) {
            if (DEBUG) Log.i(TAG, "video track input reached EOS");
            synchronized (mSync) {
                if (trackIndex == mVideoTrackIndex) {
                    mVideoInputDone = true;
                    mVideoOutputDone = true;
                } else if (trackIndex == mAudioTrackIndex) {
                    mAudioInputDone = true;
                    mAudioOutputDone = true;
                    // Fake Video track end in case we're extracting just audio
                    if (mVideoTrackIndex < 0) {
                        mVideoInputDone = true;
                        mVideoOutputDone = true;
                    }
                }
                mSync.notifyAll();
            }
        }

    }


    private void handleStop() {
        if (DEBUG) Log.v(TAG, "handleStop:");
        int prevState = mState;
        synchronized (mAudioPause) {
            synchronized (mSync) {
                mVideoInputDone = mVideoOutputDone = mAudioInputDone = mAudioOutputDone = true;
                mState = STATE_PREPARED;
            }
        }
//        handleRelease();
        if (prevState == STATE_PLAYING) {
            mOnCompletionListener.onCompletion(this);
        }
    }

    private void handleRelease() {
        if (DEBUG) Log.v(TAG, "handleRelease:");
        synchronized (mAudioPause) {
            synchronized (mSync) {
                mVideoInputDone = mVideoOutputDone = mAudioInputDone = mAudioOutputDone = true;
                mState = STATE_STOP;
            }
            synchronized (mVideoTask) {
                internalStopVideo();
                mVideoTrackIndex = -1;
            }
            synchronized (mAudioTask) {
                internalStopAudio();
                mAudioTrackIndex = -1;
            }

            if (mVideoMediaCodec != null) {
                mVideoMediaCodec.stop();
                mVideoMediaCodec.release();
                mVideoMediaCodec = null;
            }
            if (mAudioMediaCodec != null) {
                mAudioMediaCodec.stop();
                mAudioMediaCodec.release();
                mAudioMediaCodec = null;
            }
            if (mVideoMediaExtractor != null) {
                mVideoMediaExtractor.release();
                mVideoMediaExtractor = null;
            }
            if (mAudioMediaExtractor != null) {
                mAudioMediaExtractor.release();
                mAudioMediaExtractor = null;
            }
            mVideoBufferInfo = mAudioBufferInfo = null;
            mVideoInputBuffers = mVideoOutputBuffers = null;
            mAudioInputBuffers = mAudioOutputBuffers = null;
            if (mMetadata != null) {
                mMetadata.release();
                mMetadata = null;
            }
        }
    }

    private void internalStopVideo() {
        if (DEBUG) Log.v(TAG, "internalStopVideo:");
    }

    private void internalStopAudio() {
        if (DEBUG) Log.v(TAG, "internalStopAudio:");
        if (mAudioTrack != null) {
            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED)
                mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
        mAudioOutTempBuf = null;
    }

    private void handlePause() {
        if (DEBUG) Log.v(TAG, "handlePause:");
        synchronized (mVideoPause) {
            isVideoPausedRequested = true;
        }
        synchronized (mAudioPause) {
            isAudioPausedRequested = true;
        }
        mState = STATE_PAUSED;
        if (DEBUG) Log.d(TAG + "pause", "pausing " + isVideoPausedRequested);
    }

    private void handleResume() {
        if (DEBUG) Log.v(TAG, "handleResume:");
        mState = STATE_PLAYING;
        synchronized (mVideoPause) {
            mVideoFrameRendered = false;
            isVideoPausedRequested = false;
            mVideoPause.notify();
        }
        synchronized (mAudioPause) {
            mAudioFrameRendered = false;
            isAudioPausedRequested = false;
            mAudioPause.notify();
        }
    }

    /**
     * search first track index matched specific MIME
     *
     * @param extractor
     * @param mimeType  "video/" or "audio/"
     * @return track index, -1 if not found
     */
    protected final int selectTrack(final MediaExtractor extractor, final String mimeType) {
        final int numTracks = extractor.getTrackCount();
        MediaFormat format;
        String mime;
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                if (DEBUG) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                format.setLong(MediaFormat.KEY_DURATION, mVideoLength);
// TODO place a few checks here                mCallback.addTrack(format);
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setClipSize(long startTime, long endTime) {
        mBeginTime = startTime;
        mEndTime = endTime;
        Log.d(TAG, "Start time: " + startTime + " End time: " + mEndTime);
        mVideoLength = mEndTime - mBeginTime;
    }

    public void readyForNextFrame() {
        synchronized (mReadyForNextFrame) {
            mIsReadyForNextFrame = true;
            if (VERBOSE) Log.d(TAG, "Notify for next frame");
            mReadyForNextFrame.notifyAll();
        }
    }

    @Override
    public void setExtractionMode(int extractionMode) {
        mExtractionMode = extractionMode;
    }

    public void setPrevVideoFrameTs(long videoRenderTimeStamp) {
        if (seekVideo) return;
        previousVideoPresentationTimeUs = videoRenderTimeStamp;
        readyForNextFrame();
    }

    public void setPrevAudioFrameTs(long audioRenderTimestamp) {
        if (seekAudio) return;
        previousAudioPresentationTimeUs = audioRenderTimestamp;
//        if (!extractVideo()) {
            readyForNextFrame();
//        }
    }

    @Override
    public void setDataSource(MediaSource source) throws IllegalArgumentException {
        if ((source instanceof UriSource)) {
            //throw new IllegalArgumentException("Only FileSource supported as of now");
            mSourcePath = ((UriSource) source).getUri().getPath();
        } else if ((source instanceof FileSource)) {
            mSourcePath = ((FileSource) source).getFile().getAbsolutePath();
        } else {
            throw new IllegalArgumentException("source path is of unsupported types or null");
        }
    }

    @Override
    public void setSurface(Surface surface) {
        mOutputSurface = surface;
    }

    @Override
    public synchronized void setLooping(boolean doLoop) {
        mDoLoop = doLoop;
    }

    @Override
    public boolean isLooping() {
        return mDoLoop;
    }

    @Override
    public synchronized void setPlayAudio(boolean playAudio) {
        mPlayAudio = playAudio;
    }

    @Override
    public synchronized void setRealTimeExtract(boolean realTimeExtract) {
        mRealTimeExtract = realTimeExtract;
    }

    @Override
    public void setVolume(float audioVolume) {
        mAudioVolume = audioVolume;
        if (mAudioTrack == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack.setVolume(audioVolume);
        } else {
            mAudioTrack.setStereoVolume(audioVolume, audioVolume);
        }
    }
}
