/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roposo.creation.graphics.gles;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.BitmapFactoryUtils;
import com.roposo.creation.av.AVUtils;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.gifdecoder.GifManager;
import com.roposo.creation.graphics.gles.ProgramCache.ProgramDescription;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.roposo.creation.graphics.gles.Caches.BYTES_PER_FLOAT;
import static com.roposo.creation.graphics.gles.Caches.ELEMENTS_TYPE_COUNT;
import static com.roposo.creation.graphics.gles.Caches.INDICES;
import static com.roposo.creation.graphics.gles.Caches.TEXTURES;
import static com.roposo.creation.graphics.gles.Caches.VERTICES;
import static com.roposo.creation.graphics.gles.Caches.mWallDist;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d extends Drawable {
    private static final String TAG = Drawable2d.class.getSimpleName();
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    public static final int BLEND_MODE_NONE = 0;
    public static final int BLEND_MODE_ADD = 1;
    public static final int BLEND_MODE_DISSOLVE = 2;

    public static final int SCALE_TYPE_NONE = 0;    // Drawable dimensions determine how it's displayed.
    public static final int SCALE_TYPE_INSIDE = 1;  // Best Fit, Preserves Aspect ratio;
    public static final int SCALE_TYPE_CROP = 2;    // FULL Screen, Preserves Aspect ratio, CROPS;
    public static final int SCALE_TYPE_FIT = 3;     // FULL Screen, Does NOT preserve Aspect ratio;
    public static final int SCALE_TYPE_SQUARE = 4;  // LARGEST SQUARE, Preserves Aspect ratio, CROPS;

    public static final int SCALE_TYPE_FIT_WIDTH = 5;   // FIT HEIGHT, Preserves Aspect ratio;
    public static final int SCALE_TYPE_FIT_HEIGHT = 6;  // FIT WIDTH, Preserves Aspect ratio;

    //JFYI, If it is bitmap based, then texture coordinates would be different from when it is texture/Surface Texture based.

    public static final int SHADING_SOURCE_NONE = 0x00;
    public static final int SHADING_SOURCE_COLOR = 0x01;
    public static final int SHADING_SOURCE_BITMAP = 0x02;
    public static final int SHADING_SOURCE_EXTERNAL_TEXTURE = 0x04;
    public static final int SHADING_SOURCE_RAW_TEXTURE = 0x08;
    public static final int SHADING_SOURCE_FBO = 0x10;
    public static final int SHADING_SOURCE_GIF = 0x20;

    private int mBlendMode = 0;
    private boolean mIsFlipped = false;
    private List<String> mColorFilterMode = new ArrayList<>();
    private int mConvolutionType = 0;
    private int mScaleType;
    private boolean mVerticalVideoMode = false;

    private BaseFilter mFilter;
    private double sScale;

    // To be used only when drawable is using external texture
    private int mIncomingWidth;
    private int mIncomingHeight;

    private volatile double animatedValue;
    private ValueAnimator valueAnimator;

    private ArrayList<ImageSource> mImageSources = new ArrayList<>();
    private FloatBuffer mVertexBuffer, mTextureBuffer;
    ShortBuffer mIndicesBuffer;
    public boolean isVertexBufferBased;

    public int[] bufferObjectArray = new int[ELEMENTS_TYPE_COUNT];

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    void measure() {
        if (getIncomingWidth() == 0 || getIncomingHeight() == 0) {
            return;
        }

        double inputAspectRatio = ((double) getIncomingWidth() * getROI().width()) / (getIncomingHeight() * getROI().height());

        double scaleX = 1.0f;
        double scaleY = 1.0f;

        double drawableWidth = getGeometryScale()[0];
        double drawableHeight = getGeometryScale()[1];

        double outputAspectRatio = drawableWidth / drawableHeight;

        if (!(inputAspectRatio == 0 || Double.isNaN(inputAspectRatio))) {
            switch (mScaleType) {
                case Drawable2d.SCALE_TYPE_CROP:
                    if (outputAspectRatio < inputAspectRatio) {
                        scaleX = inputAspectRatio / outputAspectRatio;
                        scaleY = 1.0f / outputAspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / inputAspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 2");
                    }
                    break;
                case Drawable2d.SCALE_TYPE_INSIDE:
                    if (outputAspectRatio > inputAspectRatio) {
                        scaleX = inputAspectRatio / outputAspectRatio;
                        scaleY = 1.0f / outputAspectRatio;
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / inputAspectRatio;
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 2");
                    }
                    break;
                case Drawable2d.SCALE_TYPE_SQUARE:
                    if (outputAspectRatio > inputAspectRatio) {
                        scaleX = inputAspectRatio / outputAspectRatio;
                        scaleY = 1.0f / outputAspectRatio;
                        if (VERBOSE) Log.d(TAG, "square vertical normal mode 1");
                    } else {
                        scaleX = inputAspectRatio;
                        scaleY = 1.0f;
                        if (VERBOSE) Log.d(TAG, "square vertical normal mode 2");
                    }
                    double scale = Math.min(scaleX, scaleY);
                    if (scale < 1.0f / outputAspectRatio) {
                        scale *= outputAspectRatio;
                        scaleX *= outputAspectRatio;
                        scaleY *= outputAspectRatio;
                    }
                    break;
                case Drawable2d.SCALE_TYPE_FIT:
                    scaleX = 1.0f;
                    scaleY = 1.0f / outputAspectRatio;
                    mClipRect = null;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_HEIGHT:
                    scaleX = inputAspectRatio / outputAspectRatio;
                    scaleY = 1.0f / outputAspectRatio;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_WIDTH:
                    scaleX = 1.0f;
                    scaleY = 1.0f / inputAspectRatio;
                    break;
                default:
                    if (VERBOSE) {
                        Log.d(TAG, "vertical video mode... Unknown scale type: " + toString());
                    }
            }
        }
        scaleX *= drawableWidth;
        scaleY *= drawableWidth;

        setGeometryScale(drawableWidth, drawableHeight, getGeometryScale()[2]);
        multiplyScale(new double[]{scaleX, scaleY, 1.0f});
    }

    public void onFilterReady(RenderTarget renderTarget, BaseFilter filter) {
        if (mLifecycleListener != null) {
            mLifecycleListener.onFilterSetup(renderTarget, this, filter);
        }
    }

    public void onFilterPredraw(OpenGLRenderer.Fuzzy renderTargetType, RenderTarget renderTarget, BaseFilter filter) {
        if (mLifecycleListener != null) {
            mLifecycleListener.onFilterPredraw(renderTargetType, renderTarget, this, filter);
        }
    }

    public Drawable2d() {
        super();

        setDefaultGeometryScale(1.0f, 1.0f);
        mScaleType = SCALE_TYPE_INSIDE;
        mIncomingWidth = 0;
        mIncomingHeight = 0;

        mBlendMode = BLEND_MODE_NONE;
    }

    public Drawable2d(Drawable2d drawable) {
        copy(drawable);
    }

    @Override
    public Drawable2d clone() {
        Drawable2d copy = (Drawable2d) super.clone();

        copy.mImageSources = new ArrayList<>(mImageSources.size());
        for (ImageSource imageSource : mImageSources) {
            copy.mImageSources.add(imageSource.clone());
        }
        copy.setChildren(new SparseArray<TreeNode>());
        int childrenCount = getChildren().size();
        SparseArray<TreeNode> children = getChildren();
        for (int i = 0; i < childrenCount; i++) {
            Drawable2d node = (Drawable2d) children.valueAt(i);
            if (node != null) {
                copy.addChild(copy.getNumberOfChildren(), node.clone());
            }
        }
        return copy;
    }

    public static Drawable2d createBitmapDrawable() {
        return createBitmapDrawable((Bitmap) null);
    }

    public static Drawable2d createBitmapDrawable(Bitmap bitmap) {
        return createBitmapDrawable(bitmap, Drawable2d.SCALE_TYPE_CROP);
    }

    public static Drawable2d createBitmapDrawable(String path) {
        return createBitmapDrawable(path, Drawable2d.SCALE_TYPE_CROP);
    }

    public static Drawable2d createColorDrawable() {
        Drawable2d drawable = new Drawable2d();
        drawable.setupDraw();
        drawable.setDirty(true);
        return drawable;
    }

    public static Drawable2d createExternalSourceDrawable(boolean isCameraSource, int scaleType) {
        Drawable2d drawable = new Drawable2d();
        drawable.setScaleType(scaleType);
        drawable.setImageSource(new ImageSource(true));
        drawable.setDirty(true);
        drawable.setupDraw();
        return drawable;
    }

    public static Drawable2d createBitmapDrawable(Bitmap bitmap, int scaleType) {
        Drawable2d drawable = new Drawable2d();
        drawable.addImageSource(new ImageSource(bitmap));
        drawable.setScaleType(scaleType);
        drawable.setDirtyRecursive(true);
        drawable.setupDraw();

        return drawable;
    }

    public static Drawable2d create() {
        Drawable2d drawable = new Drawable2d();
        //drawable.setScaleType(SCALE_TYPE_FIT_WIDTH);
        drawable.setScaleType(SCALE_TYPE_CROP);
        drawable.setDirty(true);
        drawable.setupDraw();
        return drawable;
    }

    public static Drawable2d create(ImageSource imageSource) {
        Drawable2d drawable = new Drawable2d();
        //drawable.setScaleType(SCALE_TYPE_FIT_WIDTH);
        int scaleType = SCALE_TYPE_FIT_WIDTH;
        if (imageSource.getShadingSourceType() == SHADING_SOURCE_BITMAP) {
            scaleType = SCALE_TYPE_FIT_WIDTH;
        } else if (imageSource.getShadingSourceType() == SHADING_SOURCE_EXTERNAL_TEXTURE) {
            if (imageSource.mIsCamera) {
                scaleType = SCALE_TYPE_CROP;
            } else {
                scaleType = SCALE_TYPE_FIT_WIDTH;
            }
        }
        drawable.setScaleType(scaleType);

        drawable.setDirty(true);
        drawable.setImageSource(imageSource.clone());
        drawable.setupDraw();
        return drawable;
    }

    public static Drawable2d create(ArrayList<ImageSource> imageSources) {
        Drawable2d drawable;
        if (!imageSources.isEmpty()) {
            drawable = create(imageSources.get(0));
        } else {
            drawable = create();
        }
        for (int i = 1; i < imageSources.size(); i++) {
            drawable.addImageSource(imageSources.get(i));
        }
        drawable.setupDraw();
        return drawable;
    }

    public void addImageSource(ImageSource imageSource, RectF roi) {
        imageSource = imageSource.clone();
        imageSource.setRegionOfInterest(roi, null);
        mImageSources.add(imageSource);
        setDirtyRecursive(true);
    }

    public void addImageSource(ImageSource imageSource) {
        addImageSource(imageSource, imageSource.getROI());
    }

    public void setImageSource(ImageSource imageSource, RectF roi) {
        mImageSources.clear();
        addImageSource(imageSource, roi);
    }

    public void setImageSource(ImageSource imageSource) {
        setImageSource(imageSource, imageSource.getROI());
    }

    public void clearImageSources() {
        mImageSources.clear();
        setDirtyRecursive(true);
    }

    public static Drawable2d createBitmapDrawable(String path, int scaleType) {
        Drawable2d drawable = new Drawable2d();
        drawable.setScaleType(scaleType);
        drawable.addImageSource(new ImageSource(path));
        drawable.setDirtyRecursive(true);
        drawable.setupDraw();
        return drawable;
    }

    public void setIsFlipped(boolean isFlipped) {
        mIsFlipped = isFlipped;
    }

    public boolean getIsFlipped() {
        return mIsFlipped;
    }

    public int getScaleType() {
        return mScaleType;
    }

    public void setScaleType(int scaleType) {
        mScaleType = scaleType;
        setDirtyRecursive(true);
    }

    public int getConvolutionType() {
        return mConvolutionType;
    }

    void setConvolutionType(int convolutionType) {
        mConvolutionType = convolutionType;
    }

    public List<String> getColorFilterMode() {
        return mColorFilterMode;
    }

    public void setFilterMode(String filterMode) {
        if (isFilterModeSame(Collections.singletonList(filterMode))) {
            return;
        }

        mColorFilterMode.clear();
        addFilterMode(filterMode);
        setDirtyRecursive(true);
    }

    public void setFilterMode(List<String> filterMode) {
        if (isFilterModeSame(filterMode)) {
            return;
        }

        mColorFilterMode.clear();
        for (String mode : filterMode) {
            addFilterMode(mode);
        }
        setDirtyRecursive(true);
    }

    private void addFilterMode(String filterMode) {
        if (!TextUtils.isEmpty(filterMode)) {
            mColorFilterMode.add(filterMode);
        }
    }

    private boolean isFilterModeSame(List<String> newFilterMode) {
        if (newFilterMode.size() != mColorFilterMode.size()) {
            return false;
        }

        for (int i = 0; i < newFilterMode.size(); i++) {
            if (!newFilterMode.get(i).equals(mColorFilterMode.get(i))) {
                return false;
            }
        }

        return true;
    }

    int getShadingSourceType() {
        int shadingSourceType = SHADING_SOURCE_NONE;
        if (mImageSources != null && mImageSources.size() > 0) {
            shadingSourceType = mImageSources.get(0).mSourceType;
        }
        return shadingSourceType;
    }

    public void setDescription(ProgramDescription description) {
        mDescription = description;
    }

    public ProgramDescription getDescription() {
        return mDescription;
    }

    int getBlendMode() {
        return mBlendMode;
    }

    void setBlendMode(int blendMode) {
        mBlendMode = blendMode;
    }

    private String getScaleTypeAsString() {
        String scaleType = "";
        switch (mScaleType) {
            case SCALE_TYPE_CROP:
                scaleType = "crop";
                break;
            case SCALE_TYPE_FIT:
                scaleType = "fit";
                break;
            case SCALE_TYPE_FIT_HEIGHT:
                scaleType = "fit_height";
                break;
            case SCALE_TYPE_FIT_WIDTH:
                scaleType = "fit_width";
                break;
            case SCALE_TYPE_INSIDE:
                scaleType = "inside";
                break;
            case SCALE_TYPE_SQUARE:
                scaleType = "square";
                break;
            case SCALE_TYPE_NONE:
                scaleType = "none";
                break;
            default:
                scaleType = "unknown";
        }
        return scaleType;
    }

    private String getShadingSourceTypeAsString() {
        String sourceType = "";
        switch (getShadingSourceType()) {
            case SHADING_SOURCE_BITMAP:
                sourceType = "bitmap";
                break;
            case SHADING_SOURCE_COLOR:
                sourceType = "color";
                break;
            case SHADING_SOURCE_EXTERNAL_TEXTURE:
                sourceType = "external_texture";
                break;
            case SHADING_SOURCE_RAW_TEXTURE:
                sourceType = "raw_texture";
                break;
            case SHADING_SOURCE_FBO:
                sourceType = "fbo";
                break;
            case SHADING_SOURCE_GIF:
                sourceType = "gif";
                break;
            default:
                sourceType = "unknown";
        }
        return sourceType;
    }

    int getIncomingWidth() {
        return mIncomingWidth;
    }

    public void setIncomingWidth(int incomingWidth) {
        mIncomingWidth = incomingWidth;
    }

    public void setIncomingHeight(int incomingHeight) {
        mIncomingHeight = incomingHeight;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void setIncomingSize(int incomingWidth, int incomingHeight) {
        mIncomingWidth = incomingWidth;
        mIncomingHeight = incomingHeight;
        //Camera image width and height parameters depend on the orientation, In portrait case, height is actually the width and vice-versa
        if ((getShadingSourceType() == SHADING_SOURCE_EXTERNAL_TEXTURE) && mImageSources.get(0).mIsCamera) {
            mIncomingHeight = incomingWidth;
            mIncomingWidth = incomingHeight;
        }
        if (getShadingSourceType() == SHADING_SOURCE_EXTERNAL_TEXTURE) {
            mImageSources.get(0).setIncomingSize(mIncomingWidth, mIncomingHeight);
        }
        setDirtyRecursive(true);
    }

    int getIncomingHeight() {
        return mIncomingHeight;
    }

    private void setupDrawWithFilter(List<String> filterMode) {
        mDescription.hasColorFilter = (filterMode.size() > 0);
        mDescription.setFilterMode(filterMode);
    }

    @Override
    void setupDraw() {
        super.setupDraw();
        mDescription.reset();

        mDescription.imageSources.clear();
        mDescription.imageSources.addAll(mImageSources);

        setupDrawWithFilter(getColorFilterMode());

/*        final byte[] indexData = {
                (byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0
        };*/

        final short[] indexData = {
                (short) 0, (short) 1, (short) 2, (short) 2, (short) 3, (short) 0
        };

        ShortBuffer indexDataBuffer = ByteBuffer
                .allocateDirect(indexData.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        indexDataBuffer.put(indexData).position(0);

//        setIndicesBuffer(indexDataBuffer);

        float[] vertexData = {
                -1.0f, -1.0f, -mWallDist,     // 0 bottom left
                1.0f, -1.0f, -mWallDist,      // 1 bottom right
                1.0f, 1.0f, -mWallDist,       // 2 top right
                -1.0f, 1.0f, -mWallDist,      // 3 top left
        };

        FloatBuffer vertexDataBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(vertexData).position(0);
//        setVertexBuffer(vertexDataBuffer);

        float[] textureData = new float[]{
//Starting from bottom left (for SurfaceTexture or self created textures etc)
                0.0f, 0.0f,     // 0 bottom left
                1.0f, 0.0f,      // 3 bottom right
                1.0f, 1.0f,     // 2 top right
                0.0f, 1.0f     // 1 top left
                ,
//In case when Surface Texture is flipped about Y Axis (Front camera case)
                1.0f, 0.0f,     // 0 bottom right
                0.0f, 0.0f,      // 3 bottom left
                0.0f, 1.0f,     // 2 top left
                1.0f, 1.0f     // 1 top right
                ,
                0.0f, 1.0f,     // 1 bottom right
                1.0f, 1.0f,     // 2 top right
                1.0f, 0.0f,      // 3 top left
                0.0f, 0.0f     // 0 bottom left
        };

        FloatBuffer textureDataBuffer = ByteBuffer
                .allocateDirect(textureData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureDataBuffer.put(textureData).position(0);
//        setTextureBuffer(textureDataBuffer);
    }

    /**
     * @deprecated Please use setFilterMode instead.
     */
    @Deprecated
    public void setFilter(BaseFilter filter) {
        mFilter = filter;
        mDescription.setFilterMode(filter.mFilterMode);
        setDirtyRecursive(true);
    }

    public void removeAllChildren() {
        super.cleanup();
    }

    public void cleanup() {
        super.cleanup();
    }

    @Override
    void computeDrawableProps(double targetAspectRatio) {
        if (getShadingSourceType() == Drawable2d.SHADING_SOURCE_EXTERNAL_TEXTURE) {
            if (sIncomingWidth <= 0 || sIncomingHeight <= 0) {
                setActive(false);
                return;
            }
            setActive(true);
            setVerticalVideoMode(mVerticalVideoMode);
            setIncomingSize(sIncomingWidth, sIncomingHeight);
            if (AVUtils.VERBOSE) Log.d(TAG, "Set incoming size for external source based drawable: " + "incomingWidth: " + mIncomingWidth + " incomingHeight: " + mIncomingHeight);
        } else if (getShadingSourceType() != SHADING_SOURCE_NONE) {
            ImageSource imageSource = mImageSources.get(0);
            if (imageSource != null) {
                setIncomingSize(imageSource.mIncomingWidth, imageSource.mIncomingHeight);
            }
        }

        double inputAspectRatio = ((double) getIncomingWidth() * getROI().width()) / (getIncomingHeight() * getROI().height());

        Drawable parent = (Drawable) getParent();

        double drawableWidth = getDefaultGeometryScale()[0];
        double drawableHeight = getDefaultGeometryScale()[1];

        double outDrawableWidth = drawableWidth;
        double outDrawableHeight = drawableHeight;

        if (drawableWidth == GraphicsConsts.MATCH_PARENT) {
            outDrawableWidth = 1.0f;
        }
        if (drawableHeight == GraphicsConsts.MATCH_PARENT) {
            if (parent == null) {
                outDrawableHeight = 1.0f / targetAspectRatio;
            } else {
                outDrawableHeight = 1.0f;
            }
        }

        if (parent != null) {
            if (outDrawableWidth > 0) {
                outDrawableWidth *= parent.getGeometryScale()[0];
            }
            if (outDrawableHeight > 0) {
                outDrawableHeight *= parent.getGeometryScale()[1];
            }
        }

        if (!(inputAspectRatio == 0 || Double.isNaN(inputAspectRatio))) {
            if (outDrawableHeight == GraphicsConsts.WRAP_CONTENT && outDrawableWidth > 0) {
                outDrawableHeight = outDrawableWidth / inputAspectRatio;
            } else if (outDrawableWidth == GraphicsConsts.WRAP_CONTENT && outDrawableHeight > 0) {
                outDrawableWidth = outDrawableHeight * inputAspectRatio;
            } else if (outDrawableHeight > 0 && outDrawableWidth > 0) {
            } else {
                outDrawableWidth = getIncomingWidth();
                outDrawableHeight = getIncomingHeight();
            }
        }

        setGeometryScale(outDrawableWidth, outDrawableHeight, getGeometryScale()[2]);

        super.computeDrawableProps(targetAspectRatio);
    }

    @Override
    public void copy(TreeNode node) {
        if (!(node instanceof Drawable2d)) {
            return;
        }
        Drawable2d src = (Drawable2d) node;
        super.copy(src);

        mBlendMode = src.mBlendMode;

        mIsFlipped = src.mIsFlipped;
        mColorFilterMode = new ArrayList<>(src.mColorFilterMode);
        mConvolutionType = src.mConvolutionType;
        mScaleType = src.mScaleType;
        mVerticalVideoMode = src.mVerticalVideoMode;

        mIncomingWidth = src.mIncomingWidth;
        mIncomingHeight = src.mIncomingHeight;

        if (mGeometryScale != null) {
            mGeometryScale = src.mGeometryScale.clone();
        }

        mModelMatrix = src.mModelMatrix.clone();

        if (src.mMVPMatrix != null) {
            mMVPMatrix = src.mMVPMatrix.clone();
        }

        if (valueAnimator != null) {
            valueAnimator = src.valueAnimator.clone();
        }
    }

    @Override
    public String toString() {
        String result = "\n\t";
        result += super.toString() + "\n";
        result += "ShadingSource:" + getShadingSourceTypeAsString() + "\n";

        result += " ScaleType: " + getScaleTypeAsString() + "\n";

        result += "\n";
        return result;
    }

    public void setVerticalVideoMode(boolean verticalVideoMode) {
        mVerticalVideoMode = verticalVideoMode;
    }

    @Override
    void updateImageSources(final int timestampMs) {
        for (final ImageSource imageSource : mImageSources) {
            if (imageSource.mSourceType == SHADING_SOURCE_BITMAP) {
                if ((imageSource.mBitmap == null || imageSource.mBitmap.isRecycled()) && imageSource.mPath != null) {
                    Bitmap bitmap = LruBitmapCache.INSTANCE.get(imageSource.mPath);
                    if (bitmap == null) {
                        bitmap = BitmapFactoryUtils.decodeFile(imageSource.mPath, Math.min(1080, AndroidUtilities.widthInPixel()));
                        if (bitmap == null) {
                            continue;
                        }
                        LruBitmapCache.INSTANCE.put(imageSource.mPath, bitmap);
                    }
                    imageSource.setBitmap(bitmap);
                    setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
                }
            } else if (imageSource.mSourceType == SHADING_SOURCE_GIF) {
                if (imageSource.mGifManager == null) {
                    imageSource.mGifManager = new GifManager(new GifManager.GifCallback() {
                         @Override
                        public void onGIFDecoderReady() {
                             imageSource.mGifManager.decodeFrame(timestampMs);
                        }

                        @Override
                        public void onFrameAvailable(Bitmap bitmap, int timestampMs, int frameCount) {
                            imageSource.setBitmap(bitmap);
                        }

                        @Override
                        public void onNextFrame(int delay, final int frameCount) {
//                            imageSource.mGifManager.decodeNextFrame();
                        }

                        @Override
                        public void onError(File file) {

                        }
                    });
                    imageSource.mGifManager.setGifFile(new File(imageSource.mPath));
                    imageSource.mGifManager.init();
                    imageSource.mGifManager.start();
                } else {
                    imageSource.mGifManager.decodeFrame(timestampMs);
                }
            }
        }

        SparseArray<TreeNode> drawableList = getChildren();
        int childCount = drawableList.size();
        for (int i = 0; i < childCount; i++) {
            Drawable2d child = (Drawable2d) drawableList.valueAt(i);
            if (child != null) {
                child.updateImageSources(timestampMs);
            }
        }
    }

/*    @Override
    public void draw() {
        if (valueAnimator != null) {
            mDrawableUpdateListener.update(this, valueAnimator.getAnimatedValue());
        }
        onPreDraw();
        if (mFilter != null) {
            mFilter.draw(this);
        }
        onDraw();
        onPostDraw();
    }*/

    private void onPreDraw() {

    }

    void onDraw() {

    }

    private void onPostDraw() {
    }

    private void postSetup() {
        if (valueAnimator == null) {
            return;
        }
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Drawable2d.this.onAnimationUpdate(animation);
            }
        });
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                valueAnimator.start();
            }
        });
    }

    private void onAnimationUpdate(ValueAnimator animation) {
        animatedValue = (double) animation.getAnimatedValue();
        Log.d(TAG, "drawable: " + super.toString() + " onAnimationUpdate: " + animation.getAnimatedValue());
    }

    public BaseFilter getFilter() {
        return mFilter;
    }

    public RectF getROI() {
        ArrayList<ImageSource> imageSources = getImageSources();
        if (!imageSources.isEmpty()) {
            return imageSources.get(0).getROI();
        } else {
            return ImageSource.DEFAULT_ROI;
        }
    }

    public void setAnimator(ValueAnimator animator) {
        valueAnimator = animator;
    }

    public ArrayList<ImageSource> getImageSources() {
        return mImageSources;
    }

    void setClipRect(Rect clipRect) {
        mClipRect = clipRect;
    }

    public void setVertexBuffer(FloatBuffer vertexBuffer) {
        mVertexBuffer = vertexBuffer;
        isVertexBufferBased = vertexBuffer != null;
    }

    public void setIndicesBuffer(ShortBuffer indicesBuffer) {
        mIndicesBuffer = indicesBuffer;
    }

    public void setTextureBuffer(FloatBuffer textureBuffer) {
        mTextureBuffer = textureBuffer;
    }

    public Buffer getTextureBuffer() {
        return mTextureBuffer;
    }

    public void setup() {
        if (isVertexBufferBased) {
            GLES20.glGenBuffers(ELEMENTS_TYPE_COUNT, bufferObjectArray, 0);
            Caches.setupBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBuffer, bufferObjectArray[VERTICES], mVertexBuffer.capacity(), 4);
            Caches.setupBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBuffer, bufferObjectArray[INDICES], mIndicesBuffer.capacity(), 2);
            Caches.setupBuffer(GLES20.GL_ARRAY_BUFFER, mTextureBuffer, bufferObjectArray[TEXTURES], mTextureBuffer.capacity(), 4);
        }
    }

    public int getVertexCount() {
        return mVertexBuffer.capacity() / 3;
    }
}