package com.roposo.creation.graphics.scenes;

import android.graphics.RectF;
import android.support.annotation.FloatRange;

import com.roposo.creation.graphics.SceneManager;

/**
 * @author akshaychauhan on 2/3/18.
 */

public interface ISceneAdjustments {

    void setSaturation(float saturation);

    void setContrast(float contrast);

    void setBrightness(float brightness);

    void setSceneRotateInZ(double sceneRotateInZ);

    void setSceneZoom(double[] sceneZoom);

    void setSceneTranslateByInfo(double[] sceneTranslateByInfo);

    void setSceneRectFInfo(RectF rectFInfo);

    SceneManager.SceneDescription cropMedia();

    void addLut(SceneManager.LutDescription leftLut, SceneManager.LutDescription rightLut,
                @FloatRange(from = 0F, to = 1F) float transitionPoint,
                @FloatRange(from = 0F, to = 1F) float intensity);
}
