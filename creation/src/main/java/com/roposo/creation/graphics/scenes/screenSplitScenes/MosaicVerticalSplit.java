package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.TreeNode;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This effect first divide the screen in 1 * 3 drawables , the 0th and 2nd drawables are further
 * divided into 3 parts row-wise.
 * The drawables are then loaded in a pre-decided order with required filters applied to them.
 * Once all the drawables are loaded they go off screen in reverse order of their arrival sequence
 * Created by akshaychauhan on 1/3/18.
 */

public class MosaicVerticalSplit extends ScreenSplitBase {

    private final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private List<Drawable2d> sceneDrawablesList = new ArrayList<>();
    private List<Drawable2d> sceneDrawablesListInverse;
    private double animationTimeInSeconds = 5.6;
    private boolean partOneComplete = false;
    private static List<String> requiredFiltersSet = Arrays.asList(FilterManager.BRIGHT_TO_FADE_FILTER, FilterManager.IMAGE_FILTER);

    public MosaicVerticalSplit(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription, long captureTimestamp) {
        super("MosaicVerticalSplit", sceneDescription, captureTimestamp);
        int gridColumns = 3;
        int gridRows = 1;
        Drawable mRootDrawable = mRootDrawables.get(0);
        mRootDrawable.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.WRAP_CONTENT);
        ((Drawable2d) mRootDrawables.get(0)).addImageSource(imageSources.get(0));
        mRootDrawables.get(0).setVisibility(false);

        double width = (1.0 / gridColumns);
        double height = (1.0 / gridRows);

        for (int j = 0; j < gridRows; j++) {
            for (int i = 0; i < gridColumns; i++) {
                double xTranslate = (i - (gridColumns - 1.0) / 2.0) * width;
                Drawable2d columnDrawable = new Drawable2d();
                columnDrawable.setDefaultTranslate(xTranslate, 0.0f);

                Drawable2d drawables[];
                if (i % 2 == 0) {
                    drawables = new Drawable2d[3]; // Divide odd numbered ( 0 and 2 in this case in 3 drawables)
                } else {
                    drawables = new Drawable2d[1];
                }

                for (int k = 0; k < drawables.length; k++) {
                    double yTranslate = (k - (drawables.length - 1.0) / 2.0) * height / drawables.length;

                    float minX = (float) i / gridColumns;
                    float maxX = (float) (i + 1) / gridColumns;

                    float minY = (float) k / drawables.length;
                    float maxY = (float) (k + 1) / drawables.length;

                    drawables[k] = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);
                    drawables[k].setImageSource(imageSources.get(0), new RectF(minX, minY, maxX, maxY));

                    drawables[k].setDefaultTranslate(0.0f, yTranslate);
                    drawables[k].setDefaultGeometryScale(width, height / drawables.length);

                    if (VERBOSE) {
                        Log.d(TAG, "For i = " + i + " and j =" + j +
                                " Translate X : " + xTranslate + ", Y :" + yTranslate);
                        Log.d(TAG, "Rect values : minx " + minX + ", minY " + minY +
                                ", maxX " + maxX + " and maxY " + maxY);
                    }

                    columnDrawable.addChild(drawables[k]);
                }
                mRootDrawable.addChild(columnDrawable);
            }
        }
        createSceneDrawablesList();
        setVisibility(false);
    }

    private void createSceneDrawablesList() {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();

        //Three Parent Drawables
        if (allDrawables.size() == 3) {
            Drawable2d leftParentDrawable = (Drawable2d) allDrawables.get(0);
            Drawable2d middleParentDrawable = (Drawable2d) allDrawables.get(1);
            Drawable2d rightParentDrawable = (Drawable2d) allDrawables.get(2);

            //First add the middle drawable
            sceneDrawablesList.add(middleParentDrawable);

            SparseArray<TreeNode> leftDrawables = leftParentDrawable.getChildren();
            SparseArray<TreeNode> rightDrawables = rightParentDrawable.getChildren();

            int leftSize = leftDrawables.size();
            int rightSize = rightDrawables.size();

            if (leftSize == rightSize) {
                for (int i = 0; i < leftSize; i++) {
                    //The drawables in left and right drawable are added in opposite order
                    sceneDrawablesList.add((Drawable2d) leftDrawables.get(leftSize - i - 1));
                    sceneDrawablesList.add((Drawable2d) rightDrawables.get(i));
                }
                sceneDrawablesListInverse = new ArrayList<>(sceneDrawablesList);
                Collections.reverse(sceneDrawablesListInverse);
            } else {
                Log.e(TAG, "Check the drawables dimensions");
            }
        }
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timestamp) {
        double usableTimeStampInSeconds = (timestamp/1000.0) % animationTimeInSeconds; //5.6 seconds is total animation time
        updateScene(usableTimeStampInSeconds);
        OpenGLRenderer.getRenderer(renderTargetType).drawFrame(mRootDrawables.get(0));
    }

    private void setVisibility(boolean visibility) {
        SparseArray<TreeNode> allDrawables = mRootDrawables.get(0).getChildren();
        for (int i = 0; i < allDrawables.size(); i++) {
            Drawable2d drawable2d = (Drawable2d) allDrawables.get(i);
            setVisiblity(drawable2d, visibility);
        }
    }


    private void updateScene(double usableTimeFactor) {
        boolean partOneAnimation = usableTimeFactor < (animationTimeInSeconds/2.0);

        //Part One animation is switching on visibility of the drawables in the scene
        if (partOneAnimation) {
            Pair<Integer, List<String>> drawableNumberAndFilters = findDrawableNumberAndFilters(usableTimeFactor);

            setDrawablesFilters(drawableNumberAndFilters.first,
                    usableTimeFactor,
                    (int) usableTimeFactor + 0.10f,
                    drawableNumberAndFilters.second,
                    true,
                    false);

        } else {
            // In case of capture if timestamp directly comes over here , we ensure that initially all drawable's
            // visibility is turned on then we move ahead for animation
            if (!partOneComplete) {
                setVisibility(true);
                partOneComplete = true;
            }

            usableTimeFactor = usableTimeFactor - (animationTimeInSeconds/2);
            Pair<Integer, List<String>> drawableNumberAndFilters = findDrawableNumberAndFilters(usableTimeFactor);
            float limit = (int) usableTimeFactor + 0.10f;
            boolean visibility = usableTimeFactor < limit;

            setDrawablesFilters(drawableNumberAndFilters.first,
                    usableTimeFactor,
                    limit,
                    Arrays.asList(FilterManager.BRIGHT_TO_FADE_FILTER),
                    visibility,
                    true);
        }
    }

    private Pair<Integer, List<String>> findDrawableNumberAndFilters (double timeValue) {
        int drawableNumber = 0;

        if (isBetween(timeValue, 0f, 0.4f)) {
            drawableNumber = 0;
        }
        else if (isBetween(timeValue, 0.4f, 0.8f)) {
            drawableNumber = 1;
        }
        else if (isBetween(timeValue, 0.8f, 1.2f)) {
            drawableNumber = 2;
        }
        else if (isBetween(timeValue, 1.2f, 1.6f)) {
            drawableNumber = 3;
        }
        else if (isBetween(timeValue, 1.6f, 2.0f)) {
            drawableNumber = 4;
        }
        else if (isBetween(timeValue, 2.0f, 2.4f)) {
            drawableNumber = 5;
        }
        else if (isBetween(timeValue, 2.4f, 2.8f)) {
            drawableNumber = 6;
        }

        return new Pair<>(drawableNumber, requiredFiltersSet);
    }

    private static boolean isBetween(double x, double lower, double upper) {
        return lower <= x && x < upper;
    }

    private void setDrawablesFilters(Integer drawableNumber,
                                     double partVal,
                                     double limit,
                                     List<String> filterModes,
                                     boolean visibility,
                                     boolean reverse) {
        Drawable2d drawable2d;
        drawable2d = getDrawable2d(drawableNumber, reverse);

        int numberOfFilters = filterModes.size();
        int filterCount = (int) (partVal / limit) % numberOfFilters;
        if (visibility) {
            setFilterMode(drawable2d, filterModes.get(filterCount));
        }

        // This is done to ensure that in case of capture , the scene is set in accordance to expected state
        for (int i = 0; i <= drawableNumber; i++) {
            // Don't switch off visibility of middle drawable AND
            // In case of reverse animation if visibility is on , then don't repeat operation for
            // all preceding drawables
            if ((!visibility &&
                    (((!reverse && i == 0)) || i == sceneDrawablesList.size() - 1 && reverse)) ||
                (reverse && visibility)) {
                continue;
            }

            drawable2d = getDrawable2d(i, reverse);
            setVisiblity(drawable2d, visibility);
        }

        // For Part One Animation , when we reach last drawable to show , we mark the
        // part one animation to be complete
        if (!reverse && (drawableNumber == sceneDrawablesList.size() -1)) {
            partOneComplete = true;
        }
    }

    private Drawable2d getDrawable2d(Integer drawableNumber, boolean reverse) {
        Drawable2d drawable2d;
        if (reverse) {
            drawable2d = sceneDrawablesListInverse.get(drawableNumber);
        }
        else {
            drawable2d = sceneDrawablesList.get(drawableNumber);
        }
        return drawable2d;
    }

    @Override
    public Scene clone() {
        MosaicVerticalSplit scene = (MosaicVerticalSplit) super.clone();
        Drawable2d drawable2d = (Drawable2d) scene.mRootDrawables.get(0);
        SparseArray<TreeNode> childDrawables = drawable2d.getChildren();
        cloneDrawableLists(scene, childDrawables);
        return scene;
    }

    private void cloneDrawableLists(MosaicVerticalSplit scene, SparseArray<TreeNode> childDrawables) {
        int childDrawableSize = childDrawables.size();
        if (childDrawableSize == 3) {
            List<Drawable2d> clonedSceneDrawableList = new ArrayList<>();
            List<Drawable2d> clonedSceneDrawableReverseList;

            Drawable2d leftDrawable = (Drawable2d) childDrawables.get(0);
            Drawable2d middleDrawable = (Drawable2d) childDrawables.get(1);
            Drawable2d rightDrawable = (Drawable2d) childDrawables.get(2);

            int leftDrawableChildrenSize = leftDrawable.getChildren().size();
            int rightDrawableChildrenSize = rightDrawable.getChildren().size();

            if (leftDrawableChildrenSize == 3 && rightDrawableChildrenSize == 3) {
                clonedSceneDrawableList.add(middleDrawable);
                for (int i = 0; i < leftDrawableChildrenSize; i++) {
                    //The drawables in left and right drawable are added in opposite order
                    clonedSceneDrawableList.add((Drawable2d) leftDrawable.getChildAt(leftDrawableChildrenSize - i - 1));
                    clonedSceneDrawableList.add((Drawable2d) rightDrawable.getChildAt(i));
                }

                scene.sceneDrawablesList = clonedSceneDrawableList;
                clonedSceneDrawableReverseList = new ArrayList<>(clonedSceneDrawableList);
                Collections.reverse(clonedSceneDrawableReverseList);
                scene.sceneDrawablesListInverse = clonedSceneDrawableReverseList;
            }

        } else {
            Log.e(TAG, "Check size of child drawables in scene " + childDrawableSize);
        }
    }
}
