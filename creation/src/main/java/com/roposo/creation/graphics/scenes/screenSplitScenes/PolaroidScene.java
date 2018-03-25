package com.roposo.creation.graphics.scenes.screenSplitScenes;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 1/13/18.
 */

public class PolaroidScene extends ScreenSplitBase {

    public PolaroidScene(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription) {
        this(imageSources, sceneDescription , FilterManager.ROPOSO_QUADRANT_FILTER);
    }

    private PolaroidScene(ArrayList<ImageSource> imageSources,
                         SceneManager.SceneDescription sceneDescription,
                         String filterMode) {
        super("PolaroidScene", sceneDescription, 500);

        mRootDrawables.get(0).setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);

        Drawable2d backgroundDrawable = setDrawableProperties(
                imageSources,
                sceneDescription,
                1.0f,
                1.0f,
                0.0f,
                0.0f);

        backgroundDrawable.setFilterMode(filterMode);

        Drawable2d foregroundDrawable = setDrawableProperties(imageSources,
                 sceneDescription,
                0.8f,
                0.7f,
                0.0f,
                0.1f);

        mRootDrawables.get(0).addChild(backgroundDrawable);
        mRootDrawables.get(0).addChild(foregroundDrawable);

    }

    private Drawable2d setDrawableProperties(ArrayList<ImageSource> imageSources,
                                             SceneManager.SceneDescription sceneDescription,
                                             float geometryWidth,
                                             float geometryHeight,
                                             float translateX,
                                             float translateY) {
        Drawable2d drawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
        drawable.setDefaultGeometryScale(geometryWidth, geometryHeight);
        drawable.setDefaultTranslate(translateX, translateY);

        return drawable;
    }
}
