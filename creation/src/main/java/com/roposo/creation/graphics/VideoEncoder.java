package com.roposo.creation.graphics;

import android.graphics.Bitmap;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.creation.av.AVUtils;
import com.roposo.creation.av.AndroidEncoder;
import com.roposo.creation.av.Muxer;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.av.VideoEncoderCore;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.EglCore;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.WindowSurface;
import com.roposo.creation.graphics.scenes.Scene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by bajaj on 12/07/16.
 */
@RequiresApi(17)
public final class VideoEncoder extends BaseRenderer implements Runnable, AndroidEncoder.FrameListener {
    private static final boolean VERBOSE = false || AVUtils.VERBOSE;
    private static final boolean SHOW_FRAME_LOGS = true;
    private final Handler mUIHandler;

    /**
     * The time at which the first frame is rendered.
     * For frames that we generate ourselves, we consider mFirstFrameRendertimestamp as the first frame's timestamp (passed to muxer) as well.
     */
    private long mFirstFrameRenderTimeStamp;
    private int mFrameRenderFreq = 15;
    private boolean mCanStartRecording;
    private boolean mPrevSkipped = false;

    public void translateCanvas(float distanceX, float distanceY) {
        if (mHandler != null) {
            // OpenGL Y axis is inverse of window system's Y axis
            mHandler.sendMessage(mHandler.obtainMessage(MSG_TRANSLATE_CANVAS, (int) (distanceX * 1000), (int) (-distanceY * 1000), null));
        }
    }

    @Override
    public void onFrameRenderered(long timestamp) {
        mController.onVideoFrameRendered(timestamp);
    }

    public boolean canStartRecording() {
        return mCanStartRecording;
    }

    public void setCanStartRecording(boolean canStartRecording) {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                // OpenGL Y axis is inverse of window system's Y axis
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CAN_START_RECORDING));
            }
        }
    }

    private enum STATE {
        /* Stopped or pre-construction */
        UNINITIALIZED,
        /* Construction-prompted initialization */
        INITIALIZING,
        /* Camera frames are being received */
        INITIALIZED,
        /* Camera frames are being sent to Encoder */
        RECORDING,
        /* Was recording, and is now stopping */
        STOPPING,
        /* Releasing resources. */
        RELEASING,
        /* This instance can no longer be used */
        RELEASED
    }

    private volatile STATE mState = STATE.UNINITIALIZED;
    private volatile STATE mNextState = STATE.UNINITIALIZED;

    private static final int MSG_START_RECORDING = 1;
    private static final int MSG_STOP_RECORDING = 2;
    private static final int MSG_REQUEST_RENDER = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_DESTROY_RENDERER = 5;
    private static final int MSG_RESET = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_QUIT = 8;
    private static final int MSG_SET_TIMESTAMP = 9;
    private static final int MSG_CAN_START_RECORDING = 10;

    private static final int MSG_UPDATE_FLIP_STATE = 11;
    private static final int MSG_SET_FILTER = 12;
    private static final int MSG_SET_ZOOM = 13;
    private static final int MSG_UPDATE_PREVIEW_PARAMS = 14;
    private static final int MSG_UPDATE_CAMERA_PREVIEW_SIZE = 15;
    private static final int MSG_SET_RENDER_PARAMS = 16;
    private static final int MSG_TRANSLATE_CANVAS = 17;

    private static final int MSG_CREATE_SCENE_DEP = 40;
    private static final int MSG_CREATE_SCENE = 41;
    private static final int MSG_ADD_DRAWABLE = 42;
    private static final int MSG_INVALIDATE_SCENE = 43;

    private int mFrameNum;

    private VideoEncoderCore mVideoEncoder;
    private SessionConfig mSessionConfig;


    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private final Object mStopFence = new Object();

    protected boolean mPauseEnabled;

    private final Object mReadyForFrameFence = new Object();    // guards mReadyForFrames/mRecording
    private boolean mReadyForFrames;                            // Is the SurfaceTexture et all created
    private boolean mRecording;                                 // Are frames being recorded
    private boolean mEosRequested;                              // Should an EOS be sent on next frame. Used to stop encoder
    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running


    private boolean mEncodedFirstFrame;

    // Windowing mechanism
    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mSurface;
    private EglCore mEglCore;

    public VideoEncoder(SessionConfig config, ControllerInterface controller) {
        super(controller);
        TAG = "VideoEncoder";

        mState = STATE.INITIALIZING;
        init(config);
        startThread("VideoEncThread");
        mUIHandler = new Handler(Looper.getMainLooper());
        mState = STATE.INITIALIZED;
    }

    private void init(SessionConfig config) {
        mEncodedFirstFrame = false;
        mReadyForFrames = false;
        mRecording = false;
        mEosRequested = false;

        mSessionConfig = config;

        reset();
    }

    public void setVideoConfig(SessionConfig config) {
        mSessionConfig = checkNotNull(config);
    }

    void handleSetCanStartRecording(boolean canStartRecording) {
        mCanStartRecording = canStartRecording;
    }

    /**
     * Prepare for a new recording with the given parameters.
     * This must be called after {@link #stopRecording()} and before {@link #release()}
     *
     * @param config the desired parameters for the next recording. Make sure you're
     *               providing a new {io.kickflip.sdk.av.SessionConfig} to avoid
     *               overwriting a previous recording.
     */
    public void reset(SessionConfig config) {
        synchronized (mReadyFence) {
            if (mState != STATE.UNINITIALIZED)
                throw new IllegalArgumentException("reset called in invalid state " + mState.name());
            mState = STATE.INITIALIZING;
            if (VERBOSE) {
                Log.i(TAG, "reset :: new state: " + mState);
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET, config));
        }
    }

    private void handleReset(SessionConfig config) throws IOException {
        if (mState != STATE.INITIALIZING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleReset");
        init(config);

// Don't prepare the Encoder here... We're anyway going to do it in startRendering
/*        // Make display EGLContext current
        prepareEncoder(mSharedEGLContext,
                mSessionConfig.getVideoWidth(),
                mSessionConfig.getVideoHeight(),
                mSessionConfig.getVideoBitrate(),
                30, //TODO major. Find appropriate fps for output video.
                mSessionConfig.getMuxer());*/
        mReadyForFrames = true;
        mState = STATE.INITIALIZED;
        if (VERBOSE) {
            Log.i(TAG, "handleReset :: new state: " + mState);
        }

        switch (mNextState) {
            case RECORDING:
                startRecording();
                break;
            case STOPPING:
                stopRecording();
                break;
        }
    }

    public SessionConfig getConfig() {
        return mSessionConfig;
    }

    void setSharedEGLContext(EGLContext sharedEGLContext) {
        mSharedEGLContext = sharedEGLContext;
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<VideoEncoder> mWeakEncoder;

        EncoderHandler(VideoEncoder videoRenderer) {
            super();
            mWeakEncoder = new WeakReference<>(videoRenderer);
        }

        EncoderHandler(VideoEncoder videoRenderer, Looper looper) {
            super(looper);
            mWeakEncoder = new WeakReference<>(videoRenderer);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            VideoEncoder videoRenderer = mWeakEncoder.get();
            if (videoRenderer == null) {
                Log.w("EncoderHandler", "MediaHandler.handleMessage: encoder is null");
                return;
            }

            try {
                switch (what) {
                    case MSG_START_RECORDING:
                        videoRenderer.handleStartRecording();
                        break;
                    case MSG_STOP_RECORDING:
                        videoRenderer.handleStopRecording();
                        break;
                    case MSG_REQUEST_RENDER:
                        videoRenderer.onDrawFrame();
                        break;
                    case MSG_SET_TIMESTAMP:
                        videoRenderer.handleSetFrameTimestamp((long) inputMessage.obj);
                        break;
/*                    case MSG_SET_FILTER:
                        videoRenderer.handleSetExternalTextureFilter((String) inputMessage.obj);
                        break;*/
                    case MSG_SET_ZOOM:
                        videoRenderer.handleSetExternalTextureScale(((float) inputMessage.arg1) / 10000);
                        break;
                    case MSG_UPDATE_PREVIEW_PARAMS:
                        videoRenderer.handleSetPreviewParams(inputMessage.arg1, inputMessage.arg2);
                        break;
                    case MSG_SET_RENDER_PARAMS:
                        videoRenderer.handleSetRenderParams();
                        break;
                    case MSG_UPDATE_FLIP_STATE:
                        videoRenderer.handleSetFlip((inputMessage.arg1 > 0));
                        break;
                    case MSG_QUIT:
                        videoRenderer.shutdown();
                        break;
                    case MSG_DESTROY_RENDERER:
                        videoRenderer.handleDestroyRenderer();
                        break;
                    case MSG_RELEASE:
                        videoRenderer.handleRelease();
                        break;
                    case MSG_RESET:
                        videoRenderer.handleReset((SessionConfig) obj);
                        break;
                    case MSG_UPDATE_CAMERA_PREVIEW_SIZE:
                        videoRenderer.handleUpdateCameraPreviewSize(inputMessage.arg1, inputMessage.arg2);
                        break;
                    case MSG_CAN_START_RECORDING:
                        videoRenderer.handleSetCanStartRecording(true);
                        break;
                    case MSG_TRANSLATE_CANVAS:
                        videoRenderer.handleScroll((float) inputMessage.arg1 / 1000, (float) inputMessage.arg2 / 1000);
                        break;
                    case MSG_CREATE_SCENE:
                        videoRenderer.handleCreateScene((Scene) inputMessage.obj);
                        break;
                    case MSG_INVALIDATE_SCENE:
                        videoRenderer.handleCreateScene((Drawable) inputMessage.obj);
                        break;
                    case MSG_ADD_DRAWABLE:

                        break;
                    default:
                        throw new RuntimeException("Unhandled msg what=" + what);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Called from UI thread
     */
    public void startRecording() {
        synchronized (mReadyFence) {
            if (mState != STATE.INITIALIZED) {
                Log.w(TAG, "startRendering called in invalid state. Postponing");
                mNextState = STATE.RECORDING;
                return;
            }
            mNextState = STATE.UNINITIALIZED;
            synchronized (mReadyForFrameFence) {
                mFrameNum = 0;
                mRecording = true;
                mState = STATE.RECORDING;
                if (VERBOSE) {
                    Log.i(TAG, "startRendering :: new state: " + mState);
                }
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING));
        }
    }

    private void startThread(String id) {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, id + "running when start requested");
                return;
            }
            mRunning = true;
            Thread thread = new Thread(this, id);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    /**
     * Stop recording. After this call you must call either {@link #release()} to release resources if you're not going to
     * make any subsequent recordings, or {@link #reset(SessionConfig)} to prepare
     * the encoder for the next recording
     * <p/>
     * Called from UI thread
     */
    public void stopRecording() {
        synchronized (mReadyFence) {
            if (mState == STATE.RECORDING) {
                Log.d(TAG, "StopRecording called");
            } else {
                if (mState == STATE.UNINITIALIZED || mState == STATE.STOPPING) {
                    Log.d(TAG, "StopRecording called. Current State: " + mState + ". Ignoring");
                } else {
                    Log.w(TAG, "StopRecording called in invalid state: " + mState + " . Postponing");
                    mNextState = STATE.STOPPING;
                }
                synchronized (mReadyForFrameFence) {
                    mEosRequested = true;
                }
                return;
            }
            mNextState = STATE.UNINITIALIZED;
            mState = STATE.STOPPING;
            if (VERBOSE) {
                Log.i(TAG, "stopRecording :: new state: " + mState);
            }
            synchronized (mReadyForFrameFence) {
                mEosRequested = true;
            }
        }
        // The video encoder must be fed one frame at the end,
        // since the encoder is stopped and drained and EOS sent
        // when one frame is received after mEOSRequest set to true.
        Log.d(TAG, "Video Rendering last frame in search of EOS");
        mController.onFrameAvailable();

        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));

    }

    /**
     * Release resources, including the Camera.
     * After this call this instance of CameraEncoder is no longer usable.
     * This call blocks until release is complete.
     * <p/>
     * Called from UI thread
     */
    public void release() {
        if (mState == STATE.STOPPING) {
            Log.i(TAG, "Release called while stopping. Trying to sync");
            synchronized (mStopFence) {
                while (mState != STATE.UNINITIALIZED) {
                    Log.i(TAG, "Release called while stopping. Waiting for uninit'd state. Current state: " + mState);
                    try {
                        mStopFence.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.i(TAG, "Stopped. Proceeding to release");
        } else if (mState != STATE.UNINITIALIZED) {
            Log.i(TAG, "release called in invalid state " + mState);
            return;
            //throw new IllegalArgumentException("release called in invalid state");
        }
        mState = STATE.RELEASING;
        if (VERBOSE) {
            Log.i(TAG, "release :: new state: " + mState);
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE));
    }

    public void setPauseEnabled(boolean pauseEnabled) {
        mPauseEnabled = pauseEnabled;
    }

    public boolean isPauseEnabled() {
        return mPauseEnabled;
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void requestRender(/*float [] stMatrix, long timeStamp*/) {
        super.requestRender();
        if (VERBOSE && SHOW_FRAME_LOGS) {
            Log.d(TAG, "requestRender");
        }
        if (!mRendererReady) {
            Log.w(TAG, "Renderer not ready yet");
            return;
        }
        synchronized (mReadyFence) {
            if (!mReady) {
                if (VERBOSE) {
                    Log.d(TAG, "Encoder not ready yet!");
                }
                return;
            }

/*        synchronized (this) {
            mSTMatrix = stMatrix;
            mTimeStamp = timeStamp;
        }*/
            if (mHandler != null) {
                if (VERBOSE && SHOW_FRAME_LOGS) {
                    Log.d(TAG, "Proceeding to render the frame!");
                }
//                mHandler.removeMessages(MSG_REQUEST_RENDER);
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_REQUEST_RENDER, (int)((timeStamp >> 32) & 0xFFFFFFFF), (int)(timeStamp & 0xFFFFFFFF), stMatrix));
                mHandler.sendMessage(mHandler.obtainMessage(MSG_REQUEST_RENDER));
            } else {
                Log.d(TAG, "Handler is null. So skipping the frame!");
            }
        }
    }

    @Override
    protected void addDrawable(Drawable drawable) {
        if (drawable == null) return;
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_DRAWABLE, drawable));
        }
    }

    @Override
    protected void addDrawable(String path, float posX, float posY, float width, float height, int scaleType) {

    }

/*    public void setFilter(String filter) {
        mCurrentFilter = filter;
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, filter));
        }
    }*/

    void setZoom(float scale) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        //Multiple scale by 10000 here (since it's a float), but looks like we can only pass an int
        //And on the receiver's side, divide by 10000
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_ZOOM, (int) (scale * 10000), 0, null));
    }

    void setPreviewRenderParams(int width, int height) {
        if (VERBOSE) Log.d(TAG, "setPreviewRenderParams: " + "width: " + width + " height: " + height);
        mPreviewWidth = width;
        mPreviewHeight = height;
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PREVIEW_PARAMS, width, height));
        }
    }

    public void setFlipState(boolean flip) {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_FLIP_STATE, (flip ? 1 : 0), 0));
        }
    }

    void setRenderParams() {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_RENDER_PARAMS));
        }
    }

    void setCameraParams(int incomingWidth, int incomingHeight) {
        mExternalTextureWidth = incomingWidth;
        mExternalTextureHeight = incomingHeight;
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_CAMERA_PREVIEW_SIZE, incomingWidth, incomingHeight));
        }
    }

    protected void addDrawable(final Bitmap bitmap, final float posX, final float posY, float width, float height, final int scaleType) {
        //TODO minor send a message to the Video renderer's thread.

    }

    @Override
    public void invalidateScene(Scene scene) {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_CREATE_SCENE, scene.clone()));
        }
    }

    @Override
    void setFrameTimestamp(long timestampNs) {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TIMESTAMP, timestampNs));
        }
//        handleSetFrameTimestamp(timestampNs);
    }

    @Override
    public void createScene(Drawable mRootDrawable) {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_INVALIDATE_SCENE, mRootDrawable));
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording() {
        Log.d(TAG, "handleStartRecording " + mSessionConfig);
        prepareEncoder(mSharedEGLContext,
                mSessionConfig.getVideoWidth(),
                mSessionConfig.getVideoHeight(),
                mSessionConfig.getVideoBitrate(),
                30, //TODO major. Find appropriate fps for output video.
                mSessionConfig.getMuxer());

        mFrameCount = -1;
        mFirstFrameRenderTimeStamp = -1;
        // The video encoder must be fed one frame, since the video track is added only when the first video frame is received.
        // If no frame is rendered, the video encoder will never start...
        mFrameTimestamp = mFirstFrameRenderTimeStamp;
        mController.onVideoRecorderReady(mVideoEncoder != null);
//TODO sahil.bajaj Commenting to test video recording        mController.onFrameAvailable();
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     */
    @Override
    void onDrawFrame(/*float [] stMatrix, long timeStamp*/) {
        if (mRecording && mCanStartRecording) {
            boolean canDraw = super.onPreDraw();
            if (!canDraw) {
                return;
            }
            if (VERBOSE && SHOW_FRAME_LOGS) Log.d(TAG, "onDrawFrame " + mFrameCount + " timestamp: " + mFrameTimestamp);

            if (mCache == null) {
                Log.w(TAG, "Cache is NULL");
                return;
            }
            if (mFrameTimestamp == mPrevFrameTimestamp/* && !mPrevSkipped*/) {
                Log.d(TAG, "Skipping timestamp: " + mFrameTimestamp + " prev timestamp: " + mPrevFrameTimestamp);
                mPrevSkipped = true;
                //return;
            }
            mPrevSkipped = false;

            if (!mPauseEnabled) {
                mVideoEncoder.drainEncoder(false);

                if (mFrameCount == 0) {
                    mVideoEncoder.setFirstFrameRenderTime(System.currentTimeMillis());
                }

                synchronized (mCache.mVideoRenderFence) {
//                    drawFrame();
                    draw();
                    super.onPostDraw();

// Uncomment to debug rendering to the Video surface (Saves bitmaps in root folder of sdcard with each image uniquely identified by frame no.)
/*                    android.opengl.GLES20.glFinish();
                    if((mFrameCount % DUMP_FRAME_FREQ) == 0) {
                        Bitmap bitmap = captureFrameBuffer();
                        requestSaveBitmap(bitmap, "frame" + mFrameCount);
                    }*/

                    mSurface.setPresentationTime(mFrameTimestamp);

                    mSurface.swapBuffers();
                }
            } else {
                mVideoEncoder.pause();
            }

            if (mFrameTimestamp >= 0) {
                mController.onVideoFrameRendered(mFrameTimestamp / 1000);
            }

            // Keep rendering unconditionally until the video encoding has begun.
            // This is done for the case of Custom Scene rendering where
            // we don't render the frame until it's dirty.
            if (!mVideoEncoder.mStarted) {
                mController.onFrameAvailable();
            }
        } else {
            Log.w(TAG, "Recording is OFF!");
        }
    }

    @Override
    void draw() {
        if (mScene != null) {
            mScene.onDraw(OpenGLRenderer.Fuzzy.VIDEO, mFrameTimestampMs + mTimeOffsetMs);
        }
    }

    private void requestSaveBitmap(final Bitmap bitmap, final String fileName) {
        final String filePath = "/sdcard/" + fileName + ".jpeg";
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                saveBitmap(bitmap, filePath);
            }
        });
    }

    private void saveBitmap(Bitmap bitmap, String filePath) {
        try {
            FileOutputStream os = new FileOutputStream(new File(filePath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");

        mVideoEncoder.signalEndOfStream();

        mVideoEncoder.drainEncoder(true);
        mRecording = false;
        mEosRequested = false;
        releaseEncoder();
        mState = STATE.UNINITIALIZED;

        synchronized (mStopFence) {
            mStopFence.notify();
        }
        mController.onVideoRecordingFinished();
    }

    /**
     * Handles a request to release video and graphics resources used by encoder.
     */
    private void handleRelease() {
        if (mState != STATE.RELEASING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleRelease");
        shutdown();
        mState = STATE.RELEASED;
        if (VERBOSE) {
            Log.i(TAG, "handleRelease:: new state: " + mState);
        }
    }

    /**
     * Called by release()
     * Safe to release resources
     * <p/>
     * Called on Encoder thread
     */
    private void shutdown() {
        handleDestroyRenderer();
        releaseEglResources();
        Looper looper = Looper.myLooper();
        if (looper != null) {
            looper.quit();
        }
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            if (GraphicsUtils.MULTITHREADED_RENDERER) {
                mHandler = new EncoderHandler(this, Looper.myLooper());
            } else {
                mHandler = new EncoderHandler(this);
            }
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

    boolean isReusing() {
        return (mVideoEncoder != null);
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate, int frameRate, Muxer muxer) {
        try {
            muxer.setController(mController);
            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, muxer);
            // Don't pass frameListener internally if we're handling progress ourselves
            // mVideoEncoder.setFrameListener(this);
        } catch (IOException|IllegalStateException e) {
            CrashlyticsWrapper.logException(e);
            mRecording = false;
        }

        mTargetWidth = width;
        mTargetHeight = height;

        try {
            Log.d(TAG, "Reusing egl context received: " + sharedContext);
            mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE | EglCore.FLAG_PBUFFER);
        } catch (EglCore.EGLBadContextException exception) {
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mSurface.makeCurrent();
        // Create a separater GLRenderer for the video encoder surface
        // And give it hint so that it knows what to do special about it
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }

        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.VIDEO, mRendererInstance, this);
//        mGLRenderer.invalidateScene(mRootDrawable);

        if (!mUseSharedContext) {
            handleCreateSurfaceTexture();
        }
        onRendererPrepared();

        mCache = mGLRenderer.getCacheInstance();

//s        mGLRenderer.addExternalSourceDrawable(mIsCamera);
//        mGLRenderer.setExternalTextureFilter(mCurrentFilter, true);
        handleUpdateCameraPreviewSize(mExternalTextureWidth, mExternalTextureHeight);
        handleSetPreviewParams(mPreviewWidth, mPreviewHeight);
        handleSetRenderParams();
        handleResetCamera();

//        handleCreateScene();
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
    }

    /**
     * Release all recording-specific resources.
     * The Encoder, EGLCore and FullFrameRect are tied to capture resolution,
     * and other parameters.
     */
    private void releaseEglResources() {
        mReadyForFrames = false;
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        mSurfaceTexture = null;
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     * with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final int mFrameRate;
        final boolean mIsCamera;
        final EGLContext mEglContext;

        public EncoderConfig(int width, int height, int bitRate, int frameRate,
                             EGLContext sharedEglContext, boolean isCamera) {
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mFrameRate = frameRate;
            mIsCamera = isCamera;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " @" + mFrameRate +
                    "fps to '" + "' ctxt=" + mEglContext;
        }
    }

    private static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
