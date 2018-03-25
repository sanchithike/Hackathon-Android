package com.roposo.creation.graphics;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;
import com.roposo.creation.graphics.scenes.ISceneAdjustments;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;
import java.util.List;

import static com.roposo.creation.graphics.GraphicsConsts.MATCH_PARENT;
import static com.roposo.creation.graphics.GraphicsConsts.WRAP_CONTENT;

/**
 * @author bajaj
 *         Abstract base class for all Renderers which actually render
 *         <p>
 *         The class will have an instance of an implementation specific renderer, (OpenGLRenderer in \
 *         our case)
 *         It also maintains the basic properties needed for rendering as well as information needed \
 *         to be persisted across lifecycle events other than app restarts.
 *         </p>
 */
abstract public class BaseRenderer extends Renderer implements OpenGLRenderer.RenderLifecycleListener {
    //Temp removed -- to be moved in another module
    private boolean VERBOSE = false; //|| AVUtils.VERBOSE;

    static final float MIN_ZOOM = 1.0f;
    static final float MAX_ZOOM = 4.0f;

    static final int DUMP_FRAME_FREQ = 10;

    int mFrameCount = -1;

    private int mMediaSource;

    OpenGLRenderer mGLRenderer;

    // Size of display
    int mPreviewWidth;
    int mPreviewHeight;

    // Size of target
    int mTargetWidth;
    int mTargetHeight;

    // Size of input video/camera frame
    int mExternalTextureWidth;
    int mExternalTextureHeight;

    float mZoomLevel;
    List<String> mCurrentFilter;

    private boolean mIsExternalTextureFlipped;

    Drawable mRootDrawable;
    private float mAspectRatio;
    private float mProgress;
    private volatile boolean mRendering;

    long mFirstFrameTimestamp = 0;

    long mPrevFrameTimestamp = -1;
    volatile long mFrameTimestamp = -1;
    long mFrameTimestampMs;

    long mTimeOffsetMs;

    Scene mScene;

    BaseRenderer(ControllerInterface controller) {
        super(controller);
        TAG = "SurfaceRenderer";
        init();
    }

    private void init() {
        reset();

        mRootDrawable = new Drawable2d();
    }

    synchronized void reset() {
        mFrameCount = -1;
        mPrevFrameTimestamp = -1;

        // Any pending draws after reset will be ignored.
        // Only a requestRender after this will be entertained.
        mRendering = false;
    }

    public void requestRender() {
        if (!mRendering) {
            mRendering = true;
        }
    }

    public void setMediaSource(int mediaSource) {
        mMediaSource = mediaSource;
    }

    void handleCreateScene(Bitmap sceneBitmap, boolean isCamera) {
        mRootDrawable.cleanup();
        Drawable2d drawable;
        if (sceneBitmap == null) {
            drawable = Drawable2d.createExternalSourceDrawable(isCamera, Drawable2d.SCALE_TYPE_INSIDE);
        } else {
            drawable = Drawable2d.createBitmapDrawable(sceneBitmap, Drawable2d.SCALE_TYPE_INSIDE);
        }
        drawable.setDefaultGeometryScale(MATCH_PARENT, WRAP_CONTENT);


        // TODO later major Right now the only way to have Z ordering is by placing the objects in 3D.
        // TODO There should be a fake Z ordering also (using the concept of fake layers).
        // add external source drawable first (so it is rendered first)
        mRootDrawable.addChild(mRootDrawable.getNumberOfChildren(), drawable);
    }

    void handleCreateScene(Scene scene) {
        if (scene == null) {
            return;
        }
        mScene = scene;
        mScene.onReady();
        for (Drawable rootDrawable : mScene.mRootDrawables) {
            SparseArray<TreeNode> drawableList = rootDrawable.getChildren();
            int childCount = drawableList.size();
            for (int i = 0; i < childCount; i++) {
                Drawable2d child = (Drawable2d) drawableList.valueAt(i);
                if (child != null) {
                    child.setup();
                }
            }
        }
//        requestRender();
//        mGLRenderer.invalidateScene(scene);
    }

    Bitmap captureFrameBuffer() {
        return mGLRenderer.createBitmapFromGLSurface();
    }

    void handleScroll(float distanceX, float distanceY) {
//        mRootDrawable.translate(distanceX, distanceY);
    }

    void handleSetPreviewParams(int width, int height) {
        Log.d(TAG, "setPreviewParams: " + " width: " + width + " height: " + height);
        if (mGLRenderer != null) {
            mGLRenderer.setPreviewRenderParams(width, height);
        }
//        for (int i = 0; i < mLgdXEffects.size(); i++) {
//            mLgdXEffects.get(i).setWidth(width);
//            mLgdXEffects.get(i).setHeight(height);
//        }
    }

    /**
     * compute drawable properties down the tree forcefully
     *
     * @param drawable the root of the subtree to start from
     */
    void handleComputeDrawableProps(Drawable2d drawable) {
        mGLRenderer.computeDrawableProps(drawable, true);
    }

    void handleUpdateCameraPreviewSize(int incomingWidth, int incomingHeight) {
        if (mGLRenderer != null) {
            mGLRenderer.setCameraParams(incomingWidth, incomingHeight);
        }
    }

    void handleSetRenderParams() {
        handleSetRenderParams(mTargetWidth, mTargetHeight);
    }

    void handleSetRenderParams(int width, int height) {
        Log.d(TAG, "handleSetRenderParams :: " + width + "x" + height);
        mTargetWidth = width;
        mTargetHeight = height;
        mAspectRatio = (float) mTargetWidth / mTargetHeight;

        if (mGLRenderer != null) {
            //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
            mGLRenderer.setRenderParams(mTargetWidth, mTargetHeight);
        }
    }

    void handleSetDrawableRenderParams() {
        handleSetDrawableRenderParams(mRootDrawable);
    }

    void handleSetDrawableRenderParams(Drawable drawable) {
        drawable.setDirtyRecursive(true);
    }

    void handleUpdateDrawable(final Drawable2d drawable, final Bitmap bitmap) {
        drawable.setDirtyRecursive(true);
    }

    void handleCreateScene(Drawable rootDrawable) {
        synchronized (this) {
            mRootDrawable = rootDrawable.clone();
            if (mGLRenderer != null) {
//                mGLRenderer.invalidateScene(mRootDrawable);
            }
        }
    }

    void handleSetExternalTextureScale(float scale) {
        if (mGLRenderer != null) {
//            mGLRenderer.setExternalTextureZoom(scale);
        }
    }

    void handleSetExternalTextureFilter(ArrayList<String> filterMask) {
        if (mGLRenderer != null) {
            //mGLRenderer.setExternalTextureFilter(filterMask);
        }
    }

    void handleResetCamera() {
        if (mGLRenderer != null) {
            mGLRenderer.resetCamera();
        }
    }

    ISceneAdjustments getSceneAdjustmentsInterface() {
        return mScene;
    }

    void handleSetFlip(boolean flip) {
        mIsExternalTextureFlipped = flip;
        if (mGLRenderer != null) {
//            mGLRenderer.setExternalTextureFlip(mIsExternalTextureFlipped);
        }
    }

    void handleDestroyRenderer() {
        mRendererReady = false;
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
    }

    protected void handleAddDrawable(Bitmap bitmap, float posX, float posY) {
        handleAddDrawable(bitmap, posX, posY, 1.0f, 1.0f, Drawable2d.SCALE_TYPE_NONE);
    }

    protected void handleAddDrawable(Bitmap bitmap, float posX, float posY, float width, float height) {
        handleAddDrawable(bitmap, posX, posY, width, height, Drawable2d.SCALE_TYPE_NONE);
    }

    void handleAddDrawable(Bitmap bitmap, float posX, float posY, float width, float height, int scaleType) {
        handleAddDrawable(bitmap, posX, posY, 0, width, height, scaleType);
    }

    void handleAddDrawable(Bitmap bitmap, float posX, float posY, float posZ, float width, float height, int scaleType) {
        Drawable2d drawable = Drawable2d.createBitmapDrawable(bitmap, scaleType);
//        Drawable2d drawable = Drawable2d.createColorDrawable();
        drawable.setTranslateDevicecoords(posX, posY, posZ);
        drawable.setDefaultGeometryScale(width, height);
        if (VERBOSE) {
            Log.d(TAG, "Adding drawable at gl : " + posX + " x " + posY + " x " + posZ + " width: " + width + " height: " + height);
        }
        mRootDrawable.addChild(mRootDrawable.getNumberOfChildren(), drawable);
        if (mGLRenderer != null) {
            drawable.setDirtyRecursive(true);
        }
    }

    protected abstract void addDrawable(Drawable drawable);

    protected void addDrawable(Bitmap bitmap) {
        addDrawable(bitmap, Drawable2d.SCALE_TYPE_INSIDE);
    }

    void addDrawable(Bitmap bitmap, int scaleType) {
        addDrawable(bitmap, 0.0f, 0.0f, 1.0f, 1.0f, scaleType);
    }

    protected void addDrawable(Bitmap bitmap, float posX, float posY, float width, float height) {
        addDrawable(bitmap, posX, posY, width, height, Drawable2d.SCALE_TYPE_INSIDE);
    }

    protected abstract void addDrawable(String path, float posX, float posY, float width, float height, int scaleType);

    protected abstract void addDrawable(Bitmap bitmap, float posX, float posY, float width, float height, int scaleType);

    void setFrameTimestamp(long timeStampNs) {
    }

    void handleSetFrameTimestamp(long timestampNs) {
        mFrameTimestamp = timestampNs;
    }

    public abstract void invalidateScene(Scene scene);

    public abstract void createScene(Drawable mRootDrawable);

    private void setFirstFrameTimestamp(long frameTimestamp) {
        mFirstFrameTimestamp = frameTimestamp;
    }

    abstract void onDrawFrame();

    boolean onPreDraw() {
        if (!mRendering) {
            Log.w(TAG, "Render at invalid stage. Skipping");
            return false;
        }
        ++mFrameCount;
/*        if (mFrameTimestamp == 0 || ((mFrameCount == 0 || mFirstFrameTimestamp == 0) && mFrameTimestamp > 0)) {
            setFirstFrameTimestamp(mFrameTimestamp);
        }
        mFrameTimestamp -= mFirstFrameTimestamp;*/

        mFrameTimestampMs = mFrameTimestamp / 1000 / 1000;
        mGLRenderer.setFrameTimestamp(mFrameTimestampMs);

        if (VERBOSE) {
            Log.d(TAG, "frame timestamp: " + mFrameTimestamp);
        }
        mGLRenderer.onPreDraw();
        return true;
    }

    abstract void draw();

    public void setScaleParams(float width, float height) {
        if (this instanceof VideoEncoder) {
            // Send scale params to particle effects (due to them being cached based on preview params)
        }
    }

    public void onPostDraw() {
        mPrevFrameTimestamp = mFrameTimestamp;
        if (mGLRenderer == null) return;
        mGLRenderer.onPostDraw();
    }

    public void setDisplayOffset(long timeOffset) {
        mTimeOffsetMs = timeOffset;
    }

}
