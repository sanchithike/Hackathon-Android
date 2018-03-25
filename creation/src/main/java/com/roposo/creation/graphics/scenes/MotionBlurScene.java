package com.roposo.creation.graphics.scenes;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

import java.util.ArrayList;

/**
 * Created by tajveer on 12/22/17.
 */

public class MotionBlurScene extends Scene {

    int fb1 = 0, fb2 = 0, fb3 = 0;

    public MotionBlurScene(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription) {
        super(2, sceneDescription);

        Drawable mRootDrawable = mRootDrawables.get(0);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d drawable = Drawable2d.create(imageSources);
        drawable.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
        drawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        drawable.setFilterMode(FilterManager.BLUR_MOTION_FILTER);
        mRootDrawable.addChild(drawable);

        // for fbo
        mRootDrawable = mRootDrawables.get(1);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d drawable2d = Drawable2d.create(imageSources);
        drawable2d.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
        drawable2d.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        mRootDrawable.addChild(drawable2d);

    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);

        Drawable2d d0 = (Drawable2d) mRootDrawables.get(0);
        Drawable2d d1 = (Drawable2d) mRootDrawables.get(1);

        FBObject fbo1 = renderer.getFrameBufferObject(fb1);
        FBObject fbo2 = renderer.getFrameBufferObject(fb2);
        FBObject fbo3 = renderer.getFrameBufferObject(fb3);
        if (fbo1 == null || fbo2 == null || fbo3 == null) {
            int width = renderer.getOutgoingWidth();
            int height = renderer.getOutgoingHeight();

            fbo1 = renderer.createFrameBufferObject(width, height, false);
            fb1 = fbo1.getFrameBufferId();

            fbo2 = renderer.createFrameBufferObject(width, height, false);
            fb2 = fbo2.getFrameBufferId();

            fbo3 = renderer.createFrameBufferObject(width, height, false);
            fb3 = fbo3.getFrameBufferId();

            Drawable2d d0c0 = (Drawable2d) d0.getChildAt(0);
            d0c0.addImageSource(new ImageSource(fbo1));
            d0c0.addImageSource(new ImageSource(fbo2));
            d0c0.addImageSource(new ImageSource(fbo3));
        }

        renderer.drawFrame(d0);
        renderer.drawFrame(d1, fbo3);

        fb1 = fbo3.getFrameBufferId();
        fb2 = fbo1.getFrameBufferId();
        fb3 = fbo2.getFrameBufferId();
    }

}
