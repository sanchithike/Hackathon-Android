package com.roposo.creation.graphics.filters;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.ProgramCache;
import com.roposo.creation.graphics.gles.Texture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bajaj on 08/10/17.
 */
public class FilterGroup extends ImageFilter {
    private List<BaseFilter> mFilters;
    private List<BaseFilter> mMergedFilters;
    private int[] mFrameBuffers;

    private int[] mFrameBufferTextures;

    private int mFBORenderScale = 1;

    private final boolean USE_DEPTH_BUFFER = false;
    private int mScaledWidth;
    private int mScaledHeight;
    private Bitmap[] mBitmaps;

    private int SURFACE_SIZE_FACTOR = 1;
    // Some hardwares might want to be multiples of some powers of 2 may be 8, 16 or 32

    /**
     * Instantiates a new FilterGroup with no filters.
     */
    public FilterGroup() {
        this(null);
    }

    public FilterGroup(int sizeFactor) {
        this(null);
        mFBORenderScale = sizeFactor;
    }

    /**
     * Instantiates a new FilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    public FilterGroup(List<BaseFilter> filters) {
        mFilters = filters;
        if (mFilters == null) {
            mFilters = new ArrayList<>();
        } else {
            updateMergedFilters();
        }
        init();
    }


    @Override
    void copy(BaseFilter baseFilter) {
        super.copy(baseFilter);

        FilterGroup filterGroup = (FilterGroup) baseFilter;

        mFBORenderScale = filterGroup.mFBORenderScale;
        mScaledWidth = filterGroup.mScaledWidth;
        mScaledHeight = filterGroup.mScaledHeight;

        if (filterGroup.mFilters != null) {
            mFilters = new ArrayList<>(filterGroup.mFilters.size());
            for (BaseFilter filter: filterGroup.mFilters) {
                mFilters.add(filter.copy());
            }
        }

        /*if (filterGroup.mMergedFilters != null) {
            mMergedFilters = new ArrayList<>(filterGroup.mMergedFilters.size());
            for (BaseFilter filter: filterGroup.mMergedFilters) {
                mMergedFilters.add(filter.copy());
            }
        }

        if (filterGroup.mFrameBuffers != null) {
            filterGroup.mFrameBuffers = Arrays.copyOf(filterGroup.mFrameBuffers, filterGroup.mFrameBuffers.length);
        }

        if (filterGroup.mFrameBufferTextures != null) {
            filterGroup.mFrameBufferTextures = Arrays.copyOf(filterGroup.mFrameBufferTextures, filterGroup.mFrameBufferTextures.length);
        }*/

        if (filterGroup.mBitmaps != null) {
            filterGroup.mBitmaps = Arrays.copyOf(filterGroup.mBitmaps, filterGroup.mBitmaps.length);
        }
    }

    @Override
    void init() {
        TAG = "FilterGroup";
    }

    public void addFilter(BaseFilter filter) {
        if (filter == null) {
            return;
        }
        mFilters.add(filter);
        updateMergedFilters();
    }

/*    @Override
    public void setup(Caches cache) {
        mCache = cache;
        Log.d(TAG, "setup filter: " + this);
        if (mDescription == null) {
            Log.w(TAG, "Exit! Description is null");
            return;
        }
        int size = mMergedFilters.size();
        for (int i = 0; i < mMergedFilters.size(); i++) {
            BaseFilter filter = mMergedFilters.get(i);
            ProgramCache.ProgramDescription description = ProgramCache.ProgramDescription.clone
                    (mDescription);
            description.setFilterMode(filter.mFilterMode);

            boolean isLast = (i == size - 1);
            boolean isFirst = (i == 0);
            description.mOffscreenSurface = !isLast;
            if (isFirst) {
                description.isFBOBased = false;
            } else {
                description.isFBOBased = true;
                description.hasTexture = true;
                description.hasExternalTexture = false;
            }
            filter.setDescription(description);
            filter.setPreScale(sScale);
        }

        if (mScaledWidth > 0 && mScaledHeight > 0) {
            if (mMergedFilters != null && mMergedFilters.size() > 0) {
                size = mMergedFilters.size();
                mFrameBuffers = new int[size - 1];
                mFrameBufferTextures = new int[size - 1];

                for (int i = 0; i < size - 1; i++) {
                    Texture texture = mCache.getFBOTexture(mScaledWidth, mScaledHeight);
                    texture.markUse();
                    mFrameBufferTextures[i] = texture.id;
                    createFrameBufferObject(texture.id, mFrameBuffers, i);
                }

                for (int i = 0; i < mMergedFilters.size(); i++) {
                    BaseFilter baseFilter = mMergedFilters.get(i);
                    if (i == 0) {
                        baseFilter.setSourceTexture(-1);
                    } else {
                        *//*if (mClipRect != null)
                            baseFilter.setScaleFactors(mClipRect.width() / (float) mOutputWidth
                                    , (float) mClipRect.height() / mOutputHeight);
                        baseFilter.setSourceTexture(mFrameBufferTextures[i - 1]);*//*
                    }
                    baseFilter.setup(cache);
                }
            }
        }

        mIsInitialized = true;
    }

    @Override
    public void onDestroy() {
        destroyFramebuffers();
        for (BaseFilter filter : mFilters) {
            filter.cleanup();
        }
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        Log.d(TAG, "Destroy framebuffers");
        if (mFrameBufferTextures != null) {
            for (int frameBufferTexture : mFrameBufferTextures) {
                Texture texture = mCache.getFBOTexture(frameBufferTexture);
                mCache.deleteFBOTexture(frameBufferTexture);
            }
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);

        mScaledWidth = width / mFBORenderScale;
        mScaledHeight = height / mFBORenderScale;

        // Conform to boundary alignment and size rules (multiple of 16, may be)
        mScaledWidth = (mScaledWidth * SURFACE_SIZE_FACTOR + (SURFACE_SIZE_FACTOR - 1)) /
                SURFACE_SIZE_FACTOR;
        mScaledHeight = (mScaledHeight * SURFACE_SIZE_FACTOR + (SURFACE_SIZE_FACTOR - 1)) /
                SURFACE_SIZE_FACTOR;

        if (mFrameBuffers != null) {
            destroyFramebuffers();
        }

        int size = mMergedFilters.size();
        for (int i = 0; i < size; i++) {
            mMergedFilters.get(i).onOutputSizeChanged(width, height);
        }
    }


    @Override
    public boolean onDrawPre() {
        return true;
    }

    @Override
    public void onDrawPost() {

    }

    public void draw(Drawable drawable) {
        onDraw(drawable);
    }

    @Override
    boolean onDraw(Drawable drawable) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return false;
        }
        if (mMergedFilters != null) {
            int size = mMergedFilters.size();
            for (int i = 0; i < size; i++) {
                BaseFilter filter = mMergedFilters.get(i);
                boolean isNotLast = i < size - 1;
                if (isNotLast) {
                    Log.d(TAG, "For filter: " + filter + " \t" + i + " Bind framebuffer: " +
                            mFrameBuffers[i]);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                    int fbStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                    if (fbStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                        Log.e(TAG, "fb status: " + Integer.toHexString(fbStatus));
                    }
                    GLES20.glClearColor(0.71f, 0.6f, 0.0f, 0.7f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    if (mFBORenderScale != 1) {
                        GLES20.glViewport(0, 0, mScaledWidth, mScaledHeight);
                    }
                } else {
//                    GLES20.glClearColor(0.61f, 0.06f, 0.7f, 0.7f);
//                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    if (mFBORenderScale != 1) {
                        GLES20.glViewport(0, 0, drawable.mOutputWidth, drawable.mOutputHeight);
                        Caches.checkGlError("filtergroup: after glViewport");
                    }
                }

                boolean result = filter.onDrawPre();
                if (!result) {
                    Log.w(TAG, "returning from onDrawPre");
                    continue;
                }
//                computeTextureCoordinates(i);
                result = filter.onDraw(drawable);

                if (!result) {
                    Log.w(TAG, "returning from onDraw");
                    continue;
                }
                filter.onDrawPost();
*//*                Bitmap bitmap = OpenGLRenderer.sPreviewGLRenderer.createBitmapFromGLSurface();
                Log.d(TAG, "captured: " + bitmap);*//*

                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                    previousTexture = mFrameBufferTextures[i];
                }
                GLES20.glFlush();
            }
        }
        return true;
    }*/
    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<BaseFilter> getFilters() {
        return mFilters;
    }

    public List<BaseFilter> getMergedFilters() {
        return mMergedFilters;
    }

    @Override
    public void setDescription(ProgramCache.ProgramDescription description) {
        mDescription = description;
    }

    private void updateMergedFilters() {
        if (mFilters == null) {
            return;
        }

        if (mMergedFilters == null) {
            mMergedFilters = new ArrayList<>();
        }
        mMergedFilters.clear();

        List<BaseFilter> filters;
        for (BaseFilter filter : mFilters) {
            if (filter instanceof FilterGroup) {
                ((FilterGroup) filter).updateMergedFilters();
                filters = ((FilterGroup) filter).getMergedFilters();
                if (filters == null || filters.isEmpty()) {
                    continue;
                }
                mMergedFilters.addAll(filters);
                continue;
            }
            mMergedFilters.add(filter);
        }
    }
}
