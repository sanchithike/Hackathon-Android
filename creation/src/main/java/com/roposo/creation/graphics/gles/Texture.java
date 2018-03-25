package com.roposo.creation.graphics.gles;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

/**
 * Represents an OpenGL texture.
 */
public class Texture {

    public Texture() {
        isExternalTexture = false;
        isFBOTexture = false;
        isSBitmapTexture = false;
        cleanup = false;
        bitmapSize = 0;
        mipMap = false;
//        uvMapper = null;
        isInUse = false;
        mWrapS = GLES20.GL_CLAMP_TO_EDGE;
        mWrapT = GLES20.GL_CLAMP_TO_EDGE;
        mMinFilter = GLES20.GL_LINEAR;
        mMagFilter = GLES20.GL_LINEAR;
        mFirstFilter = true;
        mFirstWrap = true;
    }

    public Texture(Caches caches) {
        isExternalTexture = false;
        isFBOTexture = false;
        cleanup = false;
        bitmapSize = 0;
        mipMap = false;
//        uvMapper = null;
        isInUse = false;
        mWrapS = GLES20.GL_CLAMP_TO_EDGE;
        mWrapT = GLES20.GL_CLAMP_TO_EDGE;
        mMinFilter = GLES20.GL_NEAREST;
        mMagFilter = GLES20.GL_NEAREST;
        mFirstFilter = true;
        mFirstWrap = true;
    }

    public void setWrap(int wrap) {
        setWrapST(wrap, wrap, false, false, isExternalTexture ? GLES11Ext
                .GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setWrap(int wrap, boolean bindTexture) {
        setWrapST(wrap, wrap, bindTexture, false, isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setWrap(int wrap, boolean bindTexture, boolean force) {
        setWrapST(wrap, wrap, bindTexture, force, isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setWrap(int wrap, boolean bindTexture, boolean force, int renderTarget) {
        setWrapST(wrap, wrap, bindTexture, force, renderTarget);
    }

    public void setWrapST(int wrapS, int wrapT, boolean bindTexture, boolean force, int renderTarget) {
        if (mFirstWrap || force || wrapS != mWrapS || wrapT != mWrapT) {
            mFirstWrap = false;

            mWrapS = wrapS;
            mWrapT = wrapT;

            GLES20.glTexParameteri(renderTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS);
            GLES20.glTexParameteri(renderTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT);
            Caches.mGlUtil.checkGlError("wrap: glTexParameter");
        }
    }

    public void setFilter(int filter) {
        setFilterMinMag(filter, filter, false, false, isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setFilter(int filter, boolean bindTexture) {
        setFilterMinMag(filter, filter, bindTexture, false, isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setFilter(int filter, boolean bindTexture, boolean force) {
        setFilterMinMag(filter, filter, bindTexture, force, isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D);
    }

    public void setFilter(int filter, boolean bindTexture, boolean force, int renderTarget) {
        setFilterMinMag(filter, filter, bindTexture, force, renderTarget);
    }

    public void setFilterMinMag(int min, int mag, boolean bindTexture, boolean force, int renderTarget) {
        if (mFirstFilter || force || min != mMinFilter || mag != mMagFilter) {
            mFirstFilter = false;

            mMinFilter = min;
            mMagFilter = mag;

            if (mipMap && min == GLES20.GL_LINEAR) min = GLES20.GL_LINEAR_MIPMAP_LINEAR;

            GLES20.glTexParameteri(renderTarget, GLES20.GL_TEXTURE_MIN_FILTER, min);
            GLES20.glTexParameteri(renderTarget, GLES20.GL_TEXTURE_MAG_FILTER, mag);
            Caches.mGlUtil.checkGlError("glTexParameter");
        }
    }

    /**
     * Name of the texture.
     */
    public int id;
    /**
     * Generation of the backing bitmap,
     */
    public int generation;
    /**
     * Indicates whether the texture requires blending.
     */
    public boolean blend;
    /**
     * Width of the backing bitmap.
     */
    public int width = -1;
    /**
     * Height of the backing bitmap.
     */
    public int height = -1;
    /**
     * Indicates whether this texture should be cleaned up after use.
     */
    public boolean cleanup;
    /**
     * Optional, size of the original bitmap.
     */
    public int bitmapSize;
    /**
     * Indicates whether this texture will use trilinear filtering.
     */
    public boolean mipMap;

    /**
     * Optional, pointer to a texture coordinates mapper.
     */
//    final UvMapper uvMapper;

    /**
     * Whether or not the Texture is marked in use and thus not evictable for
     * the current frame. This is reset at the start of a new frame.
     */
    public boolean isInUse;

    /**
     * Whether or not the Texture is external texture i.e. coming from SurfaceTexture (say, from Camera) or EGLKHRImage
    */
    public boolean isExternalTexture = false;
    public boolean isFBOTexture = false;
    public boolean isSBitmapTexture = false;
    /**
     * Last wrap modes set on this texture. Defaults to GL_CLAMP_TO_EDGE.
     */
    private int mWrapS;
    private int mWrapT;

    /**
     * Last filters set on this texture. Defaults to GL_NEAREST.
     */
    private int mMinFilter;
    private int mMagFilter;

    private boolean mFirstFilter;
    private boolean mFirstWrap;

    public void markUse() {
        isInUse = true;
    }

    public void markUnuse() {
        isInUse = false;
    }


    @Override
    public String toString() {
        String result = "Texture: ";
        result += isExternalTexture ? "external" : (isFBOTexture) ? "FBO" : (isSBitmapTexture) ?
                "SBitmap" : "Bitmap";
        result += "  id: " + id;
        return result;
    }
}; // struct Texture
