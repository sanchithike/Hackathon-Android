package com.roposo.creation.graphics.scenes.screenSplitScenes;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import java.util.ArrayList;

/**
 * Created by akshaychauhan on 1/15/18.
 */

public class SimpleGridSplit extends ScreenSplitBase {

    public SimpleGridSplit(ArrayList<ImageSource> imageSources,
                           SceneManager.SceneDescription sceneDescription,
                           int rows,
                           int columns,
                           boolean splitview,
                           long captureTimestamp) {
        super("SimpleGridSplit", sceneDescription, 4, captureTimestamp);
        for (int i = 0; i < mRootDrawables.size(); i++) {
            mRootDrawables.get(i).setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            addDrawablesIndividually(i, sceneDescription, rows + (i * 2), columns + (i * 2), imageSources, splitview);
        }
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        updateScene(renderTargetType, timestamp);
    }

    private void updateScene(OpenGLRenderer.Fuzzy renderTargetType ,long timestamp) {
        float totalParts = mRootDrawables.size()/4;
        float partVal = (((float) Math.abs(timestamp) / 1000.0f) % (mRootDrawables.size()/2));

        boolean partOneAnimation = partVal < totalParts;
        if (partOneAnimation) {
            long animationPart = (int) (partVal * 4);
            OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get((int)animationPart));
        }
        else {
            partVal -= totalParts;
            partVal = Math.max(0, ((mRootDrawables.size() - 1) - (int)(partVal * 4))) ;
            OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get((int)partVal));
        }
    }

}
