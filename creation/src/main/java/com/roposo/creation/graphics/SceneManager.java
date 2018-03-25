package com.roposo.creation.graphics;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.R;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.scenes.DemoScene;
import com.roposo.creation.graphics.scenes.Scene;
import com.roposo.creation.graphics.scenes.Scene3D;
import com.roposo.creation.graphics.scenes.screenSplitScenes.HalfScreenSplit;
import com.roposo.creation.graphics.scenes.screenSplitScenes.HorizontalMirrorScene;
import com.roposo.creation.graphics.scenes.screenSplitScenes.ManifoldScreenSplit;
import com.roposo.creation.graphics.scenes.screenSplitScenes.MosaicHorizontalSplit;
import com.roposo.creation.graphics.scenes.screenSplitScenes.MosaicVerticalSplit;
import com.roposo.creation.graphics.scenes.screenSplitScenes.NewPolaroidScene;
import com.roposo.creation.graphics.scenes.screenSplitScenes.PolaroidScene;
import com.roposo.creation.graphics.scenes.screenSplitScenes.RoposoQuadrant;
import com.roposo.creation.graphics.scenes.screenSplitScenes.ScreenSplitFadeInOut;
import com.roposo.creation.graphics.scenes.screenSplitScenes.ThreeMirrorSplit;
import com.roposo.creation.graphics.scenes.screenSplitScenes.VerticalMirrorScene;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author bajaj on 10/11/17.
 *         Creates scenes based on scene id.
 */
@SuppressWarnings("PointlessBooleanExpression")
public class SceneManager {
    private static final String TAG = "SceneManager";
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    static LruSceneCache mCache = new LruSceneCache(7);

    private static Map<String, Class<? extends Scene>> sceneClassMap = new HashMap<>();

    public static Scene createScene(SceneDescription sceneDescription, ArrayList<ImageSource> imageSources) {
        Scene scene = mCache.get(sceneDescription);
        if (scene != null) {
            return scene;
        }
        if (sceneDescription.sceneName != null) {
            if (sceneClassMap != null) {
                if (sceneClassMap.containsKey(sceneDescription.sceneName)) {
                    Class<? extends Scene> sceneClass = sceneClassMap.get(sceneDescription.sceneName);
                    Class[] params = new Class[] {ArrayList.class};
                    try {
                        scene = sceneClass.getConstructor(params).newInstance(imageSources);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (scene != null) {
            return scene;
        }
        switch (sceneDescription.sceneName) {
            case SCENE_3D:
                scene = new Scene3D(imageSources);
                break;
            case SCENE_GRAINY_EFFECT:
                imageSources.add(new ImageSource(getBitmapFrom(R.mipmap.noise), false));
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.GRAINY_FILTER, 1500);
                break;
            case SCENE_BRIGHT_TO_FADE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.BRIGHT_TO_FADE_FILTER, 1500);
                break;
            case SCENE_CIRCULAR_CURTAIN:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.CIRCULAR_FILTER, 300);
                break;
            case SCENE_COLOR_REVEAL:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.COLOR_REVEAL_FILTER, 3000);
                break;
            case SCENE_CURTAIN_OPENER:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.CURTAIN_OPENER_FILTER, 2500);
                break;
            case SCENE_EDGE_GLOW:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.EDGE_GLOW_FILTER);
                break;
            case SCENE_EDGE_GLOW_IN_DARK:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.EDGE_GLOW_IN_DARKNESS_FILTER);
                break;
            case SCENE_FADE_IN_OUT:
                scene = new ScreenSplitFadeInOut(imageSources, sceneDescription, 1000);
                break;
            case SCENE_GHOST_EFFECT:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.GHOST_EFFECT_FILTER);
                break;
            case SCENE_GRAY_SCALE_FLICKER:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.GRAY_SCALE_FLICKER_FILTER, 0);
                break;
            case SCENE_GRID:
                scene = new ManifoldScreenSplit(imageSources, sceneDescription, 2000);
                break;
            case SCENE_HALF_SCREEN_SPLIT:
                scene = new HalfScreenSplit(imageSources, sceneDescription, 1000);
                break;
            case SCENE_MIRROR:
                scene = new VerticalMirrorScene(imageSources, sceneDescription);
                break;
            case SCENE_MIRROR2:
                scene = new HorizontalMirrorScene(imageSources, sceneDescription);
                break;
            case SCENE_MOSAIC_VERTICAL:
                scene = new MosaicVerticalSplit(imageSources, sceneDescription, 7000);
                break;
            case SCENE_MOSAIC_HORIZONTAL:
                scene = new MosaicHorizontalSplit(imageSources, sceneDescription, 2500);
                break;
            case SCENE_POLAROID:
                scene = new PolaroidScene(imageSources, sceneDescription);
                break;
            case SCENE_POLAROID_TWO:
                scene = new NewPolaroidScene(imageSources, sceneDescription, FilterManager.WHITE_FILTER, 3900);
                break;
            case SCENE_POSTERIZE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.POSTERIZE_FILTER);
                break;
            case SCENE_RADIAL_BLUR:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RADIAL_BLUR_FILTER);
                break;
            case SCENE_RED_FLICKER_WITH_NOISE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RED_FLICKER_WITH_NOISE_FILTER, 3000);
                break;
            case SCENE_RAINBOW:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RAINBOW_FILTER);
                break;
            case SCENE_RGB_OFFSET:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RGBOFFSET_FILTER);
                break;
            case SCENE_ROPOSO_QUADRANT_REPEAT:
                scene = new RoposoQuadrant(imageSources, sceneDescription, false, 2000);
                break;
            case SCENE_ROTATE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.ROTATE_FILTER, 2000);
                break;
            case SCENE_SNOW:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.SNOW_FLAKES_FILTER);
                break;
            case SCENE_THREE_MIRROR_SPLIT:
                scene = new ThreeMirrorSplit(imageSources, sceneDescription, 3000);
                break;
            case SCENE_TOON:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.TOON_FILTER);
                break;
            case SCENE_TRICOLOR:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.TRICOLOR_FILTER, 3333);
                break;
            case SCENE_WHITE_FLICKER:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.WHITE_DISCO_LIGHTS_FILTER);
                break;
            case SCENE_WHITE_AREA_ANIMATE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.WHITE_AREA_ANIMATE_FILTER);
                break;
            case SCENE_ZOOM_OUT:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.ZOOM_OUT_FILTER);
                break;
            case SCENE_HEXA:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.HEXA_FILTER);
                break;
            case SCENE_RED_HYPNO:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RED_HYPNO_FILTER, 1500);
                break;
            case SCENE_RIPPLE:
                scene = new DemoScene(imageSources, sceneDescription, FilterManager.RIPPLE_FILTER, 1500);
                break;
//            case SCENE_SHINE:
//                scene = new ShineScene(imageSources, sceneDescription);
//                break;
            default: {
                    scene = new DemoScene(imageSources, sceneDescription, FilterManager.IMAGE_FILTER);
            }
        }
        return scene;
    }

    // Currently for a scene , shallow copy of a sceneDescription is used by all the three pipelines
    public static class SceneDescription implements Cloneable, Parcelable {
        public SceneName sceneName;

        private boolean isTuningEnabled = false;

        private float saturation = 0.5f;
        private float contrast = 0.5f;
        private float brightness = 0.5f;

        private double sceneRotateInZ = 0.0; // Rotate about z axis
        private double[] sceneZoom = {1.0, 1.0, 1.0};
        private double[] sceneTranslateInfo = {0.0, 0.0, 0.0};
        private double[] sceneTranslateByInfo = {0.0, 0.0, 0.0};
        private RectF sceneRectfInfo = new RectF(0.0f, 0.0f, 1.0f, 1.0f); // Initial RectF values set in scene are according to UI coordinates , we convert themDem to texture coordinates when fetching their value
        private double[] defaultGeometryScale = {1.0, 1.0, 1.0};
        private boolean isCropped = false;

        private float lutIntensity = 1.0f;
        private float lutTransitionPoint = 1.0f;
        private float leftLutFlag = 1.0f;
        private float rightLutFlag = 0.0f;
        private LutDescription leftLutDescription;
        private LutDescription rightLutDescription;

        protected SceneDescription(Parcel in) {
            sceneName = SceneName.values()[in.readInt()];
            isTuningEnabled = in.readByte() != 0;
            saturation = in.readFloat();
            contrast = in.readFloat();
            brightness = in.readFloat();
            sceneRotateInZ = in.readDouble();
            sceneZoom = in.createDoubleArray();
            sceneTranslateInfo = in.createDoubleArray();
            sceneTranslateByInfo = in.createDoubleArray();
            sceneRectfInfo = in.readParcelable(RectF.class.getClassLoader());
            defaultGeometryScale = in.createDoubleArray();
            isCropped = in.readByte() != 0;
            lutIntensity = in.readFloat();
            lutTransitionPoint = in.readFloat();
            leftLutFlag = in.readFloat();
            rightLutFlag = in.readFloat();
            leftLutDescription = LutDescription.values()[in.readInt()];
            rightLutDescription = LutDescription.values()[in.readInt()];
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            int sceneOrdinal = sceneName != null ? sceneName.ordinal() : 0;
            dest.writeInt(sceneOrdinal);
            dest.writeByte((byte) (isTuningEnabled ? 1 : 0));
            dest.writeFloat(saturation);
            dest.writeFloat(contrast);
            dest.writeFloat(brightness);
            dest.writeDouble(sceneRotateInZ);
            dest.writeDoubleArray(sceneZoom);
            dest.writeDoubleArray(sceneTranslateInfo);
            dest.writeDoubleArray(sceneTranslateByInfo);
            dest.writeParcelable(sceneRectfInfo, flags);
            dest.writeDoubleArray(defaultGeometryScale);
            dest.writeByte((byte) (isCropped ? 1 : 0));
            dest.writeFloat(lutIntensity);
            dest.writeFloat(lutTransitionPoint);
            dest.writeFloat(leftLutFlag);
            dest.writeFloat(rightLutFlag);
            int leftLutOrdinal = leftLutDescription != null ? leftLutDescription.ordinal() : 0;
            dest.writeInt(leftLutOrdinal);
            int rightLutOrdinal = rightLutDescription != null ? rightLutDescription.ordinal() : 0;
            dest.writeInt(rightLutOrdinal);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SceneDescription> CREATOR = new Creator<SceneDescription>() {
            @Override
            public SceneDescription createFromParcel(Parcel in) {
                return new SceneDescription(in);
            }

            @Override
            public SceneDescription[] newArray(int size) {
                return new SceneDescription[size];
            }
        };

        public SceneName getSceneName() {
            return sceneName;
        }

        public boolean isTuningEnabled() {
            return isTuningEnabled;
        }

        public void setTuningEnabled(boolean tuningEnabled) {
            this.isTuningEnabled = tuningEnabled;
        }

        public float getSaturation() {
            return saturation;
        }

        public void setSaturation(float saturation) {
            this.saturation = saturation;
        }

        public float getContrast() {
            return contrast;
        }

        public void setContrast(float contrast) {
            this.contrast = contrast;
        }

        public float getBrightness() {
            return brightness;
        }

        public void setBrightness(float brightness) {
            this.brightness = brightness;
        }

        public double getSceneRotateInZ() {
            return sceneRotateInZ;
        }

        public void setSceneRotateInZ(double sceneRotateInZ) {
            this.sceneRotateInZ = sceneRotateInZ;
        }

        public double[] getSceneZoom() {
            return sceneZoom;
        }

        public void setSceneZoom(double[] sceneZoom) {
            this.sceneZoom = sceneZoom;
        }

        public double[] getSceneTranslateInfo() {
            return sceneTranslateInfo;
        }

        public void setSceneTranslateInfo(double[] sceneTranslateInfo) {
            this.sceneTranslateInfo = sceneTranslateInfo;
        }

        public double[] getSceneTranslateByInfo() {
            return sceneTranslateByInfo;
        }

        public void setSceneTranslateByInfo(double[] sceneTranslateByInfo) {
            this.sceneTranslateByInfo = sceneTranslateByInfo;
        }

        public RectF getSceneRectfInfo() {
            return sceneRectfInfo;
        }

        public void setSceneRectfInfo(RectF sceneRectfInfo) {
            this.sceneRectfInfo = sceneRectfInfo;
        }

        public RectF getSceneTextureConvertedRectFInfo() {
            return convertUICoordinatesToTextureCoordinates(sceneRectfInfo);
        }

        public double[] getDefaultGeometryScale() {
            return defaultGeometryScale;
        }

        public void setDefaultGeometryScale(double[] geometryScale) {
            this.defaultGeometryScale = geometryScale;
        }

        public boolean isCropped() {
            return isCropped;
        }

        public void setCropped(boolean cropped) {
            isCropped = cropped;
        }

        public LutDescription getLeftLutDescription() {
            return leftLutDescription;
        }

        public void setLeftLutDescription(LutDescription leftLutDescription) {
            this.leftLutDescription = leftLutDescription;
        }

        public LutDescription getRightLutDescription() {
            return rightLutDescription;
        }

        public void setRightLutDescription(LutDescription rightLutDescription) {
            this.rightLutDescription = rightLutDescription;
        }

        public float getLutIntensity() {
            return lutIntensity;
        }

        public void setLutIntensity(float lutIntensity) {
            this.lutIntensity = lutIntensity;
        }

        public float getLutTransitionPoint() {
            return lutTransitionPoint;
        }

        public void setLutTransitionPoint(float lutTransitionPoint) {
            this.lutTransitionPoint = lutTransitionPoint;
        }

        public float getLeftLutFlag() {
            return leftLutFlag;
        }

        public void setLeftLutFlag(float leftLutFlag) {
            this.leftLutFlag = leftLutFlag;
        }

        public float getRightLutFlag() {
            return rightLutFlag;
        }

        public void setRightLutFlag(float rightLutFlag) {
            this.rightLutFlag = rightLutFlag;
        }

        public SceneDescription(SceneName name) {
            this.sceneName = name;
        }

        public LutDescription getCurrentLutDesc() {
            if (leftLutFlag == 1.0 && rightLutFlag == 0.0) {
                return leftLutDescription;
            }
            if (leftLutFlag == 0.0 && rightLutFlag == 1.0) {
                return rightLutDescription;
            }
            if (leftLutFlag == 1.0 && rightLutFlag == 1.0) {
                if (lutTransitionPoint == 0.0) {
                    return rightLutDescription;
                } else {
                    return leftLutDescription;
                }
            }
            return LutDescription.DEFAULT;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            String ret = "";
            ret += sceneName;
            ret += "tuning: " + isTuningEnabled;
            return ret;
        }

        private RectF convertUICoordinatesToTextureCoordinates(RectF rectF) {
            return new RectF(rectF.left, 1.0f - rectF.top, rectF.right, 1.0f - rectF.bottom);
        }

        @Override
        public SceneDescription clone() {
            SceneDescription sceneDescription = null;
            try {
                sceneDescription = (SceneDescription) super.clone();
                sceneDescription.sceneRectfInfo = new RectF(this.getSceneRectfInfo());
                sceneDescription.sceneZoom = Arrays.copyOf(this.getSceneZoom(), 3);
                sceneDescription.sceneTranslateInfo = Arrays.copyOf(this.getSceneTranslateInfo(), 3);
                sceneDescription.sceneTranslateByInfo = Arrays.copyOf(this.getSceneTranslateByInfo(), 3);
                sceneDescription.defaultGeometryScale = Arrays.copyOf(this.getDefaultGeometryScale(), 3);

            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return sceneDescription;
        }
    }


    public enum SceneName implements BaseDescription {
        // Please add only those scenes which are working correctly
        // And ensure to add the scene case in createScene method
        SCENE_DEFAULT(getTrackStringByLocale(R.string.effect_normal), getDisplayStringByLocale(R.string.effect_normal), R.drawable.scene_ph_normal),
        SCENE_GRID(getTrackStringByLocale(R.string.effect_manifold), getDisplayStringByLocale(R.string.effect_manifold), R.drawable.scene_ph_manifold),
        SCENE_RGB_OFFSET(getTrackStringByLocale(R.string.effect_daydream), getDisplayStringByLocale(R.string.effect_daydream), R.drawable.scene_ph_daydream),
        SCENE_ROPOSO_QUADRANT_REPEAT(getTrackStringByLocale(R.string.effect_roposo), getDisplayStringByLocale(R.string.effect_roposo), R.drawable.scene_ph_roposo2),
        SCENE_WHITE_FLICKER(getTrackStringByLocale(R.string.effect_miniparty), getDisplayStringByLocale(R.string.effect_miniparty), R.drawable.scene_ph_twinke),
        SCENE_WHITE_AREA_ANIMATE(getTrackStringByLocale(R.string.effect_chalky), getDisplayStringByLocale(R.string.effect_chalky), R.drawable.scene_ph_chalky),
        SCENE_FADE_IN_OUT(getTrackStringByLocale(R.string.effect_checkers), getDisplayStringByLocale(R.string.effect_checkers), R.drawable.scene_ph_checkers),
        SCENE_GRAY_SCALE_FLICKER(getTrackStringByLocale(R.string.effect_dhikchik), getDisplayStringByLocale(R.string.effect_dhikchik), R.drawable.scene_ph_dhik_chik),
        SCENE_EDGE_GLOW(getTrackStringByLocale(R.string.effect_lantern), getDisplayStringByLocale(R.string.effect_lantern), R.drawable.scene_ph_lantern),
        SCENE_CURTAIN_OPENER(getTrackStringByLocale(R.string.effect_kickoff), getDisplayStringByLocale(R.string.effect_kickoff), R.drawable.scene_ph_kickoff),
        SCENE_POSTERIZE(getTrackStringByLocale(R.string.effect_sketch), getDisplayStringByLocale(R.string.effect_sketch), R.drawable.scene_ph_sketch),
        SCENE_THREE_MIRROR_SPLIT(getTrackStringByLocale(R.string.effect_3way), getDisplayStringByLocale(R.string.effect_3way), R.drawable.scene_ph_threeway),
        SCENE_MIRROR(getTrackStringByLocale(R.string.effect_ditto), getDisplayStringByLocale(R.string.effect_ditto), R.drawable.scene_ph_me),
        SCENE_MIRROR2(getTrackStringByLocale(R.string.effect_clone), getDisplayStringByLocale(R.string.effect_clone), R.drawable.scene_ph_me2),
        //SCENE_MONO_COLOR_ANIMATE(getTrackStringByLocale(R.string.effect_monochrome), getDisplayStringByLocale(R.string.effect_monochrome), R.drawable.scene_ph_tv60s),
        SCENE_RAINBOW(getTrackStringByLocale(R.string.effect_rainbow), getDisplayStringByLocale(R.string.effect_rainbow), R.drawable.scene_ph_rainbow),
        SCENE_MOSAIC_HORIZONTAL(getTrackStringByLocale(R.string.effect_plaid), getDisplayStringByLocale(R.string.effect_plaid), R.drawable.scene_ph_plaid),
        SCENE_ZOOM_OUT(getTrackStringByLocale(R.string.effect_window), getDisplayStringByLocale(R.string.effect_window), R.drawable.scene_ph_window),
        SCENE_TRICOLOR(getTrackStringByLocale(R.string.effect_tricolor), getDisplayStringByLocale(R.string.effect_tricolor), R.drawable.scene_ph_tricolor),
        SCENE_RED_FLICKER_WITH_NOISE(getTrackStringByLocale(R.string.effect_flicker), getDisplayStringByLocale(R.string.effect_flicker), R.drawable.scene_ph_flicker),
        SCENE_MOSAIC_VERTICAL(getTrackStringByLocale(R.string.effect_patchwork), getDisplayStringByLocale(R.string.effect_patchwork), R.drawable.scene_ph_patchwork),
        SCENE_SNOW(getTrackStringByLocale(R.string.effect_snow), getDisplayStringByLocale(R.string.effect_snow), R.drawable.scene_ph_snow),
        SCENE_COLOR_REVEAL(getTrackStringByLocale(R.string.effect_tripper), getDisplayStringByLocale(R.string.effect_tripper), R.drawable.scene_ph_tripper),
        SCENE_HEXA(getTrackStringByLocale(R.string.effect_hexagram), getDisplayStringByLocale(R.string.effect_hexagram), R.drawable.scene_ph_hexagram),
        SCENE_ROTATE(getTrackStringByLocale(R.string.effect_roller_coaster), getDisplayStringByLocale(R.string.effect_roller_coaster), R.drawable.scene_ph_roller_coster),
        SCENE_RED_HYPNO(getTrackStringByLocale(R.string.effect_hypno), getDisplayStringByLocale(R.string.effect_hypno), R.drawable.scene_ph_ripple),
        SCENE_GRAINY_EFFECT(getTrackStringByLocale(R.string.effect_filmgrain), getDisplayStringByLocale(R.string.effect_filmgrain), R.drawable.scene_ph_film_grain),
        SCENE_HALF_SCREEN_SPLIT(getTrackStringByLocale(R.string.effect_shootingstar), getDisplayStringByLocale(R.string.effect_shootingstar), R.drawable.scene_ph_shooting_star),
        SCENE_POLAROID_TWO(getTrackStringByLocale(R.string.effect_polaroid), getDisplayStringByLocale(R.string.effect_polaroid), R.drawable.scene_ph_polaroid),
        SCENE_EDGE_GLOW_IN_DARK(getTrackStringByLocale(R.string.effect_firefly), getDisplayStringByLocale(R.string.effect_firefly), R.drawable.scene_ph_firefly),
        SCENE_CIRCULAR_CURTAIN(getTrackStringByLocale(R.string.effect_hooplight), getDisplayStringByLocale(R.string.effect_hooplight), R.drawable.scene_ph_hoop_light),
        SCENE_TOON(getTrackStringByLocale(R.string.effect_comic), getDisplayStringByLocale(R.string.effect_comic), R.drawable.scene_ph_comic),
        SCENE_GHOST_EFFECT(getTrackStringByLocale(R.string.effect_demon), getDisplayStringByLocale(R.string.effect_demon), R.drawable.scene_ph_demon),
        SCENE_POLAROID(getTrackStringByLocale(R.string.effect_ropotrait), getDisplayStringByLocale(R.string.effect_ropotrait), R.drawable.scene_ph_roportrait),
        SCENE_BRIGHT_TO_FADE(getTrackStringByLocale(R.string.effect_firebolt), getDisplayStringByLocale(R.string.effect_firebolt), R.drawable.scene_ph_firebolt),
        SCENE_RADIAL_BLUR(getTrackStringByLocale(R.string.effect_dazzling), getDisplayStringByLocale(R.string.effect_dazzling), R.drawable.scene_ph_dazzling),
        SCENE_RIPPLE(getTrackStringByLocale(R.string.effect_ripple), getDisplayStringByLocale(R.string.effect_ripple), R.drawable.scene_ph_ripple2),
        SCENE_3D("scene3d","scene3d",0);

        private final String displayName, trackName;
        final private int imgRes;

        SceneName(String trackName, String displayName, int imgRes) {
            this.trackName = trackName;
            this.displayName = displayName;
            this.imgRes = imgRes;
        }

        public String getTrackName() {
            return trackName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getImgRes() {
            return imgRes;
        }
    }

    private static class LruSceneCache extends LruCache<SceneDescription, Scene> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruSceneCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, SceneDescription handle, Scene sceneObject, Scene newSceneObject) {
            if (VERBOSE)
                Log.d(TAG, "Evicting FBObject " + sceneObject + "  handle: " + handle);
        }
    }

    public enum LutDescription implements BaseDescription {
        DEFAULT(getTrackStringByLocale(R.string.lut_default), getDisplayStringByLocale(R.string.lut_default), R.raw.neutral_lut, R.drawable.lut_ph_normal),
        SOFT_GLOW(getTrackStringByLocale(R.string.lut_softglow), getDisplayStringByLocale(R.string.lut_softglow), R.raw.soft_glow, R.drawable.lut_ph_soft_glow),
        GRAYSCALE(getTrackStringByLocale(R.string.lut_greyjoy), getDisplayStringByLocale(R.string.lut_greyjoy), R.raw.grayscale, R.drawable.lut_ph_grey_joy),
        RED_OVERLAY(getTrackStringByLocale(R.string.lut_mrindia), getDisplayStringByLocale(R.string.lut_mrindia), R.raw.red_overlay, R.drawable.lut_ph_mrindia),
        STRIPY_SOFT(getTrackStringByLocale(R.string.lut_redify), getDisplayStringByLocale(R.string.lut_redify), R.raw.stripy_soft, R.drawable.lut_ph_redify),
        ACTION_GREEN(getTrackStringByLocale(R.string.lut_actiongreen), getDisplayStringByLocale(R.string.lut_actiongreen), R.raw.action_green, R.drawable.lut_ph_greened),
        ARABICA(getTrackStringByLocale(R.string.lut_1990s), getDisplayStringByLocale(R.string.lut_1990s), R.raw.arabica, R.drawable.lut_ph_arabica),
        CLOUSEAU(getTrackStringByLocale(R.string.lut_brighter), getDisplayStringByLocale(R.string.lut_brighter), R.raw.clouseau, R.drawable.lut_ph_brighter),
        CONTRASTY_COLOUR(getTrackStringByLocale(R.string.lut_contrastica), getDisplayStringByLocale(R.string.lut_contrastica), R.raw.contrasty_colour, R.drawable.lut_ph_contrastica),
        FG_CINE_BRIGHT(getTrackStringByLocale(R.string.lut_brighterati), getDisplayStringByLocale(R.string.lut_brighterati), R.raw.fg_cine_bright, R.drawable.lut_ph_brighterati),
        FG_CINE_DRAMA(getTrackStringByLocale(R.string.lut_dramastic), getDisplayStringByLocale(R.string.lut_dramastic), R.raw.fg_cine_drama, R.drawable.lut_ph_dramastic),
        HIDDENITE(getTrackStringByLocale(R.string.lut_hiddentia), getDisplayStringByLocale(R.string.lut_hiddentia), R.raw.hiddenite, R.drawable.lut_ph_hiddentia),
        HOWLITE(getTrackStringByLocale(R.string.lut_howlit), getDisplayStringByLocale(R.string.lut_howlit), R.raw.howlite, R.drawable.lut_ph_howlit),
        IWLTBAP(getTrackStringByLocale(R.string.lut_orangica), getDisplayStringByLocale(R.string.lut_orangica), R.raw.iwltbap, R.drawable.lut_ph_orangica),
        PB_BASIN(getTrackStringByLocale(R.string.lut_colorphobe), getDisplayStringByLocale(R.string.lut_colorphobe), R.raw.pb_basin, R.drawable.lut_ph_colourphobe),
        PB_POCATELLO(getTrackStringByLocale(R.string.lut_darkforest), getDisplayStringByLocale(R.string.lut_darkforest), R.raw.pb_pocatello, R.drawable.lut_ph_dark_forest),
        PB_PRAGUE(getTrackStringByLocale(R.string.lut_saffrontie), getDisplayStringByLocale(R.string.lut_saffrontie), R.raw.pb_prague, R.drawable.lut_ph_saffronite),;

        final private String displayName, trackName;
        final private int resourceId;
        final private int imgRes;
        private boolean showTip;

        LutDescription(String trackName, String displayName, int resourceId, int imgRes) {
            this.trackName = trackName;
            this.displayName = displayName;
            this.resourceId = resourceId;
            this.imgRes = imgRes;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public int getImgRes() {
            return imgRes;
        }

        public int getResourceId() {
            return resourceId;
        }

        public String getTrackName() {
            return trackName;
        }

        public void setShowTip(boolean showTip) {
            this.showTip = showTip;
        }

        public boolean isShowTip() {
            return showTip;
        }
    }

    public interface BaseDescription {
        String getDisplayName();

        int getImgRes();
    }

    public static Bitmap getBitmapFrom(int resourceId) {
        Bitmap bitmap = Caches.getBitmap("resId_" + resourceId);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(ContextHelper.getContext().getResources(), resourceId);
            Caches.putBitmap("resId_" + resourceId, bitmap);
        }
        return bitmap;
    }

    @NonNull
    private static String getDisplayStringByLocale(int id) {
        return ContextHelper.getContext().getString(id);
    }

    @NonNull
    private static String getTrackStringByLocale(int id) {
        return getStringByLocale(ContextHelper.getContext(), id, Locale.ENGLISH.getDisplayName());
    }

    private static String getStringByLocale(Context context, int id, String locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(new Locale(locale));
        return context.createConfigurationContext(configuration).getResources().getString(id);
    }

    public static void registerScene(Scene sceneName, Class<? extends Scene> sceneClass) {
//        sceneClassMap.put(sceneName, sceneClass);
    }
}