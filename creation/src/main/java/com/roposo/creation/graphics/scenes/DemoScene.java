package com.roposo.creation.graphics.scenes;

import android.graphics.RectF;
import android.support.v4.math.MathUtils;
import android.text.TextUtils;
import android.util.Log;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.RenderTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tajveer on 12/22/17.
 */

public class DemoScene extends Scene {
    private static final String TAG = "DemoScene";
    private static boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private double aspectRatio;

    public DemoScene(ArrayList<ImageSource> imageSources) {
        this(imageSources,
             new SceneManager.SceneDescription(SceneManager.SceneName.SCENE_DEFAULT),
             FilterManager.IMAGE_FILTER);
    }

    public DemoScene(ArrayList<ImageSource> imageSources,
                     SceneManager.SceneDescription sceneDescription) {
        this(imageSources, sceneDescription, FilterManager.IMAGE_FILTER);
    }

    public DemoScene(ArrayList<ImageSource> imageSources,
                     SceneManager.SceneDescription sceneDescription,
                     String filterMode) {
        this(imageSources, sceneDescription, filterMode, 500);
    }

    public DemoScene(ArrayList<ImageSource> imageSources,
                     SceneManager.SceneDescription sceneDescription,
                     String filterMode,
                     long captureTimeStamp) {
        super(sceneDescription);

        Drawable mRootDrawable;
        mRootDrawable = mRootDrawables.get(0);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        this.mPreferredCaptureTimestamp = captureTimeStamp;

        Drawable2d drawable;
        drawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription, filterMode);
        if (!sceneDescription.isCropped()) {
            drawable.setDefaultTranslate(0.0f, 0.0f);
        }
        else {
            drawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        }

        mRootDrawable.addChild(drawable);

        //mRootDrawable2 = mRootDrawable.clone();
        //mRootDrawables.add(mRootDrawable2);
    }

    @Override
    protected Drawable2d createSceneDrawableFromSceneDescription(ArrayList<ImageSource> imageSources,
                                                                 SceneManager.SceneDescription sceneDescription) {
        return createSceneDrawableFromSceneDescription(imageSources, sceneDescription, null);
    }

    private Drawable2d createSceneDrawableFromSceneDescription(ArrayList<ImageSource> imageSources,
                                                               SceneManager.SceneDescription sceneDescription,
                                                               String filterMode) {
        Drawable2d drawable2d = super.createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
        setGeometryScaleDrawable(drawable2d, currentSceneDescription.getSceneTextureConvertedRectFInfo());
        drawable2d.setDefaultTranslate(sceneDescription.getSceneTranslateInfo()[0], sceneDescription.getSceneTranslateInfo()[1]);
        drawable2d.setZoom(sceneDescription.getSceneZoom()[0], sceneDescription.getSceneZoom()[1], sceneDescription.getSceneZoom()[2]);
        drawable2d.setRotate(0.0, 0.0, sceneDescription.getSceneRotateInZ());

        List<String> filterModes = new ArrayList<>();
        if (sceneDescription.isTuningEnabled()) {
            filterModes.add(FilterManager.IMAGE_ADJUSTMENT_FILTER);
        }

        if (!TextUtils.isEmpty(filterMode)) {
            filterModes.add(filterMode);
        }
        drawable2d.setFilterMode(filterModes);
        return drawable2d;
    }

    private void setGeometryScaleDrawable(Drawable2d drawable, RectF rectF) {
        double sideMargin = rectF.left + (1.0 - rectF.right);
        double heightMargin = (1.0 - rectF.top) + rectF.bottom;
        drawable.setDefaultGeometryScale(1.0 - sideMargin, 1.0 - heightMargin);
        currentSceneDescription.setDefaultGeometryScale(new double[]{1.0 - sideMargin, 1.0 - heightMargin, 1.0});
    }


    @Override
    public void onMeasure(RenderTarget renderTarget, Drawable drawable, double width, double height) {
        super.onMeasure(renderTarget, drawable, width, height);
        if (width > 0 && height > 0) {
            Drawable2d drawable2d = (Drawable2d) drawable.getParent();
            if (drawable2d == null) {
                aspectRatio = width / height;
            }
        }
        else {
            aspectRatio = 9.0/16.0;
        }
    }

    @Override
    protected long onPreDraw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
        Drawable2d sceneDrawable = (Drawable2d) mRootDrawables.get(0).getChild(0);

        //Rotation
        rotateInZAxis(sceneDrawable);

        //Zoom
        zoomInScene(sceneDrawable);

        //Panning
        panInScene(sceneDrawable);

        return super.onPreDraw(renderTargetType, timeStampMs);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
//        Log.d(TAG, "timestamp: " + timeStampMs);

        Drawable mRootDrawable = mRootDrawables.get(0);
        //Drawable mRootDrawable2 = mRootDrawables.get(1);

        // normal draw - directly to display
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawable);

        // draw using fbo
        /*OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);

        FBObject fbo1 = renderer.getFrameBufferObject(fb1);
        if (fbo1 == null) {
            fbo1 = renderer.createFrameBufferObject(renderer.getOutgoingWidth() / 2, renderer.getOutgoingHeight() / 2, false);
            fb1 = fbo1.getFrameBufferId();
            ((Drawable2d)mRootDrawable2.getChildAt(0)).setImageSource(new ImageSource(fbo1));
        }

        renderer.drawFrame(mRootDrawable, fbo1);
        renderer.drawFrame(mRootDrawable2);*/
    }

    private void rotateInZAxis(Drawable2d sceneDrawable) {
        double[] rotateInfoFromDrawable = sceneDrawable.getRotate();
        if (rotateDirty) {
            if (VERBOSE) {
                Log.d(TAG, "Rotate by : " + currentSceneDescription.getSceneRotateInZ());
            }
            sceneDrawable.setRotate(rotateInfoFromDrawable[0], rotateInfoFromDrawable[1], currentSceneDescription.getSceneRotateInZ());
            rotateDirty = false;
        }
    }

    private void zoomInScene(Drawable2d sceneDrawable) {
        double[] zoomInfoFromDrawable = sceneDrawable.getZoom();
        if (zoomDirty) {
            if (VERBOSE) {
                Log.d(TAG, "Scale by : " + currentSceneDescription.getSceneZoom()[0] + " and " + currentSceneDescription.getSceneZoom()[1]);
                Log.d(TAG, "Drawable Zoom : " + zoomInfoFromDrawable[0] + " and " + zoomInfoFromDrawable[1]);
            }

            sceneDrawable.setZoom(currentSceneDescription.getSceneZoom()[0], currentSceneDescription.getSceneZoom()[1], currentSceneDescription.getSceneZoom()[2]);
            //TODO: Add check for zoom out
            recenterOnZoom(sceneDrawable);
            zoomDirty = false;
        }
    }

    private void recenterOnZoom(Drawable2d sceneDrawable) {
        double left = sceneDrawable.getTranslate()[0] - (sceneDrawable.getZoom()[0] / 2.0);
        double right = sceneDrawable.getTranslate()[0] + (sceneDrawable.getZoom()[0] / 2.0);

        double top = (sceneDrawable.getTranslate()[1] * aspectRatio) + (sceneDrawable.getZoom()[1] / 2.0);
        double bottom = (sceneDrawable.getTranslate()[1] * aspectRatio) - (sceneDrawable.getZoom()[1] / 2.0);

        double xTranslateBy = getxTranslateBy(left, right);
        double yTranslateBy = getyTranslateBy(top, bottom);

        double newXCoord = sceneDrawable.getDefaultTranslate()[0] + xTranslateBy;
        //TODO : Ideally aspect ratio should be multiplied with y-coord, it doesnt work
        double newYCoord = (sceneDrawable.getDefaultTranslate()[1]) + yTranslateBy;

        if (VERBOSE) {
            Log.d(TAG, "New x-centre " + newXCoord + ", new y-centre " + newYCoord);
        }
        double[] newTranslatePoints = getNewTranslatePoints(newXCoord, newYCoord);
        sceneDrawable.setDefaultTranslate(newTranslatePoints[0], newTranslatePoints[1]);
    }

    private double getxTranslateBy(double left, double right) {
        double xTranslateBy = 0.0;

        // Following calculation takes the screen coordinates to be in range of
        // -0.5 + rectF factor to 0.5 + rectF factor in x and y
        RectF rectF = currentSceneDescription.getSceneTextureConvertedRectFInfo();

        if (left > -(0.5 - rectF.left)) {
            xTranslateBy = -(left + (0.5 - rectF.left));
        } else if (right < 0.5 - (1.0 - rectF.right)) {
            xTranslateBy = (0.5 - (1.0 - rectF.right)) - right;
        }
        return xTranslateBy;

    }

    private double getyTranslateBy(double top, double bottom) {
        double yTranslateBy = 0.0;

        RectF rectF = currentSceneDescription.getSceneTextureConvertedRectFInfo();
        if (top < 0.5 - (1.0 - rectF.top)) {
            yTranslateBy = (0.5 - (1.0 - rectF.top)) - top;
        } else if (bottom > -(0.5 - rectF.bottom)) {
            yTranslateBy = -(bottom + (0.5 - rectF.bottom));
        }

        return yTranslateBy;
    }

    private double[] getNewTranslatePoints(double newXCoord, double newYCoord) {
        RectF currentSceneRectF = currentSceneDescription.getSceneTextureConvertedRectFInfo();
        double originalZoom = 1.0;

        double absLeftForView = 0.5 - currentSceneRectF.left;
        double absRightForView = 0.5 - (1.0 - currentSceneRectF.right);
        double maxTranslateLeft = (currentSceneDescription.getSceneZoom()[0] - originalZoom) * (absRightForView);
        double maxTranslateRight = (currentSceneDescription.getSceneZoom()[0] - originalZoom) * (absLeftForView);

        double absBottomForView = 0.5 - currentSceneRectF.bottom;
        double absTopForView = 0.5 - (1.0 - currentSceneRectF.top);

        double maxTranslateTop = (currentSceneDescription.getSceneZoom()[1] - originalZoom) * (absBottomForView) ;
        double maxTranslateBottom = (currentSceneDescription.getSceneZoom()[1] - originalZoom) * (absTopForView);

        if (VERBOSE) {
            Log.d(TAG, "X-Range => " + -maxTranslateLeft + " and " + maxTranslateRight);
            Log.d(TAG, "Y-Range => -" + maxTranslateBottom + " and " + maxTranslateTop);
        }

        return new double[]{
                MathUtils.clamp(newXCoord, 0.0 - maxTranslateLeft, 0.0 + maxTranslateRight),
                MathUtils.clamp(newYCoord, 0.0 - maxTranslateBottom, 0.0 + maxTranslateTop),
                0.0};
    }

    private void panInScene(Drawable2d sceneDrawable) {
        double[] translateInfoFromDrawable = sceneDrawable.getTranslate();
        if (panDirty) {
            if (currentSceneDescription.getSceneZoom()[0] > 1.0 || currentSceneDescription.getSceneZoom()[1] > 1.0) {
                double newXCoordinate = (translateInfoFromDrawable[0]) + currentSceneDescription.getSceneTranslateByInfo()[0];
                double newYCoordinate = (translateInfoFromDrawable[1] * aspectRatio) + currentSceneDescription.getSceneTranslateByInfo()[1];
                double[] newTranslatePoints = getNewTranslatePoints(newXCoordinate, newYCoordinate);

                if (VERBOSE) {
                    Log.d(TAG, "Old Translate Value : x : " + translateInfoFromDrawable[0] + " , y : " + translateInfoFromDrawable[1] * aspectRatio);
                    Log.d(TAG, "Translate by : x : " + currentSceneDescription.getSceneTranslateByInfo()[0] + " and " + currentSceneDescription.getSceneTranslateByInfo()[1]);
                    Log.d(TAG, "Expected Values : x : " + newXCoordinate + " , y : "+ newYCoordinate);
                }
                sceneDrawable.setDefaultTranslate(newTranslatePoints[0], newTranslatePoints[1]);
                currentSceneDescription.setSceneTranslateInfo(newTranslatePoints);
            }
            resetTranslateByValues();
            panDirty = false;
        }
    }

    private void resetTranslateByValues() {
        double[] resetTranslateBy = {0.0, 0.0, 0.0};
        setSceneTranslateByInfo(resetTranslateBy);
    }

    @Override
    public SceneManager.SceneDescription cropMedia() {
        SceneManager.SceneDescription requiredSceneDescription = null;
        if (mRootDrawables.size() > 0 && mRootDrawables.get(0).getChildren().size() > 0) {
            requiredSceneDescription = this.currentSceneDescription.clone();
            requiredSceneDescription.setCropped(true);
        }

        return requiredSceneDescription;
    }

}
