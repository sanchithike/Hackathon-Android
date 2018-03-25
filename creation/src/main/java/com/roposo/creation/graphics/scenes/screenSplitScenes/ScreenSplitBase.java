package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.TreeNode;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.roposo.creation.graphics.gles.GraphicsUtils.VERBOSE;

/**
 * This is going to be the base class for the screen split effect
 * We will provide the number of rows and columns we want to split the screen into,
 * Also we will provide the info whether we want to split the view or
 * repeat the view in different drawables
 * Created by akshaychauhan on 12/28/17.
 */

public abstract class ScreenSplitBase extends Scene {
    protected String TAG = "ScreenSplitBase";
    private int gridRows;
    private int gridColumns;

    ScreenSplitBase(String tag, SceneManager.SceneDescription sceneDescription, long captureTimestamp) {
        this(tag, sceneDescription, 1, captureTimestamp);
    }

    ScreenSplitBase(String tag,
                    SceneManager.SceneDescription sceneDescription,
                    int gridRows,
                    int gridColumns,
                    ArrayList<ImageSource> imageSources,
                    boolean splitview,
                    boolean addRowWise) {
        this(tag, sceneDescription, gridRows, gridColumns, imageSources, splitview, addRowWise, 500);
    }

    ScreenSplitBase(String tag,
                    SceneManager.SceneDescription sceneDescription,
                    int gridRows,
                    int gridColumns,
                    ArrayList<ImageSource> imageSources,
                    boolean splitview,
                    boolean addRowWise,
                    long captureTimestamp) {
        this(tag, sceneDescription, captureTimestamp);
        mRootDrawables.get(0).setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.WRAP_CONTENT);
        ((Drawable2d) mRootDrawables.get(0)).addImageSource(imageSources.get(0));
        mRootDrawables.get(0).setVisibility(false);

        this.gridRows = gridRows;
        this.gridColumns = gridColumns;

        if (addRowWise) {
            addDrawablesRowWise(0, sceneDescription, gridRows, gridColumns, imageSources, splitview);
        } else {
            addDrawablesIndividually(0, sceneDescription, gridRows, gridColumns, imageSources, splitview);
        }
    }

    ScreenSplitBase(String tag, SceneManager.SceneDescription sceneDescription, int numberOfRootDrawables, long captureTimestamp) {
        super(numberOfRootDrawables, sceneDescription);
        if (tag != null) {
            this.TAG = tag;
        }
        this.mPreferredCaptureTimestamp = captureTimestamp;
    }

    int getGridRows() {
        return gridRows;
    }

    int getGridColumns() {
        return gridColumns;
    }

    /**
     * This method will add the drawables row wise i.e one row will be the parent
     * of all the drawables created within it , this row will in turn will be the child of the
     * root scene drawable
     */
    private void addDrawablesRowWise(int rootDrawableNumber,
                                     SceneManager.SceneDescription sceneDescription,
                                     int gridRows,
                                     int gridColumns,
                                     ArrayList<ImageSource> imageSources,
                                     boolean splitview) {
        double width = (1.0 / gridColumns);
        double height = (1.0 / gridRows);

        for (int j = 0; j < gridRows; j++) {
            double yTranslate = (j - (gridRows - 1.0) / 2.0) * height;

            Drawable2d rowDrawable = new Drawable2d();
            rowDrawable.setDefaultTranslate(0.0f, yTranslate);

            for (int i = 0; i < gridColumns; i++) {
                Drawable2d drawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);

                double xTranslate = (i - (gridColumns - 1.0) / 2.0) * width;

                if (VERBOSE) {
                    Log.w(TAG, "For i = " + i + " and j =" + j +
                            " Translate X : " + xTranslate + ", Y :" + yTranslate);
                }

                drawable.setDefaultTranslate(xTranslate, 0.0f);
                drawable.setDefaultGeometryScale(width, height);

                if (splitview) {
                    float minX = (float) i / gridColumns;
                    float maxX = (float) (i + 1) / gridColumns;

                    float minY = (float) j / gridRows;
                    float maxY = (float) (j + 1) / gridRows;
                    drawable.getImageSources().get(0).setRegionOfInterest(new RectF(minX, minY, maxX, maxY), drawable);
                }
                rowDrawable.addChild(drawable);
            }
            // Since the super (Scene) is called , it will add the required drawable2d to mRootDrawables
            // and no NPE should occur
            if (mRootDrawables.size() < rootDrawableNumber) {
                mRootDrawables.get(rootDrawableNumber).addChild(rowDrawable);
            }
        }
    }

    /**
     * This method will add the drawables individually to the screen drawable
     */
    void addDrawablesIndividually(int rootDrawableNumber,
                                  SceneManager.SceneDescription sceneDescription,
                                  int gridRows,
                                  int gridColumns,
                                  ArrayList<ImageSource> imageSources,
                                  boolean splitview) {
        double width = (1.0 / gridColumns);
        double height = (1.0 / gridRows);

        for (int j = 0; j < gridRows; j++) {
            for (int i = 0; i < gridColumns; i++) {
                Drawable2d drawable = createSceneDrawableFromSceneDescription(imageSources, sceneDescription);

                double xTranslate = (i - (gridColumns - 1) / 2.0) * width;
                double yTranslate = (j - (gridRows - 1) / 2.0) * height;

                if (VERBOSE) {
                    Log.d(TAG, "For i = " + i + " and j =" + j +
                            " Translate X : " + xTranslate + ", Y :" + yTranslate);
                }

                drawable.setDefaultTranslate(xTranslate, yTranslate);
                drawable.setDefaultGeometryScale(width, height);

                if (splitview) {
                    float minX = (float) i / gridColumns;
                    float maxX = (float) (i + 1) / gridColumns;

                    float minY = (float) j / gridRows;
                    float maxY = (float) (j + 1) / gridRows;
                    drawable.getImageSources().get(0).setRegionOfInterest(new RectF(minX, minY, maxX, maxY), drawable);
                }
                // Since the super (Scene) is called , it will add the required drawable2d to mRootDrawables
                // and no NPE should occur
                if (mRootDrawables.size() > rootDrawableNumber) {
                    mRootDrawables.get(rootDrawableNumber).addChild(drawable);
                }
            }
        }
    }


    /**
     * This function checks the drawable children and if it's a leaf level drawable it sets the
     * required visibility value
     */
    protected void setVisiblity(Drawable2d drawable2d, boolean visibility) {
        if (!drawable2d.hasChildren()) {
            drawable2d.setVisibility(visibility);
            return;
        }

        SparseArray<TreeNode> children = drawable2d.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Drawable2d drawable = (Drawable2d) children.get(i);
            setVisiblity(drawable, visibility);
        }
    }

    protected void setAlpha(Drawable2d drawable2d, float alpha) {
        if (!drawable2d.hasChildren()) {
            drawable2d.setAlpha(alpha);
            return;
        }

        SparseArray<TreeNode> children = drawable2d.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Drawable2d drawable = (Drawable2d) children.get(i);
            setAlpha(drawable, alpha);
        }
    }

}
