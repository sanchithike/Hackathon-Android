package com.roposo.creation.graphics.gles;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;

import com.roposo.creation.av.AVUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@SuppressWarnings({"ConstantConditions", "WeakerAccess"})
public class TextureCache {

    String mLock = "TextureCache";
    static String TAG = "TextureCache";

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    LruTextureCache mCache;
    private SparseArray<Texture> mFBOTextures;
    private SparseArray<Texture> mExternalTextures;

    int mSize;
    private boolean mDebugEnabled = true;

    private long mNonBitmapIdStartIndex = 1 << 32;

    private int[] mBoundTextures;
    private int MAX_TEXTURE_UNITS_COUNT = 15;

    int mTextureUnit;
    private Texture mExternalTexture;

    public TextureCache() {
        if (mCache == null)
/*            mCache = new LruTextureCache(11 * 1024 * 1024); // 11 MB*/
            mCache = new LruTextureCache(20); // 9 textures
        mFBOTextures = new SparseArray<>();
        mExternalTextures = new SparseArray<>();

        mBoundTextures = new int[MAX_TEXTURE_UNITS_COUNT];

        resetBoundTextures();
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        mTextureUnit = 0;

    }

    int SURFACE_SIZE_FACTOR = 4;

    Texture createFBOTexture(int width, int height) {
        // Conform to boundary alignment and size rules (multiple of 16, may be)
        width = (width * SURFACE_SIZE_FACTOR + (SURFACE_SIZE_FACTOR - 1)) /
                SURFACE_SIZE_FACTOR;
        height = (height * SURFACE_SIZE_FACTOR + (SURFACE_SIZE_FACTOR - 1)) /
                SURFACE_SIZE_FACTOR;

        Texture texture = new Texture();
        texture.isFBOTexture = true;
        texture.width = width;
        texture.height = height;
        generateTexture(texture);
        if (AVUtils.VERBOSE) Log.d(TAG, "Create FBO texture: " + texture.id);
        mFBOTextures.put(texture.id, texture);
        return texture;
    }

    // sending true returns the ExternalTexture from Cache. false returns a new texture.
    Texture createExternalTexture() {
        Texture texture = new Texture();
        texture.isExternalTexture = true;
        generateTexture(texture);
        mExternalTexture = texture;
        mExternalTextures.put(texture.id, texture);
        return texture;
    }

    Texture getTexture(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Texture texture = getCachedTexture(bitmap);

        if (texture == null) {

            final int size = bitmap.getByteCount();
            texture = new Texture();
            texture.bitmapSize = size;
            generateTexture(bitmap, texture, false);
            texture.cleanup = true;
        }

        return texture;
    }

    Texture getTexture(Buffer buffer) {
        if (buffer == null) {
            return null;
        }
        Texture texture = getCachedTexture(buffer);
        return texture;
    }

    Texture getCachedTexture(SurfaceTexture surfaceTexture) {
        Texture texture = mCache.get(surfaceTexture);
        return texture;
    }

    Texture getCachedTexture(Buffer buffer) {
        Texture texture = mCache.get(buffer);
        if (texture == null) {
            texture = new Texture();
            int size = 4 * buffer.capacity();
            texture.bitmapSize = size;
            generateTexture(buffer, texture);
            mSize += size;

            if(VERBOSE) Log.d(TAG, "TextureCache::get: create texture for buffer" + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created, size = " + size);
            }
            mCache.put(buffer, texture);
        }
        return texture;
    }

    Texture getCachedTexture(Bitmap bitmap) {
        Texture texture = mCache.get(bitmap);
        if (texture == null) {
            texture = new Texture();
            final int size = bitmap.getByteCount();
            texture.bitmapSize = size;
            generateTexture(bitmap, texture, false);

            mSize += size;
            if(VERBOSE) Log.d(TAG, "TextureCache::get: create texture " + bitmap + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created, size = " + size + " for bitmap: " + bitmap);
            }
            mCache.put(bitmap, texture);
        } else if (!texture.isInUse && bitmap.getGenerationId() != texture.generation) {
            // Texture was in the cache but is dirty, re-upload
            // TODO: Re-adjust the cache size if the bitmap's dimensions hab changed
            generateTexture(bitmap, texture, true);
        }
        return texture;
    }

    /**
     * Just sets up an already created texture for which id is known
     */
    void generateTexture(Texture texture, int textureId) {
        texture.id = textureId;

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        Caches.checkGlError("glBindTexture " + textureId);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        Caches.checkGlError("glTexParameter");
    }

    /**
     * Generates a Texture
     */
    void generateTexture(Texture texture) {
        if (texture == null) {
            Log.e(TAG, "Input Texture is null, there is a serious issue");
            return;
        }
        if (AVUtils.VERBOSE) Log.d(TAG, "Upload Input/External Texture");

        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        Caches.checkGlError("generate External Texture :: glGenTextures");

        int textureId = textureHandles[0];
        texture.id = textureId;

        int textureTarget = texture.isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES :
                GLES20.GL_TEXTURE_2D;
        GLES20.glBindTexture(textureTarget, textureId);
        Caches.checkGlError("glBindTexture " + textureId);

        if (!texture.isExternalTexture) {
            GLES20.glTexImage2D(textureTarget, 0, GLES20.GL_RGBA, texture.width,
                    texture.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        }

        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        Caches.checkGlError("generateTexture: glTexParameter");
    }

    /**
     * Generates the texture from a bitmap into the specified texture structure.
     *
     * @param regenerate If true, the bitmap data is reuploaded into the texture, but
     *                   no new texture is generated.
     */
    void generateTexture(final Bitmap bitmap, Texture texture, boolean regenerate) {

        if (VERBOSE)
            Log.d(TAG, "Upload Texture " + "width: " + bitmap.getWidth() + "height: " + bitmap.getHeight());

        // If the texture had mipmap enabled but not anymore,
        // force a glTexImage2D to discard the mipmap levels
        final boolean resize = !regenerate || bitmap.getWidth() != texture.width || bitmap.getHeight() != texture.height;

        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        if (!regenerate) {
            GLES20.glGenTextures(1, textureHandles, 0);
            texture.id = textureHandles[0];
        }

        texture.generation = bitmap.getGenerationId();
        texture.width = bitmap.getWidth();
        texture.height = bitmap.getHeight();

        bindTexture(texture.id);
        int[] pixels = null;

        Bitmap.Config config = bitmap.getConfig();
        if (config == null) config = Bitmap.Config.ARGB_8888;
        switch (config) {
            case ALPHA_8:
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                pixels = new int[bitmap.getWidth() * bitmap.getHeight() / 4];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                uploadToTexture(resize, GLES20.GL_ALPHA, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_BYTE, pixels);
                texture.blend = true;
                break;
            case RGB_565:
/*                pixels = new int[bitmap.getWidth() * bitmap.getHeight() / 2];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());*/
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//                uploadToTexture(resize, GLES20.GL_RGB, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_SHORT_5_6_5, pixels);
                texture.blend = false;
                break;
            case ARGB_8888:
                pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                uploadToTexture(resize, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_BYTE, pixels);
                texture.blend = false;
                break;
            default:
                Log.w(TAG, "Unsupported bitmap colorType: " + config);
                break;
        }

        if (texture.mipMap) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        }
        if (!regenerate) {
            texture.setFilter(GLES20.GL_LINEAR);
            texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        }
    }

    void generateTexture(Bitmap bitmap, Texture texture) {
        generateTexture(bitmap, texture, false);
    }

    void generateTexture(Buffer buffer, Texture texture) {
        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        texture.id = textureHandles[0];

        texture.generation = buffer.hashCode();
        texture.width = buffer.capacity();
        texture.height = 1;

        bindTexture(texture.id);
        buffer.position(0);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, texture.width, texture.height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer);
        Caches.checkGlError("TexImage2d for 1D blur Kernel");

        texture.setFilter(GLES20.GL_NEAREST); // Very important to make it GL_NEAREST.
        // Because we need precise values for blur coefficients.

        texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        activeTexture(0);
        bindTexture(0);
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, int[] pixels) {
        IntBuffer data = IntBuffer.wrap(pixels);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, data);
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, Buffer pixelBuf) {
        if (pixelBuf instanceof ByteBuffer) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, ((ByteBuffer) pixelBuf).asIntBuffer());
        } else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf);
        }
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, ByteBuffer pixelBuf) {
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf.asIntBuffer());
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, IntBuffer pixelBuf) {
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf);
    }

    void bindTexture(int target, int texture, int textureUnit) {
        activeTexture(textureUnit);
        if (VERBOSE) Log.v(TAG, "Caches :: bindTexture < texture");
//        if (mBoundTextures[mTextureUnit] != texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        Caches.checkGlError("glBindTexture GL_TEXTURE_2D");
        mBoundTextures[mTextureUnit] = texture;
//        }
    }

    void bindTexture(int texture) {

        if (VERBOSE) Log.v(TAG, "Caches :: bindTexture < texture");
//        if (mBoundTextures[mTextureUnit] != texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        Caches.checkGlError("glBindTexture GL_TEXTURE_2D");
        mBoundTextures[mTextureUnit] = texture;
//        }
    }

    void bindTexture(Texture texture, int textureParameterSymbol) {
        int target = texture.isExternalTexture ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;
        bindTexture(target, texture.id);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, textureParameterSymbol);
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, textureParameterSymbol);
    }

    void bindTexture(int target, int texture) {
        if (VERBOSE) Log.v(TAG, "Caches :: bindTexture < target, texture");
        if (target == GLES20.GL_TEXTURE_2D) {
            bindTexture(texture);
        } else {
            // GLConsumer directly calls glBindTexture() with
            // target=GL_TEXTURE_EXTERNAL_OES, don't cache this target
            // since the cached state could be stale
            GLES20.glBindTexture(target, 0);
            GLES20.glBindTexture(target, texture);
            Caches.checkGlError("glBindTexture GL_TEXTURE_EXTERNAL_OES");
        }
    }

    public Texture getFBOTexture(int textureId) {
        return mFBOTextures.get(textureId);
    }

    public Texture getExternalTexture() {
        return mExternalTexture;
    }

    public Texture getExternalTexture(int textureId) {
        if (textureId == -1) {
            return mExternalTextures.valueAt(0);
        }
        return mExternalTextures.get(textureId);
    }

    public void deleteFBO(int textureId) {
        Texture texture = mFBOTextures.get(textureId);
        if (texture != null) {
            mFBOTextures.remove(textureId);
            deleteTexture(textureId);
        } else {
            Log.w(TAG, "No FBO texture exists with id: " + textureId);
        }
    }

    public void deleteExternalTexture(int textureId) {
        Texture texture = mExternalTextures.get(textureId);
        if (texture != null) {
            mExternalTextures.remove(textureId);
            deleteTexture(textureId);
        } else {
            Log.w(TAG, "No External texture exists with id: " + textureId);
        }
    }

    public static void deleteTexture(int textureId) {
        int[] tempArr = new int[1];
        tempArr[0] = textureId;
        if (AVUtils.VERBOSE) Log.d(TAG, "Deleting texture: " + textureId);
        GLES20.glDeleteTextures(1, tempArr, 0);
    }

    void activeTexture(int textureUnit) {
//s        if (mTextureUnit != textureUnit) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
        if (Caches.checkGlError("Caches :: activeTexture: " + textureUnit)) {
            Log.d(TAG, "activetexture issue");
        }
//s            mTextureUnit = textureUnit;
//s        }
    }

    public void resetActiveTexture() {
        mTextureUnit = -1;
    }

    public void resetBoundTextures() {
        for (int i = 0; i < MAX_TEXTURE_UNITS_COUNT; i++) {
            mBoundTextures[i] = 0;
        }
    }

    public void unbindTexture(int textureTarget) {
        GLES20.glBindTexture(textureTarget, 0);
    }

    public void unbindTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Clears the cache. This causes all textures to be deleted.
     */
    void clear() {
        mCache.evictAll();
/*        Collection<Texture> textureCollection = mCache.values();
        Iterator<Texture> textureIterator = textureCollection.iterator();
        while (textureIterator.hasNext()) {
            Texture texture = textureIterator.next();
            texture.deleteTexture();
        }
        mCache.clear();*/
        if (AVUtils.VERBOSE) Log.d(TAG, "TextureCache:clear(), mSize = " + mSize);
    }

/*    private void deleteTexture(Texture texture) {
        texture.deleteTexture();
    }*/

    private static class LruTextureCache extends LruCache<Object, Texture> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruTextureCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Object bitmap, Texture texture, Texture newTexture) {
            if (VERBOSE)
                Log.d(TAG, "Evicting texture " + texture + " sourced from Bitmap " + bitmap);
            deleteTexture(texture.id);
        }

/*        @Override
        protected int sizeOf(Object bitmap, Texture texture) {
            if(bitmap instanceof Bitmap) {
                return ((Bitmap) bitmap).getByteCount();
            } else if(bitmap instanceof OpenGLRenderer.SBitmap) {
                return ((OpenGLRenderer.SBitmap) bitmap).getByteCount();
            } else {
                return texture.bitmapSize*4;
            }
        }*/
    }
}
