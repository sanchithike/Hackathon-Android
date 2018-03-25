package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;

import java.util.ArrayList;


/**
 * This effect initially has visibility unset for all the drawables ,
 * it will gradually set visibility for each of the drawable and then gradually unset them again
 * Created by akshaychauhan on 12/27/17.
 */

public class ScreenSplitFadeInOut extends ScreenSplitBase {

    private static double animationDuration = 4.0;

    public ScreenSplitFadeInOut(ArrayList<ImageSource> imageSources,
                                SceneManager.SceneDescription sceneDescription,
                                long captureTimestamp) {
        super("ScreenSplitFadeInOut", sceneDescription,6, 6, imageSources, true, false, captureTimestamp);
        setVisibilityOff();
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        double usableTimeFactor = (timestamp/1000.0) % animationDuration;
        updateScene(usableTimeFactor);
        // normal draw - directly to display
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void setVisibilityOff() {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();
        for (int i = 0; i < allDrawables.size(); i++) {
            Drawable2d drawable2d = (Drawable2d) allDrawables.get(i);
            setVisiblity(drawable2d, false);
        }
    }


    private void updateScene(double usableTimeFactor) {
        double probability;
        if (usableTimeFactor <= 1.0) {
            probability = 2.0 / (getGridRows() * getGridColumns());
            animateDrawables(probability, true);
        } else if (usableTimeFactor <= 2.0) {
            animateDrawables(0.75, true);
        } else if (usableTimeFactor <= 3.0) {
            animateDrawables(1.0, true);
        } else if (usableTimeFactor <= 3.5) {
            animateDrawables(0.25, false);
        } else if (usableTimeFactor <= 3.75) {
            animateDrawables(0.5, false);
        } else {
            animateDrawables(1.0, false);
        }
    }

    private void animateDrawables(double probability, boolean visibility) {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();
        for (int i = 0; i < allDrawables.size(); i++) {
            Drawable2d drawable2d = (Drawable2d) allDrawables.get(i);
            if (isWithinProbablity(probability)) {
                setVisiblity(drawable2d, visibility);
            }
        }
    }

    private boolean isWithinProbablity(double probability) {
        return Math.random() < probability;
    }
}
