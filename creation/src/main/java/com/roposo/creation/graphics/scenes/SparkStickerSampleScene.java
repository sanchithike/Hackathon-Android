package com.roposo.creation.graphics.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.R;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

import java.util.Random;

import static com.roposo.creation.graphics.GraphicsConsts.MATCH_PARENT;

/**
 * Created by tajveer on 12/27/17.
 */

public class SparkStickerSampleScene extends Scene {

    static Random r = new Random();

    public SparkStickerSampleScene(SceneManager.SceneDescription sceneDescription, boolean isCamera, Bitmap sceneBitmap) {
        super(sceneDescription);
        Drawable2d drawable = Drawable2d.createExternalSourceDrawable(true, Drawable2d.SCALE_TYPE_INSIDE);
        drawable.setDefaultGeometryScale(MATCH_PARENT, MATCH_PARENT);

        mRootDrawables.add(drawable);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {

        Drawable mRootDrawable = mRootDrawables.get(0);

        Bitmap bmp = BitmapFactory.decodeResource(ContextHelper.getContext().getResources(), R.drawable.apple_icon);
        Drawable2d child = Drawable2d.createBitmapDrawable(bmp, Drawable2d.SCALE_TYPE_INSIDE);
        child.setDefaultGeometryScale(0.1f, 0.1f);
        child.setDefaultTranslate(Math.signum(r.nextFloat() -  0.5f) *  r.nextFloat(), Math.signum(r.nextFloat() -  0.5f) * r.nextFloat());

        mRootDrawable.removeChildren();
        mRootDrawable.addChild(child);

        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawable);
    }
}
