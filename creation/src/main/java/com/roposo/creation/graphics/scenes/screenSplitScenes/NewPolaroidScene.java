package com.roposo.creation.graphics.scenes.screenSplitScenes;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.RenderTarget;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 2/21/18.
 */

public class NewPolaroidScene extends ScreenSplitBase {

    private static int NUM_PASSES = 6;
    int fb1 = 0, fb2 = 0;
    private double animationDuration = 1.0;

    public NewPolaroidScene(ArrayList<ImageSource> imageSources,
                            SceneManager.SceneDescription sceneDescription,
                            String filterMode,
                            long captureTimestamp) {
        super("NewPolaroidScene", sceneDescription, NUM_PASSES + 1, captureTimestamp);

        // 1st pass
        Drawable2d mRootDrawable = (Drawable2d) mRootDrawables.get(0);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d drawable = Drawable2d.create(imageSources);
        drawable.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
        drawable.setDefaultTranslate(0.0f, 0.0f);
        drawable.setDefaultGeometryScale(1.0f, 1.0f);
        drawable.setFilterMode(FilterManager.BLUR_H_FILTER);
        mRootDrawable.addChild(drawable);

        // 2nd pass
        for (int i = 1; i < NUM_PASSES; i++) {
            Drawable irootDrawable = mRootDrawables.get(i);
            irootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

            Drawable2d drawable2d = new Drawable2d();
            drawable2d.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
            drawable2d.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            drawable2d.setFilterMode(i % 2 == 0 ? FilterManager.BLUR_H_FILTER : FilterManager.BLUR_V_FILTER);
            irootDrawable.addChild(drawable2d);
        }

        createPolaroidDrawable(imageSources, sceneDescription, filterMode);

        //Initial Display
        Drawable2d displayDrawableParent = (Drawable2d) mRootDrawables.get(NUM_PASSES);
        displayDrawableParent.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        Drawable2d displayDrawableChild = Drawable2d.create(imageSources);
        displayDrawableChild.setDefaultTranslate(0.0f, 0.0f);
        displayDrawableChild.setDefaultGeometryScale(1.0f, 1.0f);
        displayDrawableParent.addChild(displayDrawableChild);

    }

    private void createPolaroidDrawable(ArrayList<ImageSource> imageSources,
                                        SceneManager.SceneDescription sceneDescription,
                                        String filterMode) {
        //Polaroid Drawable
        Drawable2d polaroidDrawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
        polaroidDrawable.setDefaultTranslate(0.0, 0.08, 0.0);
        polaroidDrawable.setDefaultGeometryScale(0.0f, 0.0f);

        Drawable2d frameDrawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
        frameDrawable.setDefaultTranslate(0.0, 0.0);
        frameDrawable.setDefaultGeometryScale(1.0, 1.0);
        frameDrawable.setFilterMode(filterMode);

        Drawable2d pictureDrawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
        pictureDrawable.setDefaultGeometryScale(0.9, 0.7);
        pictureDrawable.setDefaultTranslate(0.0, 0.1);
        frameDrawable.addChild(pictureDrawable);
        polaroidDrawable.addChild(frameDrawable);

        mRootDrawables.get(NUM_PASSES - 1).addChild(polaroidDrawable);
    }


    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);
        FBObject fbo1 = renderer.getFrameBufferObject(fb1);
        FBObject fbo2 = renderer.getFrameBufferObject(fb2);
        if (fbo1 == null || fbo2 == null) {
            int width = renderer.getOutgoingWidth() / 2;
            int height = renderer.getOutgoingHeight() / 2;

            fbo1 = renderer.createFrameBufferObject(width, height, false);
            fb1 = fbo1.getFrameBufferId();

            fbo2 = renderer.createFrameBufferObject(width, height, false);
            fb2 = fbo2.getFrameBufferId();

            for (int i = 0; i < NUM_PASSES - 1; i++) {
                ((Drawable2d) mRootDrawables.get(i + 1).getChildAt(0)).setImageSource(new ImageSource(i % 2 == 0 ? fbo1 : fbo2));
            }
        }

        for (int i = 0; i < NUM_PASSES - 1; i++) {
            renderer.drawFrame(mRootDrawables.get(i), i % 2 == 0 ? fbo1 : fbo2);
        }

        Drawable2d drawable2d = (Drawable2d) mRootDrawables.get(NUM_PASSES - 1);
        Drawable2d backgroundDrawable = (Drawable2d) drawable2d.getChildAt(0);
        backgroundDrawable.setAlpha(0.6f);

        Drawable2d polaroidDrawable = (Drawable2d) drawable2d.getChildAt(1);


        boolean showPolaroid = animateDrawableAndCheckPolaroidRequired(timeStampMs, polaroidDrawable);
        if (showPolaroid) {
            renderer.drawFrame(drawable2d);
        } else {
            renderer.drawFrame(mRootDrawables.get(NUM_PASSES));
        }
    }

    private boolean animateDrawableAndCheckPolaroidRequired(long timestamp, Drawable2d drawable2d) {
        double usableTimeFactor = (timestamp/1000.0) % animationDuration;
        double[] scale = drawable2d.getDefaultGeometryScale();
        boolean isPolaroid = true;
        if (timestamp/1000.0 < animationDuration && (scale[0] < 0.9 || scale[1] < 0.6)) {
            float scaleFactor = (float) usableTimeFactor;
            float scaleFactorX = (float) usableTimeFactor * 1.5f;
            drawable2d.setDefaultGeometryScale(Math.min(0.9, scaleFactorX), Math.min(0.6, scaleFactor));
            isPolaroid = scale[0] > 0.30;
        }
        else {
            if (scale[0] != 0.9 || scale[1] != 0.6) {
                drawable2d.setDefaultGeometryScale(0.9, 0.6);
            }
        }

        return isPolaroid ;
    }

    @Override
    protected void setLutInScene() {
        if (lutDirty) {
            Drawable2d resultingDrawable = (Drawable2d) mRootDrawables.get(NUM_PASSES - 1);
            Drawable2d polaroidDrawable = (Drawable2d) resultingDrawable.getChildAt(1);
            setLutInDrawableTree(polaroidDrawable);
            lutDirty = false;
        }
    }

}

