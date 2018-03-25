package com.roposo.creation.av;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * Created by Amud on 18/07/16.
 */


/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p/>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p/>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
public class VideoEncoderCore extends AndroidEncoder {
    private static final String TAG = "VideoEncoderCore";

    private static final boolean VERBOSE = false || AVUtils.VERBOSE;

    // TODO: minor these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;
    private static final float BPP = 0.25f;
    private static final int IFRAME_INTERVAL =1; // TODO major only if more control is needed.
    private final int mWidth;
    private final int mHeight;

    // 5 seconds between I-frames

    private Surface mInputSurface;


    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public VideoEncoderCore(int width, int height, int bitRate, Muxer muxer) throws IOException, IllegalStateException, MediaCodec.CodecException {
        mWidth=Math.max(width, 128);
        mHeight=Math.max(height, 128);
        mMuxer = muxer;
        mBufferInfo = new MediaCodec.BufferInfo();

        mStarted = false;
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

/*        format.setInteger("crop-left", width/3);
        format.setInteger("crop-right", 2*width/3 - 1);
        format.setInteger("crop-top", height/3);
        format.setInteger("crop-bottom", 2*height/3 - 1);*/

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate/*calcBitRate()*/);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        mTrackIndex = -1;

        mIsAudio = false;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    private int calcBitRate() {
/*        final int bitrate = (int)(BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));*/
        final int bitrate = 2048 * 1024;
        return bitrate;
    }

    @Override
    protected boolean isSurfaceInputEncoder() {
        return true;
    }
}
