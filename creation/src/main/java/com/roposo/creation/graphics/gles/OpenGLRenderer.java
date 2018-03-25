package com.roposo.creation.graphics.gles;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.creation.av.AVUtils;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.Renderer;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.gles.ProgramCache.ProgramDescription;
import com.roposo.creation.graphics.scenes.Scene3D;

import java.nio.IntBuffer;
import java.util.ArrayList;

import static com.roposo.creation.graphics.gles.Caches.TEXTURES;
import static com.roposo.creation.graphics.gles.Drawable.sDefaultTextureTransform;

/**
 * OpenGL Renderer implementation.
 * Should be accessed from GLRender thread
 */
public class OpenGLRenderer {
    public static OpenGLRenderer sPreviewGLRenderer;
    private String TAG = "OpenGLRenderer";
    private static final boolean VERBOSE = true; //false && GraphicsUtils.VERBOSE; // Discourage logs here
    private static final boolean SHOW_FRAME_LOGS = false;

    private static volatile int mRendererCount;
    private static SparseArray<OpenGLRenderer> mRenderers = new SparseArray<>();

    public long mTimeStampMs = -1;

    private int mIncomingWidth;
    private int mIncomingHeight;

    private int mOutgoingWidth;
    private int mOutgoingHeight;

    // In order for encoder to be able to access and use Preview's width and height
    // (As of now) Going to be used to determine whether to choose Vertical Video mode
    private int mPreviewWidth;
    private int mPreviewHeight;

    int bitmapBuffer[]/*, bitmapSource[]*/;
    Bitmap mCaptureBitmap;


    //Used to determine whether rendering to preview or encoder currently
    private boolean mIsPreview;
    private boolean mIsVideo;
    private boolean mIsOffscreen;
    private Fuzzy mRenderTargetType;

    //    float mRefDistance = 1.0; //(float) (1.0f/mAspectRatio * Math.tan((double)mFOV/2.0));
    private float mAspectRatio = 0.0f;
    private float mCameraAspectRatio = 0.5625f;

// OpenGL Camera Parameters end

    private SurfaceTexture mSurfaceTexture;

    private static final int LAYER_LOWER = 0;
    private static final int LAYER_MIDDLE = 1;
    private static final int LAYER_UPPER = 2;

    ArrayList<RectF> mDrawableLocations;

    // Disable Vertical Mode by default
    // Will see when to enable this
    private static boolean sVerticalModeEnabled = false;

    private boolean mVerticalVideoMode = false;
    private boolean CAPTURE_ENABLED = false;
    private boolean mEnvironmentDirty = true;
    public RenderTarget mRenderTarget;
    private RenderLifecycleListener mRenderLifecycleListener;
    private boolean mActive;

    public RectF getTransformedCoord(int x, int y) {
/*        if (mRootDrawable == null) return null;
        Drawable drawable = (Drawable) mRootDrawable.getChild(0);
        if (drawable == null) return null;

//        float[] zoom = drawable.getSceneZoom();
        float[] scale = drawable.getScale();

        float X = (float) x - mOutgoingWidth / 2;
        float Y = (float) y - mOutgoingHeight / 2;

        Log.d(TAG, "X, Y: " + X + " X " + Y);

        X /= scale[0];
        Y /= scale[1] * ((float) mOutgoingWidth / mOutgoingHeight);

        X += mOutgoingWidth / 2;
        Y += mOutgoingHeight / 2;

        Log.d(TAG, "Focus X: " + X + " Y: " + Y);*/
        return new RectF(0, 0, 100, 100);
    }

    public void clearErrors() {
        Caches.mGlUtil.checkGlError("Attempting to clear Errors");
    }

    public static void destroyCache() {
        mRendererCount = 0;
    }

    public void setFrameTimestamp(long timestampMs) {
        mTimeStampMs = timestampMs;
    }

    public void onPreDraw() {

    }

    public void onPostDraw() {
        mEnvironmentDirty = false;
    }

    public void resetCamera() {
        mRenderTarget.resetCamera();
    }

    enum ModelViewMode {
        /**
         * Used when the model view should simply translate geometry passed to the shader. The resulting
         * matrix will be a simple translation.
         */
        kModelViewMode_Translate,

        /**
         * Used when the model view should translate and scale geometry. The resulting matrix will be a
         * translation + scale. This is frequently used together with VBO 0, the (0,0,1,1) rect.
         */
        kModelViewMode_TranslateAndScale,
    }

    public Caches mCache;

    private Renderer mRenderer;

    private Rect mClipRect;
    private int mViewportWidth;
    private int mViewportHeight;

    private boolean mDirtyClip;

    // Color description
    boolean mColorSet;
    float mColorA, mColorR, mColorG, mColorB;
    // Indicates that the shader should get a color
    boolean mSetShaderColor;

    private float[] mModelViewMatrix;

    public enum Fuzzy {
        PREVIEW,
        VIDEO,
        OFFSCREEN
    }

    public Caches getCacheInstance() {
        return mCache;
    }

    public void terminateCache() {
        Caches.terminateCache(mRenderer);
    }

    public OpenGLRenderer(Fuzzy type, Renderer renderer, RenderLifecycleListener renderLifecycleListener) {

        mIsPreview = type == Fuzzy.PREVIEW;
        mIsVideo = type == Fuzzy.VIDEO;
        mIsOffscreen = type == Fuzzy.OFFSCREEN;
        mRenderTargetType = type;

        mRenderer = renderer;
        mCache = Caches.getCacheInstance(mRenderer);
        mCache.setGlErrorFlag(VERBOSE);

        mRenderLifecycleListener = renderLifecycleListener;
        init();

        if (type == Fuzzy.PREVIEW) {
            TAG = "Preview" + "GLRenderer";
//            mAnimatableDrawables = new Tree<>();
            mDrawableLocations = new ArrayList<>();
            sPreviewGLRenderer = this;
        } else if (type == Fuzzy.VIDEO) {
            TAG = "Video" + "GLRenderer";
            mDrawableLocations = new ArrayList<>();
        } else if (type == Fuzzy.OFFSCREEN) {
            TAG = "OffScreen" + "GLRenderer";
            CAPTURE_ENABLED = true;
        } else {
            TAG = "Unknown" + "GLRenderer";
        }

        OpenGLRenderer.registerRenderer(type, this);
    }

    private static void registerRenderer(Fuzzy type, OpenGLRenderer renderer) {
        mRenderers.put(type.ordinal(), renderer);
    }

    public static OpenGLRenderer getRenderer(Fuzzy type) {
        return mRenderers.get(type.ordinal());
    }

    private void init() {
        mRendererCount++;

        //bitmapSource = null;
        bitmapBuffer = null;
        mCaptureBitmap = null;
        mActive = true;
    }

/*    public void setExternalTextureZoom(float scale) {
        if (scale < 1.0f || scale > 3.0f) {
            return;
        }
        ArrayList<Drawable2d> defaultDrawables = mRootDrawable.get(DRAWABLES_DEFAULT);
        if (defaultDrawables != null) {
            Iterator<Drawable2d> itr = defaultDrawables.iterator();
            while (itr.hasNext()) {
                Drawable2d drawable = itr.next();
                drawable.setSceneZoom(scale, scale, 1.0f);
//                drawable.multiplyScale(drawable.getSceneZoom());
            }
        }
    }

    public void resetLifecycle() {
        resetDrawableProperties();
    }

*/
    private void onViewportInitialized() {
        GLES20.glEnable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_ALPHA);
//        GLES11Ext.glAlphaFuncxOES(GLES20.GL_GEQUAL, 50);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        if (mIsVideo) {
//            GLES20.glClearColor(0.1f, 0.07f, 0.1f, 1.0f);
//        } else {
////            GLES20.glClearColor(0.07f, 0.1f, 0.1f, 1.0f);
//            GLES20.glClearColor(0.01f, 0.6f, 0.7f, 0.7f);
//        }
//        clear(0, 0, mViewportWidth, mViewportHeight, false);

        Caches.mGlUtil.checkGlError("onViewportInitialized");

        setDrawableRenderParams(true);
    }

    public Bitmap createBitmapFromGLSurface() {
        return createBitmapFromGLSurface(mRenderTarget);
    }

    public Bitmap createBitmapFromGLSurface(RenderTarget renderTarget) {
/*        if (renderTarget instanceof FBObject) {
            return createBitmapFromGLSurface(0, 0, ((FBObject) renderTarget).get, ((FBObject) renderTarget).mHeight);
        } else {*/
            return createBitmapFromGLSurface(0, 0, renderTarget.getExtent().width(), renderTarget.getExtent().height());
/*        }*/
    }

/*    public int[] readPixelsFromGLSurface(int x, int y, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = readPixelsFromGLSurface(x, y, w, h);
        bitmap.setPixels();
    }*/

    public int[] readPixelsFromGLSurface(RenderTarget renderTarget, int x, int y, int w, int h) {
        bindTargetBuffer(renderTarget);
        int[] pixels = readPixelsFromGLSurface(x, y, w, h);
        if (pixels.length >1) {
            for (int i=0; i<pixels.length;i++) {
                Log.d("pixel", "pixelx"+i+":" + ((pixels[i] & 0xff) / 256.0) * 2.0);
                Log.d("pixel", "pixely"+i+":" + (((pixels[i] >>8) & 0xff) / 256.0));
            }
        }
        unbindTargetBuffer();
        return pixels;
    }

    public int[] readPixelsFromGLSurface(int x, int y, int w, int h) {
        int[] pixelBuffer = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(pixelBuffer);
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
        } catch (GLException e) {
            return null;
        }
        return pixelBuffer;
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h) {
        if (bitmapBuffer == null) {
            // bitmapBuffer is NULL in a miraculous case.. Just return null and let caller handle.
            Log.e(TAG, "Bitmap buffer is null");
            return null;
        }
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            swizzleAndInvertBitmap(w, h);

        } catch (GLException e) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(bitmapBuffer, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private void swizzleAndInvertBitmap(int w, int h) {
        int offset1, offset2;
        int texturePixel1, texturePixel2;

        for (int i = 0; i < h/2; i++) {
            offset1 = i * w;
            offset2 = (h - i - 1) * w;
            for (int j = 0; j < w; j++) {
                texturePixel1 = bitmapBuffer[offset1 + j];
                texturePixel2 = bitmapBuffer[offset2 + j];

                bitmapBuffer[offset1 + j] = convertRGBAtoBGRA(texturePixel2);
                bitmapBuffer[offset2 + j] = convertRGBAtoBGRA(texturePixel1);
            }
        }

        if (h % 2 == 1) {
            for (int j = 0; j < w; j++) {
                bitmapBuffer[h / 2 + j] = convertRGBAtoBGRA(bitmapBuffer[h / 2 + j]);
            }
        }
    }

    private int convertRGBAtoBGRA(int texturePixel) {
        int blue = (texturePixel >> 16) & 0xff;
        int red = (texturePixel << 16) & 0x00ff0000;
        return (texturePixel & 0xff00ff00) | red | blue;
    }

    /**
     * The global variant.
     * This applies new render properties to all children down the tree starting from the root.
     */
    private void setDrawableRenderParams() {
        setDrawableRenderParams(false);
    }

    public void setDrawableRenderParams(boolean force) {
        allocTempResources();

        if (VERBOSE)
            Log.v(TAG, "DRAWABLE Scaling Preview width : " + mPreviewWidth + " height : " +
                    mPreviewHeight);
        if (VERBOSE)
            Log.v(TAG, "DRAWABLE Scaling Output width : " + mOutgoingWidth + " height : " +
                    mOutgoingHeight);

/*        if(incomingHeight != 0) {
            float cameraAspect = ((float) incomingWidth / incomingHeight);
            mCameraAspectRatio = cameraAspect;
        }*/

        mEnvironmentDirty = true;
    }

    /**
     * To traverse and computing from somewhere within the tree.
     * @param drawable
     * @param forceCompute Pass true for all children for any dirty drawable encountered.
     */
    public void computeDrawableProps(Drawable drawable, boolean forceCompute) {
        // drawable's dirty flag is going to be reset after call to drawable's computeDrawableProps
        forceCompute |= drawable.mIsInvalidated;

        drawable.computeDrawableProps(mAspectRatio);

        drawable.setDirty(false);
        drawable.setInvalid(false);
        drawable.setupDraw();

        SparseArray<TreeNode> drawableList = drawable.getChildren();
        int childCount = drawableList.size();
        for (int i = 0; i < childCount; i++) {
            Drawable2d child = (Drawable2d) drawableList.valueAt(i);
            if ((child != null) && (forceCompute || child.mIsInvalidated)) {
                computeDrawableProps(child, forceCompute);
            }
        }
    }

    public void setDrawableRenderParams(Drawable2d drawable) {
        setDrawableRenderParams(drawable, true);
    }

    /**
     *
     * @param drawable The drawable whose properties need to (re)computed
     * @param force    If true, force on self and the process will be done recursively for all children
     *                 Generally true when the surface changes (and thus all the drawable settings need to be reconfigured)
     *                 And false in case when the drawable has just been created.b
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setDrawableRenderParams(Drawable2d drawable, boolean force) {
        if (drawable == null) return;
        if (mAspectRatio == 0) return;

        if (drawable.getParent() == null) {
            if (drawable.getNumberOfChildren() == 0) return;
            mVerticalVideoMode = sVerticalModeEnabled && ((mIsVideo && (mPreviewHeight > mPreviewWidth)) || (!mIsVideo && (mOutgoingHeight > mOutgoingWidth)));
        }

        if (force) {
            drawable.setDirtyRecursive(true);
        }
    }

    private void resetCameraParams() {
        mIncomingWidth = -1;
        mIncomingHeight = -1;
    }

    private void resetRenderParams() {
        mOutgoingWidth = -1;
        mOutgoingHeight = -1;
    }

    public void setCameraParams(int width, int height) {
        Log.d(TAG, "Setting camera params to: " + "width: " + width + " height: " + height);
        mIncomingWidth = width;
        mIncomingHeight = height;

        mEnvironmentDirty = true;
    }

    public void setPreviewRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    public void setRenderParams(int width, int height) {
        mOutgoingWidth = width;
        mOutgoingHeight = height;

        mAspectRatio = (float) mOutgoingWidth / (float) mOutgoingHeight;

        Log.d(TAG, "setRenderparams width : " + width + "height: " + height + "aspect ratio: " + mAspectRatio);

        setViewport(mOutgoingWidth, mOutgoingHeight);
        mRenderTarget = new RenderTarget() {
            @Override
            public void setScaleParams(float width, float height) {
                if (AVUtils.VERBOSE) Log.d(TAG, "setRenderScaleparams width : " + width + "height: " + height + "aspect ratio: " + mAspectRatio);
                mRenderLifecycleListener.setScaleParams(width, height);
            }

            @Override
            public int getTargetID() {
                return TARGET_DEFAULT;
            }
        };
        mRenderTarget.setExtent(new Rect(0, 0, mOutgoingWidth, mOutgoingHeight));
    }

    public boolean isRenderParamsInitialized() {
        return !(mOutgoingWidth <= 0 || mOutgoingHeight <= 0);
    }

    private void setViewport(int width, int height) {
        Log.d(TAG, "set Viewport " + " width: " + width + " height: " + height);
        mViewportWidth = width;
        mViewportHeight = height;
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
        Caches.mGlUtil.checkGlError("glViewport");
        onViewportInitialized();
    }

    public boolean drawFrame(Drawable drawable) {
        return drawFrame(drawable, mRenderTarget);
    }

    public boolean drawFrame(Drawable drawable, boolean clearTarget) {
        return drawFrame(drawable, mRenderTarget, clearTarget);
    }

    public boolean drawFrame(Drawable drawable, RenderTarget renderTarget) {
        return drawFrame(drawable, renderTarget, true);
    }

    public synchronized boolean drawFrame(Drawable drawable, RenderTarget renderTarget, boolean clearTarget) {
        if (drawable.getParent() != null) {
            throw new RuntimeException("Can't call drawFrame for a non-root object");
        }

        if (renderTarget == null) {
            return false;
        }

        mCache = Caches.getCacheInstance(mRenderer);
        if (mCache == null) {
            Log.d(TAG, "Renderer already destroyed");
            return false;
        }

        if (mEnvironmentDirty) {
            Drawable.sIncomingWidth = mIncomingWidth;
            Drawable.sIncomingHeight = mIncomingHeight;
            drawable.setDirtyRecursive(true);
        }
        bindTargetBuffer(renderTarget);
        setViewport(renderTarget);
        drawable.computeAndMeasure(renderTarget, (int) mTimeStampMs);

        if (clearTarget) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        }

        mCache.disableScissor();
        draw(drawable, renderTarget);

        /*Bitmap bitmap = OpenGLRenderer.sPreviewGLRenderer.createBitmapFromGLSurface();
        Log.d(TAG, "captured: " + bitmap);*/

        GLES20.glFinish();
        unbindTargetBuffer();

        return true;
    }

    private void setViewport(RenderTarget renderTarget) {
        if (renderTarget.getTargetID() == RenderTarget.TARGET_FBO) {
            Rect extent = renderTarget.getExtent();
            GLES20.glViewport(extent.left, extent.top, extent.width(), extent.height());
            mCache.enableScissor();
            GLES20.glScissor(extent.left, extent.top, extent.width(), extent.height());
        } else {
            GLES20.glViewport(0, 0, mOutgoingWidth, mOutgoingHeight);
        }
    }

    public FBObject getFrameBufferObject(int fbHandle) {
        return mCache.getFrameBufferObject(fbHandle);
    }

    public synchronized FBObject createFrameBufferObject(int width, int height, final boolean needDepthBuffer) {
        Texture texture = mCache.getFBOTexture(width, height);

        FBObject fbObject = FBObject.createFrameBufferObject(texture.id, width, height, needDepthBuffer);
        mCache.putFrameBufferObject(fbObject);

        return fbObject;
    }

    public void draw(Drawable drawable, RenderTarget renderTarget) {
        if (VERBOSE) {
            Log.v(TAG, "Drawing drawable: " + drawable);
        }
        if (drawable == null) return;

        if (drawable instanceof Drawable2d) {
            drawNode((Drawable2d) drawable, renderTarget);
        }
        // Recursively traverse the Tree and draw the drawable Nodes.
        SparseArray<TreeNode> drawableList = drawable.getChildren();
        int childCount = drawableList.size();
        for (int i = 0; i < childCount; i++) {
            draw((Drawable) drawableList.valueAt(i), renderTarget);
        }
    }

    private void drawNode(@NonNull Drawable2d drawable, RenderTarget renderTarget) {
        if (drawable.isFucked()) {
            calculateClipRegion(drawable, renderTarget);
            computeMVPMatrix(drawable, renderTarget);
            drawable.setFucked(false);
        }

        if (!drawable.mActive || !drawable.mVisible || drawable.getShadingSourceType() == Drawable2d.SHADING_SOURCE_NONE) {
            return;
        }

        drawable.onDraw();

        ProgramDescription description = drawable.getDescription();
        description.renderTargetType = mRenderTargetType;
        BaseFilter filter = mCache.getFilter(description);
        if (filter == null) {
            filter = mCache.createFilter(description);
            drawable.onFilterReady(renderTarget, filter);
        }
        drawable.onFilterPredraw(mRenderTargetType, renderTarget, filter);
        filter.draw(mCache);
        Program program = filter.getProgram();

        if (drawable.getScaleType() == Drawable2d.SCALE_TYPE_CROP) {
            mCache.enableScissor();

            Rect clipRect = drawable.mClipRect;
            mCache.setScissor(clipRect.left, clipRect.top, clipRect.width(), clipRect.height());
        }

        program.uniformMatrix4fv(ProgramCache.SHADER_VAR_MVPMATRIX, drawable.mMVPMatrix);
//        program.uniform1f(ProgramCache.SHADER_VAR_BLENDFACTOR, mBlendFactor);
        program.uniform1f(ProgramCache.SHADER_VAR_ALPHAFACTOR, drawable.getAlpha());

        if (!drawable.isVertexBufferBased) {
            mCache.bindBufferIndex(GLES20.GL_ARRAY_BUFFER, Caches.VERTICES);
            program.setAttribPointer(ProgramCache.SHADER_VAR_POSITION, 3, 4, 0);

            mCache.bindBufferIndex(GLES20.GL_ELEMENT_ARRAY_BUFFER, Caches.INDICES);
        } else {
            mCache.bindBuffer(GLES20.GL_ARRAY_BUFFER, drawable.bufferObjectArray[Caches.VERTICES]);
            program.setAttribPointer(ProgramCache.SHADER_VAR_POSITION, 3, drawable.getVertexCount(), 0);
            mCache.bindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, drawable.bufferObjectArray[Caches.INDICES]);
        }

        ArrayList<ImageSource> imageSources = drawable.getImageSources();
        for (int i = 0; i < imageSources.size(); i++) {
            ImageSource imageSource = imageSources.get(i);

            if (!drawable.isVertexBufferBased) {
                mCache.bindBuffer(GLES20.GL_ARRAY_BUFFER, mCache.getTextureCoordBuffer(imageSource.getROI()).textureDataHandle[0]);
                program.setAttribPointer(ProgramCache.getIndexedVariable(ProgramCache.SHADER_VAR_TEXCOORDS, i), 2, 4, getTexCoordsIndex(imageSource.getShadingSourceType() == Drawable2d.SHADING_SOURCE_EXTERNAL_TEXTURE, imageSource.getShadingSourceType() == Drawable2d.SHADING_SOURCE_FBO, drawable.getIsFlipped()));
            } else {
                mCache.bindBuffer(GLES20.GL_ARRAY_BUFFER, drawable.bufferObjectArray[TEXTURES]);
                program.setAttribPointer(ProgramCache.getIndexedVariable(ProgramCache.SHADER_VAR_TEXCOORDS, i), 2, drawable.getVertexCount(), 0);
            }

            program.uniform2fv(ProgramCache.getIndexedVariable(ProgramCache.SHADER_VAR_TEXELSIZE, i), new float[]{1.0f /  imageSource.mIncomingWidth, 1.0f /  imageSource.mIncomingHeight});
            //Caches.checkGlError("11 drawTextureMesh :: Before glDrawElements");

            Texture texture = mCache.getTexture(imageSource);
            if (texture == null) {
                Log.w(TAG, "Texture is null!");
                return;
            }
            mCache.activeTexture(i);
            mCache.bindTexture(texture, imageSource.getTextureParameterSymbol());
            program.setupTexture(i);
        }
        //Caches.checkGlError("22 drawTextureMesh :: Before glDrawElements");

        program.uniformMatrix4fv(ProgramCache.SHADER_VAR_TEXTURETRANSFORM, getTextureTransform(drawable));

        Caches.checkGlError("drawTextureMesh :: Before glDrawElements");
        if (VERBOSE) Log.v(TAG, "going to call glDrawElements");

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (!drawable.isVertexBufferBased) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, Caches.indexData.length, GLES20.GL_UNSIGNED_BYTE, 0);
        } else {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawable.mIndicesBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
        }

        Caches.checkGlError("drawTextureMesh :: glDrawElements");

        mCache.disableScissor();

        mCache.unbindBuffer(GLES20.GL_ARRAY_BUFFER);
        mCache.unbindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER);
        mCache.unbindTexture();
    }

    private void bindTargetBuffer(RenderTarget renderTarget) {
        if (renderTarget.getTargetID() == RenderTarget.TARGET_FBO) {
            FBObject fbo = (FBObject) renderTarget;

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo.getFrameBufferId());
            int fbStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (fbStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "fb status: " + Integer.toHexString(fbStatus));
            }
        }
    }

    private void unbindTargetBuffer() {
        mCache.disableScissor();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    static int getTexCoordsIndex(boolean isExternalTexture, boolean isFBOBased, boolean isFlipped) {
        int position;
        if (isExternalTexture || isFBOBased) {
            if (isFlipped) {
                position = 1;
            } else {
                position = 0;
            }
        } else {
            if (!isExternalTexture /* || mIsOffscreen*/) {
                if (isFlipped) {
                    position = 0;
                } else {
                    position = 2;
                }
            } else {
                position = 0;
            }
        }
        return position;
    }

    void computeModelMatrix(Drawable drawable, RenderTarget renderTarget) {
        float[] modelMatrix = drawable.mModelMatrix;

        if (modelMatrix == null) {
            if (VERBOSE) Log.w(TAG, "Model matrix is null");
            return;
        }

        Matrix.setIdentityM(modelMatrix, 0);

        Matrix.scaleM(modelMatrix, 0, renderTarget.getPreScale(), renderTarget.getPreScale(), 1.0f);

        double[] translate = drawable.getTranslate().clone();
        translate[0] *= 2.0f;
        translate[1] *= 2.0f;
        translate[2] *= 2.0f;

        Matrix.translateM(modelMatrix, 0, (float) translate[0], (float) translate[1], (float) translate[2]);

        double[] mRotate = drawable.getRotate();
        if (mRotate != null) {
            Matrix.rotateM(modelMatrix, 0, (float) mRotate[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(modelMatrix, 0, (float) mRotate[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(modelMatrix, 0, (float) mRotate[2], 0.0f, 0.0f, 1.0f);
        }

        double[] mScale = drawable.getScale();
        if (mScale != null) {
            Matrix.scaleM(modelMatrix, 0, (float) mScale[0], (float) mScale[1], (float) mScale[2]);
        }
        if (GraphicsUtils.VERBOSE) Log.d(TAG, "Drawable computed: " + drawable);
    }

    private void computeMVPMatrix(Drawable drawable, RenderTarget renderTarget) {
        float[] mvpMatrix = new float[16];

        float[] mvMatrix = new float[16];

        computeModelMatrix(drawable, renderTarget);

        float [] modelMatrix = drawable.mModelMatrix;

        if (modelMatrix == null) {
            if (VERBOSE) Log.w(TAG, "Model matrix is null");
            return;
        }

        Matrix.setIdentityM(mvMatrix, 0);
        Drawable2d drawable2d = (Drawable2d) drawable;
        if(!drawable2d.isVertexBufferBased ){
            Matrix.multiplyMM(mvMatrix, 0, renderTarget.getView(), 0, modelMatrix, 0);
        } else{
            Matrix.setLookAtM(mvMatrix,0,0,0,0,0,0,1,0,1,0);
            float ratio = 1;
            if(mAspectRatio > 1){
                ratio = 1/mAspectRatio;
            }
            Matrix.translateM(mvMatrix,0,(float) Scene3D.translation[0] * ratio,(float)Scene3D.translation[1],(float)Scene3D.translation[2]);
            Matrix.rotateM(mvMatrix,0,Scene3D.rotationAngles[1],0,1,0);
            Matrix.rotateM(mvMatrix,0,Scene3D.rotationAngles[0],1,0,0);
            Matrix.rotateM(mvMatrix,0,Scene3D.rotationAngles[2],0,0,1);
        }


        Matrix.setIdentityM(mvpMatrix, 0);
        float[] projection = new float[16];
        float frustum_near = 0.001f;
        float frustum_far = 30; //hard to estimate face too far away
        float frustum_x = 1*frustum_near/ Scene3D.cameraFocus;
        float frustum_y = (1/renderTarget.mAspectRatio)*frustum_near/Scene3D.cameraFocus;
        Matrix.frustumM(projection,0,-frustum_x,frustum_x,-frustum_y,frustum_y,frustum_near,frustum_far);
        Matrix.multiplyMM(mvpMatrix, 0, drawable2d.isVertexBufferBased?projection:renderTarget.getProjection(), 0, mvMatrix, 0);


        drawable.setMVPMatrix(mvpMatrix);
    }

    private void calculateClipRegion(Drawable2d drawable, RenderTarget renderTarget) {
        int outWidth = renderTarget.getExtent().width();
        int outHeight = renderTarget.getExtent().height();

        float aspectRatio = (float) outWidth / outHeight;

        double[] translate = drawable.getTranslate().clone();
        translate[0] *= 2.0f;
        translate[1] *= 2.0f * aspectRatio;
        translate[2] *= 2.0f;

        double[] mScale = drawable.getGeometryScale();

        double drawablePosX = (translate[0] + 1.0) / 2.0;
        double drawablePosY = (1.0f + translate[1]) / 2.0;

        double drawableWidth = mScale[0];// * drawable.getSceneZoom()[0];
        double drawableHeight = mScale[1];// * drawable.getSceneZoom()[1];

        drawableHeight *= aspectRatio;


        Rect clipRect = new Rect((int) ((drawablePosX - drawableWidth / 2) * outWidth)
        , (int) ((drawablePosY - drawableHeight / 2) * outHeight)
        , (int) ((drawablePosX + drawableWidth / 2) * outWidth)
        , (int) ((drawablePosY + drawableHeight / 2) * outHeight));

        if (VERBOSE) Log.d(TAG, "cliprect: " + clipRect + "\t for: \t" + translate[0] + "x" + translate[1] + " translate: " + drawablePosX + "x" + drawablePosY);
        drawable.setClipRect(clipRect);
    }

    private float[] getTextureTransform(Drawable2d drawable) {
        float[] matrix = sDefaultTextureTransform;

        ProgramDescription description = drawable.getDescription();
        if (description != null && description.hasExternalTexture()) {
            // Uncomment this if the App crashes just below.
            if (mCache.mSurfaceTextureTransform == null) {
                float[] dummy = new float[16];
                Matrix.setIdentityM(dummy, 0);
                mCache.mSurfaceTextureTransform = dummy;
            }
            matrix = mCache.mSurfaceTextureTransform;
        }
        return matrix;
    }

    public static SurfaceTexture createSurfaceTexture(Renderer renderer) {
        Caches cache = Caches.getCacheInstance(renderer);
        Texture texture = cache.mTextureCache.createExternalTexture(); //Sending false creates new texture
        SurfaceTexture surfaceTexture = new SurfaceTexture(texture.id);
        cache.setSurfaceTexture(surfaceTexture);
        return surfaceTexture;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public static SurfaceTexture getSurfaceTexture(Renderer renderer) {
        Caches cache = Caches.getCacheInstance(renderer);
        return cache.getSurfaceTexture();
    }

    private void allocTempResources() {
        if (!CAPTURE_ENABLED) return;
        if (mOutgoingHeight <= 0 || mOutgoingWidth <= 0) {
            return;
        }
        if ((bitmapBuffer == null || bitmapBuffer.length != mOutgoingWidth * mOutgoingHeight)) {
            if (mCaptureBitmap != null) {
                mCaptureBitmap.recycle();
                mCaptureBitmap = null;
            }
            bitmapBuffer = new int[mOutgoingWidth * mOutgoingHeight];
            //bitmapSource = new int[mOutgoingWidth * mOutgoingHeight];
            mCaptureBitmap = Bitmap.createBitmap(mOutgoingWidth, mOutgoingHeight, Bitmap.Config.ARGB_8888);
            Log.d(TAG, "mCaptureBitmap created");
        }
    }

    private void trimMemory() {
        if (mCaptureBitmap != null) {
            mCaptureBitmap.recycle();
            mCaptureBitmap = null;
        }
        //bitmapSource = null;
        bitmapBuffer = null;
        Log.d(TAG, "mCaptureBitmap destroyed");
    }

    public void destroy() {
        destroy(false);
    }

    public synchronized void destroy(boolean destroy) {
        if (!mActive) return; // to handle multiple destroy calls (may be from different threads)
        mActive = false;
        mRendererCount--;
        if (mRendererCount < 0) mRendererCount = 0;

        if (mRendererCount == 0 || destroy) {
//            mRendererCount = 0;
            //Terminate cache when no one is using it anymore...
            // which will happen when mRendererCount becomes 0
            terminateCache();
            resetCameraParams();
            destroyStatics();
        }
        trimMemory();

        mClipRect = null;

        mModelViewMatrix = null;

        resetRenderParams();
    }

    private static void destroyStatics() {
    }

    private String printMat(float[] mMVPMatrix) {
        String mat = "";
        for (int i = 0; i < 4; i++) {
            String row = "";
            for (int j = 0; j < 4; j++) {
                row += mMVPMatrix[i * 4 + j];
                row += "\t";
            }
            mat += row + "\n";
        }
        return mat;
    }

    public int getOutgoingWidth() {
        return mOutgoingWidth;
    }

    public int getOutgoingHeight() {
        return mOutgoingHeight;
    }

    public interface RenderLifecycleListener {
        void setScaleParams(float width, float height);
    }
} // class OpenGLRenderer
