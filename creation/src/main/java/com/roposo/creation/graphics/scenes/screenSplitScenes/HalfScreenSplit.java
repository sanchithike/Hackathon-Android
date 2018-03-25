package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by akshaychauhan on 12/28/17.
 */

public class HalfScreenSplit extends ScreenSplitBase {

    private double animationDuration = 8.0;

    public HalfScreenSplit(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription, long captureTimestamp) {
        super("HalfScreenSplit", sceneDescription,1, 2, imageSources, true, false, captureTimestamp);
        initialSettingsForSceneDrawables();
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        double usableTimeFactorInSeconds = (timestamp/1000.0) % animationDuration;
        updateScene(usableTimeFactorInSeconds);
        // normal draw - directly to display
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void updateScene(double usableTimeFactor) {
       animateDrawables(usableTimeFactor);
    }

    private void initialSettingsForSceneDrawables() {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();
        for (int i = 0; i < allDrawables.size(); i++) {
            Drawable2d drawable2d = (Drawable2d) allDrawables.get(i);
            if (i % 2 == 0) {
                drawable2d.setDefaultTranslate(drawable2d.getDefaultTranslate()[0], 1.0);
                setFilterMode(drawable2d, Arrays.asList(FilterManager.MONO_FILTER, FilterManager.SNOW_FLAKES_FILTER));
            }
            else {
                drawable2d.setDefaultTranslate(drawable2d.getDefaultTranslate()[0], -1.0);
                setFilterMode(drawable2d, FilterManager.SNOW_FLAKES_FILTER);
            }
        }
    }

    private void animateDrawables(double usableTimeFactor) {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();

        // Just a sanity check , if someone changes the number of drawables in scene ,
        // changes needs to be made here also
        if (allDrawables.size() == 2) {
            final Drawable2d leftDrawable = (Drawable2d) allDrawables.get(0);
            final Drawable2d rightDrawable = (Drawable2d) allDrawables.get(1);

            if (usableTimeFactor >= 0.0 && usableTimeFactor <= animationDuration/2.67) {
                double translateInY = Math.pow(usableTimeFactor/3, 3.75);
                leftDrawable.setDefaultTranslate(leftDrawable.getDefaultTranslate()[0], 1.0 - translateInY);
                rightDrawable.setDefaultTranslate(rightDrawable.getDefaultTranslate()[0], -1.0 + translateInY);
            }
            else if (usableTimeFactor > animationDuration/2.67 && usableTimeFactor < animationDuration/1.6) {
                //Remove the mono when it has completed transition
                setFilterMode(leftDrawable, FilterManager.SNOW_FLAKES_FILTER);
                if (leftDrawable.getDefaultTranslate()[1] != 0) {
                    leftDrawable.setDefaultTranslate(leftDrawable.getDefaultTranslate()[0], 0.0);
                }
                if (rightDrawable.getDefaultTranslate()[1] != 0) {
                    rightDrawable.setDefaultTranslate(rightDrawable.getDefaultTranslate()[0], 0.0);
                }
            }
            else {
                usableTimeFactor -= animationDuration/1.6;
                double translateInY = Math.pow(usableTimeFactor/3, 3.75);

                leftDrawable.setDefaultTranslate(leftDrawable.getDefaultTranslate()[0], - translateInY);
                rightDrawable.setDefaultTranslate(rightDrawable.getDefaultTranslate()[0], translateInY);
            }
        }
    }
}
