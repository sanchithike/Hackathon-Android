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
import static com.roposo.creation.graphics.gles.GraphicsUtils.VERBOSE;

/**
 * This effect will create three mirrored drawables with mono effect over the centre drawable and
 * red overlay over the remaining two drawables. The left and right drawables gradually translate into
 * the scene
 * Created by akshaychauhan on 12/29/17.
 */
public class ThreeMirrorSplit extends ScreenSplitBase {

    private boolean animate = true;

    public ThreeMirrorSplit(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription, long captureTimestamp) {
        super("ThreeMirrorSplit", sceneDescription ,1, 3, imageSources, false, false, captureTimestamp);
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        if (timestamp == 0) {
            initialEffectAndVisibility();
        }
        updateScene(timestamp);
        // normal draw - directly to display
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void initialEffectAndVisibility() {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();

        for (int i = 0; i < allDrawables.size(); i++) {
            Drawable2d drawable2d = (Drawable2d) allDrawables.get(i);

            if (i % 2 != 0) {
                setFilterMode(drawable2d, FilterManager.MONO_FILTER);
            } else {
                setFilterMode(drawable2d, FilterManager.RED_COLOR_OVERLAY_FILTER);
                setVisiblity(drawable2d, false);
            }
        }
    }

    private void updateScene(long timestamp) {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();
        if (allDrawables.size() == 3) {
            Drawable2d leftDrawable = (Drawable2d) allDrawables.get(0);
            Drawable2d middleDrawable = (Drawable2d) allDrawables.get(1);
            Drawable2d rightDrawable = (Drawable2d) allDrawables.get(2);

            float partVal = ((float) timestamp / 1000) % getGridColumns();
            float normalisedVal = partVal / getGridColumns();

            if (animate) {
                if (VERBOSE) {
                    Log.d(TAG, " Left Position - x : " + leftDrawable.getDefaultTranslate()[0] +
                            " and y : " + leftDrawable.getDefaultTranslate()[1] +
                            " Middle Position - x : " + middleDrawable.getDefaultTranslate()[0] +
                            " and y : " + middleDrawable.getDefaultTranslate()[1] +
                            " Right Position - x : " + rightDrawable.getDefaultTranslate()[0] +
                            " and y : " + rightDrawable.getDefaultTranslate()[1]);
                }

                float translateValue = Math.min((float) Math.sqrt(normalisedVal / getGridColumns()), 1.0f / getGridColumns());

                leftDrawable.setDefaultTranslate(-0.66f + translateValue, leftDrawable.getDefaultTranslate()[1]);
                rightDrawable.setDefaultTranslate(0.66f - translateValue, rightDrawable.getDefaultTranslate()[1]);

                if (partVal < 1.0f) {
                    setVisiblity(leftDrawable, true);
                    setVisiblity(rightDrawable, true);
                } else {
                    setFilterMode(leftDrawable, FilterManager.IMAGE_FILTER);
                    setFilterMode(rightDrawable, FilterManager.IMAGE_FILTER);
                }
                animate = partVal < getGridColumns() - 0.1;
            }
        }
    }

}
