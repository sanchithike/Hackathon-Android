package com.roposo.creation.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.EventTrackUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.roposo.creation.av.AVUtils.MAX_FRAME_INTERVAL;

/**
 * Created by Amud on 18/07/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AndroidMuxer extends Muxer implements Runnable {
    private static final String TAG = "AndroidMuxer";

    private static final boolean VERBOSE = false || AVUtils.VERBOSE;

    private static final int VIDEO_FRAME_BUFFER_SIZE = 100 * 1024; // bytes
    private static final int AUDIO_FRAME_BUFFER_SIZE = 20 * 1024; // bytes

    private static final int FRAME_QUEUE_CAPACITY[] = new int[]{AUDIO_FRAME_QUEUE_CAPACITY, VIDEO_FRAME_QUEUE_CAPACITY};
    private static final int FRAME_BUFFER_SIZE[] = new int[]{AUDIO_FRAME_BUFFER_SIZE, VIDEO_FRAME_BUFFER_SIZE};

    private MediaMuxer mMuxer;
    private boolean mStarted;
    private final Lock mReadyFence = new ReentrantLock();
    private final Lock mMuxerLock = new ReentrantLock();
    private Condition mCondition = mMuxerLock.newCondition();

    private boolean mRunning = false;
    private boolean mReady = false;

    // Indicate whether the last audio/video frame have been fed to the Muxer queue
    private volatile boolean mLastAudioFrameReceived, mLastVideoFrameReceived;
    private long []mFirstFrameTs;
    private boolean firstAudioFrameReceived, firstVideoFrameReceived;
    private volatile long[] mLastFrameTs;
    private volatile long[] mPrevFrameTs;
    private boolean[] lastFrameWritten;

    private long[] mPresentationTimeOffset;

    @SuppressWarnings("unchecked")
    private AndroidMuxer(String outputFile, FORMAT format) {
        super(outputFile, format);
        try {
            switch(format) {
                case MPEG4:
                    mMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized format!");
            }
        } catch (IOException e) {
            HashMap<String, String> map = new HashMap<>();
            map.put("path", outputFile);
            EventTrackUtil.logDebug("AndroidMuxer", "constructor", "AndroidMuxer", map, 4);
            CrashlyticsWrapper.logException(e);
        }

        mStarted = false;

        mFirstFrameTs = new long[MAX_TRACKS_SUPPORTED];
        mLastFrameTs = new long[MAX_TRACKS_SUPPORTED];
        mPrevFrameTs = new long[MAX_TRACKS_SUPPORTED];
        lastFrameWritten = new boolean[MAX_TRACKS_SUPPORTED];

        mPresentationTimeOffset = new long[MAX_TRACKS_SUPPORTED];

        for (int i = 0; i < mFirstFrameTs.length; i++) {
            mFirstFrameTs[i] = -1;
            mLastFrameTs[i] = 0;
            mPrevFrameTs[i] = 0;
            lastFrameWritten[i] = false;
            mPresentationTimeOffset[i] = 0;
        }
        startMuxerThread();
    }

    public static AndroidMuxer create(String outputFile, FORMAT format) {
        return new AndroidMuxer(outputFile, format);
    }

    private void startMuxerThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "AndroidMuxer").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void run() {
        synchronized (mReadyFence) {
            mRunning = true;
            mReady = true;
            mReadyFence.notify();
        }

        try {
            runMuxer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mReadyFence.notify();
        }
        mController.onRecordingFinished(true);
    }

    private void runMuxer() throws InterruptedException {
        while(true) {
            if(mDrainStopped) {
                Log.d(TAG, "Exiting Muxer loop");
                break;
            }
            mMuxerLock.lock();
            mCondition.await();
            mMuxerLock.unlock();

            drainMuxer();
        }
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        if(mStarted)
            throw new RuntimeException("format changed twice");
        int track = mMuxer.addTrack(trackFormat);

        String mime = trackFormat.getString(MediaFormat.KEY_MIME);
        if(mime.startsWith("video/")) {
            mVideoTrackIndex = track;
            FRAME_QUEUE_CAPACITY[track] = VIDEO_FRAME_QUEUE_CAPACITY;
            FRAME_BUFFER_SIZE[track] = VIDEO_FRAME_BUFFER_SIZE;
        } else if(mime.startsWith("audio/")) {
            mAudioTrackIndex = track;
            FRAME_QUEUE_CAPACITY[track] = AUDIO_FRAME_QUEUE_CAPACITY;
            FRAME_BUFFER_SIZE[track] = AUDIO_FRAME_BUFFER_SIZE;
        } else {
            Log.d(TAG, "addTrack :: invalid mime type");
        }
        mFrameQueues[track] = new PriorityBlockingQueue<>(FRAME_QUEUE_CAPACITY[track]); // Assuming that we want to keep at most 5 elements in the queue
        mTempQueues[track] = new PriorityBlockingQueue<>(mMinFramesToKeep[track]);
        mFrameBuffers[track] = new ConcurrentLinkedQueue<>();

        Log.d(TAG, "allocating buffers");
/*        for(int i = 0; i < FRAME_QUEUE_CAPACITY[track]; i++) {
            ByteBuffer buffer;
            buffer = ByteBuffer.allocate(FRAME_BUFFER_SIZE[track]);
            mFrameBuffers[track].offer(buffer);
        }*/
        if(allTracksAdded()) {
            Log.d(TAG+"all track","done");
            start();
        }
        return track;
    }

    protected void start() {
        mMuxer.start();
        Log.d(TAG, "Muxer started");
        mLastAudioFrameReceived = false;
        mLastVideoFrameReceived = false;
        mStarted = true;
    }

    protected synchronized void stop() {
        if(mStarted) {
            try {
                mMuxer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                mController.onRecordingFinished(false);
            }
            Log.d(TAG, "Muxer stopped");
            mStarted = false;
        }
        if(mFrameBuffers != null) {
            for (ConcurrentLinkedQueue<ByteBuffer> frameBuffer : mFrameBuffers) {
                if (frameBuffer != null) {
                    frameBuffer.clear();
                }
            }
        }
        if(mFrameQueues != null) {
            for (PriorityBlockingQueue<MediaFrame> frameBuffer : mFrameQueues) {
                if (frameBuffer != null) {
                    frameBuffer.clear();
                }
            }
        }
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    public synchronized void writeSampleData(MediaFrame mediaFrame) {
        if (VERBOSE) Log.d(TAG, "write media frame: " + " track index: " + mediaFrame.mTrackIndex + " ts: " + mediaFrame.mBufferInfo.presentationTimeUs + "\tsize: " + mediaFrame.mBufferInfo.size + "\tframetype: " + mediaFrame.mFrameType + "\tflags: " + mediaFrame.mBufferInfo.flags);
        int trackIndex = mediaFrame.mTrackIndex;
        if(trackIndex < 0) {
            Log.w(TAG, "invalid track index, ignoring frame");
            return;
        }
        long firstFrameTs = mFirstFrameTs[trackIndex];
        if(firstFrameTs >= 0) {
            if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                mediaFrame.mBufferInfo.presentationTimeUs -= mFirstFrameTs[trackIndex];
            }
            if(mediaFrame.mBufferInfo.presentationTimeUs < 0) {
                Log.d(TAG, "Ignoring timestamp: " + mediaFrame.mBufferInfo.presentationTimeUs);
                return;
            }
            pushBufferToQueue(mediaFrame, trackIndex);
        } else {
//            if (mIsAudioSourceMic && trackIndex == mAudioTrackIndex && mHasVideo && mVideoTrackIndex < 0) {
//                offerFrameBuffer(trackIndex, mediaFrame.mFrameData);
//                return;
//            }
            mTempQueues[trackIndex].offer(mediaFrame);
            if (VERBOSE) {
                Log.d(TAG, "TEMP QUEUE STATE: track: " + trackIndex + "\n " + String.valueOf(mTempQueues[trackIndex]));
                Log.d(TAG, "TEMP QUEUE STATE: track: " + (1 - trackIndex) + "\n " + String.valueOf(mTempQueues[1 - trackIndex]));
            }
            if (mTempQueues[trackIndex].size() >= mMinFramesToKeep[trackIndex]) {
                if (mIsAudioSourceMic && mHasVideo && mVideoTrackIndex < 0) {
                    MediaFrame frame = mTempQueues[trackIndex].peek();
                    if (frame != null && (frame.mBufferInfo.flags & (MediaCodec.BUFFER_FLAG_CODEC_CONFIG | MediaCodec.BUFFER_FLAG_END_OF_STREAM)) == 0) {
                        releaseMediaFrame(mTempQueues[trackIndex].poll(), false);
                    }
                } else {
                    if (VERBOSE) {
                        Log.d(TAG, "Finally TEMP QUEUE STATE: track: " + trackIndex + "\n " + String.valueOf(mTempQueues[trackIndex]));
                        Log.d(TAG, "Finally TEMP QUEUE STATE: track: " + (1 - trackIndex) + "\n " + String.valueOf(mTempQueues[1 - trackIndex]));
                    }
                    // temporarily poll a buffer to be able to access 2nd buffer
                    MediaFrame frame = mTempQueues[trackIndex].poll();
                    long minTs = frame.mBufferInfo.presentationTimeUs;
                    long minTs2 = mTempQueues[trackIndex].peek().mBufferInfo.presentationTimeUs;

                    // Since we've grabbed them from priority queue, minTs2 is always greater than minTs
                    if ((minTs2 - minTs) > MAX_FRAME_INTERVAL) {
                        // this means that it's probably in absolute timestamps starting from a fixed reference time (say, System clock)
                        firstFrameTs = minTs2;
                    } else {
                        firstFrameTs = minTs;
                    }
                    if (VERBOSE)
                        Log.d(TAG, "track: " + trackIndex + " first frame ts: " + firstFrameTs);
                    mTempQueues[trackIndex].offer(frame); // Put the buffer back in the queue.

                    if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        mFirstFrameTs[trackIndex] = firstFrameTs;
                    }

                    while (!mTempQueues[trackIndex].isEmpty()) {
                        mediaFrame = mTempQueues[trackIndex].poll();
                        if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            mediaFrame.mBufferInfo.presentationTimeUs -= firstFrameTs;
                        }
                        if (mediaFrame.mBufferInfo.presentationTimeUs < 0) {
                            Log.d(TAG, "Ignoring timestamp: " + mediaFrame.mBufferInfo.presentationTimeUs);
                            continue;
                        }
                        pushBufferToQueue(mediaFrame, trackIndex);
                    }
                }
            }
        }
    }

    private void pushBufferToQueue(MediaFrame mediaFrame, int trackIndex) {
        if (!firstAudioFrameReceived || !firstVideoFrameReceived) {
            if (mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_AUDIO) {
                firstAudioFrameReceived = true;
            } else if (mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_VIDEO) {
                firstVideoFrameReceived = true;
            }
        }
        if (mFrameQueues[trackIndex] == null) {
            return;
        }
        synchronized (mFrameQueues[trackIndex]) {
            mediaFrame.mBufferInfo.presentationTimeUs = mediaFrame.mBufferInfo.presentationTimeUs - mPresentationTimeOffset[trackIndex];
        }

        boolean frameAdded = mFrameQueues[trackIndex].offer(mediaFrame);
        if (VERBOSE) {
            Log.d(TAG, "FRAME QUEUE STATE: track: " + trackIndex + "\n " + String.valueOf(mFrameQueues[trackIndex]));
            Log.d(TAG, "FRAME QUEUE STATE: track: " + (1 - trackIndex) + "\n " + String.valueOf(mFrameQueues[1 - trackIndex]));
        }
        if (VERBOSE) {
            Log.d(TAG, "finally write media frame: " + mediaFrame + " \ttrack index: " + mediaFrame.mTrackIndex + " \tts: " + mediaFrame.mBufferInfo.presentationTimeUs);
        }

        if (VERBOSE) {
            Log.d(TAG, "trackIndex: " + trackIndex + " Media Frame added :: " + " queue size: " + mFrameQueues[trackIndex].size() + " ts: " + mediaFrame.mBufferInfo.presentationTimeUs);
        }

        if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) {
            synchronized (this) {
                if (mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_AUDIO || mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_AUDIO_EOS) {
                    mLastAudioFrameReceived = true;
                    if (!lastFrameWritten[trackIndex]) {
                        mLastFrameTs[trackIndex] = mediaFrame.mBufferInfo.presentationTimeUs;
                    }
                    Log.d(TAG, "EOS received for audio: " + mLastFrameTs[trackIndex]);
                } else if (mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_VIDEO || mediaFrame.mFrameType == MediaFrame.FRAME_TYPE_VIDEO_EOS) {
                    mLastVideoFrameReceived = true;
                    if (!lastFrameWritten[trackIndex]) {
                        mLastFrameTs[trackIndex] = mediaFrame.mBufferInfo.presentationTimeUs;
                    }
                    Log.d(TAG, "EOS received for video: " + mLastFrameTs[trackIndex]);
                }
            }
        }
        mMuxerLock.lock();
        mCondition.signal();
        mMuxerLock.unlock();
    }

    private void drainMuxer() throws InterruptedException {
        while(true) {
            if (!mStarted) return;
            if(mAudioTrackIndex < 0 && mVideoTrackIndex < 0) {
                break;
            }

            // If it's not the last frame, drain muxer only if the queue is atleast half full
            // This is done to ensure that we get to sort the queue based on timestamps
            // Again all this is based on the assumption that beyond @link{AndroidMuxer.FRAME_QUEUE_CAPACITY}, we can't have frames in reverse order.

            // If any of the queues full
/*            for (int i = 0; i < mFrameQueues.length; i++) {
                if(mFrameQueues[i] == null) continue;
                while (mFrameQueues[i].size() > FRAME_QUEUE_CAPACITY[i]) {
                    Log.w(TAG, "1 " + ((i== mAudioTrackIndex) ? "audio" : "video") + "Queue " + i + " full");
                    MediaFrame frame = mFrameQueues[i].remove();
                    MediaFrame top = mFrameQueues[i].peek();
                    synchronized (mFrameQueues[i]) {
                        if (top != null) {
                            mPresentationTimeOffset[i] += top.mBufferInfo.presentationTimeUs - frame.mBufferInfo.presentationTimeUs;
                            ArrayList<MediaFrame> frames = new ArrayList<>(mFrameQueues[i].size());
                            while (!mFrameQueues[i].isEmpty()) {
                                frames.add(mFrameQueues[i].poll());
                            }
                            for (MediaFrame mediaFrame : frames) {
                                if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    mediaFrame.mBufferInfo.presentationTimeUs = mediaFrame.mBufferInfo.presentationTimeUs - mPresentationTimeOffset[i];
                                }
                                if (mediaFrame.mBufferInfo.presentationTimeUs >= mPrevFrameTs[i]) {
                                    mFrameQueues[i].offer(mediaFrame);
                                }
                            }
                            frames.clear();
                        }
                    }
                }
            }*/
            if (VERBOSE) Log.d(TAG, "video track index: " + mVideoTrackIndex + " audio track index: " + mAudioTrackIndex);
            // frame queue must have a minimum no. of frames to be drained,
            // unless of course, last frames for both haven't been received
            if ((!mHasAudio || !mLastAudioFrameReceived)
                    && (!mHasVideo || !mLastVideoFrameReceived)
                    && ((mHasAudio && (mAudioTrackIndex < 0
                    || mFrameQueues[mAudioTrackIndex].size() < mMinFramesToKeep[mAudioTrackIndex]))
                    || (mHasVideo && (mVideoTrackIndex < 0
                    || mFrameQueues[mVideoTrackIndex].size() < mMinFramesToKeep[mVideoTrackIndex])))) {
                if (VERBOSE) Log.d(TAG, "Trying to fill the buffer queue upto: " + (mAudioTrackIndex >= 0 ? mMinFramesToKeep[mAudioTrackIndex] : -1) + " size" + " audio: " + ((mAudioTrackIndex >= 0) ? mFrameQueues[mAudioTrackIndex].size() : -1) + " video: " + ((mVideoTrackIndex >= 0) ? mFrameQueues[mVideoTrackIndex].size() : -1));
                // Wait for someone to post data to (any of) the queues
                break;
            } else {
                if (VERBOSE) Log.d(TAG, "Trying to drain muxer: " + "lastAudioReceived: " + mLastAudioFrameReceived + " lastVideoReceived: " + mLastVideoFrameReceived + " audioqueue size: " + ((mAudioTrackIndex >= 0) ? mFrameQueues[mAudioTrackIndex].size() : -1) + " videoqueuesize: " + ((mVideoTrackIndex >= 0) ? mFrameQueues[mVideoTrackIndex].size() : -1));
            }

            MediaFrame audioFrame = null, videoFrame = null;
            if (mAudioTrackIndex >= 0) {
                audioFrame = mFrameQueues[mAudioTrackIndex].poll(1, TimeUnit.MILLISECONDS);
                if (audioFrame != null) {
                    mFrameQueues[mAudioTrackIndex].offer(audioFrame);
                }
            }
            if (mVideoTrackIndex >= 0) {
                videoFrame = mFrameQueues[mVideoTrackIndex].poll(1, TimeUnit.MILLISECONDS);
                if (videoFrame != null) {
                    mFrameQueues[mVideoTrackIndex].offer(videoFrame);
                }
            }

            if (VERBOSE) Log.d(TAG, "test audioFrame: " + (audioFrame != null ? audioFrame.mBufferInfo.presentationTimeUs : -1) + " videoFrame: " + (videoFrame != null ? videoFrame.mBufferInfo.presentationTimeUs : -1));
            if (mVideoTrackIndex >= 0) {
                if (!mHasAudio || ((mAudioTrackIndex >= 0) && mFrameQueues[mAudioTrackIndex].isEmpty() && mLastAudioFrameReceived)) {
                    // No need of merging from multiple queues here. 'Sink' directly from the (only) queue
                    MediaFrame mediaFrame = mFrameQueues[mVideoTrackIndex].poll();
                    if (mediaFrame != null) {
                        if (VERBOSE) Log.d(TAG, "Sinking video frames");
                        writeFrameToMuxer(mediaFrame);
                    } else {
                        if (VERBOSE) Log.w(TAG, "Video: How is MediaFrame NULL!");
                    }
                    continue;
                }
            }
            if (mAudioTrackIndex >= 0) {
                if (!mHasVideo || ((mVideoTrackIndex >= 0) && mFrameQueues[mVideoTrackIndex].isEmpty() && mLastVideoFrameReceived)) {
                    // No need of merging from multiple queues here. 'Sink' directly from the (only) queue
                    MediaFrame mediaFrame = mFrameQueues[mAudioTrackIndex].poll();
                    if (mediaFrame != null) {
                        if (VERBOSE) Log.d(TAG, "Sinking audio frames");
                        writeFrameToMuxer(mediaFrame);
                    } else {
                        if (VERBOSE) Log.w(TAG, "Audio: How is MediaFrame NULL!");
                    }
                    continue;
                }
            }

            if(audioFrame == null || videoFrame == null) {
                break;
            }

            long audioVideoTsDiff = 0;
            if (audioFrame != null && videoFrame != null) {
                if (VERBOSE) Log.d(TAG, "audio ts: " + audioFrame.mBufferInfo.presentationTimeUs + " video ts: " + videoFrame.mBufferInfo.presentationTimeUs);
                audioVideoTsDiff = audioFrame.mBufferInfo.presentationTimeUs - videoFrame.mBufferInfo.presentationTimeUs;
            } else if (audioFrame != null) {
                if (VERBOSE) Log.d(TAG, "1 Push frame audio ts: " + audioFrame.mBufferInfo.presentationTimeUs);
                audioVideoTsDiff = -1; // Push audio stream
            } else {
                if (VERBOSE) Log.d(TAG, "2 Push frame video ts: " + videoFrame.mBufferInfo.presentationTimeUs);
                audioVideoTsDiff = 1; // Push video stream.
            }
            if (audioVideoTsDiff > MAX_FRAME_INTERVAL) {
                Log.w(TAG, "audio leading. Queue " + mVideoTrackIndex + " full" + "\t reason: timing jump");
                if (VERBOSE) Log.d(TAG, "audioTs: " + audioFrame.mBufferInfo.presentationTimeUs + "\tvideoTs: " + videoFrame.mBufferInfo.presentationTimeUs + "\taudio size: " + mFrameQueues[mAudioTrackIndex].size() + "\tvideo size: " + mFrameQueues[mVideoTrackIndex].size());
                MediaFrame mediaFrame = mFrameQueues[mVideoTrackIndex].remove();
                releaseMediaFrame(mediaFrame, false);
            } else if (audioVideoTsDiff < -MAX_FRAME_INTERVAL) {
                Log.w(TAG, "video leading. Queue " + mAudioTrackIndex + " full" + "\t reason: timing jump");
                MediaFrame mediaFrame = mFrameQueues[mAudioTrackIndex].remove();
                releaseMediaFrame(mediaFrame, false);
            } else {
                int trackIndex = (audioVideoTsDiff < 0)  ? mAudioTrackIndex : mVideoTrackIndex;
                try {
                    MediaFrame frame = mFrameQueues[trackIndex].poll();
                    writeFrameToMuxer(frame);
                    if(!mDrainStarted && (frame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) > 0) {
                        mDrainStarted = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }
        }
    }

    private void writeFrameToMuxer(MediaFrame mediaFrame) {
        super.drainMuxer(mediaFrame);

        int trackIndex = mediaFrame.mTrackIndex;

        if (lastFrameWritten[1 - trackIndex] && mLastFrameTs[1- trackIndex] > 0 && mediaFrame.mBufferInfo.presentationTimeUs > mLastFrameTs[1 - trackIndex]) {
            if (!lastFrameWritten[trackIndex]) {
                Log.d(TAG, "Last frame " + " track index: " + trackIndex + " ts: " + mediaFrame.mBufferInfo.presentationTimeUs);
                mediaFrame.mBufferInfo.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                releaseMediaFrame(mediaFrame, true);
            } else {
                releaseMediaFrame(mediaFrame, false);
            }
        } else {
            releaseMediaFrame(mediaFrame, true);
        }
    }

    private void releaseMediaFrame(MediaFrame mediaFrame, boolean render) {
        int trackIndex = mediaFrame.mTrackIndex;
        MediaCodec.BufferInfo bufferInfo = mediaFrame.mBufferInfo;
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0 && !lastFrameWritten[trackIndex]) {
            synchronized (this) {
                signalEndOfTrack();
                lastFrameWritten[trackIndex] = true;
                mediaFrame.mFrameType = (trackIndex == mAudioTrackIndex) ? MediaFrame.FRAME_TYPE_AUDIO_EOS : MediaFrame.FRAME_TYPE_VIDEO_EOS;
                mLastFrameTs[trackIndex] = mediaFrame.mBufferInfo.presentationTimeUs;
            }
        }

        if (VERBOSE) {
            Log.d(TAG, "write data : " + " track index: " + mediaFrame.mTrackIndex + " \tbufferInfo: " + " \tsize: " + mediaFrame.mBufferInfo.size + " \ttime: " + mediaFrame.mBufferInfo.presentationTimeUs + "\tflags: " + mediaFrame.mBufferInfo.flags + " render: " + render);
        }

        if (render) {
            try {
                mPrevFrameTs[trackIndex] = mediaFrame.mBufferInfo.presentationTimeUs;
                ByteBuffer encodedData = mediaFrame.mFrameData;
                mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // Make the frame buffer usable again.
        offerFrameBuffer(trackIndex, mediaFrame.mFrameData);

        if (render) {
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) {
                if (allTracksFinished()) {
                    Log.d(TAG, "finished: " + "track index: " + trackIndex);
                    stop();
                    mDrainStopped = true;
                }
            }
        }

    }

    @Override
    public void forceStop() {
        stop();
    }

    public MediaMuxer getMuxer() {
        return mMuxer;
    }
}

