package com.roposo.creation.graphics.scenes;

import android.graphics.Bitmap;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

/**
 * Created by tajveer on 12/22/17.
 */

public class ShineDetectSampleScene extends Scene {
    private static final String TAG = "ShineDetectSampleScene";
    int fb1 = 0, fb2 = 0;

    public ShineDetectSampleScene(boolean isCamera, Bitmap sceneBitmap, SceneManager.SceneDescription sceneDescription) {
        super(3, sceneDescription);

        Drawable mRootDrawable = mRootDrawables.get(0);
        Drawable mRootDrawable2 = mRootDrawables.get(1);
        Drawable mRootDrawable3 = mRootDrawables.get(2);

        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d drawable;
        if (sceneBitmap == null) {
            drawable = Drawable2d.createExternalSourceDrawable(isCamera,
                    isCamera ? Drawable2d.SCALE_TYPE_CROP : Drawable2d.SCALE_TYPE_FIT_WIDTH);
        } else {
            drawable = Drawable2d.createBitmapDrawable(sceneBitmap, Drawable2d.SCALE_TYPE_FIT_WIDTH);
        }

        drawable.setDefaultTranslate(0.0f, 0.0f);
        drawable.setDefaultGeometryScale(1.0f, 1.0f);
        drawable.setFilterMode(FilterManager.BLUR_2D_FILTER);
        mRootDrawable.addChild(drawable);

        mRootDrawable2.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        Drawable2d drawable2d = new Drawable2d();
        drawable2d.setFilterMode(FilterManager.BLUR_2D_FILTER);
        drawable2d.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        mRootDrawable2.addChild(drawable2d);

        mRootDrawable3.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        Drawable2d drawable2d1;
        if (sceneBitmap == null) {
            drawable2d1 = Drawable2d.createExternalSourceDrawable(isCamera,
                    isCamera ? Drawable2d.SCALE_TYPE_CROP : Drawable2d.SCALE_TYPE_FIT_WIDTH);
        } else {
            drawable2d1 = Drawable2d.createBitmapDrawable(sceneBitmap, Drawable2d.SCALE_TYPE_FIT_WIDTH);
        }
        drawable2d1.setFilterMode(FilterManager.SHINE_DETECT_FILTER);
        drawable2d1.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        mRootDrawable3.addChild(drawable2d1);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
//        Log.d(TAG, "timestamp: " + timeStampMs);

        Drawable mRootDrawable = mRootDrawables.get(0);
        Drawable mRootDrawable2 = mRootDrawables.get(1);
        Drawable mRootDrawable3 = mRootDrawables.get(2);

        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);

        FBObject fbo1 = renderer.getFrameBufferObject(fb1);
        FBObject fbo2 = renderer.getFrameBufferObject(fb2);
        if (fbo1 == null || fbo2 == null) {
            int width = renderer.getOutgoingWidth() / 4 ;
            int height = renderer.getOutgoingHeight() / 4;

            ((Drawable2d)mRootDrawable.getChildAt(0)).addImageSource(new ImageSource(Caches.blurKernel));

            fbo1 = renderer.createFrameBufferObject(width, height, false);
            fb1 = fbo1.getFrameBufferId();

            ((Drawable2d)mRootDrawable2.getChildAt(0)).setImageSource(new ImageSource(fbo1));
            ((Drawable2d)mRootDrawable2.getChildAt(0)).addImageSource(new ImageSource(Caches.blurKernel));

            fbo2 = renderer.createFrameBufferObject(width, height, false);
            fb2 = fbo2.getFrameBufferId();

            ((Drawable2d)mRootDrawable3.getChildAt(0)).addImageSource(new ImageSource(fbo1 /*fbo2*/));
        }

        renderer.drawFrame(mRootDrawable, fbo1);
        //renderer.drawFrame(mRootDrawable2, fbo2);
        renderer.drawFrame(mRootDrawable3);
    }
}
