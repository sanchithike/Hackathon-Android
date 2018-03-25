package com.roposo.creation.graphics.gles;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.gles.ProgramCache.ProgramDescription;

/**
 * Created by bajaj on 14/07/16.
 */
public class Drawable extends TreeNode {
    private static final String TAG = Drawable.class.getSimpleName();
    public static int sIncomingWidth;
    public static int sIncomingHeight;
    private final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    static float[] sDefaultTextureTransform = new float[16];
    double[] mTextureTransform = new double[16];

    /**
     * Set to true if this drawable's properties get changed. Will lead to invalidation and recomputation of all its children.
     */
    protected boolean mIsDirty;

    /**
     * Set to true if any of the children (anywhere down the hierarchy) has been dirtied.
     * A dirty drawable is always invalid. But the opposite is not true. (Minimizes the if checks, when traversing down the tree)
     */
    protected boolean mIsInvalidated;

    protected  boolean mPathDirty;

    protected boolean mVisible;
    boolean mActive = true;

    //Generic drawable class.

    ProgramDescription mDescription;
    // The information the renderer needs to know to choose the program to use to render this drawable.

    private float mAlpha;
    private RectF mRectF;
    double[] mRotateM = new double[]{0.0f, 0.0f, 0.0f};
    double[] mFinalScaleM = new double[]{1.0f, 1.0f, 1.0f};
    double[] mDefaultTranslateM = new double[]{0.0f, 0.0f, 0.0f};
    double[] mTranslateM = new double[]{0.0f, 0.0f, 0.0f};
    double[] mDefaultScale = new double[]{1.0f, 1.0f, 1.0f};
    double[] mScaleM = new double[]{1.0f, 1.0f, 1.0f};
    double[] mDefaultZoom = new double[]{1.0f, 1.0f, 1.0f};
    double[] mZoomM = new double[]{1.0f, 1.0f, 1.0f};
    double[] mDefaultGeometryScale = new double[]{1.0f, 1.0f, 1.0f};
    double[] mGeometryScale = new double[]{1.0f, 1.0f, 1.0f};

    float[] mModelMatrix = new float[16];

    public float[] mMVPMatrix = new float[16];

    public Rect mClipRect = new Rect();

    int mPositionIndex = 0;
    int mSourceTexture = -1;
    private boolean fucked;

    LifecycleListener mLifecycleListener;

    public Drawable() {
        super();
        mDescription = new ProgramDescription();

        setTranslateDevicecoords(0.5f, 0.5f, 0.0f);
        setGeometryScale(1.0f, 1.0f, 1.0f);
        setDefaultGeometryScale(1.0f, 1.0f, 1.0f);
        setScale(1.0f, 1.0f);
        setZoom(1.0f, 1.0f, 1.0f);

        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        mAlpha = 1.0f;
        mVisible = true;

        mIsDirty = true;
        mIsInvalidated = true;
        mPathDirty = true;
    }

    public Drawable(Drawable drawable) {
        copy(drawable);
    }

    void setupDraw() {

    }

    void draw() {

    }

    void onDraw() {

    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    public double[] getRotate() {
        return mRotateM;
    }

    public double[] getDefaultZoom() {
        return mDefaultZoom;
    }

    public double[] getZoom() {
        return mZoomM;
    }

    public RectF getBounds() {
        return mRectF;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public double[] getDefaultTranslate() {
        return mDefaultTranslateM;
    }

    public double[] getTranslate() {
        return mTranslateM;
    }

    public double[] getRawScale() {
        return mScaleM;
    }

    public double[] getScale() {
        double[] finalScale = new double[3];
        finalScale[0] = mFinalScaleM[0] * mZoomM[0];
        finalScale[1] = mFinalScaleM[1] * mZoomM[1];
        finalScale[2] = mFinalScaleM[2] * mZoomM[2];
        return finalScale;
    }

    public void setTranslateDeviceCoords(double[] translate, double[] offset) {
        mTranslateM = translate.clone();
        mTranslateM[0] += offset[0];
        mTranslateM[1] += offset[1];
        mTranslateM[2] += offset[2];

        mTranslateM[0] = 2 * mTranslateM[0] - 1.0f;
        mTranslateM[1] = 1.0f - 2.0f * mTranslateM[1];
    }

    void setGeometryScale(double scaleX, double scaleY) {
        setGeometryScale(scaleX, scaleY, 1.0f);
    }

    public void setDefaultGeometryScale(double scaleX, double scaleY) {
        setDefaultGeometryScale(scaleX, scaleY, 1.0f);
    }

    void setGeometryScale(double[] scale) {
        mGeometryScale[0] = scale[0];
        mGeometryScale[1] = scale[1];
        mGeometryScale[2] = scale[2];
        setInvalidRecursive(true);
    }

    void setGeometryScale(double scaleX, double scaleY, double scaleZ) {
        mGeometryScale[0] = scaleX;
        mGeometryScale[1] = scaleY;
        mGeometryScale[2] = scaleZ;
        setInvalidRecursive(true);
    }

    void setDefaultGeometryScale(double scaleX, double scaleY, double scaleZ) {
        mDefaultGeometryScale[0] = scaleX;
        mDefaultGeometryScale[1] = scaleY;
        mDefaultGeometryScale[2] = scaleZ;
        setGeometryScale(scaleX, scaleY, 1.0f);
        setDirtyRecursive(true);
    }

    public double[] getDefaultGeometryScale() {
        return mDefaultGeometryScale;
    }

    double[] getGeometryScale() {
        return mGeometryScale;
    }

    public void setZoom(double[] zoom, double[] offset) {
        mZoomM = zoom.clone();
        mZoomM[0] *= offset[0];
        mZoomM[1] *= offset[1];
        mZoomM[2] *= offset[2];
        setInvalidRecursive(true);
    }

    public void setTranslate(double[] translate, double[] offset) {
        mTranslateM = translate.clone();
        mTranslateM[0] += offset[0];
        mTranslateM[1] += offset[1];
        mTranslateM[2] += offset[2];

        setInvalidRecursive(true);
    }

    public void setTranslate(double[] translate) {
        mTranslateM = translate.clone();
        mDefaultTranslateM = translate.clone();

        setInvalidRecursive(true);
    }

    public void setDefaultTranslate(double translateX, double translateY) {
        setDefaultTranslate(translateX, translateY, 0.0f);
    }

    public void setDefaultTranslate(double translateX, double translateY, double translateZ) {
        mDefaultTranslateM[0] = translateX;
        mDefaultTranslateM[1] = translateY;
        mDefaultTranslateM[2] = translateZ;

        setInvalidRecursive(true);
    }

    public void setTranslateDevicecoords(double translateX, double translateY) {
        setTranslateDevicecoords(translateX, translateY, 0.0f);
    }

    public void setTranslateDevicecoords(double translateX, double translateY, double translateZ) {
        setTranslate(2 * translateX - 1.0f, 1.0f - 2.0f * translateY, translateZ);
    }

    void setTranslate(double translateX, double translateY) {
        setTranslate(translateX, translateY, 0.0f);
    }

    void setTranslate(double translateZ) {
        mDefaultTranslateM[2] = translateZ;

        mTranslateM[2] = translateZ;

        setInvalidRecursive(true);
    }

    void setTranslate(double translateX, double translateY, double translateZ) {
        mTranslateM[0] = translateX;
        mTranslateM[1] = translateY;
        mTranslateM[2] = translateZ;

        setInvalidRecursive(true);
    }

    void translate(double translateX, double translateY) {
        mTranslateM[0] += translateX;
        mTranslateM[1] += translateY;
        setInvalidRecursive(true);
    }

    void translate(double translateZ) {
        mDefaultTranslateM[2] += translateZ;

        mTranslateM[2] += translateZ;
        setInvalidRecursive(true);
    }

    void setDirty(boolean isDirty) {
        mIsDirty = isDirty;
        setInvalid(isDirty);
    }

    public void setDirtyRecursive(boolean isDirty) {
        setDirtyRecursive(isDirty, true);
    }

    private void setDirtyRecursive(boolean isDirty, boolean subtreeRoot) {
        setDirty(isDirty);

        SparseArray<TreeNode> drawableList = getChildren();
        int childCount = drawableList.size();
        for (int i = 0; i < childCount; i++) {
            ((Drawable)drawableList.valueAt(i)).setDirtyRecursive(isDirty, false);
        }

        if (subtreeRoot) {
            Drawable parent = (Drawable) getParent();
            if (parent != null) {
                parent.setPathDirtyRecursive(isDirty);
            }
        }
    }

    private void setPathDirty(boolean isPathDirty) {
        mPathDirty = isPathDirty;
    }

    private void setPathDirtyRecursive(boolean isPathDirty) {
        setPathDirty(isPathDirty);
        Drawable parent = (Drawable) getParent();
        if (parent != null) {
            parent.setPathDirtyRecursive(isPathDirty);
        }
    }

    /**
     * Invalidate the parent drawable and its ancestors all the way up to the root.
     *
     * @param isInvalid the final invalidation state.
     */
    public void setInvalid(boolean isInvalid) {
        mIsInvalidated = isInvalid;
        setPathDirty(isInvalid);
    }

    private void setInvalidRecursive(boolean isInvalid) {
        setInvalid(isInvalid);

        setInvalidRecursive(isInvalid, true);
    }

    private void setInvalidRecursive(boolean isInvalid, boolean subtreeRoot) {
        SparseArray<TreeNode> drawableList = getChildren();
        int childCount = drawableList.size();
        for (int i = 0; i < childCount; i++) {
            ((Drawable)drawableList.valueAt(i)).setInvalidRecursive(isInvalid, false);
        }

        if (subtreeRoot) {
            Drawable parent = (Drawable) getParent();
            if (parent != null) {
                parent.setPathDirtyRecursive(isInvalid);
            }
        }
    }

    public void translate(double translateX, double translateY, double translateZ) {
        mDefaultTranslateM[0] += translateX;
        mDefaultTranslateM[1] += translateY;
        mDefaultTranslateM[2] += translateZ;

        mTranslateM[0] += translateX;
        mTranslateM[1] += translateY;
        mTranslateM[2] += translateZ;
        setInvalidRecursive(true);
    }

    public void multiplyTranslate(double translateX, double translateY, double translateZ) {
        mTranslateM[0] = mDefaultTranslateM[0] * translateX;
        mTranslateM[1] = mDefaultTranslateM[1] * translateY;
        mTranslateM[2] = mDefaultTranslateM[2] * translateZ;
    }

    public void setScale(double[] scale) {
        mDefaultScale = scale.clone();
        mScaleM = scale.clone();

        mFinalScaleM = mScaleM.clone();
        setInvalidRecursive(true);
    }

    protected void setScale(double scaleX, double scaleY) {
        setScale(scaleX, scaleY, 1.0f);
    }

    void setScale(double scaleX, double scaleY, double scaleZ) {
        mScaleM[0] = scaleX;
        mScaleM[1] = scaleY;
        mScaleM[2] = scaleZ;

        mFinalScaleM = mScaleM.clone();
        setInvalidRecursive(true);
    }

    public void resetScale() {
        mFinalScaleM = mScaleM.clone();
        computeBounds();
    }

    public void setRotate(double rotateX, double rotateY, double rotateZ) {
        mRotateM[0] = rotateX;
        mRotateM[1] = rotateY;
        mRotateM[2] = rotateZ;
        setInvalidRecursive(true);
    }

    public void rotate(double rotateX, double rotateY, double rotateZ) {
/*        mRotateM[0] += rotateX;
        mRotateM[1] += rotateY;*/
        mRotateM[2] += rotateZ;
        setInvalidRecursive(true);
    }

    public void setZoom(double scaleX, double scaleY, double scaleZ) {
        mZoomM[0] = scaleX;
        mZoomM[1] = scaleY;
        mZoomM[2] = scaleZ;
        mDefaultZoom = mZoomM.clone();

        setInvalidRecursive(true);
    }

    // When scalex, scaley and scalez are passed separately, then they simply are multiplied with final scale's current values
    // This behavior is different from when they are passed in a single array (Check below)
    // TODO there has to be a better way (A duplicate tree perhaps).
    public void multiplyScale(double scaleX, double scaleY, double scaleZ) {
        mFinalScaleM[0] *= scaleX;
        mFinalScaleM[1] *= scaleY;
        mFinalScaleM[2] *= scaleZ;
    }

    // When scalex, scaley and scalez are passed, packed as single array, then they are multiplied with scale values to obtain finalScale values
    // This behavior is different from when they are passed as separate elements (Check above)
    // sahil.bajaj@instalively.com
    public void multiplyScale(double[] scale) {
        mFinalScaleM[0] = mScaleM[0] * scale[0];
        mFinalScaleM[1] = mScaleM[1] * scale[1];
        mFinalScaleM[2] = mScaleM[2] * scale[2];
        setInvalidRecursive(true);
    }

    void computeBounds() {
        double geometryScaleX = mGeometryScale[0] * mZoomM[0];
        double geometryScaleY = mGeometryScale[1] * mZoomM[1];
        double x1 = mTranslateM[0] - geometryScaleX / 2;
        double y1 = mTranslateM[1] - geometryScaleY / 2;
        double x2 = x1 + geometryScaleX;
        double y2 = y1 + geometryScaleY;
        mRectF.set((float) x1, (float) y1, (float) x2, (float) y2);
    }

    void updateImageSources(int timestampMs) {

    }

    @Override
    public Drawable clone() {
        TreeNode clone = super.clone();
        return (Drawable) clone;
    }

    @Override
    public void copy(TreeNode node) {
        Drawable src = (Drawable) node;
        super.copy(src);
        mIsDirty = src.mIsDirty;
        mIsInvalidated = src.mIsInvalidated;
        mVisible = src.mVisible;
        mAlpha = src.mAlpha;
        mRectF = new RectF(src.mRectF);

        mDescription = ProgramDescription.clone(src.mDescription);

        mScaleM = src.mScaleM.clone();
        mTranslateM = src.mTranslateM.clone();
        mRotateM = src.mRotateM.clone();
        mFinalScaleM = src.mFinalScaleM.clone();
        mZoomM = src.mZoomM.clone();
        mDefaultTranslateM = src.mDefaultTranslateM.clone();
        mDefaultScale = src.mDefaultScale.clone();
        mDefaultZoom = src.mDefaultZoom.clone();
        mDefaultGeometryScale = src.mDefaultGeometryScale.clone();
        mGeometryScale = src.mGeometryScale.clone();
    }

    public void cleanup() {
        super.cleanup();
/*        mScaleM = null;
        mFinalScaleM = null;
        mRotateM = null;

        mDefaultTranslateM = null;
        mZoomM = null;
        mDescription = null;*/
    }

    public double[] getDefaultTranslateScreen() {
        double[] translate = mDefaultTranslateM.clone();
        translate[0] = (1.0f + translate[0]) / 2.0f;
        translate[1] = (1.0f - translate[1]) / 2.0f;
        return translate;
    }

    public double[] getTranslateScreen() {
        double[] translate = mTranslateM.clone();
        translate[0] = (1.0f + translate[0]) / 2.0f;
        translate[1] = (1.0f - translate[1]) / 2.0f;
        return translate;
    }

    public void setVisibility(boolean visible) {
        mVisible = visible;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public void multiplyGeometry(double scaleX, double scaleY) {
        mGeometryScale[0] *= scaleX;
        mGeometryScale[1] *= scaleY;
    }

    public void addChild(Drawable child) {
        addChild(getNumberOfChildren(), child);
    }

    public void addChild(int key, Drawable child) {
        super.addChild(key, child);
        child.setLifecycleListener(mLifecycleListener);
        child.setDirtyRecursive(true);
    }

    void measure() {

    }

    final void computeAndMeasure(RenderTarget renderTarget, int timestampMs) {
        updateImageSources(timestampMs);
        if (mPathDirty) {
            if (mIsInvalidated) {
                Rect rect = renderTarget.getExtent();
                double targetAspectRatio = ((double) rect.width()) / rect.height();
                computeDrawableProps(targetAspectRatio);
                if (mIsDirty) {
                    setupDraw();
                    measure();
                    if (mLifecycleListener != null) {
                        mLifecycleListener.onMeasure(renderTarget, this, getGeometryScale()[0], getGeometryScale()[1]);
                    }
                    mIsDirty = false;
                }
                mIsInvalidated = false;
            }

            SparseArray<TreeNode> drawableList = getChildren();
            int childCount = drawableList.size();
            for (int i = 0; i < childCount; i++) {
                Drawable drawable = ((Drawable) drawableList.valueAt(i));
                if (drawable != null) {
                    drawable.computeAndMeasure(renderTarget, timestampMs);
                }
                else {
                    Log.w("Drawable2d", "Child null. draw should not have been called");
                }
            }
            mPathDirty = false;
        }
    }

    void computeDrawableProps(double targetAspectRatio) {
        Drawable parent = (Drawable) getParent();
        if (parent != null) {
            setTranslate(multVec3(getDefaultTranslate(), multVec3(parent.getGeometryScale(), 1.0f)), parent.getTranslate());
//            setGeometryScale(multVec3(getGeometryScale(), parent.getGeometryScale()));
            setZoom(getDefaultZoom(), parent.getZoom());
        }
        computeBounds();

        setFucked(true);
    }

    double[] mScaledValues = new double[3];

    private double[] multVec3(double[] values, double factor) {
        double[] scaledValues = new double[3];

        scaledValues[0] = values[0] * factor;
        scaledValues[1] = values[1] * factor;
        scaledValues[2] = values[2] * factor;

        return scaledValues;
    }

    double[] multVec3(double[] values, double[] factor) {
        double[] scaledValues = new double[3];

        scaledValues[0] = values[0] * factor[0];
        scaledValues[1] = values[1] * factor[1];
        scaledValues[2] = values[2] * factor[2];

        return scaledValues;
    }

    @Override
    public String toString() {
        String result = "";
        result += super.toString() + "\n";
        result += "geometry scale: " + mGeometryScale[0] + " x " + mGeometryScale[1] + " x " + mGeometryScale[2] + "\n";
        result += "scale: " + mScaleM[0] + " x " + mScaleM[1] + " x " + mScaleM[2] + "\n";
        result += "final scale: " + mFinalScaleM[0] + " x " + mFinalScaleM[1] + " x " + mFinalScaleM[2] + "\n";
        result += "translate: " + mTranslateM[0] + " x " + mTranslateM[1] + " x " + mTranslateM[2] + "\n";

        return result;
    }

    public void setFucked(boolean fucked) {
        this.fucked = fucked;
    }

    public boolean isFucked() {
        return fucked;
    }

    public void setMVPMatrix(float[] mvpMatrix) {
        this.mMVPMatrix = mvpMatrix;
    }

    public void setLifecycleListenerRecursive(LifecycleListener lifecycleListener) {
        setLifecycleListener(lifecycleListener, true);
    }

    public void setLifecycleListener(LifecycleListener lifecycleListener) {
        setLifecycleListener(lifecycleListener, false);
    }

    public void setLifecycleListener(LifecycleListener lifecycleListener, boolean recurse) {
        mLifecycleListener = lifecycleListener;
        if (recurse) {
            SparseArray<TreeNode> drawableList = getChildren();
            int childCount = drawableList.size();
            for (int i = 0; i < childCount; i++) {
                ((Drawable) drawableList.valueAt(i)).setLifecycleListener(lifecycleListener, true);
            }
        }
    }

    public interface LifecycleListener {
        void onReady();
        void onMeasure(RenderTarget renderTarget, Drawable drawable, double width, double height);
        void onFilterSetup(RenderTarget renderTarget, Drawable drawable, BaseFilter filter);
        void onFilterPredraw(OpenGLRenderer.Fuzzy renderTargetType, RenderTarget renderTarget, Drawable drawable, BaseFilter filter);
    }
}
