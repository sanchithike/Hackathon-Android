package com.roposo.creation.graphics.filters;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache.ProgramDescription;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author bajaj on 05/10/17.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class BaseFilter implements Cloneable {
    static int count;

    String TAG = "BaseFilter";

    private static final boolean VERBOSE = false; // || GraphicsUtils.VERBOSE;

    private final ConcurrentLinkedQueue<Runnable> mRunOnDraw = new ConcurrentLinkedQueue<>();

    String mVertexShader;
    String mFragmentShader;

    boolean mIsInitialized;

    private HashSet<Integer> mBoundAttribslots = new HashSet<>(5);

    public List<String> mFilterMode = new ArrayList<>(3);

    Program mProgram;

    volatile float mBlendFactor = 1.0f;

    ProgramDescription mDescription;

    private static final String sTAG = "BaseFilters";
    long mTimestamp;
    Integer resourceId;

    float mSaturation = 1.0f;
    float mBrightness = 0.0f;
    float mContrast = 1.0f;

    BaseFilter() {
        resourceId = null;
    }

    abstract void init();

    BaseFilter(final String vertexShader, final String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    protected BaseFilter clone() throws CloneNotSupportedException {
        return (BaseFilter) super.clone();
    }

    public BaseFilter copy() {
        BaseFilter filter;
        try {
            filter = clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        filter.copy(this);
        return filter;
    }

    @CallSuper
    void copy(BaseFilter filter) {
        mVertexShader = filter.mVertexShader;
        mFragmentShader = filter.mFragmentShader;

        mIsInitialized = filter.mIsInitialized;

        mBoundAttribslots = new HashSet<>();

        mFilterMode = new ArrayList<>();
        mFilterMode.addAll(filter.mFilterMode);

        mProgram = null;

        mBlendFactor = filter.mBlendFactor;

        setDescription(filter.mDescription);
    }

    public void setup(Caches cache) {
        cleanup();

/*        if (mProgram == null) {*/
        // Wizard mode (concatenated strings based on description in ProgramCache)
        if (mDescription != null) {
            mProgram = cache.getProgram(mDescription);
            mVertexShader = mProgram.getVertexShader();
            mFragmentShader = mProgram.getFragmentShader();
        } else if (mVertexShader != null && mFragmentShader != null) {
            mProgram = cache.getProgram(mVertexShader, mFragmentShader);
        } else {
            if (VERBOSE) Log.w(TAG, this + " vertex shader/fragment shader is null");
        }
/*        }*/
        Caches.checkGlError("before program use");
        if (mProgram != null) {
            mProgram.use();
            mIsInitialized = true;
        }
        Caches.checkGlError("after program use");
    }

    public void cleanup() {
        mIsInitialized = false;
        mRunOnDraw.clear();
        mBoundAttribslots.clear();

        onDestroy();
    }

    public void onDestroy() {
    }

    public void onOutputSizeChanged(final int width, final int height) {
    }

    public void draw(Caches cache) {
        boolean result = onDrawPre(cache);
        if (!result) {
            if (VERBOSE) Log.w(TAG, "onDrawPre: return from draw");
            return;
        }
        onDraw();

        onDrawPost();
    }

    public boolean onDraw() {
        if (!mIsInitialized) {
            return false;
        }

        return true;
    }

    public boolean onDrawPre(Caches cache) {
        if (mProgram == null) return false;
        if (mProgram.mProgramHandle < 0) {
            setup(cache);
        }
        mProgram.use();

        runPendingOnDrawTasks();

        return true;
    }

    public void onDrawPost() {
        for (int slot : mBoundAttribslots) {
            GLES20.glDisableVertexAttribArray(slot);
        }
    }

    void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            Runnable runnable = mRunOnDraw.poll();
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    final boolean isInitialized() {
        return mIsInitialized;
    }

    public Program getProgram() {
        return mProgram;
    }

    void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.offer(runnable);
        }
    }

    public static String loadShader(String file, Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream ims = assetManager.open("shaders/" + file);

            String re = convertStreamToString(ims);
            ims.close();
            return re;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public synchronized void setDescription(ProgramDescription description) {
        mDescription = description;
    }

    public void setBlendFactor(float blendFactor) {
        mBlendFactor = blendFactor;
    }

    public synchronized ProgramDescription getDescription() {
        return mDescription;
    }

    void generateVariableValues() {

    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
        generateVariableValues();
    }

    public void setBrightness(float brightness) {
        mBrightness = brightness;
    }

    public void setContrast(float contrast) {
        mContrast = contrast;
    }

    public void setSaturation(float saturation) {
        mSaturation = saturation;
    }


}