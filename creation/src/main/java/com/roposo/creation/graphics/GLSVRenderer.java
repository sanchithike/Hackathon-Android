package com.roposo.creation.graphics;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.scenes.Scene;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by bajaj on 12/07/16.
 */
public final class GLSVRenderer extends BaseRenderer implements GLSurfaceView.Renderer {
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private static final boolean SHOW_FRAME_LOGS = true;
    private int FRAME_LOG_FREQ = 120;
    Handler mGenericHandler;

    GLSurfaceView mGLSurfaceView;
    private boolean mRendererPrepared;

    GLSVRenderer(ControllerInterface controller) {
        super(controller);
        TAG = "GLSVRenderer";
        mGenericHandler = new Handler(Looper.getMainLooper());
        mController = controller;
    }

    public void setDestinationView(GLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        destroyRenderer();
        onRendererPrepared();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
        handleSetRenderParams(width, height);

        mController.onDisplaySurfaceChanged(width, height);

        if (mRootDrawable.getNumberOfChildren() == 0) {
//            handleCreateScene();
        }
        handleResetCamera();
/*        if (mRendererPrepared) {
            requestRender();
        }*/
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        onDrawFrame();
    }

    @Override
    void onDrawFrame() {
        boolean canDraw = super.onPreDraw();
        if (!canDraw) return;
        if (SHOW_FRAME_LOGS || mFrameCount % FRAME_LOG_FREQ == 0) {
            if (VERBOSE) {
                Log.d(TAG, "onDrawFrame: " + mFrameTimestamp);
            }
        }

        mController.onFrameDisplayed(mFrameTimestamp / 1000);

        draw();
        super.onPostDraw();
    }

    @Override
    void draw() {
        if (mScene != null) {
            synchronized (mCache.mGLSVRenderFence) {
                mScene.onDraw(OpenGLRenderer.Fuzzy.PREVIEW, mFrameTimestampMs);
            }
        }
    }

    @Override
    void handleSetPreviewParams(int width, int height) {
        super.handleSetPreviewParams(width, height);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    void onRendererPrepared() {
        Log.d(TAG, "onRendererPrepared");

        mSharedEGLContext = EGL14.eglGetCurrentContext();

        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.PREVIEW, this, this);
        //mGLRenderer.invalidateScene(mScene);
        mCache = mGLRenderer.getCacheInstance();
//        setFilter(mCurrentFilter);

        if (!mUseSharedContext) {
            handleCreateSurfaceTexture();
        }
        super.onRendererPrepared();
    }

    /**
     * TODO major Either to be moved to Render thread or <br>
     * put in a synchronised block along with all render thread tasks i.e. handle*()
     */

    void destroyRenderer() {
        mRendererPrepared = false;
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
    }

    public void destroy() {
        destroyRenderer();
    }

    // Just for sake of completeness
    // preview width/height doesn't make any sense in case of display renderer because it's anyway equal to the target width/height.
    public void setPreviewRenderParams(int width, int height) {
        if (VERBOSE) Log.d(TAG, "setPreviewRenderParams: " + "width: " + width + " height: " + height);
        mPreviewWidth = width;
        mPreviewHeight = height;
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetPreviewParams(mPreviewWidth, mPreviewHeight);
            }
        });
    }

    public void setRenderParams() {
        setRenderParams(mTargetWidth, mTargetHeight);
    }

    public void setRenderParams(int width, int height) {
        mTargetWidth = width;
        mTargetHeight = height;

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetRenderParams(mTargetWidth, mTargetHeight);
            }
        });
    }

    public void setZoom(float zoom) {
        float scale = zoom * mZoomLevel;

        mZoomLevel = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
    }

    public void setCameraParams(int incomingWidth, int incomingHeight) {
        mExternalTextureWidth = incomingWidth;
        mExternalTextureHeight = incomingHeight;
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleUpdateCameraPreviewSize(mExternalTextureWidth, mExternalTextureHeight);
            }
        });
    }

/*    public int decrementFilter() {
        if(mCurrentFilter == 0) {
            mCurrentFilter = FilterManager.FILTER_COUNT;
        }
        setFilter(--mCurrentFilter);
        return mCurrentFilter;
    }

    public int incrementFilter() {
        if(mCurrentFilter >= FilterManager.FILTER_COUNT-1) {
            mCurrentFilter = -1;
        }
        setFilter(++mCurrentFilter);
        return mCurrentFilter;
    }*/

/*    void invalidateScene() {
        mGLRenderer.setDrawables(new Drawable2d());
        int gridColumnCount = (int) Math.sqrt(mRes.length);
        int gridRowCount = (int) Math.sqrt(mRes.length);
        for(int i = 0; i < gridRowCount; i++) {
            for(int j = 0; j < gridColumnCount; j++) {
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mRes[i*gridColumnCount+j]);
                addDrawable(bitmap, (j * 1.0f + 0.5f) / gridColumnCount, (i * 1.0f + 0.5f) / gridColumnCount, 1.0f / gridColumnCount, 1.0f / gridColumnCount);
                Log.d(TAG, "Adding drawable: " + mRootDrawable);
            }
        }
        setDrawableRenderParams();
        requestRender();
    }*/

    public void resetCamera() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleResetCamera();
            }
        });
    }

    private void setDrawableRenderParams() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetDrawableRenderParams(mRootDrawable);
            }
        });
    }
/*
    void handleCreateScene() {
        int gridColumnCount = (int) Math.sqrt(mRes.length);
        int gridRowCount = (int) Math.sqrt(mRes.length);
        mRootDrawable.cleanup();
        Drawable2d drawable = mGLRenderer.createExternalSourceDrawable(true);
        // add external source drawable first (so it is rendered first)
        mRootDrawable.addChild(mRootDrawable.getNumberOfChildren(), drawable);

        for(int i = 0; i < gridRowCount; i++) {
            for(int j = 0; j < gridColumnCount; j++) {
                float z = 3 * (((float)gridColumnCount * gridRowCount / 2) - (i * gridColumnCount + j)) / (gridColumnCount * gridRowCount);
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mRes[i*gridColumnCount+j]);
                handleAddDrawable(bitmap, (j + 0.5f) * 1.0f / gridColumnCount, (i + 0.5f) * 1.0f / gridColumnCount, z, 1.0f / gridColumnCount, 1.0f / gridColumnCount, Drawable2d.SCALE_TYPE_INSIDE);
            }
        }
    }*/

    protected void addDrawable(final Bitmap bitmap, final float posX, final float posY, final float width, final float height, final int scaleType) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleAddDrawable(bitmap, posX, posY, width, height, scaleType);
            }
        });
    }

    @Override
    public void invalidateScene(final Scene scene) {
        if (scene == null) return;
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleCreateScene(scene.clone());
            }
        });
    }

    @Override
    void setFrameTimestamp(final long timestampNs) {
/*        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {*/
        handleSetFrameTimestamp(timestampNs);
/*            }
        });*/
    }

    @Override
    public void createScene(final Drawable rootDrawable) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleCreateScene(rootDrawable);
            }
        });
    }

    //TODO to be done for VideoEncoder also.
    public void updateDrawable(final Drawable2d drawable, final Bitmap bitmap) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleUpdateDrawable(drawable, bitmap);
            }
        });
    }

    @Override
    public void requestRender() {
        super.requestRender();
        if (!mRendererReady) {
            Log.w(TAG, "Renderer not ready yet");
            return;
        }
        mGLSurfaceView.requestRender();
    }

    @Override
    protected void addDrawable(Drawable drawable) {

    }

    @Override
    protected void addDrawable(String path, float posX, float posY, float width, float height, int scaleType) {

    }

    public void translateCanvas(final float distanceX, final float distanceY) {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
//                ((Drawable)mRootDrawable.getChildAt(5)).translate(distanceX/mPreviewWidth, -distanceY/mPreviewHeight);
                // OpenGL Y axis is inverse of window system's Y axis
                handleScroll(distanceX, -distanceY);
//                requestRender();
            }
        });
    }

    public RectF getTransformedCoord(int x, int y) {
        if (mGLRenderer == null) {
            Log.w(TAG, "mGLRenderer is null");
            return null;
        }
        return mGLRenderer.getTransformedCoord(x, y);
    }
}
