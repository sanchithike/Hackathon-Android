package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.TreeNode;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 3/9/18.
 */

public class HorizontalMirrorScene extends ScreenSplitBase {

    public HorizontalMirrorScene(ArrayList<ImageSource> imageSources,
                               SceneManager.SceneDescription sceneDescription) {
        super("HorizontalMirrorScene", sceneDescription,2, 1, imageSources, false, false);
        assignFilterModesToDrawables();
    }

    private void assignFilterModesToDrawables() {
        Drawable2d rootDrawables = (Drawable2d) mRootDrawables.get(0);
        SparseArray<TreeNode> allDrawables = rootDrawables.getChildren();
        if (allDrawables.size() == 2) {
            Drawable2d bottomDrawable = (Drawable2d) allDrawables.get(0);
            setFilterMode(bottomDrawable, FilterManager.HORIZONTAL_MIRROR_FILTER);
        }
    }
}
