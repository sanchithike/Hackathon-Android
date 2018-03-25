package com.roposo.creation.av;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Trace;
import android.support.annotation.IntDef;
import android.util.Log;

import com.roposo.creation.av.Muxer.MediaFrame;
import com.roposo.creation.graphics.Renderer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by Amud on 18/07/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioEncoder implements Runnable {
    private static final boolean TRACE = false;
    //Temp removed -- to be moved in another module
    private static final boolean VERBOSE = false || AVUtils.VERBOSE;
    private static final String TAG = "AudioEncoder";

    protected static final int SAMPLES_PER_FRAME = 1024;                            // AAC frame size. Audio encoder input size is a multiple of this
    protected static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Object mReadyFence = new Object();    // Synchronize audio thread readiness
    private int mAudioInputType;
    private boolean mThreadReady;                       // Is audio thread ready
    private boolean mThreadRunning;                     // Is audio thread running
    private final Object mRecordingFence = new Object();

    private AudioRecord mAudioRecord;
    private AudioEncoderCore mEncoderCore;
    protected boolean mPauseEnabled;

    private volatile boolean mRecordingRequested;
    private long mFrameCount;

    PriorityBlockingQueue<MediaFrame> mFrameQueues = new PriorityBlockingQueue<>();
    PriorityBlockingQueue<ByteBuffer> mFrameBuffers = new PriorityBlockingQueue<>();

    Renderer.ControllerInterface mController;

    public static final int BLANK_AUDIO = 0;
    public static final int AUDIO = 1;
    public static final int MIC = 2;
    private ByteBuffer mBlankAudioBuffer;

    @IntDef({
            BLANK_AUDIO,
            AUDIO,
            MIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioInputType {
    }

    public AudioEncoder(Renderer.ControllerInterface controller, @AudioInputType int audioInputType) throws IOException {
        mController = controller;
        setAudioInputType(audioInputType);
    }

    public void setAudioInputType(@AudioInputType int audioInputType) {
        mAudioInputType = audioInputType;
    }

    private void init(SessionConfig config) throws IOException {
        mEncoderCore = new AudioEncoderCore(config.getNumAudioChannels(),
                config.getAudioBitrate(),
                config.getAudioSamplerate(),
                config.getMuxer());
        mMediaCodec = null;
        mThreadReady = false;
        mThreadRunning = false;
        mRecordingRequested = false;
        mFrameCount = 0;
        startThread();
        if (VERBOSE) Log.i(TAG, "Finished init. encoder : " + mEncoderCore.mEncoder);
    }

/*    public void setVideoConfig(SessionConfig config) {
        try {
            init(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private void setupAudioRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(mEncoderCore.mSampleRate,
                mEncoderCore.mChannelConfig, AUDIO_FORMAT);

        // TODO permission TODO sahilbaja TODO anilshar Permission check and request
        // Will be in uninitialized state if permission cannot be obtained.
        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.CAMCORDER, // source
                mEncoderCore.mSampleRate,            // sample rate, hz
                mEncoderCore.mChannelConfig,         // channels
                AUDIO_FORMAT,                        // audio format
                minBufferSize * 4);                  // buffer size (bytes)

    }

    public void startRecording() {
        if (VERBOSE) Log.i(TAG, "startRendering");
        synchronized (mRecordingFence) {
            totalSamplesNum = 0;
            startPTS = 0;
            mFrameCount = 0;
            mRecordingRequested = true;

            mEncoderCore.reset();
            mRecordingFence.notify();
        }
    }

    public void stopRecording() {
        Log.i(TAG, "stopRecording");
        synchronized (mRecordingFence) {
            mRecordingRequested = false;
        }
    }

    public void reset(SessionConfig config) throws IOException {
        if (VERBOSE) Log.i(TAG, "reset");
        if (mThreadRunning) Log.e(TAG, "reset called before stop completed");
        init(config);
    }

    public void setPauseEnabled(boolean pauseEnabled) {
        mPauseEnabled = pauseEnabled;
    }

    public boolean isPauseEnabled() {
        return mPauseEnabled;
    }

    public boolean isRecording() {
        return mRecordingRequested;
    }


    public boolean isReusing() {
        return (mEncoderCore != null);
    }

    private void startThread() {
        Log.d(TAG, "startThread Audio");
        synchronized (mReadyFence) {
            if (mThreadRunning) {
                Log.w(TAG, "Audio thread running when start requested");
                return;
            }

            Thread audioThread = new Thread(this, "AudioEncoder");
            audioThread.setPriority(Thread.NORM_PRIORITY);
            audioThread.start();
            while (!mThreadReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            mController.onAudioRecorderReady(true);
        }
    }

    @Override
    public void run() {
        if (mAudioInputType == MIC) {
            setupAudioRecord();
        }
        synchronized (mReadyFence) {
            mThreadReady = true;
            mReadyFence.notify();
        }

        synchronized (mRecordingFence) {
            while (!mRecordingRequested) {
                try {
                    mRecordingFence.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mAudioInputType == MIC) {
            mAudioRecord.startRecording();
        }
        if (VERBOSE) Log.i(TAG, "Begin Audio transmission to encoder. encoder : " + mEncoderCore.mEncoder);

        while (mRecordingRequested) {

            if (!mPauseEnabled) {
                if (TRACE) Trace.beginSection("drainAudio");
                mEncoderCore.drainEncoder(false);
                if (TRACE) Trace.endSection();

                if (TRACE) Trace.beginSection("sendAudio");
                if (mAudioInputType == MIC) {
                    sendAudioToEncoder(false);
                } else if (mAudioInputType == AUDIO) {
                    synchronized (mRecordingFence) {
                        if (mRecordingRequested) {
                            try {
                                if (VERBOSE) Log.d(TAG, "audio encoder: wait for next frame signal");
                                mRecordingFence.wait(40);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (VERBOSE) Log.d(TAG, "audio encoder: got the next frame signal");
                        }
                    }
                    sendExtractedAudioToEncoder();
                } else if (mAudioInputType == BLANK_AUDIO) {
                    sendBlankAudioToEncoder(false);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (TRACE) Trace.endSection();
            } else {
                Log.d(TAG, "pause enabled");
                mEncoderCore.pause();
            }
        }
        mThreadReady = false;
        if (TRACE) Trace.beginSection("sendAudio");
        if (mAudioInputType == MIC) {
            sendAudioToEncoder(true);
        } else if (mAudioInputType == AUDIO) {
            sendExtractedAudioToEncoder();
        } else if (mAudioInputType == BLANK_AUDIO) {
            sendBlankAudioToEncoder(true);
        }
        if (TRACE) Trace.endSection();

        if (mAudioInputType == MIC) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
        if (TRACE) Trace.beginSection("drainAudioFinal");
        mEncoderCore.drainEncoder(true);
        if (TRACE) Trace.endSection();
        mEncoderCore.release();
        mThreadRunning = false;
        mController.onAudioRecordingFinished();
    }

    private ByteBuffer cloneBuffer(ByteBuffer src, MediaCodec.BufferInfo bufferInfo, ByteBuffer target) {
        if(target == null || ((bufferInfo != null) && ((target.capacity() < bufferInfo.size) || (target.remaining() < bufferInfo.size)))) {
            Log.w(TAG, "Existing buffer size not enough. Allocating bigger buffer");
            target = ByteBuffer.allocate(src.capacity()).order(src.order());
        }
        if (bufferInfo == null) {
            if (target.remaining() < src.remaining()) return null;
        }

        target.order(src.order());

        int offset = 0;
        int size = src.capacity();

        if (bufferInfo != null) {
            offset = bufferInfo.offset;
            size = bufferInfo.size;

            // Set buffer info's offset to 0
            bufferInfo.offset = 0;
        }

        src.position(offset);

        target.position(offset);
        target.limit(offset + size);

        target.put(src);

        target.position(offset);
        target.limit(offset + size);

        src.position(0);
        return target;
    }

    private void sendFrameToEncoder(ByteBuffer src, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer target = mFrameBuffers.poll();

        target = cloneBuffer(src, bufferInfo, target);

        MediaFrame mediaFrame = new MediaFrame(target, cloneBufferInfo(bufferInfo), -1, (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0 ? MediaFrame.FRAME_TYPE_AUDIO : MediaFrame.FRAME_TYPE_AUDIO_EOS, -1);

        boolean added = mFrameQueues.add(mediaFrame);
        if (VERBOSE) Log.d(TAG, "Audio frame added: " + added + mediaFrame + "\t Frame Queue size: " +
                mFrameQueues.size());
        synchronized (mRecordingFence) {
            mRecordingFence.notify();
        }
    }

    MediaCodec.BufferInfo cloneBufferInfo(MediaCodec.BufferInfo bufferInfo) {
        MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
        outBufferInfo.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
        return outBufferInfo;
    }

    // Variables recycled between calls to sendAudioToEncoder
    MediaCodec mMediaCodec;
    int audioInputBufferIndex;
    int audioInputLength;
    long audioAbsolutePtsUs;

    private void sendExtractedAudioToEncoder() {
        if (mMediaCodec == null)
            mMediaCodec = mEncoderCore.getMediaCodec();
        // send current frame data to encoder
        while (!mFrameQueues.isEmpty()) {
            try {
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                if (audioInputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                    inputBuffer.clear();
                    MediaFrame mediaFrame = mFrameQueues.poll();
                    audioInputLength = mediaFrame.mBufferInfo.size;
                    audioAbsolutePtsUs = mediaFrame.mBufferInfo.presentationTimeUs;
                    cloneBuffer(mediaFrame.mFrameData, cloneBufferInfo(mediaFrame.mBufferInfo), inputBuffer);

                    mFrameBuffers.offer(mediaFrame.mFrameData);

                    if (VERBOSE)
                        Log.i(TAG, "queueing " + mediaFrame.mBufferInfo.size + " audio bytes with pts " + audioAbsolutePtsUs);
                    mController.onAudioFrameRendered(audioAbsolutePtsUs);
                    if ((mediaFrame.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) {
                        if (VERBOSE) Log.i(TAG, "EOS received in sendAudioToEncoder");
                    }
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, mediaFrame.mBufferInfo.flags);
                } else {
                    if (VERBOSE) Log.d(TAG, "AUDIO FRAME Buffer not available during dequeue");
                    break;
                    // Wait for the next call to sendExtractedAudioToEncoder
                }
            } catch (Throwable t) {
                Log.e(TAG, "_offerAudioEncoder exception");
                t.printStackTrace();
            }
        }
    }

    private void sendBlankAudioToEncoder(boolean endOfStream) {
        if (mMediaCodec == null)
            mMediaCodec = mEncoderCore.getMediaCodec();

        if (mBlankAudioBuffer == null) {
            mBlankAudioBuffer = ByteBuffer.allocate(SAMPLES_PER_FRAME * 2).order(ByteOrder.nativeOrder());
            mBlankAudioBuffer.position(0);
            byte[] blankData = new byte[SAMPLES_PER_FRAME * 2];
            mBlankAudioBuffer.put(blankData);
            mBlankAudioBuffer.position(0);
        }

        // send current frame data to encoder
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(AndroidEncoder.TIMEOUT_USEC);
            if (audioInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                inputBuffer.clear();
                audioInputLength = mBlankAudioBuffer.capacity();
                cloneBuffer(mBlankAudioBuffer, null, inputBuffer);
                audioAbsolutePtsUs = mEncoderCore.getPTSUs();
                // We divide audioInputLength by 2 because audio samples are
                // 16bit.
                //  audioAbsolutePtsUs = getJitterFreePTS(audioAbsolutePtsUs, audioInputLength / 2);

                if(audioInputLength == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Audio read error: invalid operation");
                } else if (audioInputLength == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Audio read error: bad value");
                } else {
                    mEncoderCore.setFirstFrameRenderTime(System.currentTimeMillis());
                }
                if (VERBOSE) Log.i(TAG, "queueing " + audioInputLength + " audio bytes with pts " + audioAbsolutePtsUs);
                mController.onAudioFrameRendered(audioAbsolutePtsUs);
                if (endOfStream) {
                    if (VERBOSE) Log.i(TAG, "EOS received in sendAudioToEncoder");
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, 0);
                }
            } else {
                if (VERBOSE) Log.d(TAG, "Buffer not available during dequeue");
            }
        } catch (Throwable t) {
            Log.e(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    private void sendAudioToEncoder(boolean endOfStream) {
        if (mMediaCodec == null)
            mMediaCodec = mEncoderCore.getMediaCodec();
        // send current frame data to encoder
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(AndroidEncoder.TIMEOUT_USEC);
            if (audioInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                inputBuffer.clear();
                audioInputLength = mAudioRecord.read(inputBuffer, SAMPLES_PER_FRAME * 2);
                audioAbsolutePtsUs = mEncoderCore.getPTSUs();
                // We divide audioInputLength by 2 because audio samples are
                // 16bit.
              //  audioAbsolutePtsUs = getJitterFreePTS(audioAbsolutePtsUs, audioInputLength / 2);

                if(audioInputLength == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Audio read error: invalid operation");
                } else if (audioInputLength == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Audio read error: bad value");
                } else {
                    mEncoderCore.setFirstFrameRenderTime(System.currentTimeMillis());
                }
                if (VERBOSE) Log.i(TAG, "queueing " + audioInputLength + " audio bytes with pts " + audioAbsolutePtsUs);
                mController.onAudioFrameRendered(audioAbsolutePtsUs);
                if (endOfStream) {
                    if (VERBOSE) Log.i(TAG, "EOS received in sendAudioToEncoder");
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, 0);
                }
            } else {
                if (VERBOSE) Log.d(TAG, "Buffer not available during dequeue");
            }
        } catch (Throwable t) {
            Log.e(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    public void sendAudioToEncoder(ByteBuffer data, MediaCodec.BufferInfo bufferInfo, boolean endOfStream) {
        if (!mPauseEnabled) {
            sendFrameToEncoder(data, bufferInfo);
        }
        else {
            mEncoderCore.pause();
        }
    }

    private void subSendAudioToEncoder(ByteBuffer data, MediaCodec.BufferInfo bufferInfo, boolean endOfStream) {

        if (mMediaCodec == null)
            mMediaCodec = mEncoderCore.getMediaCodec();
        // send current frame data to encoder
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (audioInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                inputBuffer.clear();
                data.position(bufferInfo.offset);
                inputBuffer.put(data);
                data.clear();
                // We divide audioInputLength by 2 because audio samples are
                // 16bit.
                //  audioAbsolutePtsUs = getJitterFreePTS(audioAbsolutePtsUs, audioInputLength / 2);

                if(endOfStream) {
                    bufferInfo.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                }
                if(audioInputLength == AudioRecord.ERROR_INVALID_OPERATION)
                    Log.e(TAG, "Audio read error: invalid operation");
                if (audioInputLength == AudioRecord.ERROR_BAD_VALUE)
                    Log.e(TAG, "Audio read error: bad value");

                long timestamp = (long) (mFrameCount * 22675736l) / 1000;
                mFrameCount++;
                bufferInfo.presentationTimeUs = timestamp;

                mController.onAudioFrameRendered(timestamp);

                if (VERBOSE)
                    Log.i(TAG, "audio frame flags: " + bufferInfo.flags + " with pts " + bufferInfo.presentationTimeUs + "size: " + bufferInfo.size);
                mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
            } else {
                if (VERBOSE) Log.d(TAG, "Buffer not available during dequeue");
            }
        } catch (Throwable t) {
            Log.e(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    long startPTS = 0;
    long totalSamplesNum = 0;

    /**
     * Ensures that each audio pts differs by a constant amount from the previous one.
     * @param bufferPts presentation timestamp in us
     * @param bufferSamplesNum the number of samples of the buffer's frame
     * @return
     */
    private long getJitterFreePTS(long bufferPts, long bufferSamplesNum) {
        long correctedPts = 0;
        long bufferDuration = (1000000 * bufferSamplesNum) / (mEncoderCore.mSampleRate);
        bufferPts -= bufferDuration; // accounts for the delay of acquiring the audio buffer
        if (totalSamplesNum == 0) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
        }
        correctedPts = startPTS +  (1000000 * totalSamplesNum) / (mEncoderCore.mSampleRate);
        if(bufferPts - correctedPts >= 2*bufferDuration) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
            correctedPts = startPTS;
        }
        totalSamplesNum += bufferSamplesNum;
        return correctedPts;
    }
}

