package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.Log;
import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by akshaychauhan on 1/18/18.
 */

public class RoposoQuadrant extends ScreenSplitBase {

    /**
     * Drawables numbering is :
     * 2 3
     * 0 1
     * <p>
     * The original filter modes are :
     * Y M
     * R C
     * <p>
     * These are rotated in the clockwise direction.
     */

    private static final List<String> filterModes = Arrays.asList(FilterManager.RED_BLEND_HUE_FILTER,
            FilterManager.CYAN_BLEND_HUE_FILTER, FilterManager.YELLOW_BLEND_HUE_FILTER,
            FilterManager.MAGENTA_BLEND_HUE_FILTER);

    private static final int[][] filterForDrawableAndTick = {{0, 1, 3, 2},
                                                            {1, 3, 2, 0},
                                                            {2, 0, 1, 3},
                                                            {3, 2, 0, 1}};    // rows = drawable numbers, columns = tick number

    public RoposoQuadrant(ArrayList<ImageSource> imageSources,
                          SceneManager.SceneDescription sceneDescription,
                          boolean splitview,
                          long captureTimestamp) {
        super("RoposoQuadrant", sceneDescription, 2, 2, imageSources, splitview, false, captureTimestamp);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        updateScene(Math.ceil(timestamp/1000.0f));
        // normal draw - directly to display
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    //if two seconds have passed by, change the filter
    private void updateScene(double timestamp) {
        //important - draw is not called at every value of timestamp.
        Drawable2d rootDrawable = (Drawable2d) mRootDrawables.get(0);
        SparseArray<TreeNode> childDrawables = rootDrawable.getChildren();
        if (filterModes.size() != childDrawables.size()) {
            Log.e(TAG, "Please check the filter-modes and drawable size");
        }
        for (int i = 0; i < childDrawables.size(); i++) {
            Drawable2d childDrawable = (Drawable2d) childDrawables.get(i);
            int mode = filterForDrawableAndTick[i][(int)timestamp % 4];
            setFilterMode(childDrawable, filterModes.get(mode));
        }
    }

}
