package com.roposo.creation.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by Amud on 18/07/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class AndroidEncoder {
    private final static String TAG = "AndroidEncoder";
    private final static boolean VERBOSE = false || AVUtils.VERBOSE;

    final static int TIMEOUT_USEC = 2500;

    protected com.roposo.creation.av.Muxer mMuxer;
    protected MediaCodec mEncoder;
    protected MediaCodec.BufferInfo mBufferInfo;
    protected int mTrackIndex;
    protected volatile boolean mForceEos = false;
    int mEosSpinCount = 0;
    final int MAX_EOS_SPINS = 10;

    public boolean mStarted;
    private long mTimeStamp, mPrevTimestamp;
    private long mPauseTimeStamp =0;
    private long mResumeTimeStamp =0;
    private boolean mPause;
    private boolean mEOSReceived = false;

    protected boolean mIsAudio; // true if Audio, false for Video

    private int mFrameCount;

    public long mFirstFrameRenderTime;
    private FrameListener frameListener;

    long mTimestampOffset = -1;
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
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
            if (VERBOSE) Log.i(TAG, "Released encoder");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void adjustBitrate(int targetBitrate) {
        if(isKitKat() && mEncoder != null) {
            Bundle bitrate = new Bundle();
            bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, targetBitrate);
            mEncoder.setParameters(bitrate);
        }else if (!isKitKat()) {
            Log.w(TAG, "Ignoring adjustVideoBitrate call. This functionality is only available on Android API 19+");
        }
    }

    MediaCodec.BufferInfo cloneBufferInfo(MediaCodec.BufferInfo bufferInfo) {
        MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
        outBufferInfo.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
        return outBufferInfo;
    }

    public synchronized boolean drainEncoder(boolean forceDrain) {
        boolean ret = false;
        if (forceDrain && VERBOSE) {
            if (isSurfaceInputEncoder()) {
                Log.i(TAG, "final video drain");
            } else {
                Log.i(TAG, "final audio drain");
            }
        }
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + forceDrain + ") track: " + mTrackIndex);

        if (forceDrain && mEOSReceived) {
            return false;
        }

        if (mEncoder == null) {
            Log.w(TAG, "Encoder is null. Returning");
            return ret;
        }
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();

        while (true) {
            if (!forceDrain && mFrameCount > 30 && mTrackIndex >= 0 && !mMuxer.isFrameBufferAvailable(mTrackIndex)) {
                if(VERBOSE) Log.d(TAG, "no buffers available in pool. Breaking!");
                // Break if this is not the last frame and we're past the first few frames and no buffer is available in the pool
                break;
            }
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!forceDrain) {
                    ret = false;
                    if (VERBOSE) Log.d(TAG, "Try again later: " + mTrackIndex);
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "Rendering for EOS");
                    ret = true;
                    mEosSpinCount++;
                    if (mEosSpinCount > MAX_EOS_SPINS) {
                        if (VERBOSE) Log.i(TAG, "Force shutting down Muxer : " + "isVideo? " + isSurfaceInputEncoder());
                        // Atleast fucking send the EOS before stopping
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        bufferInfo.set(0, 0, prevOutputPTSUs + 105, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        byte[] data = new byte[1];
                        Log.d(TAG, "Sent EOS at time: " + prevOutputPTSUs);
                        Muxer.MediaFrame mediaFrame = new Muxer.MediaFrame(ByteBuffer.wrap(data), bufferInfo, -1, mIsAudio ? Muxer.MediaFrame.FRAME_TYPE_AUDIO_EOS : Muxer.MediaFrame.FRAME_TYPE_VIDEO_EOS, mTrackIndex);
                        mMuxer.writeSampleData(mediaFrame);
                        if (frameListener != null) {
                            frameListener.onFrameRenderered(mediaFrame.mBufferInfo.presentationTimeUs);
                        }
/*                            mMuxer.forceStop();*/
                        break;
                    }
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
                mMuxer.setMinFramesToKeep(mTrackIndex, encoderOutputBuffers.length);
                Log.d(TAG, "Encoder output buffers changed");
//                    mMuxer.resizeBufferQueue(mTrackIndex, mEncoder.getOutputBuffers().length);
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                MediaFormat newFormat = mEncoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.setMinFramesToKeep(mTrackIndex, encoderOutputBuffers.length);
//                    mMuxer.resizeBufferQueue(mTrackIndex, mEncoder.getOutputBuffers().length);
                String mime = newFormat.getString(MediaFormat.KEY_MIME);
                mStarted = true;
                // Muxer is responsible for starting/stopping itself
                // based on knowledge of expected # tracks
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                if (VERBOSE) {
                    Log.d(TAG, (mIsAudio ? "audio" : "video") + "encoder " + " track index: " + mTrackIndex + " timestamp: " + mBufferInfo.presentationTimeUs + " flags: " + mBufferInfo.flags);
                }

                if (mTimestampOffset < 0 && (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    mTimestampOffset = mBufferInfo.presentationTimeUs;
                }
                long timestamp = mBufferInfo.presentationTimeUs;
                if (mTimestampOffset >  0) {
                    timestamp -= mTimestampOffset;
                }
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (VERBOSE) {
                    Log.d(TAG, "track index: " + mTrackIndex + " buffer index: " + encoderStatus);
                }
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if (mBufferInfo.size >= 0) {    // Allow zero length buffer for purpose of sending 0 size video EOS Flag
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

//                        long prevTs = mBufferInfo.presentationTimeUs;
//                        Log.d(TAG, "Video timestamp before: " + prevTs + " after: " + mBufferInfo.presentationTimeUs);

                    if (mPause && mPauseTimeStamp != 0) {
                        mPause = false;
                        mResumeTimeStamp = timestamp;
                        //hack: 50 is to be rethink
                        timestamp = mPauseTimeStamp + 50;
                    } else if (!mPause && mResumeTimeStamp != 0) {
                        timestamp = mPauseTimeStamp +
                                timestamp - mResumeTimeStamp;
                    }

                    if (timestamp == 0 && ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0) && (mPrevTimestamp > timestamp)) {
                        // Seeing this after device is toggled off/on with power button.  The
                        // first frame back has a zero timestamp.
                        //
                        // MPEG4Writer thinks this is cause to abort() in native code, so it's very
                        // important that we just ignore the frame.
                        Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
                        return ret;
                    }
                    mPrevTimestamp = mTimeStamp;

                    mTimeStamp = timestamp;

                    MediaCodec.BufferInfo bufferInfo = cloneBufferInfo(mBufferInfo);
                    bufferInfo.presentationTimeUs = timestamp;
                    bufferInfo.offset = 0;

                    ByteBuffer copiedData = cloneBuffer(encodedData, bufferInfo);
                    if (copiedData != null) {
                        Muxer.MediaFrame mediaFrame = new Muxer.MediaFrame(copiedData, bufferInfo, encoderStatus, (((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) ? (mIsAudio ? Muxer.MediaFrame.FRAME_TYPE_AUDIO_EOS : Muxer.MediaFrame.FRAME_TYPE_VIDEO_EOS) : (mIsAudio ? Muxer.MediaFrame.FRAME_TYPE_AUDIO : Muxer.MediaFrame.FRAME_TYPE_VIDEO)), mTrackIndex);
                        mMuxer.writeSampleData(mediaFrame);
                        if (frameListener != null) {
                            frameListener.onFrameRenderered(mediaFrame.mBufferInfo.presentationTimeUs);
                        }
                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);

                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, \t ts=" +
                            mBufferInfo.presentationTimeUs + "track " + mTrackIndex);
                } else {
                    Log.w(TAG, "buffer info invalid size: " + mBufferInfo.size);
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "end of stream reached for track " + mTrackIndex);
                    mEOSReceived = true;
                    break;      // out of while
                }
            }
        }

        if (mForceEos && !mEOSReceived) {
            ByteBuffer eosBuffer = ByteBuffer.allocate(1);
            MediaCodec.BufferInfo eosInfo = cloneBufferInfo(mBufferInfo);
            eosInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            eosInfo.size = 0;
            eosInfo.offset = 0;
            Muxer.MediaFrame eosFrame = new Muxer.MediaFrame(eosBuffer, eosInfo, -1, mIsAudio ? Muxer.MediaFrame.FRAME_TYPE_AUDIO_EOS : Muxer.MediaFrame.FRAME_TYPE_VIDEO_EOS, mTrackIndex);
            mMuxer.writeSampleData(eosFrame);
            Log.i(TAG, "Forcing EOS");
        }

        if (forceDrain && VERBOSE) {
            if (isSurfaceInputEncoder()) {
                Log.i(TAG, "final video drain complete");
            } else {
                Log.i(TAG, "final audio drain complete");
            }
        }
        return ret;
    }

    private ByteBuffer cloneBuffer(ByteBuffer src, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer target = mMuxer.getFrameBuffer(mTrackIndex, bufferInfo.size, src.order());

        if (target == null) return null;
        target.order(src.order());

        src.position(bufferInfo.offset);

        target.position(0);
        target.limit(bufferInfo.size);

        target.put(src);

        target.position(0);
        target.limit(bufferInfo.size);

        return target;
    }

    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected long getPTSUs() {
        mFrameCount++;
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = prevOutputPTSUs + 9938;
        result = (long) (mFrameCount * 23219.954);
        return result;
    }

    public void pause()
    {
        if (!mPause) {
            mPauseTimeStamp=mTimeStamp;
            mPause = true;
        }
    }




    protected abstract boolean isSurfaceInputEncoder();

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= 19;
    }

    public void reset() {

        mEOSReceived = false;
        mEosSpinCount = 0;
        mForceEos = false;
        mFirstFrameRenderTime = -1;
    }

    public void setFirstFrameRenderTime(long timeMillis) {
        mFirstFrameRenderTime = timeMillis;
        if(mIsAudio) {
            mMuxer.mFirstAudioFrameRenderTime = timeMillis;
        } else {
            mMuxer.mFirstVideoFrameRenderTime = timeMillis;
        }
    }

    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }
    public interface FrameListener {
        void onFrameRenderered(long timestamp);
    }
}

