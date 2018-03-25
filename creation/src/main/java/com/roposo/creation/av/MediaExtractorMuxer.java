package com.roposo.creation.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.roposo.creation.listeners.TrimmingCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Amud on 29/07/16.
 */

@TargetApi(18)
public class MediaExtractorMuxer {
    private static final boolean DEBUG = true;
    private static final String TAG_STATIC = "MediaExtractorMuxer:";
    private final String TAG = TAG_STATIC + getClass().getSimpleName();

    private final TrimmingCallback mCallback;
    private final boolean mAudioEnabled;

    public MediaExtractorMuxer(String outputLocation, final TrimmingCallback callback, final boolean audio_enable) throws NullPointerException {
        try {
            mMuxer = new MediaMuxer(outputLocation, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (DEBUG) Log.v(TAG, "Constructor:");

        mCallback = callback;
        mAudioEnabled = audio_enable;
        new Thread(mMoviePlayerTask, TAG).start();
        synchronized (mSync) {
            try {
                if (!mIsRunning)
                    mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }


    public final void prepare(final String src_movie) {
        if (DEBUG) Log.v(TAG, "prepare:");
        synchronized (mSync) {
            mSourcePath = src_movie;
            mRequest = REQ_PREPARE;
            mSync.notifyAll();
        }
    }


    public final void play() {
        if (DEBUG) Log.v(TAG, "play:");
        synchronized (mSync) {
            if (mState == STATE_PLAYING) return;
            mRequest = REQ_START;
            mSync.notifyAll();
        }
    }

    public final void seek(final long newTime) {
        if (DEBUG) Log.v(TAG, "seek");
        synchronized (mSync) {
            mRequest = REQ_SEEK;
            mRequestTime = newTime;
            mSync.notifyAll();
        }
    }


    public final void stop() {
        if (DEBUG) Log.v(TAG, "stop:");
        synchronized (mSync) {
            if (mState != STATE_STOP) {
                mRequest = REQ_STOP;
                mSync.notifyAll();
                try {
                    mSync.wait(50);
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    public final void resume() {
        if (DEBUG) Log.v(TAG, "resume:");
        synchronized (mSync) {
            mSync.notifyAll();
        }
    }

    public final void release() {
        if (DEBUG) Log.v(TAG, "release:");
        stop();
        synchronized (mSync) {
            mRequest = REQ_QUIT;
            mSync.notifyAll();
        }
    }

    //================================================================================
    private static final int TIMEOUT_USEC = 10000;    // 10msec


    private static final int STATE_STOP = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_PLAYING = 2;


    private static final int REQ_NON = 0;
    private static final int REQ_PREPARE = 1;
    private static final int REQ_START = 2;
    private static final int REQ_SEEK = 3;
    private static final int REQ_STOP = 4;
    private static final int REQ_QUIT = 9;


    protected MediaMetadataRetriever mMetadata;
    private final Object mSync = new Object();
    private volatile boolean mIsRunning;
    private int mState;
    private String mSourcePath;
    private int mRequest;
    private long mRequestTime;
    protected MediaExtractor mVideoMediaExtractor;
    private volatile int mVideoTrackIndex;
    private boolean mVideoInputDone;
    private boolean mVideoOutputDone;
    protected MediaExtractor mAudioMediaExtractor;
    private volatile int mAudioTrackIndex;
    private boolean mAudioInputDone;
    private boolean mAudioOutputDone;
    Thread videoThread = null, audioThread = null;

    protected MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
//--------------------------------------------------------------------------------
    /**
     * playback control task
     */
    private final Runnable mMoviePlayerTask = new Runnable() {
        @Override
        public final void run() {
            boolean local_isRunning = false;
            int local_req;
            try {
                synchronized (mSync) {
                    local_isRunning = mIsRunning = true;
                    mState = STATE_STOP;
                    mRequest = REQ_NON;
                    mRequestTime = -1;
                    mSync.notifyAll();
                }
                for (; local_isRunning; ) {
                    try {
                        synchronized (mSync) {
                            local_isRunning = mIsRunning;
                            local_req = mRequest;
                            mRequest = REQ_NON;
                        }
                        switch (mState) {
                            case STATE_STOP:
                                local_isRunning = processStop(local_req);
                                break;
                            case STATE_PREPARED:
                                local_isRunning = processPrepared(local_req);
                                break;
                            case STATE_PLAYING:
                                local_isRunning = processPlaying(local_req);
                                break;
                        }

                    } catch (final InterruptedException e) {
                        break;
                    } catch (final Exception e) {
                        Log.e(TAG, "MoviePlayerTask:", e);
                        break;
                    }
                }
            } finally {
                if (DEBUG) Log.v(TAG, "player task finished:local_isRunning=" + local_isRunning);
                handleStop();
            }
        }
    };

//--------------------------------------------------------------------------------
    /**
     * video playback task
     */
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
    };

//--------------------------------------------------------------------------------
    /**
     * audio playback task
     */
    private final Runnable mAudioTask = new Runnable() {
        @Override
        public void run() {

            if (DEBUG) Log.v(TAG, "AudioTask:start");
            for (; mIsRunning && !mAudioInputDone; ) {
                try {
                    if (!mAudioInputDone) {
                        handleInputMuxing(mAudioMediaExtractor, mAudioTrackIndex);
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "VideoTask:", e);
                    break;
                }
            } // end of for
            if (DEBUG) Log.v(TAG, "AudioTask:finished");
            synchronized (mSync) {
                mAudioInputDone = mAudioOutputDone = true;
                mSync.notifyAll();
            }
        }
    };

    private final boolean processStop(final int req) throws InterruptedException, IOException {
        boolean local_isRunning = true;
        switch (req) {
            case REQ_PREPARE:
                handlePrepare(mSourcePath);
                break;
            case REQ_START:
                throw new IllegalStateException("invalid state:" + mState);
            case REQ_QUIT:
                local_isRunning = false;
                break;
            default:
                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        }
        synchronized (mSync) {
            local_isRunning &= mIsRunning;
        }
        return local_isRunning;
    }


    private final boolean processPrepared(final int req) throws InterruptedException {
        boolean local_isRunning = true;
        switch (req) {
            case REQ_START:
                handleStart();
                break;
            case REQ_STOP:
                handleStop();
                break;
            case REQ_QUIT:
                local_isRunning = false;
                break;
//		case REQ_PREPARE:
//		case REQ_SEEK:
            default:
                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        } // end of switch (req)
        synchronized (mSync) {
            local_isRunning &= mIsRunning;
        }
        return local_isRunning;
    }

    /**
     * @param req
     * @return
     */
    private final boolean processPlaying(final int req) {
        boolean local_isRunning = true;
        switch (req) {
            case REQ_PREPARE:
            case REQ_START:
            case REQ_SEEK:
                handleSeek(mRequestTime);
                break;
            case REQ_STOP:
                handleStop();
                break;
            case REQ_QUIT:
                local_isRunning = false;
                break;
            default:
                handleLoop(mCallback);
                break;
        } // end of switch (req)
        synchronized (mSync) {
            local_isRunning &= mIsRunning;
        }
        return local_isRunning;
    }

    /**
     * @param req
     * @return
     * @throws InterruptedException
     */
    private final boolean processPaused(final int req) throws InterruptedException {
        boolean local_isRunning = true;
        switch (req) {
            case REQ_PREPARE:
            case REQ_START:
                throw new IllegalStateException("invalid state:" + mState);
            case REQ_SEEK:
                handleSeek(mRequestTime);
                break;
            case REQ_STOP:
                handleStop();
                break;
            case REQ_QUIT:
                local_isRunning = false;
                break;
            default:
                synchronized (mSync) {
                    mSync.wait();
                }
                break;
        }
        synchronized (mSync) {
            local_isRunning &= mIsRunning;
        }
        return local_isRunning;
    }

    private final void handlePrepare(final String source_file) throws IOException {
        if (DEBUG) Log.v(TAG, "handlePrepare:" + source_file);
        synchronized (mSync) {
            if (mState != STATE_STOP) {
                throw new RuntimeException("invalid state:" + mState);
            }
        }
        final File src = new File(source_file);
        if (TextUtils.isEmpty(source_file) || !src.canRead()) {
            throw new FileNotFoundException("Unable to read " + source_file);
        }
        mVideoTrackIndex = mAudioTrackIndex = -1;
        mMetadata = new MediaMetadataRetriever();
        mMetadata.setDataSource(source_file);
        mVideoTrackIndex = internal_prepare_video(source_file);
        if (mAudioEnabled)
            mAudioTrackIndex = internal_prepare_audio(source_file);
        if ((mVideoTrackIndex < 0) && (mAudioTrackIndex < 0)) {
            throw new RuntimeException("No video and audio track found in " + source_file);
        }
        synchronized (mSync) {
            mState = STATE_PREPARED;
        }
        mCallback.onPrepared();
    }

    long mBeginTime = 0, mEndTime = 0, mVideoLength = 0; // us

    protected int internal_prepare_video(final String source_path) {
        int trackindex = -1;
        mVideoMediaExtractor = new MediaExtractor();
        try {
            mVideoMediaExtractor.setDataSource(source_path);
            trackindex = selectTrack(mVideoMediaExtractor, "video/");
            if (trackindex >= 0) {
                mVideoMediaExtractor.selectTrack(trackindex);
                mVideoMediaExtractor.seekTo(mBeginTime, MediaExtractor.SEEK_TO_NEXT_SYNC);
            }
        } catch (final IOException e) {
        }
        return trackindex;
    }


    protected int internal_prepare_audio(final String source_file) {
        int trackindex = -1;
        mAudioMediaExtractor = new MediaExtractor();
        try {
            mAudioMediaExtractor.setDataSource(source_file);
            trackindex = selectTrack(mAudioMediaExtractor, "audio/");
            if (trackindex >= 0) {
                mAudioMediaExtractor.selectTrack(trackindex);
                mAudioMediaExtractor.seekTo(mBeginTime, MediaExtractor.SEEK_TO_NEXT_SYNC);
            }
        } catch (final IOException e) {
        }
        return trackindex;
    }

    private final void handleStart() {
        if (DEBUG) Log.v(TAG, "handleStart:");
        synchronized (mSync) {
            if (mState != STATE_PREPARED)
                throw new RuntimeException("invalid state:" + mState);
            mState = STATE_PLAYING;
        }
        if (mRequestTime > 0) {
            handleSeek(mRequestTime);
        }
        mVideoInputDone = mVideoOutputDone = true;

        if (mVideoTrackIndex >= 0) {
            mVideoInputDone = mVideoOutputDone = false;
            videoThread = new Thread(mVideoTask, "VideoTask");
        }
        mAudioInputDone = mAudioOutputDone = true;
        if (mAudioTrackIndex >= 0) {
            mAudioInputDone = mAudioOutputDone = false;
            audioThread = new Thread(mAudioTask, "AudioTask");
        }
        mMuxer.start();
        mMuxerStarted = true;
        if (videoThread != null) videoThread.start();
        if (audioThread != null) audioThread.start();
    }


    private final void handleSeek(final long newTime) {
        if (DEBUG) Log.d(TAG, "handleSeek");
        if (newTime < 0) return;

        if (mVideoTrackIndex >= 0) {
            mVideoMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mVideoMediaExtractor.advance();
        }
        if (mAudioTrackIndex >= 0) {
            mAudioMediaExtractor.seekTo(newTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            mAudioMediaExtractor.advance();
        }
        mRequestTime = -1;
    }

    private final void handleLoop(final TrimmingCallback frameCallback) {

        synchronized (mSync) {
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        if (mVideoInputDone && mVideoOutputDone && mAudioInputDone && mAudioOutputDone) {
            if (DEBUG) Log.d(TAG, "Reached EOS, looping check");
            handleStop();
        }
    }


    private final void handleInputMuxing(MediaExtractor extractor, int mTrackIndex) {
        int inputChunk = 0;


        mVideoLength = mEndTime - mBeginTime;
        boolean muxingDone = false;
        boolean inputDone = false;
        int bufferInfoSize = 0;
        int bufferInfoOffset = 0;
        long bufferInfoPTS = 0;
        int bufferInfoFlag = 0;
        ByteBuffer inputBuf = ByteBuffer.allocate(512 * 512);
        while (!inputDone) {
            int chunkSize = extractor.readSampleData(inputBuf, 0);
            int flag = extractor.getSampleFlags();
            long presentationTimeUs = extractor.getSampleTime();
            Log.d(TAG, "time : " + presentationTimeUs + " track " + mTrackIndex);
            if (presentationTimeUs > mEndTime) {
                muxingDone = true;
                inputDone = true;
                bufferInfoFlag = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            } else {
                if (flag == MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                else if (flag == MediaCodec.BUFFER_FLAG_SYNC_FRAME)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                else if (flag == MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    bufferInfoFlag = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }

            Log.d(TAG, "flag : " + flag + " track " + mTrackIndex);


            if (chunkSize < 0) {
                muxingDone = true;
                inputDone = true;
                Log.d(TAG, "sent input EOS");
            } else {
                if (extractor.getSampleTrackIndex() != mTrackIndex) {
                    Log.w(TAG, "WEIRD: got sample from track " +
                            extractor.getSampleTrackIndex() + ", expected " + mTrackIndex);
                }
                bufferInfoSize = chunkSize;
                bufferInfoPTS = extractor.getSampleTime();
                Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                        chunkSize);

                inputChunk++;
                extractor.advance();
            }


            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.set(bufferInfoOffset, bufferInfoSize, bufferInfoPTS, bufferInfoFlag);
            inputBuf.position(bufferInfo.offset);
            inputBuf.limit(bufferInfo.offset + bufferInfo.size);
            mMuxer.writeSampleData(mTrackIndex, inputBuf, bufferInfo);

            Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer, ts=" +
                    bufferInfo.presentationTimeUs);

        }
        if (muxingDone) {
            if (DEBUG) Log.i(TAG, "video track input reached EOS");
            synchronized (mSync) {
                if (mTrackIndex == mVideoTrackIndex)
                    mVideoInputDone = true;
                else if (mTrackIndex == mAudioTrackIndex)
                    mAudioInputDone = true;
                mSync.notifyAll();
            }
        }

    }


    private final void handleStop() {
        if (DEBUG) Log.v(TAG, "handleStop:");
        synchronized (mVideoTask) {
            internal_stop_video();
            mVideoTrackIndex = -1;
        }
        synchronized (mAudioTask) {
            internal_stop_audio();
            mAudioTrackIndex = -1;
        }

        if (mMuxer != null) {
            if (mMuxerStarted)
                mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            Log.d(TAG, "muxer stoped");
        }

        if (mVideoMediaExtractor != null) {
            mVideoMediaExtractor.release();
            mVideoMediaExtractor = null;
        }
        if (mAudioMediaExtractor != null) {
            mAudioMediaExtractor.release();
            mAudioMediaExtractor = null;
        }
        if (mMetadata != null) {
            mMetadata.release();
            mMetadata = null;
        }
        synchronized (mSync) {
            mVideoOutputDone = mVideoInputDone = mAudioOutputDone = mAudioInputDone = true;
            mState = STATE_STOP;
        }
        mCallback.onFinished();
    }

    protected void internal_stop_video() {
        if (DEBUG) Log.v(TAG, "internal_stop_video:");
    }

    protected void internal_stop_audio() {
        if (DEBUG) Log.v(TAG, "internal_stop_audio:");
    }


    final int selectTrack(final MediaExtractor extractor, final String mimeType) {
        final int numTracks = extractor.getTrackCount();
        MediaFormat format;
        String mime;
        int trackIndex;
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video/")) {
                int rotation = format.getInteger(MediaFormat.KEY_ROTATION);
                mMuxer.setOrientationHint(rotation);
            }
            if (mime.startsWith(mimeType)) {
                if (DEBUG) {
                    trackIndex = mMuxer.addTrack(format);
                    Log.d(TAG_STATIC, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return trackIndex;
            }
        }
        return -1;
    }


    public void setStartTime(long time) {
        mBeginTime = time;
    }

    public void setEndTime(long time) {
        mEndTime = time;
    }


}

