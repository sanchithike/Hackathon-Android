package com.roposo.creation.graphics;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.roposo.core.util.FileUtilities;
import com.roposo.creation.camera.CameraPreview;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.EglCore;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OffscreenSurface;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.scenes.Scene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bajaj on 05/09/17.
 */
@RequiresApi(17)
public class CaptureRenderer extends BaseRenderer implements Runnable {
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private static final boolean SHOW_FRAME_LOGS = true;
    private final Handler mUIHandler;

    /**
     * The time at which the first frame is rendered.
     * For frames that we generate ourselves, we consider mFirstFrameRendertimestamp as the first frame's timestamp (passed to muxer) as well.
     */
    private long mFrameTimestamp;
    private OffscreenSurface mSurface;
    private boolean mReadyForFrames;
    private CameraPreview.CameraCaptureCallback mCaptureCallback;

    public void translateCanvas(float distanceX, float distanceY) {
        if (mHandler != null) {
            // OpenGL Y axis is inverse of window system's Y axis
            mHandler.sendMessage(mHandler.obtainMessage(MSG_TRANSLATE_CANVAS, (int) (distanceX * 1000), (int) (-distanceY * 1000), null));
        }
    }

    private static final int MSG_PREPARE = 1;
    private static final int MSG_REQUEST_RENDER = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_DESTROY_RENDERER = 5;
    private static final int MSG_RESET = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_QUIT = 8;
    private static final int MSG_SET_TIMESTAMP = 9;

    private static final int MSG_UPDATE_FLIP_STATE = 11;
    private static final int MSG_SET_FILTER = 12;
    private static final int MSG_SET_ZOOM = 13;
    private static final int MSG_UPDATE_PREVIEW_PARAMS = 14;
    private static final int MSG_UPDATE_CAMERA_PREVIEW_SIZE = 15;
    private static final int MSG_SET_RENDER_PARAMS = 16;
    private static final int MSG_TRANSLATE_CANVAS = 17;

    private static final int MSG_CREATE_SCENE_DIP = 40;
    private static final int MSG_CREATE_SCENE = 41;
    private static final int MSG_ADD_DRAWABLE = 42;
    private static final int MSG_INVALIDATE_SCENE = 43;

    // ----- accessed by multiple threads -----
    private volatile CaptureHandler mHandler;

    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running

    private EglCore mEglCore;

    public CaptureRenderer(ControllerInterface controller) {
        super(controller);
        TAG = "CaptureRenderer";

        startThread("CaptureThread");
        mUIHandler = new Handler(Looper.getMainLooper());
    }

    void setSharedEGLContext(EGLContext sharedEGLContext) {
        mSharedEGLContext = sharedEGLContext;
    }

    public void setCaptureCallback(CameraPreview.CameraCaptureCallback captureCallback) {
        mCaptureCallback = captureCallback;
    }

    void cancelCaptureRequest() {
        if (mHandler != null) {
            mHandler.removeMessages(MSG_REQUEST_RENDER);
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class CaptureHandler extends Handler {
        private WeakReference<CaptureRenderer> mWeakRenderer;

        CaptureHandler(CaptureRenderer videoRenderer) {
            super();
            mWeakRenderer = new WeakReference<>(videoRenderer);
        }

        CaptureHandler(CaptureRenderer videoRenderer, Looper looper) {
            super(looper);
            mWeakRenderer = new WeakReference<>(videoRenderer);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            CaptureRenderer videoRenderer = mWeakRenderer.get();
            if (videoRenderer == null) {
                Log.w("CaptureHandler", "MediaHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_PREPARE:
                    videoRenderer.handlePrepareRenderer((EGLContext) inputMessage.obj, inputMessage.arg1, inputMessage.arg2);
                    break;
                case MSG_REQUEST_RENDER:
                    videoRenderer.onDrawFrame();
                    break;
                case MSG_SET_TIMESTAMP:
                    videoRenderer.handleSetFrameTimestamp((long) inputMessage.obj);
                    break;
                case MSG_SET_FILTER:
                    videoRenderer.handleSetExternalTextureFilter((ArrayList<String>) inputMessage.obj);
                    break;
                case MSG_SET_ZOOM:
                    videoRenderer.handleSetExternalTextureScale(((float) inputMessage.arg1) / 10000);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    videoRenderer.handleUpdateSharedContext((EGLContext) inputMessage.obj);
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
                case MSG_UPDATE_CAMERA_PREVIEW_SIZE:
                    videoRenderer.handleUpdateCameraPreviewSize(inputMessage.arg1, inputMessage.arg2);
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
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void startThread(String id) {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, id + "running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, id).start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    void prepareRenderer(EGLContext sharedEglContext, int width, int height) {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_PREPARE, width, height, sharedEglContext));
            }
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    void updateSharedContext(EGLContext sharedContext) {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
            }
        }
    }

    /**
     * Release resources, including the Camera.
     * After this call this instance of CameraEncoder is no longer usable.
     * This call blocks until release is complete.
     * <p/>
     * Called from UI thread
     */
    public void release() {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE));
            }
        }
    }


    public void destroy() {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
            }
        }
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
    public void requestRender() {
        super.requestRender();
        if (VERBOSE && SHOW_FRAME_LOGS) {
            Log.d(TAG, "requestRender");
        }
        synchronized (mReadyFence) {
            if (!mReady) {
                if (VERBOSE) {
                    Log.d(TAG, "CaptureRenderer not ready yet!");
                }
                return;
            }

            if (mHandler != null) {
                if (VERBOSE && SHOW_FRAME_LOGS) {
                    Log.d(TAG, "Proceeding to render the frame!");
                }
                if (mCaptureCallback != null) {
                    mCaptureCallback.onShutter();
                }
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

    public void setFilter(List<String> filter) {
        mCurrentFilter = filter;
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, filter));
        }
    }

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
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     */
    @Override
    void onDrawFrame(/*float [] stMatrix, long timeStamp*/) {
        if (VERBOSE && SHOW_FRAME_LOGS) Log.d(TAG, "onDrawFrame " + mFrameCount);
        boolean canDraw = super.onPreDraw();
        if (!canDraw) return;

        Bitmap bitmap;
        synchronized (mCache.mCaptureRenderFence) {
            draw();
            super.onPostDraw();

            bitmap = captureFrameBuffer();
            if (bitmap != null && FileUtilities.hasAvailableSize(2 * bitmap.getRowBytes() * bitmap.getHeight())) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
            } else {
                bitmap = null;
            }
            mSurface.swapBuffers();
        }

        if (mCaptureCallback != null) {
            mCaptureCallback.onCameraCapture(bitmap, null, ExifInterface.ORIENTATION_NORMAL, 0);
        }
    }

    @Override
    void draw() {
        if (mGLRenderer == null) return;
        if (mScene != null) {
            mScene.onDraw(OpenGLRenderer.Fuzzy.OFFSCREEN, mFrameTimestampMs);
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
     * Handles a request to release video and graphics resources used by encoder.
     */
    private void handleRelease() {
        Log.i(TAG, "handleRelease");
        releaseEglResources();
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
                mHandler = new CaptureRenderer.CaptureHandler(this, Looper.myLooper());
            } else {
                mHandler = new CaptureRenderer.CaptureHandler(this);
            }
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Capture thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
            mReadyFence.notify();
        }
    }

    private void handlePrepareRenderer(EGLContext sharedContext, int width, int height) {
        if (sharedContext != null && sharedContext.equals(mSharedEGLContext) && (mTargetWidth == width) && (mTargetHeight == height)) {
            onRendererPrepared();
            return;
        }
        handleRelease();

        if (sharedContext != null) {
            mSharedEGLContext = sharedContext;
        } else {
            mSharedEGLContext = EGL14.EGL_NO_CONTEXT;
        }

        mTargetWidth = width;
        mTargetHeight = height;

        try {
            Log.d(TAG, "Reusing egl context received: " + mSharedEGLContext);
            mEglCore = new EglCore(mSharedEGLContext, EglCore.FLAG_PBUFFER);
        } catch (EglCore.EGLBadContextException exception) {
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mSurface = new OffscreenSurface(mEglCore, width, height);
        mSurface.makeCurrent();
        // Create a separater GLRenderer for the video encoder surface
        // And give it hint so that it knows what to do special about it
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }

        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.OFFSCREEN, mRendererInstance, this);
//        mGLRenderer.invalidateScene(mRootDrawable);

        onRendererPrepared();

        mCache = mGLRenderer.getCacheInstance();

        handleUpdateCameraPreviewSize(mExternalTextureWidth, mExternalTextureHeight);
        handleSetPreviewParams(mPreviewWidth, mPreviewHeight);
        handleSetRenderParams();
        handleResetCamera();
    }

    private void releaseEncoder() {
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
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        if (mSurface != null) {
            mSurface.releaseEglSurface();
        }

        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }

        if (mEglCore != null) {
            mEglCore.release();
        }
        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE | EglCore.FLAG_PBUFFER);

        mSurface = new OffscreenSurface(mEglCore, mTargetWidth, mTargetHeight);

        mSurface.makeCurrent();
    }
}
