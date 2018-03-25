package com.roposo.creation.graphics;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.roposo.core.util.FileUtilities;
import com.roposo.creation.graphics.gifdecoder.GifManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;

import java.nio.Buffer;

import static com.roposo.creation.graphics.gles.Drawable2d.SHADING_SOURCE_BITMAP;
import static com.roposo.creation.graphics.gles.Drawable2d.SHADING_SOURCE_EXTERNAL_TEXTURE;
import static com.roposo.creation.graphics.gles.Drawable2d.SHADING_SOURCE_FBO;
import static com.roposo.creation.graphics.gles.Drawable2d.SHADING_SOURCE_GIF;
import static com.roposo.creation.graphics.gles.Drawable2d.SHADING_SOURCE_RAW_TEXTURE;

/**
 * @author bajaj on 16/01/18.
 */

public class ImageSource implements Cloneable {
    public static final RectF DEFAULT_ROI = new RectF(0, 0, 1, 1);

    public int mSourceType;

    public boolean mIsCamera;
    public String mPath;
    public Bitmap mBitmap;
    public Buffer mBuffer;
    public FBObject mFBSource;
    private boolean mIsLUT;

    public int mIncomingWidth;
    public int mIncomingHeight;
    public GifManager mGifManager;

    private RectF mROI;
    private int mTextureParameterSymbol = GLES20.GL_LINEAR;

    @Override
    public ImageSource clone() {
        ImageSource imageSource = null;
        try {
            imageSource = (ImageSource) super.clone();
            if (getROI() != null) {
                RectF rect = new RectF(getROI());
                imageSource.setRegionOfInterest(rect, null);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return imageSource;
    }

    public ImageSource(Bitmap bitmap, boolean isLUT) {
        this(bitmap);
        this.mIsLUT = isLUT;
    }

    public ImageSource(String path) {
        if (FileUtilities.isFileTypeVideo(path)) {
            mSourceType = SHADING_SOURCE_EXTERNAL_TEXTURE;
        } else if (FileUtilities.isFileTypeImage(path)) {
            mSourceType = SHADING_SOURCE_BITMAP;
        } else if (FileUtilities.isFileTypeGif(path)) {
            mSourceType = SHADING_SOURCE_GIF;
        }
        mPath = path;
    }

    public ImageSource(Bitmap bitmap) {
        mSourceType = SHADING_SOURCE_BITMAP;
        setBitmap(bitmap);
    }

    public ImageSource(Buffer buffer) {
        mSourceType = SHADING_SOURCE_RAW_TEXTURE;
        mBuffer = buffer;
        mTextureParameterSymbol = GLES20.GL_NEAREST; // because current use cases require exact value sampling from buffer
        setIncomingSize(buffer.capacity(), 1);
    }

    public ImageSource(FBObject fbObject) {
        mSourceType = SHADING_SOURCE_FBO;
        mFBSource = fbObject;
        setIncomingSize(fbObject.getWidth(), fbObject.getHeight());
    }

    public ImageSource(boolean isCamera) {
        mIsCamera = isCamera;
        mSourceType = SHADING_SOURCE_EXTERNAL_TEXTURE;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;

        setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
    }

    public void setIncomingSize(int incomingWidth, int incomingHeight) {
        mIncomingWidth = incomingWidth;
        mIncomingHeight = incomingHeight;
    }

    public int getShadingSourceType() {
        return mSourceType;
    }

    public RectF getROI() {
        return mROI;
    }

    // containerDrawable is the drawable which contains this image source.
    // it needs to be marked dirty for recomputation based on new ROI
    public ImageSource setRegionOfInterest(RectF rect, Drawable containerDrawable) {
        mROI = rect != null ? rect : DEFAULT_ROI;
        if (containerDrawable != null) {
            containerDrawable.setDirtyRecursive(true);
        }
        return this;
    }

    public boolean hasTexture() {
        return mSourceType != Drawable2d.SHADING_SOURCE_NONE &&
                mSourceType != Drawable2d.SHADING_SOURCE_COLOR;
    }

    public boolean hasExternalTexture() {
        return mSourceType == SHADING_SOURCE_EXTERNAL_TEXTURE;
    }

    public boolean isBGRA() {
        return mSourceType == SHADING_SOURCE_BITMAP || mSourceType == SHADING_SOURCE_GIF;
    }

    public boolean isLUT() {
        return mIsLUT;
    }

    public ImageSource setTextureParameterSymbol(int textureParameterSymbol) {
        mTextureParameterSymbol = textureParameterSymbol;
        return this;
    }

    public int getTextureParameterSymbol() {
        return mTextureParameterSymbol;
    }

    // only to generate unique program description for caching
    @Override
    public String toString() {
        return Integer.toString(mSourceType);
    }

}