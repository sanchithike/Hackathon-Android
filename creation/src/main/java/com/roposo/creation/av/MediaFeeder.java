package com.roposo.creation.av;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by bajaj on 18/08/16.
 */
public class MediaFeeder implements Runnable {
    private final static String TAG = MediaFeeder.class.getSimpleName();

    private final static boolean VERBOSE = false || AVUtils.VERBOSE;
    private SessionConfig mConfig;

    protected Muxer mMuxer;
    protected BufferInfo mBufferInfo;
    protected int mTrackIndex;
    protected volatile boolean mForceEos = false;
    int mEosSpinCount = 0;
    final int MAX_EOS_SPINS = 10;

    public boolean mStarted;
    private long mTimeStamp;
    private long mPauseTimeStamp =0;
    private long mResumeTimeStamp =0;
    private boolean mPause;

    private boolean mIsVideoFeeder;

    private static long mFirstAudioTimestamp = -1;
    private static long mFirstVideoTimestamp = -2;

    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;

    private static final int MSG_FRAME_AVAILABLE = 1;
    private MediaHandler mHandler;
    private boolean mPauseEnabled;

    /**
     * Doesn't run on a separate thread as of now.
     */
    public MediaFeeder(SessionConfig config) {
        mConfig = config;
        if(mConfig != null) {
            mMuxer = mConfig.getMuxer();
        }
    }

    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new MediaHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
            mReadyFence.notify();
        }
    }

    public void setPauseEnabled(boolean pauseEnabled) {
        mPauseEnabled = pauseEnabled;
    }

    public void writeFrame(Muxer.MediaFrame mediaFrame) {
        BufferInfo bufferInfo = mediaFrame.mBufferInfo;

        Log.d(TAG, "sent " + bufferInfo.size+ " bytes to muxer");
        Log.d(TAG, "bufferInfo: " + bufferInfo.flags + " " + bufferInfo.presentationTimeUs);

        mMuxer.writeSampleData(mediaFrame);
    }

    public void setVideoConfig(SessionConfig videoConfig) {
        mConfig = videoConfig;
        if(mConfig != null) {
            mMuxer = mConfig.getMuxer();
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class MediaHandler extends Handler {
        private WeakReference<MediaFeeder> mWeakFeeder;

        public MediaHandler(MediaFeeder mediaFeeder) {
            mWeakFeeder = new WeakReference<>(mediaFeeder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            MediaFeeder mediaFeeder = mWeakFeeder.get();
            if (mediaFeeder == null) {
                Log.w(TAG, "MediaFeeder.handleMessage: encoder is null");
                return;
            }

            try {
                switch (what) {
                    case MSG_FRAME_AVAILABLE:
                        mediaFeeder.handleFrameAvailable();
                        break;
                    default:
                        Log.d(TAG, "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFrameAvailable() {

    }

    /**
     * This method should be called before the last input packet is queued
     * Some devices don't honor MediaCodec#signalEndOfInputStream
     * e.g: Google Glass
     */
    public void signalEndOfStream() {
        mForceEos = true;
    }

    public void release() {
        if(mMuxer != null)
            mMuxer.onEncoderReleased(mTrackIndex);

        if (VERBOSE) Log.i(TAG, "Released");
    }

    public void addTrack(MediaFormat format) {
        Log.d(TAG, "Adding track for: " + format);
        // now that we have the Magic Goodies, start the muxer
        mTrackIndex = mMuxer.addTrack(format);
        mStarted = true;
    }

    public void drainEncoder(boolean endOfStream) {
        //TODO critical remove this hard coding
        ByteBuffer encodedData = null;

        if (endOfStream && VERBOSE) {
            if (mIsVideoFeeder) {
                Log.i(TAG, "final video drain");
            } else {
                Log.i(TAG, "final audio drain");
            }
        }
        synchronized (mMuxer) {
            final int TIMEOUT_USEC = 1000;
            if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ") track: " + mTrackIndex);

            if((mTrackIndex == 0) && (mFirstVideoTimestamp < 0)) {
//TODO have a look critical                break;
            }
            if((mTrackIndex == 1) && (mFirstVideoTimestamp < 0)) {
                mFirstVideoTimestamp++;
            }

            if (mBufferInfo.size >= 0) {    // Allow zero length buffer for purpose of sending 0 size video EOS Flag
                // adjust the ByteBuffer values to match BufferInfo (not needed?)
                encodedData.position(mBufferInfo.offset);
                encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                if (mForceEos) {
                    mBufferInfo.flags = mBufferInfo.flags | MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    Log.i(TAG, "Forcing EOS");
                }
                // It is the muxer's responsibility to release encodedData

//TODO critical
/*                mTimeStamp = timestamp;
                mBufferInfo.presentationTimeUs = timestamp;
                mMuxer.writeSampleData(mTrackIndex, encoderStatus, encodedData, mBufferInfo);*/
                if (VERBOSE) {
                    Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, \t ts=" +
                            mBufferInfo.presentationTimeUs + "track " + mTrackIndex);
                }
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (!endOfStream) {
                    Log.w(TAG, "reached end of stream unexpectedly");
                } else {
                    if (VERBOSE) Log.d(TAG, "end of stream reached for track " + mTrackIndex);
                }
            }
        }
        if (endOfStream && VERBOSE ) {
            if (mIsVideoFeeder) {
                Log.i(TAG, "final video drain complete");
            } else {
                Log.i(TAG, "final audio drain complete");
            }
        }
    }

    private void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "MediaFeeder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }
}