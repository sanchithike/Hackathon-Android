package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.util.SparseArray;

import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.TreeNode;

import java.util.ArrayList;

/**
 * Created by akshaychauhan on 1/16/18.
 */

public class VerticalMirrorScene extends ScreenSplitBase {

    public VerticalMirrorScene(ArrayList<ImageSource> imageSources,
                               SceneManager.SceneDescription sceneDescription) {
        super("VerticalMirrorScene", sceneDescription,1, 2, imageSources, false, false);
        assignFilterModesToDrawables();
    }

    private void assignFilterModesToDrawables() {
        Drawable2d rootDrawables = (Drawable2d) mRootDrawables.get(0);
        SparseArray<TreeNode> allDrawables = rootDrawables.getChildren();
        if (allDrawables.size() == 2) {
            Drawable2d rightDrawable = (Drawable2d) allDrawables.get(1);
            setFilterMode(rightDrawable, FilterManager.VERTICAL_MIRROR_FILTER);
        }
    }

}
