package com.roposo.creation.graphics;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.EglCore;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OffscreenSurface;

import java.lang.ref.WeakReference;

/**
 * Created by bajaj on 31/07/16.
 */
@SuppressWarnings("unused")
public final class DummyRenderer extends Renderer {
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private static final boolean SHOW_FRAME_LOGS = true;

    private CustomHandlerThread mHandlerThread = null;

    private Object mUpdateTextureFence = new Object(); // For synchronisation between rendering and updateTexImage

    private static final int MSG_PREPARE_RENDERER = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 3;

    private static final int MSG_PAUSE_UPDATE = 4;
    private static final int MSG_RELEASE_RENDERER = 5;
    private static final int MSG_CHANGE_RECORDING_STATE = 6;
    private static final int MSG_NOTIFY_PAUSING = 7;

    private Object mNotifyPausingFence;
    private volatile boolean mNotifyPaused;

    private Handler mHandler = null;

    private float[] mSTMatrix = new float[16];

    private OffscreenSurface mRenderSurface = null;
    private EglCore mEglCore;
    //        private OpenGLRenderer mGLRenderer;

    private int mOutgoingWidth = 0;
    private int mOutgoingHeight = 0;

    private int mFrameCount;


    // Early releases of marshmallow come with a bug where eglSwapBuffers on PBuffer surface causes deadlocks if context is shared
    // So for now, for marshmallow, we'll just not swap the buffers. And let glReadPixels handle everything.
    boolean mSwappable = false;

    private Caches mCache;

    private boolean mUpdate;

    private boolean mNotifyPausing;

    DummyRenderer(ControllerInterface controller) {
        super(controller);
        TAG = "DummyRenderer";

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        mUpdate = true;
        mHandlerThread = new CustomHandlerThread(this, "DummyRendererThread");
        mHandler = mHandlerThread.getHandler();

        //1x1 frame buffer just enough to be able to create a context :D
        mOutgoingWidth = 1;
        mOutgoingHeight = 1;
    }

    /**
     * Handles encoder state change requests.  The handler is created on the Camera Capture thread.
     */
    private static class CustomHandler extends Handler {
        private WeakReference<DummyRenderer> mCameraCaptureWeakRef;
        private boolean VERBOSE = false || GraphicsUtils.VERBOSE;

        public CustomHandler(DummyRenderer surfaceRenderer) {
            super();
            mCameraCaptureWeakRef = new WeakReference<DummyRenderer>(surfaceRenderer);
        }

        private CustomHandler(DummyRenderer dummyRenderer, Looper looper) {
            super(looper);
            mCameraCaptureWeakRef = new WeakReference<DummyRenderer>(dummyRenderer);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            DummyRenderer surfaceRenderer = mCameraCaptureWeakRef.get();
            if (surfaceRenderer == null) {
                Log.w("DummyRenderer", "DummyRenderer handleMessage: surfaceRenderer is null");
                return;
            }

            switch (what) {
                case MSG_PREPARE_RENDERER:
                    surfaceRenderer.handlePrepareRenderer((EGLContext) obj);
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timeStampNs = (((long) inputMessage.arg1) << 32) | ((long) inputMessage.arg2 & 0xffffffffL);
//                    long timeStampNs = (((long)inputMessage.arg1) << INT_BITS) | (long)inputMessage.arg2;
                    surfaceRenderer.onFrameAvailable(timeStampNs);
                    break;
                case MSG_PAUSE_UPDATE:
//                        surfaceRenderer.handlePauseUpdates(inputMessage.arg1);
                    break;
                case MSG_RELEASE_RENDERER:
                    surfaceRenderer.handleRelease();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handlePauseUpdates(int update) {
        mUpdate = update <= 0;
    }

    public void pauseUpdates(boolean update) {
        if (update) {
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MSG_PAUSE_UPDATE, (update ? 1 : 0), 0));
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_PAUSE_UPDATE, (update ? 1 : 0), 0));
        }
    }

    private static class CustomHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CustomHandlerThread(DummyRenderer dummyRenderer, String tag) {
            super(tag);
            start();
            if (GraphicsUtils.MULTITHREADED_RENDERER) {
                mHandler = new CustomHandler(dummyRenderer, getLooper());
            } else {
                mHandler = new CustomHandler(dummyRenderer);
            }
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private void handlePrepareRenderer(EGLContext sharedContext) {
        if (sharedContext != null && sharedContext.equals(mSharedEGLContext)) {
            onRendererPrepared();
            return;
        }

        handleRelease();

        Log.d(TAG, "handlePrepareRenderer");
        if (sharedContext != null) {
            mSharedEGLContext = sharedContext;
        } else {
            mSharedEGLContext = EGL14.EGL_NO_CONTEXT;
        }

        mFrameCount = 0;

        try {
            mEglCore = new EglCore(mSharedEGLContext, EglCore.FLAG_PBUFFER);
            if (!mUseSharedContext) {
                mSharedEGLContext = mEglCore.getContext();
            }
            Log.d(TAG, "egl context: " + mSharedEGLContext);
        } catch (EglCore.EGLBadContextException exception) {
            exception.printStackTrace();
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mRenderSurface = new OffscreenSurface(mEglCore, mOutgoingWidth, mOutgoingHeight);
        mRenderSurface.makeCurrent();

        mUpdate = true;

        // TODO temporarily sahilbaja. Need to find alternative.
        if (mCache != null) Caches.terminateCache(mRendererInstance);

        mCache = Caches.getCacheInstance(mRendererInstance);

        handleCreateSurfaceTexture();

        onRendererPrepared();
    }

    private void handleRelease() {
        mSharedEGLContext = null;
        mUpdate = false;
        if (mRenderSurface != null) {
            mRenderSurface.release();
            mRenderSurface = null;
        }
/*            if (mGLRenderer != null) {
                mGLRenderer.destroy();
                mGLRenderer = null;
            }*/
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private void handleDestroy() {
        handleRelease();
        mHandler = null;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    void prepareRenderer(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PREPARE_RENDERER, mOutgoingWidth, mOutgoingHeight, sharedContext));
    }

    private void onFrameAvailable() {
        onFrameAvailable(-1);
    }

    private void onFrameAvailable(long timeStampNs) {
        if (SHOW_FRAME_LOGS && VERBOSE) Log.v(TAG, "onFrameAvailable ");
        if (!mUpdate) {
            Log.d(TAG, "Returning from onFrameAvailable without updateTexImage");
        } else {
//            Log.d(TAG, "Calling updateTexImage");
            synchronized (mCache.mCaptureRenderFence) {
            synchronized (mCache.mVideoRenderFence) {
                synchronized (mCache.mGLSVRenderFence) {
                    SurfaceTexture surfaceTexture = mCache.getSurfaceTexture();
                    // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
                    // was there before.
                    try {
                        surfaceTexture.updateTexImage();
                    } catch (Exception e) {
                        Log.e(TAG, "updateTexImage failed");
                        e.printStackTrace();
                        return;
                    }
                    if (timeStampNs < 0L) {
                        timeStampNs = surfaceTexture.getTimestamp();
                        if (VERBOSE) {
                            Log.d(TAG, "surfacetexture timestamp: " + timeStampNs);
                        }
                    }
                    mCache.mSurfaceTextureTimestamp = timeStampNs;
                    surfaceTexture.getTransformMatrix(mSTMatrix);
                    mCache.mSurfaceTextureTransform = mSTMatrix;
                }
            }
            }
        }
        //Now is the time to trigger rendering on all threads responsible for rendering to various surfaces.
        mController.onFrameAvailable();
    }

    void requestRender() {
        requestRender(-1);
    }

    private static final int INT_BITS = 32;
    private static final int INT_MASK = 0xFFFFFFFF;

    private void requestRender(long timeStampNs) {
        if (VERBOSE)
            Log.v(TAG, "Renderer: timestamp sent is: " + timeStampNs);
        if (mHandler.hasMessages(MSG_FRAME_AVAILABLE)) {
            Log.w(TAG, "Still has frame message in queue... bad sign");
        }
//        mHandler.removeMessages(MSG_FRAME_AVAILABLE);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int) (timeStampNs >> 32), (int) timeStampNs));
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void clearPendingFrameQueue() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public void release() {
        mUpdate = false;
        mSharedEGLContext = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_RENDERER));
    }
}

