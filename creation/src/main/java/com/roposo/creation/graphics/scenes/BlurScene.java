package com.roposo.creation.graphics.scenes;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

import java.util.ArrayList;

/**
 * Created by tajveer on 12/22/17.
 */

public class BlurScene extends Scene {

    private static int NUM_PASSES = 6;

    int fb1 = 0, fb2 = 0;

    public BlurScene(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription) {
        super(NUM_PASSES, sceneDescription);

        Drawable mRootDrawable, mRootDrawable2;

        // 1st pass
        mRootDrawable = mRootDrawables.get(0);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d drawable = Drawable2d.create(imageSources);

        drawable.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
        drawable.setDefaultTranslate(0.0f, 0.0f);
        drawable.setDefaultGeometryScale(1.0f, 1.0f);
        drawable.setFilterMode(FilterManager.BLUR_H_FILTER);
        //drawable.addImageSource(new ImageSource(Caches.blurKernel));
        mRootDrawable.addChild(drawable);

        // 2nd pass
        for (int i = 1; i < NUM_PASSES; i++) {
            mRootDrawable2 = mRootDrawables.get(i);
            mRootDrawable2.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

            Drawable2d drawable2d = new Drawable2d();
            drawable2d.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
            drawable2d.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            drawable2d.setFilterMode(i % 2 == 0 ? FilterManager.BLUR_H_FILTER : FilterManager.BLUR_V_FILTER);
            mRootDrawable2.addChild(drawable2d);
        }
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
//        Log.d(TAG, "timestamp: " + timeStampMs);
/*
        Drawable mRootDrawable = mRootDrawables.get(0);
        Drawable mRootDrawable2 = mRootDrawables.get(1);*/

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
                ((Drawable2d) mRootDrawables.get(i+1).getChildAt(0)).setImageSource(new ImageSource(i % 2 == 0 ? fbo1 : fbo2));
                //((Drawable2d) mRootDrawables.get(i+1).getChildAt(0)).addImageSource(new ImageSource(Caches.blurKernel));
            }
        }

        for (int i = 0; i < NUM_PASSES - 1; i++) {
            renderer.drawFrame(mRootDrawables.get(i), i % 2 == 0 ? fbo1 : fbo2);
        }
        renderer.drawFrame(mRootDrawables.get(NUM_PASSES - 1));
        //renderer.drawFrame(mRootDrawables.get(0));
    }

}
