package com.roposo.creation.av;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.roposo.creation.graphics.Renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

public abstract class Muxer {
    private static final String TAG = "Muxer";

    private static final boolean VERBOSE = false || AVUtils.VERBOSE;

    public long mFirstVideoFrameRenderTime;
    public long mFirstAudioFrameRenderTime;
    Renderer.ControllerInterface mController;
    boolean mIsAudioSourceMic;
    int minFrameQueueSize;

    static final int AUDIO_FRAME_QUEUE_CAPACITY = 30;
    static final int VIDEO_FRAME_QUEUE_CAPACITY = 12;
    private int buffersAllocated;

    public void removeFrameBuffer(int index) {
        if(mFrameBuffers[index].isEmpty()) return;
        mFrameBuffers[index].remove();
    }

    public void offerFrameBuffer(int index, ByteBuffer buffer) {
        mFrameBuffers[index].add(buffer);
    }

    public synchronized boolean isFrameBufferAvailable(int index) {
        return mFrameBuffers[index].peek() != null;
    }

    public synchronized ByteBuffer getFrameBuffer(int index, int capacity, ByteOrder order) {
        if (mFrameBuffers == null || mDrainStopped) return null;
        if (VERBOSE) Log.d(TAG, "BUFFER POOL " + index + " SIZE: " + mFrameBuffers[index].size());
        ByteBuffer buffer = mFrameBuffers[index].poll();

        if(buffer == null || buffer.capacity() < capacity) {
            Log.w(TAG, "Existing buffer size not enough. Allocating bigger buffer" + " trackIndex: " + index + " buffer size: " + ((buffer != null) ? buffer.capacity() : 0) + " required size: " + capacity);
            buffer = ByteBuffer.allocate(Math.max(capacity, Math.min(200 * 1000, capacity * 2))).order(order); // TODO query size from Encoder
//            mMuxer.removeFrameBuffer(mTrackIndex);
        }

        return buffer;
    }

    public void setController(Renderer.ControllerInterface controller) {
        mController = controller;
    }

    public void setHasVideo(boolean hasVideo) {
        mHasVideo = hasVideo;
        updateTrackCount();
    }

    public void setHasAudio(boolean hasAudio) {
        mHasAudio = hasAudio;
        updateTrackCount();
    }

    private void updateTrackCount() {
        mExpectedNumTracks = 0;
        if (mHasVideo) {
            mExpectedNumTracks++;
        }
        if (mHasAudio) {
            mExpectedNumTracks++;
        }
    }

    public void setIsAudioSourceMic(boolean isAudioSourceMic) {
        mIsAudioSourceMic = isAudioSourceMic;
    }

    public static enum FORMAT { MPEG4, HLS }

    int MAX_TRACKS_SUPPORTED = 2;
    protected int mExpectedNumTracks = 0;
    // Infact this should be queried from the config (which will determine the number of tracks required)

    protected FORMAT mFormat;
    protected String mOutputPath;
    protected int mNumTracks;
    protected int mNumTracksFinished;
    protected long mFirstPts;
    protected long mLastPts[];

    protected volatile int mVideoTrackIndex, mAudioTrackIndex;
    PriorityBlockingQueue<MediaFrame>[] mFrameQueues = new PriorityBlockingQueue[MAX_TRACKS_SUPPORTED];
    ConcurrentLinkedQueue<ByteBuffer>[] mFrameBuffers = new ConcurrentLinkedQueue[MAX_TRACKS_SUPPORTED];

    PriorityBlockingQueue<MediaFrame>[] mTempQueues = new PriorityBlockingQueue[MAX_TRACKS_SUPPORTED];

    protected int[] mMinFramesToKeep;
    boolean mDrainStarted = false, mDrainStopped = false;
    boolean mHasAudio = false, mHasVideo = false;

    protected Muxer(String outputPath, FORMAT format){
        Log.i(TAG, "Created muxer for output: " + outputPath);
        mOutputPath = checkNotNull(outputPath);
        mFormat = format;
        mNumTracks = 0;
        Log.d("num track"+TAG,String.valueOf(mNumTracks));
        mNumTracksFinished = 0;
        Log.d("finish track"+TAG,String.valueOf(mNumTracksFinished));
        mFirstPts = 0;
        mLastPts = new long[MAX_TRACKS_SUPPORTED];
        mMinFramesToKeep = new int[MAX_TRACKS_SUPPORTED];
        for(int i=0; i< mLastPts.length; i++) {
            mLastPts[i] = 0;
        }
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;

        mDrainStarted = false;
        mDrainStopped = false;

        for (int index = 0; index < mMinFramesToKeep.length; index++) {
            mMinFramesToKeep[index] = 4;
        }
    }

    /**
     * Returns the absolute output path.
     *
     * e.g /sdcard/app/uuid/index.m3u8
     * @return
     */
    public String getOutputPath(){
        return mOutputPath;
    }

    /**
     * Adds the specified track and returns the track index
     *
     * @param trackFormat MediaFormat of the track to add. Gotten from MediaCodec#dequeueOutputBuffer
     *                    when returned status is INFO_OUTPUT_FORMAT_CHANGED
     * @return index of track in output file
     */
    public synchronized int addTrack(MediaFormat trackFormat){
        mNumTracks++;
        Log.d("num track"+TAG,String.valueOf(mNumTracks));
        Log.d(TAG, "track added: " + trackFormat);
        return mNumTracks - 1;
    }

    // Not used now..
    // Because we're using a size of Frame Queue different (and larger) than Codec buffer queue
    public void resizeBufferQueue(int trackIndex, int length) {
        PriorityBlockingQueue<MediaFrame> newQueue = new PriorityBlockingQueue<>(length);
        PriorityBlockingQueue<MediaFrame> oldQueue = mFrameQueues[trackIndex];
        newQueue.addAll(oldQueue);
        oldQueue.clear();
        mFrameQueues[trackIndex] = newQueue;
    }

    /**
     * Called by the hosting Encoder
     * to notify the Muxer that it should no
     * longer assume the Encoder resources are available.
     *
     */
    public void onEncoderReleased(int trackIndex){
    }

    public boolean isStarted() {
        return false;
    }

    /**
     * Write the MediaCodec output buffer. This method <b>must</b>
     * be overridden by subclasses to release encodedData, transferring
     * ownership back to encoder, by calling encoder.releaseOutputBuffer(bufferIndex, false);
     *
     * @param mediaFrame
     */
    public void drainMuxer(MediaFrame mediaFrame) {
        MediaCodec.BufferInfo bufferInfo = mediaFrame.mBufferInfo;
        int trackIndex = mediaFrame.mTrackIndex;
    }

    public synchronized void setMinFramesToKeep(int trackIndex, int minFramesToKeep) {
        mMinFramesToKeep[trackIndex] = minFramesToKeep; //Math.max(mMinFramesToKeep[trackIndex], minFramesToKeep);
        minFrameQueueSize = Math.min(mMinFramesToKeep[trackIndex], mMinFramesToKeep[1 - trackIndex]);
    }

    public abstract void writeSampleData(MediaFrame mediaFrame);
//    public abstract void writeSampleData(int trackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo);

    public abstract void forceStop();

    protected boolean allTracksFinished() {
        if (mNumTracks > 0) {
            if (VERBOSE) Log.d(TAG, "num tracks: " + String.valueOf(mNumTracks));
            if (VERBOSE) Log.d(TAG, "finished tracks: " + String.valueOf(mNumTracksFinished));
            return (mNumTracks == mNumTracksFinished);
        }
        return false;
    }

    protected boolean allTracksAdded() {
        Log.d(TAG, "Add track" + " count: " + String.valueOf(mNumTracks));
        return (mNumTracks == mExpectedNumTracks);
    }

    /**
     * Muxer will call this itself if it detects BUFFER_FLAG_END_OF_STREAM
     * in writeSampleData.
     */
    protected synchronized void signalEndOfTrack() {
        mNumTracksFinished++;
        Log.d(TAG, "finish track" + String.valueOf(mNumTracksFinished));
    }

    /**
     * Does this Muxer's format require AAC ADTS headers?
     * see http://wiki.multimedia.cx/index.php?title=ADTS
     * @return
     */
    protected boolean formatRequiresADTS(){
        switch(mFormat){
            case HLS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Does this Muxer's format require
     * copying and buffering encoder output buffers.
     * Generally speaking, is the output a Socket or File?
     * @return
     */
    protected boolean formatRequiresBuffering(){
        if (Build.VERSION.SDK_INT >= 21) return true;

        switch(mFormat){
            case HLS:
                return false;
            default:
                return false;
        }
    }

    /**
     * Return a relative pts given an absolute pts and trackIndex.
     *
     * This method advances the state of the Muxer, and must only
     * be called once per call to {@link #writeSampleData(MediaFrame mediaFrame)}.
     */
    protected long getNextRelativePts(long absPts, int trackIndex) {
        if (mFirstPts == 0) {
            mFirstPts = absPts;
            return 0;
        }
        return getSafePts(absPts - mFirstPts, trackIndex);
    }

    /**
     * Sometimes packets with non-increasing pts are dequeued from the MediaCodec output buffer.
     * This method ensures that a crash won't occur due to non monotonically increasing packet timestamp.
     */
    private long getSafePts(long pts, int trackIndex) {
        if (mLastPts[trackIndex] >= pts) {
            // Enforce a non-zero minimum spacing
            // between pts
            mLastPts[trackIndex] += 9643;
            return mLastPts[trackIndex];
        }
        mLastPts[trackIndex] = pts;
        return pts;
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static class MediaFrame implements Comparable<MediaFrame> {
        public static final int FRAME_TYPE_AUDIO = 1;
        public static final int FRAME_TYPE_VIDEO = 2;
        public static final int FRAME_TYPE_AUDIO_EOS = 3;
        public static final int FRAME_TYPE_VIDEO_EOS = 4;

        ByteBuffer mFrameData;
        MediaCodec.BufferInfo mBufferInfo;
        int mTrackIndex;
        int mBufferIndex;

        int mFrameType;

        public MediaFrame(ByteBuffer frameData, MediaCodec.BufferInfo bufferInfo, int bufferIndex, int frameType, int trackIndex) {
            mFrameData = frameData;
            mBufferInfo = bufferInfo;
            mBufferIndex = bufferIndex;
            mFrameType = frameType;
            mTrackIndex = trackIndex;
        }

        @Override
        public int compareTo(MediaFrame rhs) {
            int ret = 0;

            // Ideally we've liked to just return the difference between both timestamps
            // But typecasting from larger to smaller data type can result in changed signs as well.
            if(mBufferInfo.presentationTimeUs == rhs.mBufferInfo.presentationTimeUs) {
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) > 0) {
                    ret = -1;
                } else if ((rhs.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) > 0) {
                    ret = 1;
                } else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) > 0) {
                    ret = -1;
                } else {
                    ret = 0;
                }
            } else if(mBufferInfo.presentationTimeUs > rhs.mBufferInfo.presentationTimeUs) {
                ret = 1;
            } else {
                ret = -1;
            }
            return ret;
        }

        @Override
        public String toString() {
            String result = "MediaFrame";
            result += " pts: " + mBufferInfo.presentationTimeUs + "\tsize: " + mBufferInfo.size +
                    " \tflags: " + mBufferInfo.flags + "\n";
            return result;
        }
    }
}


