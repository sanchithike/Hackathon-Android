package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 3/7/18.
 */

public class ManifoldScreenSplit extends ScreenSplitBase {

    private double animationDuration = 7.0;
    private double initialScaleValueX;
    private double initialScaleValueY;
    private Drawable2d dummyDrawable;

    public ManifoldScreenSplit(ArrayList<ImageSource> imageSources,
                               SceneManager.SceneDescription sceneDescription,
                               long captureTimestamp) {
        super("ManifoldScreenSplit",
                sceneDescription,
                7,
                7,
                imageSources,
                false,
                false,
                captureTimestamp);

        Drawable rootDrawable = mRootDrawables.get(0);

        SparseArray<TreeNode> allChildren = rootDrawable.getChildren();
        rootDrawable.removeChildren();

        dummyDrawable = Drawable2d.create(imageSources);
        for (int i = 0; i < allChildren.size(); i++) {
            dummyDrawable.addChild((Drawable2d) allChildren.get(i));
        }

        rootDrawable.addChild(dummyDrawable);
        initialScaleValueX = dummyDrawable.getDefaultGeometryScale()[0] * 7.0;
        initialScaleValueY = dummyDrawable.getDefaultGeometryScale()[1] * 7.0;
        dummyDrawable.setDefaultGeometryScale(initialScaleValueX, initialScaleValueY);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        double usableTimeFactor = (timestamp / 1000.0) % animationDuration;
        updateScene(usableTimeFactor);
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void updateScene(double usableTimeFactor) {
        Drawable2d drawable2d = (Drawable2d) mRootDrawables.get(0).getChildAt(0);
        double scaleX;
        double scaleY;

        double changeFactor = calculateChangeFactor(usableTimeFactor);
        scaleX = initialScaleValueX / changeFactor;
        scaleY = initialScaleValueY / changeFactor;
        drawable2d.setDefaultGeometryScale(scaleX, scaleY);
    }

    private double calculateChangeFactor(double usableTimeFactor) {
        double changeFactor, timefactor;
        if (usableTimeFactor > animationDuration / 2.0) {
            usableTimeFactor -= (animationDuration / 2.0);
            usableTimeFactor = (animationDuration / 2.0) - usableTimeFactor;
        }

        double fractionalPart = usableTimeFactor - (int) usableTimeFactor;
        if (fractionalPart <= 0.10) {
            timefactor = 2.0 * (int)usableTimeFactor;
        } else {
            timefactor = 2.0 * usableTimeFactor;
        }

        changeFactor = Math.min((timefactor + 1.0), 7.0);

        return changeFactor;
    }

    @Override
    public Scene clone(){
        ManifoldScreenSplit scene = (ManifoldScreenSplit) super.clone();
        scene.dummyDrawable = this.dummyDrawable.clone();
        return scene;
    }

}
