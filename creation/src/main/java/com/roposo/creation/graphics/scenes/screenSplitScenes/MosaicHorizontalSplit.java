package com.roposo.creation.graphics.scenes.screenSplitScenes;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 1/2/18.
 */

public class MosaicHorizontalSplit extends ScreenSplitBase {

    private static double animationDuration = 6.0;

    public MosaicHorizontalSplit(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription, long captureTimestamp) {
        super("MosaicHorizontalSplit", sceneDescription,  3, 1, imageSources, true, false, captureTimestamp);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        double usableTimeFactor = (timestamp/1000.0) % animationDuration;
        updateScene(usableTimeFactor);
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void updateScene(double usableTimeFactor) {
        Drawable2d bottomDrawable = (Drawable2d) mRootDrawables.get(0).getChildren().get(0);
        Drawable2d topDrawable = (Drawable2d) mRootDrawables.get(0).getChildren().get(2);

        //Both the first and second part of animation will be of "totalParts" seconds each
        boolean firstPartAnimate = usableTimeFactor <= animationDuration/2.0;

        if (firstPartAnimate) {
            // In this part for initial 1 second normal image is required
            // For next 3 second show translation from
            // +1.0 to 0.0 for top drawable and
            // -1.0 to 0.0 for bottom drawable
            animateDrawablesIntoScene(bottomDrawable, topDrawable, usableTimeFactor);
        } else {
            // Initial 1 second show normal image
            // For next 3 second , translate from :
            // 0 to -1 for top drawable
            // 0 to 1 for bottom drawable
            usableTimeFactor -= animationDuration/2.0;
            animateDrawablesOutOfScene(bottomDrawable, topDrawable, usableTimeFactor);
        }
    }

    private void animateDrawablesIntoScene(Drawable2d bottomDrawable,
                                           Drawable2d topDrawable,
                                           double usableTimeFactor) {
        //ALl these values are in seconds
        double initialAnimateTime = animationDuration/12;
        double translateToNewPointTime = animationDuration/6.0;

        if (usableTimeFactor <= initialAnimateTime) {
            topDrawable.setDefaultTranslate(0.0, topDrawable.getDefaultTranslate()[1]);
            bottomDrawable.setDefaultTranslate(0.0, bottomDrawable.getDefaultTranslate()[1]);
        }
        else if (usableTimeFactor <= translateToNewPointTime) {
            topDrawable.setDefaultTranslate(1.0, topDrawable.getDefaultTranslate()[1]);
            bottomDrawable.setDefaultTranslate(-1.0, bottomDrawable.getDefaultTranslate()[1]);
        }
        else {
            double movementFactor = Math.pow(usableTimeFactor/2, 5);
            topDrawable.setDefaultTranslate(Math.max(1.0 - movementFactor, 0.0), topDrawable.getDefaultTranslate()[1]);
            bottomDrawable.setDefaultTranslate(Math.min(-1.0 + movementFactor, 0.0), bottomDrawable.getDefaultTranslate()[1]);
        }

    }

    private void animateDrawablesOutOfScene(Drawable2d bottomDrawable,
                                            Drawable2d topDrawable,
                                            double usableTimeFactor) {
        double initialAnimateTime = animationDuration/6.0;

        if (usableTimeFactor <= initialAnimateTime) {
            topDrawable.setDefaultTranslate(0.0, topDrawable.getDefaultTranslate()[1]);
            bottomDrawable.setDefaultTranslate(0.0, bottomDrawable.getDefaultTranslate()[1]);
        } else {
            double movementFactor = Math.pow(usableTimeFactor/2, 5);
            if (topDrawable.getDefaultTranslate()[0]  > -1.0 || bottomDrawable.getDefaultTranslate()[0] < 1.0) {
                topDrawable.setDefaultTranslate(Math.max(-1.0, -movementFactor), topDrawable.getDefaultTranslate()[1]);
                bottomDrawable.setDefaultTranslate(Math.min(1.0, movementFactor), bottomDrawable.getDefaultTranslate()[1]);
            }
        }
    }
}
