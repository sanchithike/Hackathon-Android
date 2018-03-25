package com.roposo.creation.graphics.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import android.graphics.RectF;
import android.util.SparseArray;

import com.roposo.core.kotlinExtensions.CollectionsExtensionsKt;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.EventTrackUtil;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.filters.CompositeFilter;
import com.roposo.creation.graphics.filters.ImageAdjustmentFilter;
import com.roposo.creation.graphics.filters.ImageFilter;
import com.roposo.creation.graphics.filters.LutFilter;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.OpenGLRenderer.Fuzzy;
import com.roposo.creation.graphics.gles.RenderTarget;
import com.roposo.creation.graphics.gles.TreeNode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

/**
 * @author bajaj on 10/11/17.
 */

public class Scene implements Drawable.LifecycleListener, Cloneable, ISceneAdjustments {
    private static final String TAG = "Scene";
    private static boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    public ArrayList<Drawable> mRootDrawables = new ArrayList<>();

    private long mFirstFrameTs = -1;
    private long mTimeStampMs;
    public SceneManager.SceneDescription currentSceneDescription;
    boolean rotateDirty;
    boolean zoomDirty;
    boolean panDirty;
    protected boolean lutDirty;

    protected long mPreferredCaptureTimestamp = 500;

    private LinkedList<Long> recentFrameTs;

    public Scene(SceneManager.SceneDescription sceneDescription) {
        this(1, sceneDescription);
    }

    public Scene(int numDrawables, SceneManager.SceneDescription sceneDescription) {
        mRootDrawables.ensureCapacity(numDrawables);
        for (int i = 0; i < numDrawables; i++) {
            Drawable2d drawable = new Drawable2d();
            drawable.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
            mRootDrawables.add(drawable);
        }

        if (VERBOSE) {
            recentFrameTs = new LinkedList<>();
        }

        this.currentSceneDescription = sceneDescription;
    }

    public Scene(Drawable root, SceneManager.SceneDescription sceneDescription) {
        mRootDrawables.add(root);
        this.currentSceneDescription = sceneDescription;
    }

    public void onDraw(Fuzzy renderTargetType, long timeStampMs) {
        timeStampMs = onPreDraw(renderTargetType, timeStampMs);

        if (VERBOSE) {
            recentFrameTs.add(timeStampMs);
            if (recentFrameTs.size() > 10) {
                Log.d("TVS", "fps: " + 1000 * 10f / (timeStampMs - recentFrameTs.pollFirst()));
            }
        }

        mTimeStampMs = timeStampMs;
        draw(renderTargetType, timeStampMs);
        onPostDraw(renderTargetType, timeStampMs);
    }

    private void onPostDraw(Fuzzy renderTargetType, long timeStampMs) {
    }

    long onPreDraw(Fuzzy renderTargetType, long timeStampMs) {
        if (timeStampMs <= 0) {
            timeStampMs = 0;
        }

        if ((mFirstFrameTs < 0 || timeStampMs == 0) && renderTargetType != Fuzzy.OFFSCREEN) {
            mFirstFrameTs = timeStampMs;
        }
        long newTimeStamp = timeStampMs - mFirstFrameTs;
        if (newTimeStamp < 0) {
            HashMap<String, String> map = new HashMap<>();
            map.put("timestamp", String.valueOf(timeStampMs));
            map.put("returnedtimestamp", String.valueOf(newTimeStamp));
            EventTrackUtil.logDebug("onPreDraw","InvalidTimestamp", "Scene", map, 4);
            newTimeStamp = 0;
        }

        setLutInScene();
        return newTimeStamp;
    }

    public void draw(Fuzzy renderTargetType, long timeStampMs) {
        if (mRootDrawables.isEmpty()) {
            return;
        }

        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);
        renderer.drawFrame(mRootDrawables.get(0));
    }

    public void onProgress(long timeStampUs) {

    }

    public long getCaptureTimestamp() {
        return mPreferredCaptureTimestamp;
    }

    @Override
    public Scene clone() {
        Scene clone;
        try {
            clone = (Scene) super.clone();
            clone.mRootDrawables = new ArrayList<>(mRootDrawables.size());
            for (Drawable mRootDrawable : mRootDrawables) {
                clone.mRootDrawables.add(mRootDrawable.clone());
            }

            if (VERBOSE) {
                clone.recentFrameTs = new LinkedList<>();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onReady() {
        mFirstFrameTs = -1;
        if (mRootDrawables.isEmpty()) {
            return;
        }

        for (Drawable rootDrawable : mRootDrawables) {
            rootDrawable.setLifecycleListener(this, true);
        }
    }

    @Override
    public void onMeasure(RenderTarget renderTarget, Drawable drawable, double width, double height) {
        if (drawable instanceof Drawable2d && !(((Drawable2d)drawable).getImageSources().isEmpty()) && ((Drawable2d)drawable).getImageSources().get(0).getShadingSourceType() == Drawable2d.SHADING_SOURCE_EXTERNAL_TEXTURE) {
            renderTarget.setScaleParams((float) width, (float) height);
        }
    }

    @Override
    public void onFilterSetup(RenderTarget renderTarget, Drawable drawable, BaseFilter filter) {

    }

    @Override
    public void onFilterPredraw(OpenGLRenderer.Fuzzy renderTargetType, RenderTarget renderTarget, Drawable drawable, BaseFilter filter) {
        filter.setTimestamp(mTimeStampMs);
        checkFiltersAndUpdateParams(filter);
    }

    private void checkFiltersAndUpdateParams(BaseFilter filter) {
        List<String> filterModes = filter.mFilterMode;
        if (filterModes.contains(FilterManager.IMAGE_ADJUSTMENT_FILTER) ||
            filterModes.contains(FilterManager.LUT_FILTER))
        {
            if (filter instanceof CompositeFilter) {
                ImageFilter[] allFilters = ((CompositeFilter)filter).getFilters();
                for (ImageFilter imageFilter : allFilters) {
                    if (imageFilter instanceof ImageAdjustmentFilter) {
                        setImageAdjustmentParameters((ImageAdjustmentFilter)imageFilter);
                    }
                    else if (imageFilter instanceof LutFilter) {
                        setLutFilterParameters((LutFilter)imageFilter);
                    }
                }
            }
            else if (filter instanceof ImageAdjustmentFilter) {
                setImageAdjustmentParameters((ImageAdjustmentFilter) filter);
            }
            else if (filter instanceof LutFilter) {
                setLutFilterParameters((LutFilter) filter);
            }
        }
    }

    private void setImageAdjustmentParameters(ImageAdjustmentFilter filter) {
        filter.setSaturation(this.currentSceneDescription.getSaturation());
        filter.setContrast(this.currentSceneDescription.getContrast());
        filter.setBrightness(this.currentSceneDescription.getBrightness());
    }

    private void setLutFilterParameters(LutFilter filter) {
        filter.setIntensity(this.currentSceneDescription.getLutIntensity());
        filter.setTransitionPoint(this.currentSceneDescription.getLutTransitionPoint());
        filter.setLeftLutFlag(this.currentSceneDescription.getLeftLutFlag());
        filter.setRightLutFlag(this.currentSceneDescription.getRightLutFlag());
    }

    @Override
    public void setSaturation(float saturation) {
        this.currentSceneDescription.setSaturation(saturation);
    }

    @Override
    public void setContrast(float contrast) {
        this.currentSceneDescription.setContrast(contrast);
    }

    @Override
    public void setBrightness(float brightness) {
        this.currentSceneDescription.setBrightness(brightness);
    }

    @Override
    public void setSceneRotateInZ(double sceneRotateInZ) {
       this.currentSceneDescription.setSceneRotateInZ(sceneRotateInZ);
       this.rotateDirty = true;
    }

    @Override
    public void setSceneZoom(double[] sceneZoom) {
        this.currentSceneDescription.setSceneZoom(sceneZoom);
        this.zoomDirty = true;
    }

    @Override
    public void setSceneTranslateByInfo(double[] sceneTranslateByInfo) {
        this.currentSceneDescription.setSceneTranslateByInfo(sceneTranslateByInfo);
        this.panDirty = true;
    }

    @Override
    public void setSceneRectFInfo(RectF rectInfo) {
        this.currentSceneDescription.setSceneRectfInfo(rectInfo);
    }

    @Override
    public SceneManager.SceneDescription cropMedia() {
        return null;
    }


    protected Drawable2d createSceneDrawableFromSceneDescription(ArrayList<ImageSource> imageSources,
                                                                 SceneManager.SceneDescription sceneDescription) {
        Drawable2d drawable2d = Drawable2d.create(imageSources);
        SceneManager.LutDescription leftLutDescription = sceneDescription.getLeftLutDescription();
        SceneManager.LutDescription rightLutDescription = sceneDescription.getRightLutDescription();

        if (leftLutDescription != null || rightLutDescription != null) {
            addLut(leftLutDescription != null ? leftLutDescription : SceneManager.LutDescription.DEFAULT,
                    rightLutDescription != null ? rightLutDescription : SceneManager.LutDescription.DEFAULT,
                    sceneDescription.getLutTransitionPoint(),
                    sceneDescription.getLutIntensity());
        }

        return drawable2d;
    }

    /********************************* LUT Related Functions ***************************************/

    @Override
    public void addLut(@NotNull SceneManager.LutDescription leftLut,
                       @NotNull SceneManager.LutDescription rightLut,
                       float lutTransitionPoint,
                       float intensity) {
        if (mRootDrawables.size() > 0) {
            setLeftLutFlag(leftLut);
            setRightLutFlag(rightLut);
            setLutIntensity(intensity);
            setLutTransitionPoint(lutTransitionPoint);
            lutDirty = true;
        }
    }

    private void setLeftLutFlag(SceneManager.LutDescription leftLutDescription) {
        this.currentSceneDescription.setLeftLutDescription(leftLutDescription);
        this.currentSceneDescription.setLeftLutFlag(leftLutDescription != SceneManager.LutDescription.DEFAULT ? 1 : 0);
    }

    private void setRightLutFlag(SceneManager.LutDescription rightLutDescription) {
        this.currentSceneDescription.setRightLutDescription(rightLutDescription);
        this.currentSceneDescription.setRightLutFlag(rightLutDescription != SceneManager.LutDescription.DEFAULT ? 1 : 0);
    }

    private void setLutIntensity(float lutIntensity) {
        this.currentSceneDescription.setLutIntensity(lutIntensity);
    }

    private void setLutTransitionPoint(float lutTransitionPoint) {
        this.currentSceneDescription.setLutTransitionPoint(lutTransitionPoint);
    }

    protected void setLutInScene() {
        if (lutDirty && mRootDrawables.size() > 0) {
            for (int i = 0 ; i < mRootDrawables.size(); i++) {
                setLutInDrawableTree((Drawable2d) mRootDrawables.get(i));
            }
            lutDirty = false;
        }
    }

    protected void setLutInDrawableTree(Drawable2d drawable2d) {
        if (!drawable2d.hasChildren()) {
            setLutPropertiesFromSceneDescription(drawable2d);
            return;
        }

        SparseArray<TreeNode> children = drawable2d.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Drawable2d drawable = (Drawable2d) children.get(i);
            setLutInDrawableTree(drawable);
        }
    }

    private void setLutPropertiesFromSceneDescription(Drawable2d childDrawable) {
        SceneManager.LutDescription leftLut = currentSceneDescription.getLeftLutDescription();
        SceneManager.LutDescription rightLut = currentSceneDescription.getRightLutDescription();

        ImageSource leftImageSource;
        leftImageSource = new ImageSource(SceneManager.getBitmapFrom(leftLut.getResourceId()), true);

        ImageSource rightImageSource;
        rightImageSource = new ImageSource(SceneManager.getBitmapFrom(rightLut.getResourceId()), true);

        List<ImageSource> childDrawableImageSources = new ArrayList<>(childDrawable.getImageSources());

        if (!CollectionsExtensionsKt.isNullOrEmpty(childDrawableImageSources)) {
            ImageSource baseImageSource = childDrawableImageSources.get(0);
            childDrawable.setImageSource(baseImageSource);
            for (int i = 1; i < childDrawableImageSources.size(); i++) {
                ImageSource imageSource = childDrawableImageSources.get(i);
                if (!imageSource.isLUT()) {
                    childDrawable.addImageSource(imageSource);
                }
            }

            childDrawable.addImageSource(leftLut != SceneManager.LutDescription.DEFAULT ? leftImageSource : rightImageSource);
            childDrawable.addImageSource(rightLut != SceneManager.LutDescription.DEFAULT ? rightImageSource : leftImageSource);

            List<String> filterModes = childDrawable.getColorFilterMode();
            if (!filterModes.contains(FilterManager.LUT_FILTER)) {
                filterModes.add(FilterManager.LUT_FILTER);
            }
        }

    }

    private void checkLutAndAddFilters(Drawable2d drawable2d, List<String> filterModes) {
        SceneManager.LutDescription leftLutDescription = this.currentSceneDescription.getLeftLutDescription();
        SceneManager.LutDescription rightLutDescription = this.currentSceneDescription.getRightLutDescription();

        List<String> newFilterModes = new ArrayList<>();
        newFilterModes.addAll(filterModes);

        if ((leftLutDescription != null && leftLutDescription != SceneManager.LutDescription.DEFAULT) ||
                (rightLutDescription != null && rightLutDescription != SceneManager.LutDescription.DEFAULT)) {

            //TODO: Contains on a list needs to be changed, maybe drawable needs to have a support for set
            if (!newFilterModes.contains(FilterManager.LUT_FILTER)) {
                newFilterModes.add(FilterManager.LUT_FILTER);
            }
        }

        drawable2d.setFilterMode(newFilterModes);
    }

    /******************************** Lut Related methods ends ************************************/

    /**
     * Recursively checks the drawable for children , if it's a leaf level
     * drawable it sets the filter value to it
     *
     * @param drawable2d The drawable to which we want to apply the filter
     * @param filterMode The filter to be applied
     */
    protected void setFilterMode(Drawable2d drawable2d, String filterMode) {
        setFilterMode(drawable2d, Arrays.asList(filterMode));
    }


    protected void setFilterMode(Drawable2d drawable2d, List<String> filterModes) {
        if (!drawable2d.hasChildren()) {
            checkLutAndAddFilters(drawable2d, filterModes);
            return;
        }

        SparseArray<TreeNode> children = drawable2d.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Drawable2d drawable = (Drawable2d) children.get(i);
            setFilterMode(drawable, filterModes);
        }
    }
}
