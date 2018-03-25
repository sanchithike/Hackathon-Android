package com.roposo.creation.graphics.gles;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.util.LruCache;

import com.roposo.creation.av.AVUtils;
import com.roposo.creation.graphics.FBCache;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.Renderer;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.filters.FilterCache;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Caches {

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    public static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    public static final int BYTES_PER_INT = Integer.SIZE / 8;

    public static final int VERTICES = 0;
    public static final int INDICES = 1;
    public static final int TEXTURES = 2;
    public static final int COLORS = 3;
    public static final int ELEMENTS_TYPE_COUNT = 4;

    public int[] bufferObjectArray = new int[ELEMENTS_TYPE_COUNT];

//    private FloatBuffer mCurrentPositionPointer;
//    public ShortBuffer mCurrentIndicesPointer;
//    private FloatBuffer mCurrentTexCoordsPointer;

    private int MAX_TEXTURE_UNITS_COUNT = 15;
    private int MAX_MESH_COUNT = 5;

    public static final float mWallDist = 8.0f; //1.906f; //1.2368f*2;

    private int[] mBoundTextures;

    private int[] mTextures;

    public SurfaceTexture mSurfaceTexture;
    public volatile float[] mSurfaceTextureTransform;
    public volatile long mSurfaceTextureTimestamp = -1;

    protected String sPreviewTerminationLock = "plock";
    protected String sEncoderTerminationLock = "elock";

    static GlUtil mGlUtil = null;
    static GlUtil mGlErrorUtil = null;

    static {
        mGlUtil = GlErrorUtil.getInstance();
        mGlErrorUtil = GlErrorUtil.getInstance();
    }

    Lock mLock;
    public final Object mGLSVRenderFence = new Object();
    public final Object mVideoRenderFence = new Object();
    public final Object mCaptureRenderFence = new Object();

    public void setGlErrorFlag(boolean glErrorFlag) {
        if (!glErrorFlag) {
            mGlUtil = GlUtil.getInstance();
        } else {
            mGlErrorUtil = GlErrorUtil.getInstance();
            mGlUtil = mGlErrorUtil;
        }
    }

    private static final String TAG = "OpenGLRenderer::Caches";

    public static final byte[] indexData = {
            (byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0
    };

    public static final String[] FilterModeUniform = {
            "none"
            , "richMode"
            , "bwMode"
            ,"nightMode"
            , "vignetteMode"
            , "mirrorMode"
            , "bulgeMode"
            , "sketchMode"
            , "toonMode"
    };

    static final int gMeshCount = 4;

    private float[] textureData = new float[]{
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

    private static final float originalTextureData[] = new float[]{
            //Starting from bottom left (for SurfaceTexture or self created textures etc)
            -1.0f, -1.0f,     // 0 bottom left
            1.0f, -1.0f,      // 3 bottom right
            1.0f, 1.0f,     // 2 top right
            -1.0f, 1.0f     // 1 top left
            ,
            //In case when Surface Texture is flipped about Y Axis (Front camera case)
            1.0f, -1.0f,     // 0 bottom right
            -1.0f, -1.0f,      // 3 bottom left
            -1.0f, 1.0f,     // 2 top left
            1.0f, 1.0f     // 1 top right
            ,
            -1.0f, 1.0f,     // 1 bottom right
            1.0f, 1.0f,     // 2 top right
            1.0f, -1.0f,      // 3 top left
            -1.0f, -1.0f     // 0 bottom left
    };

    public FloatBuffer vertexDataBuffer;
    private ByteBuffer indexDataBuffer;
    public FloatBuffer textureDataBuffer;
    private FloatBuffer colorDataBuffer;

    public static ByteBuffer blurKernel;
    boolean blend;

    public int lastSrcMode;
    public int lastDstMode;
    public Program currentProgram;
    public boolean scissorEnabled;

//sahil
/*    ProgramCache mProgramCache;
    TextureCache mTextureCache;*/

    public ProgramCache mProgramCache;
    TextureCache mTextureCache;

    public FilterCache mFilterCache;
    private FBCache mFBCache;

    boolean mVertexArrayEnabled;
    boolean mTexCoordsArrayEnabled;

    int mTextureUnit;

    int mScissorX;
    int mScissorY;
    int mScissorWidth;
    int mScissorHeight;

    boolean mInitialized;

    float minX = 0, maxX = 0, minY = 0, maxY = 0;

    HashMap<Texture, SurfaceTexture> sSurfaceTextures;

    static {
        byte[] blurKernelData = new byte[64];

        {
            float[] blurKernelFloatData = new float[]{
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.02f, 0.04f, 0.08f, 0.04f, 0.02f,
                    0.04f, 0.08f, 0.16f, 0.08f, 0.04f,
                    0.02f, 0.04f, 0.08f, 0.04f, 0.02f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,

                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,

                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                    0.01f, 0.02f, 0.04f, 0.02f
            };

            for (int i = 0; i < blurKernelFloatData.length; i++) {
                blurKernelData[i] = (byte) (blurKernelFloatData[i] * 256);
            }
        }

        blurKernel = ByteBuffer.allocateDirect(blurKernelData.length).order(ByteOrder.nativeOrder());
        blurKernel.position(0);
        blurKernel.put(blurKernelData).position(0);
    }

    Caches() {
        mInitialized = false;

        init();
    }

    public boolean init() {
        if (mInitialized) return false;

        if (VERBOSE) Log.d(TAG, "Caches::init");

        mLock = new ReentrantLock();

        mProgramCache = new ProgramCache();
        mTextureCache = new TextureCache();

        mFilterCache = new FilterCache();
        mFBCache = new FBCache();

        GLES20.glGenBuffers(ELEMENTS_TYPE_COUNT, bufferObjectArray, 0);

        float[] vertexData = {
                -1.0f, -1.0f, -mWallDist,     // 0 bottom left
                1.0f, -1.0f, -mWallDist,      // 1 bottom right
                1.0f, 1.0f, -mWallDist,       // 2 top right
                -1.0f, 1.0f, -mWallDist,      // 3 top left
        };

        vertexDataBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(vertexData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[VERTICES]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * BYTES_PER_FLOAT, vertexDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        vertexData = null;

        indexDataBuffer = ByteBuffer
                .allocateDirect(indexData.length).order(ByteOrder.nativeOrder());
        indexDataBuffer.put(indexData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferObjectArray[INDICES]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexData.length, indexDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        textureDataBuffer = ByteBuffer
                .allocateDirect(textureData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureDataBuffer.put(textureData).position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * BYTES_PER_FLOAT, textureDataBuffer, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        float colorData[] = new float[] {
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.3f, 0.7f, 1.0f
        };

        colorDataBuffer = ByteBuffer
                .allocateDirect(colorData.length * 2 /* Assuming, we'd need a max of 2 such buffers  */ * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorDataBuffer.put(colorData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorData.length * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mTexCoordsArrayEnabled = false;
        mVertexArrayEnabled = false;

        // Emulate scissorEnabled, so that disableScissor disables scissor test after checking our flag :D
        scissorEnabled = true;
        disableScissor();

        mTextureUnit = 0;

        blend = false;
        lastSrcMode = GLES20.GL_ZERO;
        lastDstMode = GLES20.GL_ZERO;
        currentProgram = null;

        mInitialized = true;

        mBoundTextures = new int[MAX_TEXTURE_UNITS_COUNT];

/*        float [] blurKernelData = new float[] {
                0.000007f, 0.000023f, 0.000191f, 0.000388f, 0.000191f, 0.000023f, 0.000007f,
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f,
                0.000192f, 0.006556f, 0.054721f, 0.110982f, 0.054722f, 0.006560f, 0.000191f,
                0.000388f, 0.013304f, 0.110981f, 0.225083f, 0.110982f, 0.013304f, 0.000388f,
                0.000191f, 0.006560f, 0.054721f, 0.110982f, 0.054722f, 0.006560f, 0.000191f,
                0.000023f, 0.000786f, 0.006556f, 0.013304f, 0.006560f, 0.000786f, 0.000023f,
                0.000007f, 0.000023f, 0.000191f, 0.000388f, 0.000191f, 0.000023f, 0.000007f,
                //Dummy values to fill 64 values (min supported texture size)
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f,
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f, 0.000023f
        };*/
        return true;
    }

    public void terminate() {
        mLock.lock();

        try {
            textureData = null;

            if (!mInitialized) {
                return;
            }

            GLES20.glDeleteBuffers(ELEMENTS_TYPE_COUNT, bufferObjectArray, 0);

            mProgramCache.clear();
            mTextureCache.clear();
            mFilterCache.clear();

            bitmapCache.evictAll();
            currentProgram = null;

//        clearGarbage();

            mInitialized = false;

            mProgramCache = null;
            mTextureCache = null;
        } finally {
            mLock.unlock();
        }
    }

    public Lock getLock() {
        return mLock;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void flush() {
        if (VERBOSE) Log.d(TAG, "Flushing caches ");

        mTextureCache.clear();
    }

    public static void setupBuffer(int target, Buffer buffer, int bufferObject, int length, int unitSize) {
        GLES20.glBindBuffer(target, bufferObject);
        GLES20.glBufferData(target, length * unitSize, buffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(target, 0);
    }

    void bindBufferIndex(int target, int index) {
        bindBuffer(target, bufferObjectArray[index]);
    }

    void bindBuffer(int target, int bufferObject) {
        GLES20.glBindBuffer(target, bufferObject);
    }

    void unbindBuffer(int target) {
        GLES20.glBindBuffer(target, 0);
    }

    public void unbindMeshBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public boolean bindTextureBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        mGlUtil.checkGlError("glBindBuffer textures");
        return true;
//        return bindTextureBuffer(bufferObjectArray[TEXTURES]);
    }

    public boolean bindTextureBuffer(final int buffer) {
//s        if (mCurrentTextureHandle != buffer) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
        return true;
//s        }
//s        return false;
    }

    public void unbindTextureBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public boolean bindIndicesBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferObjectArray[INDICES]);
        mGlUtil.checkGlError("glBindBuffer indices");
        return true;
//        return bindIndicesBuffer(bufferObjectArray[INDICES]);
    }

    public void bindIndicesBuffer(final int buffer) {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer);
    }

    public void unbindIndicesBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public boolean bindColorsBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
        mGlUtil.checkGlError("glBindBuffer colors");
        return true;
    }

    public void unbindColorsBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    static HashMap<Renderer, Caches> mCaches = new HashMap<>(5);

    public static synchronized Caches getCacheInstance(Renderer renderer) {
        Caches cache = mCaches.get(renderer);
        if (cache == null) {
            cache = new Caches();
            if (VERBOSE) Log.d(TAG, "Creating Cache: " + cache + " for renderer: " + renderer);

//            cache.setRenderer(renderer);
            mCaches.put(renderer, cache);
        } else {
            if (VERBOSE) Log.d(TAG, "Cache: " + cache + " for renderer: " + renderer);
        }
        return cache;
    }

    public static synchronized void terminateCache(Renderer renderer) {
        if (mCaches != null) {
            Caches cache = mCaches.remove(renderer);
            if (cache != null) {
                cache.terminate();
            }
        }
    }

    static class TextureData {
        Buffer textureDataBuffer;
        int[] textureDataHandle = new int[1];

        TextureData(Buffer buffer, int[] handle) {
            textureDataBuffer = buffer;
            textureDataHandle = handle;
        }
    }

    private HashMap<RectF, TextureData> mTextureDataCache = new HashMap<>();

    TextureData getTextureCoordBuffer(RectF roi) {
        TextureData textureDataHolder = mTextureDataCache.get(roi);
        if (textureDataHolder == null) {
            int [] textureDataHandle = new int[1];
            GLES20.glGenBuffers(1, textureDataHandle, 0);

            int length = 3 * 4;

            float[] textureData = new float[2 * length];

            FloatBuffer textureDataBuffer = ByteBuffer
                    .allocateDirect(2 * length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            for (int i = 0; i < length; i++) {
                textureData[2 * i] = ((1.0f + originalTextureData[2 * i]) * roi.width() / 2.0f) + roi.left;
                textureData[2 * i + 1] = ((1.0f + originalTextureData[2 * i + 1]) * roi.height() / 2.0f) + roi.top;
            }

            textureDataBuffer.position(0);
            textureDataBuffer.put(textureData).position(0);

            textureDataHolder = new TextureData(textureDataBuffer, textureDataHandle);
            mTextureDataCache.put(roi, textureDataHolder);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureDataHandle[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 2 * length * BYTES_PER_FLOAT, textureDataBuffer, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
        return textureDataHolder;
    }

    public void setTextureCoordinates(float minX, float maxX, float minY, float maxY) {
        setTextureCoordinates(minX, maxX, minY, maxY, 0);
    }

    public void setTextureCoordinates(float minX, float maxX, float minY, float maxY, int position) {
        int initialIndex = position * 8;
        int index = initialIndex;
        textureDataBuffer.put(index, minX);  index++;    textureDataBuffer.put(index, minY);    index++;
        textureDataBuffer.put(index, maxX);  index++;    textureDataBuffer.put(index, minY);    index++;
        textureDataBuffer.put(index, maxX);  index++;  textureDataBuffer.put(index, maxY);      index++;
        textureDataBuffer.put(index, minX);  index++;  textureDataBuffer.put(index, maxY);      index++;

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 8 *
                BYTES_PER_FLOAT, textureDataBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void setColors(float [] colors) {
        setColors(colors, 0);
    }

    public void setColors(float [] colors, int position) {
//        Log.d(TAG, "setColors called for background");
        int initialIndex = position * 16;
        // Again position will be 0 for us, for now atleast.
        int index = initialIndex;

        colorDataBuffer.position(index);
        colorDataBuffer.put(colors);
        colorDataBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 16 * BYTES_PER_FLOAT, colorDataBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 16 * 2 * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void pushColors(float [] colors, int position, int size) {
        int initialIndex = position * 16;
        // Again position will be 0 for us, for now atleast.
        int index = initialIndex;

        float [] temp = new float[size];
        colorDataBuffer.get(temp, index, size);

        colorDataBuffer.position((initialIndex + 1) * 16);
        colorDataBuffer.put(temp);

        colorDataBuffer.position(index);
        colorDataBuffer.put(colors);

        colorDataBuffer.position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 16 * BYTES_PER_FLOAT, colorDataBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 16 * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    void disableScissor() {
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public void enableScissor() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
    }

    boolean setScissor(int x, int y, int width, int height) {
        if (x < 0) {
            width += x;
            x = 0;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }

        if (VERBOSE) Log.d(TAG, "Enabling Scissor : " + "x " + x + " y " + y + " width " + width + " height " + height);
        GLES20.glScissor(x, y, width, height);

        return true;
/*        }
        return false;*/
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        mGlUtil.checkGlError("before glCreateShader" + shaderType);
        if (VERBOSE) Log.d(TAG, "Shader source: \n" + source);
        int shader = GLES20.glCreateShader(shaderType);
//        checkGlError("glCreateShader type=" + shaderType);
        if (shader == 0) {
            Log.e(TAG, "Error while creating shader");
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        } else {
            if (VERBOSE) Log.d(TAG, "Shader compilation successful");
        }
        return shader;
    }

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        if (VERBOSE) Log.d(TAG, "both shaders created successfully");
        int program = GLES20.glCreateProgram();
        mGlUtil.checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        mGlUtil.checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        mGlUtil.checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        if (program != 0) {
            if (VERBOSE) Log.d(TAG, "Program created successfully");
        }
        return program;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    void bindTexture(int texture) {
        mTextureCache.bindTexture(texture);
    }

    void bindTexture(int textureTarget, int texture) {
        mTextureCache.bindTexture(textureTarget, texture);
    }

    void bindTexture(int target, int texture, int textureUnit) {
        mTextureCache.bindTexture(target, texture, textureUnit);
    }

    public static boolean checkGlError(String msg) {
        return mGlUtil.checkGlError(msg);
    }

/*    public Texture getTexture(boolean reuse) {
        return mTextureCache.get(reuse);
    }*/

    public FBObject getFrameBufferObject(int handle) {
        return mFBCache.get(handle);
    }

    public void putFrameBufferObject(FBObject fbObject) {
        mFBCache.put(fbObject.getFrameBufferId(), fbObject);
    }

    public Texture getTexture(Buffer buffer) {
        return mTextureCache.getTexture(buffer);
    }

    public Texture getTexture(Bitmap bitmap) {
        Texture texture = null;
        synchronized (bitmap) {
            if (!bitmap.isRecycled()) {
                texture = mTextureCache.getTexture(bitmap);
            }
        }
        return texture;
    }

    public Texture getFBOTexture(int width, int height) {
        return mTextureCache.createFBOTexture(width, height);
    }

    public Texture getFBOTexture(FBObject fbObject) {
        return mTextureCache.getFBOTexture(fbObject.mTextureId);
    }

    public Texture getFBOTexture(int textureId) {
        return mTextureCache.getFBOTexture(textureId);
    }

    public Texture getExternalTexture(boolean reuse) {
        return mTextureCache.getExternalTexture(-1);
    }

    public Texture getExternalTexture(int textureId) {
        return mTextureCache.getExternalTexture(textureId);
    }

    Texture getTexture(ImageSource imageSource) {
        Texture texture = null;
        switch (imageSource.mSourceType) {
            case Drawable2d.SHADING_SOURCE_BITMAP:
            case Drawable2d.SHADING_SOURCE_GIF:
                texture = getTexture(imageSource.mBitmap);
                break;
            case Drawable2d.SHADING_SOURCE_RAW_TEXTURE:
                texture = getTexture(imageSource.mBuffer);
                break;
            case Drawable2d.SHADING_SOURCE_FBO:
                texture = getFBOTexture(imageSource.mFBSource);
                break;
            case Drawable2d.SHADING_SOURCE_EXTERNAL_TEXTURE:
                texture = getExternalTexture(true);
            default:
                break;
        }
        return texture;
    }

    public void activeTexture(int textureUnit) {
        mTextureCache.activeTexture(textureUnit);
    }

    public void bindTexture(Texture texture, int textureParameterSymbol) {
        mTextureCache.bindTexture(texture, textureParameterSymbol);
    }

    public void unbindTexture() {
        mTextureCache.unbindTexture();
    }

    public Program getProgram(ProgramCache.ProgramDescription description) {
        return mProgramCache.get(description);
    }

    public Program getProgram(String vertexShader, String fragmentShader) {
        return mProgramCache.get(vertexShader, fragmentShader);
    }

    public void releaseProgram(Program program) {
        mProgramCache.release(program);
    }

    public void deleteFBOTexture(int frameBufferTexture) {
        mTextureCache.deleteFBO(frameBufferTexture);
    }

    public synchronized BaseFilter getFilter(ProgramCache.ProgramDescription description) {
        BaseFilter filter = mFilterCache.get(description.toString());
        return filter;
    }

    public synchronized BaseFilter createFilter(ProgramCache.ProgramDescription description) {
        BaseFilter filter = generateFilter(description);
        mFilterCache.put(description.toString(), filter);
        return filter;
    }

    private BaseFilter generateFilter(ProgramCache.ProgramDescription description) {
        BaseFilter filter = FilterManager.getFilter(description);
        // very important to clone the description here.
        // The description comes from drawable and can change in future, but the filter needs it in its current state.
        filter.setDescription(ProgramCache.ProgramDescription.clone(description));
        filter.setup(this);
        return filter;
    }

    public static Bitmap getBitmap(String key) {
        synchronized (bitmapCache) {
            return bitmapCache.getBitmap(key);
        }
    }

    public static void putBitmap(String key, Bitmap bitmap) {
        synchronized (bitmapCache) {
            bitmapCache.put(key, bitmap);
        }
    }

    public static final LruBitmapCache bitmapCache = new LruBitmapCache(GraphicsConsts.BITMAP_CACHE_SIZE);

    public static class LruBitmapCache extends LruCache<String, Bitmap> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruBitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        synchronized protected void entryRemoved (boolean evicted, String key, Bitmap bitmap, Bitmap newBitmap) {
            if (VERBOSE) Log.d(TAG, "Evicting bitmap " + bitmap + " sourced from key:" + key + "current size: " + size());
            synchronized (bitmap) {
                bitmap.recycle();
            }
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getWidth() * bitmap.getHeight() * 4;
        }

        public Bitmap getBitmap(String key) {
            Bitmap bitmap = get(key);
            if (AVUtils.VERBOSE) Log.d(TAG, "Returning bitmap: " + bitmap + " for key: " + key);
            return bitmap;
        }
    }
}
