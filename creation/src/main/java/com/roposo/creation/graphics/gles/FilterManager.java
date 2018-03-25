package com.roposo.creation.graphics.gles;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.filters.BlackWhiteNoiseFilter;
import com.roposo.creation.graphics.filters.BlendHueFilter;
import com.roposo.creation.graphics.filters.Blur1DFilter;
import com.roposo.creation.graphics.filters.Blur2DFilter;
import com.roposo.creation.graphics.filters.BrightToFadeFilter;
import com.roposo.creation.graphics.filters.CircularCurtainFilter;
import com.roposo.creation.graphics.filters.ColorRevealFilter;
import com.roposo.creation.graphics.filters.CompositeFilter;
import com.roposo.creation.graphics.filters.ContrastFilter;
import com.roposo.creation.graphics.filters.CreateImageTestingFilter;
import com.roposo.creation.graphics.filters.CurtainOpenerFilter;
import com.roposo.creation.graphics.filters.CyanColorOverlayFilter;
import com.roposo.creation.graphics.filters.DiagonalMirrorFilter;
import com.roposo.creation.graphics.filters.DonGlowFilter;
import com.roposo.creation.graphics.filters.EdgeGlowFilter;
import com.roposo.creation.graphics.filters.EdgeGlowInDarkness;
import com.roposo.creation.graphics.filters.FragmentShaderArgsFunctions;
import com.roposo.creation.graphics.filters.GhostEffectFilter;
import com.roposo.creation.graphics.filters.GrainyFilter;
import com.roposo.creation.graphics.filters.GrayScaleFlickerFilter;
import com.roposo.creation.graphics.filters.HexaFilter;
import com.roposo.creation.graphics.filters.HistoPyramidCalculatorFilter;
import com.roposo.creation.graphics.filters.HistoPyramidDivideBy1kFilter;
import com.roposo.creation.graphics.filters.HistoPyramidSumCalculatorFilter;
import com.roposo.creation.graphics.filters.HorizontalMirrorFilter;
import com.roposo.creation.graphics.filters.ImageAdjustmentFilter;
import com.roposo.creation.graphics.filters.ImageFilter;
import com.roposo.creation.graphics.filters.LutFilter;
import com.roposo.creation.graphics.filters.MagentaColorOverlayFilter;
import com.roposo.creation.graphics.filters.MirrorFilter;
import com.roposo.creation.graphics.filters.MonoFilter;
import com.roposo.creation.graphics.filters.MonoToColorAnimateFilter;
import com.roposo.creation.graphics.filters.MotionBlurFilter;
import com.roposo.creation.graphics.filters.PhotoFlickerFilter;
import com.roposo.creation.graphics.filters.PointGatheringFilter;
import com.roposo.creation.graphics.filters.PosterizeFilter;
import com.roposo.creation.graphics.filters.PurpleHypnoFilter;
import com.roposo.creation.graphics.filters.RGBAnimatedFilter;
import com.roposo.creation.graphics.filters.RGBOffsetFilter;
import com.roposo.creation.graphics.filters.RadialBlurFilter;
import com.roposo.creation.graphics.filters.RainbowFilter;
import com.roposo.creation.graphics.filters.RectangularCurtainFilter;
import com.roposo.creation.graphics.filters.RedColorOverlayFilter;
import com.roposo.creation.graphics.filters.RedFlickerWithNoise;
import com.roposo.creation.graphics.filters.RedHypnoFilter;
import com.roposo.creation.graphics.filters.RippleFilter;
import com.roposo.creation.graphics.filters.RoposoMirror3Filter;
import com.roposo.creation.graphics.filters.RoposoQuadrantFilter;
import com.roposo.creation.graphics.filters.RotateFilter;
import com.roposo.creation.graphics.filters.ShineDetectFilter;
import com.roposo.creation.graphics.filters.SnowFlakesFilter;
import com.roposo.creation.graphics.filters.SoftGlowFilter;
import com.roposo.creation.graphics.filters.ToonFilter;
import com.roposo.creation.graphics.filters.TriColorFilter;
import com.roposo.creation.graphics.filters.VerticalMirrorFilter;
import com.roposo.creation.graphics.filters.WhiteAreaAnimateFilter;
import com.roposo.creation.graphics.filters.WhiteFilter;
import com.roposo.creation.graphics.filters.WhiteFlickerFilter;
import com.roposo.creation.graphics.filters.YellowColorOverlayFilter;
import com.roposo.creation.graphics.filters.ZoomOutFilter;
import com.roposo.creation.util.CreationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bajaj on 12/07/16.
 */
public class FilterManager {
    private static final String TAG = "FilterManager";

    public static final String IMAGE_FILTER = "";
    public static final String MONO_FILTER = "MonoFilter";
    public static final String ROPOSO_MIRROR_3_FILTER = "RoposoMirror3Filter";
    public static final String ROPOSO_QUADRANT_FILTER = "RoposoQuadrantFilter";
    public static final String RGB_ANIMATED_FILTER = "RGBAnimatedFilter";
    public static final String MIRROR_FILTER = "MirrorFilter";
    public static final String VERTICAL_MIRROR_FILTER = "VerticalMirrorFilter";
    public static final String HORIZONTAL_MIRROR_FILTER = "HorizontalMirrorFilter";
    public static final String GRAINY_FILTER = "GrainyFilter";
    public static final String CIRCULAR_FILTER = "CircularFilter";
    public static final String RECTANGULAR_CURTAIN_FILTER = "RectangularCurtainFilter";
    public static final String SHINE_DETECT_FILTER = "ShineDetectFilter";
    public static final String DIAGONAL_MIRROR_FILTER = "DiagonalMirrorFilter";
    public static final String RED_COLOR_OVERLAY_FILTER = "RedColorOverlayFilter";
    public static final String WHITE_DISCO_LIGHTS_FILTER = "WhiteFlickerFilter";
    public static final String ZOOM_OUT_FILTER = "ZoomOutFilter";
    public static final String COLOR_REVEAL_FILTER = "ColorRevealFilter";
    public static final String MONO_TO_COLOR_ANIMATE_FILTER = "MonoToColorAnimate";
    public static final String BLACK_WHITE_NOISE_FILTER = "BlackWhiteNoiseFilter";
    public static final String CURTAIN_OPENER_FILTER = "CurtainOpenerFilter";
    public static final String RADIAL_BLUR_FILTER = "RadialBlurFilter";
    public static final String RGBOFFSET_FILTER = "RGBOffsetFilter";
    public static final String POSTERIZE_FILTER = "PosterizeFilter";
    public static final String TOON_FILTER = "ToonFilter";
    public static final String SOFT_GLOW_FILTER = "SoftGlowFilter";
    public static final String GRAY_SCALE_FLICKER_FILTER = "GrayScaleFlicker";
    public static final String GHOST_EFFECT_FILTER = "GhostEffectFilter";
    public static final String TRICOLOR_FILTER = "TriColorFilter";
    public static final String DON_GLOW_FILTER = "DonGlowFilter";
    public static final String ROTATE_FILTER = "RotateFilter";
    public static final String RED_FLICKER_WITH_NOISE_FILTER = "RedFlickerFilter";
    public static final String BLUR_2D_FILTER = "Blur2DFilter";
    public static final String BLUR_MOTION_FILTER = "MotionBlurFilter";
    public static final String BLUR_H_FILTER = "BlurHFilter";
    public static final String BLUR_V_FILTER = "BlurVFilter";
    public static final String PHOTO_FLICKER_FILTER = "PhotoFlickerFilter";
    public static final String BRIGHT_TO_FADE_FILTER = "BrightToFadeFilter";
    public static final String SNOW_FLAKES_FILTER = "SnowFlakesFilter";
    public static final String WHITE_FILTER = "WhiteFilter";
    public static final String CYAN_COLOR_OVERLAY = "CyanColorOverlayFilter";
    public static final String YELLOW_COLOR_OVERLAY = "YellowColorOverlayFilter";
    public static final String MAGENTA_COLOR_OVERLAY = "MagentaColorOverlayFilter";
    public static final String WHITE_AREA_ANIMATE_FILTER = "WhiteAreaAnimateFilter";
    public static final String EDGE_GLOW_FILTER = "EdgeGlowFilter";
    public static final String EDGE_GLOW_IN_DARKNESS_FILTER = "EdgeGlowInDarknessFilter";
    public static final String CONTRAST_FILTER = "ContrastFilter";
    public static final String IMAGE_ADJUSTMENT_FILTER = "ImageAdjustmentFilter";
    public static final String LUT_FILTER = "LutFilter";
    public static final String HEXA_FILTER = "HexaFilter";
    public static final String DILATION_FILTER = "DilationFilter";

    public static final String HISTOPYRAMID_CALCULATOR_FILTER = "HistoPyramidCalculatorFilter";
    public static final String HISTOPYRAMIDSUM_CALCULATOR_FILTER = "HistoPyramidSumCalculatorFilter";
    public static final String HISTOPYRAMID_DIVIDE_BY1K_FILTER = "HistoPyramidDivideBy1kFilter";
    public static final String POINT_GATHERING_FILTER = "PointGatheringFilter";
    public static final String CREATE_IMAGE_FILTER = "CreateImageFilter";

    public static final String NORMAL_BLEND = "NormalBlend";
    public static final String LIGHTEN_BLEND = "LightenBlend";
    public static final String DARKEN_BLEND = "DarkemBlend";
    public static final String MULTIPLY_BLEND = "MultiplyBlend";
    public static final String AVERAGE_BLEND = "AverageBlend";
    public static final String ADD_BLEND = "AddBlend";
    public static final String SUBSTRACT_BLEND = "SubstractBlend";
    public static final String DIFFERENCE_BLEND = "DifferenceBlend";
    public static final String NEGATION_BLEND = "NegationBlend";
    public static final String EXCLUSION_BLEND = "ExclusionBlend";
    public static final String SCREEN_BLEND = "ScreenBlend";
    public static final String OVERLAY_BLEND = "OverlayBlend";
    public static final String SOFT_LIGHT_BLEND = "SoftLightBlend";
    public static final String HARD_LIGHT_BLEND = "HardLightBlend";
    public static final String COLOR_DODGE_BLEND = "ColorDodgeBlend";
    public static final String COLOR_BURN_BLEND = "ColorBurnBlend";
    public static final String LINEAR_DODGE_BLEND = "LinearDodgeBlend";
    public static final String LINEAR_BURN_BLEND = "LinearBurnBlend";
    public static final String LINEAR_LIGHT_BLEND = "LinearLightBlend";
    public static final String VIVID_LIGHT_BLEND = "VividLightBlend";
    public static final String PIN_LIGHT_BLEND = "PinLightBlend";
    public static final String HARD_MIX_BLEND = "HardMixBlend";
    public static final String REFLECT_BLEND = "ReflectBlend";
    public static final String GLOW_BLEND = "GlowBlend";
    public static final String PHOENIX_BLEND = "PhoenixBlend";
    public static final String RED_HYPNO_FILTER = "RedHypno";
    public static final String PURPLE_HYPNO_FILTER = "PurpleHypno";
    public static final String RIPPLE_FILTER = "HypnoFilter";

    public static final String RED_BLEND_HUE_FILTER = "RedBlendHueFilter";
    public static final String CYAN_BLEND_HUE_FILTER = "CyanBlendHueFilter";
    public static final String YELLOW_BLEND_HUE_FILTER = "YellowBlendHueFilter";
    public static final String MAGENTA_BLEND_HUE_FILTER = "MagentaBlendHueFilter";

    public static final String RAINBOW_FILTER = "RainbowFilter";
    private static Map<String, Class<? extends BaseFilter>> filterClassMap = new HashMap<>();

    public static BaseFilter getFilter(ProgramCache.ProgramDescription description) {
        try {
            List<String> filterMode = description.colorFilterMode;

            switch (filterMode.size()) {
                case 0:
                    return getFilter(IMAGE_FILTER);
                case 1:
                    return getFilter(filterMode.get(0));
                default:
                    return getCompositeFilter(filterMode);
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(new CreationException("Program description = " +
                    description));
            return new ImageFilter();
        }
    }

    private static CompositeFilter getCompositeFilter(List<String> filterMode) {
        ImageFilter[] imageFilters = new ImageFilter[filterMode.size()];
        int count = 0;
        for (String mode : filterMode) {
            imageFilters[count++] = (ImageFilter) getFilter(mode.trim());
        }
        return new CompositeFilter(imageFilters);
    }

    @NonNull
    public static BaseFilter getFilter(String filterMode) {
        switch (filterMode) {
            case MONO_FILTER:
                return new MonoFilter();
            case ROPOSO_MIRROR_3_FILTER:
                return new RoposoMirror3Filter();
            case ROPOSO_QUADRANT_FILTER:
                return new RoposoQuadrantFilter();
            case RGB_ANIMATED_FILTER:
                return new RGBAnimatedFilter();
            case MIRROR_FILTER:
                return new MirrorFilter();
            case DIAGONAL_MIRROR_FILTER:
                return new DiagonalMirrorFilter();
            case RED_COLOR_OVERLAY_FILTER:
                return new RedColorOverlayFilter();
            case WHITE_DISCO_LIGHTS_FILTER:
                return new WhiteFlickerFilter();
            case ZOOM_OUT_FILTER:
                return new ZoomOutFilter();
            case COLOR_REVEAL_FILTER:
                return new ColorRevealFilter();
            case MONO_TO_COLOR_ANIMATE_FILTER:
                return new MonoToColorAnimateFilter();
            case CONTRAST_FILTER:
                return new ContrastFilter();
            case BLACK_WHITE_NOISE_FILTER:
                return new BlackWhiteNoiseFilter();
            case GRAINY_FILTER:
                return new GrainyFilter();
            case CURTAIN_OPENER_FILTER:
                return new CurtainOpenerFilter();
            case RADIAL_BLUR_FILTER:
                return new RadialBlurFilter();
            case RGBOFFSET_FILTER:
                return new RGBOffsetFilter();
            case POSTERIZE_FILTER:
                return new PosterizeFilter();
            case TOON_FILTER:
                return new ToonFilter();
            case SOFT_GLOW_FILTER:
                return new SoftGlowFilter();
            case GHOST_EFFECT_FILTER:
                return new GhostEffectFilter();
            case TRICOLOR_FILTER:
                return new TriColorFilter();
            case GRAY_SCALE_FLICKER_FILTER:
                return new GrayScaleFlickerFilter();
            case DON_GLOW_FILTER:
                return new DonGlowFilter();
            case ROTATE_FILTER:
                return new RotateFilter();
            case RED_FLICKER_WITH_NOISE_FILTER:
                return new RedFlickerWithNoise();
            case SHINE_DETECT_FILTER:
                return new ShineDetectFilter();
            case BLUR_2D_FILTER:
                return new Blur2DFilter();
            case BLUR_H_FILTER:
                return new Blur1DFilter(true);
            case BLUR_V_FILTER:
                return new Blur1DFilter(false);
            case CIRCULAR_FILTER:
                return new CircularCurtainFilter();
            case RECTANGULAR_CURTAIN_FILTER:
                return new RectangularCurtainFilter();
            case PHOTO_FLICKER_FILTER:
                return new PhotoFlickerFilter();
            case BRIGHT_TO_FADE_FILTER:
                return new BrightToFadeFilter();
            case SNOW_FLAKES_FILTER:
                return new SnowFlakesFilter();
            case WHITE_FILTER:
                return new WhiteFilter();
            case VERTICAL_MIRROR_FILTER:
                return new VerticalMirrorFilter();
            case CYAN_COLOR_OVERLAY:
                return new CyanColorOverlayFilter();
            case MAGENTA_COLOR_OVERLAY:
                return new MagentaColorOverlayFilter();
            case YELLOW_COLOR_OVERLAY:
                return new YellowColorOverlayFilter();
            case WHITE_AREA_ANIMATE_FILTER:
                return new WhiteAreaAnimateFilter();
            case EDGE_GLOW_FILTER:
                return new EdgeGlowFilter();
            case EDGE_GLOW_IN_DARKNESS_FILTER:
                return new EdgeGlowInDarkness();
            case IMAGE_ADJUSTMENT_FILTER:
                return new ImageAdjustmentFilter();
            case LUT_FILTER:
                return new LutFilter();
            case HEXA_FILTER:
                return new HexaFilter();
            case HISTOPYRAMID_CALCULATOR_FILTER:
                return new HistoPyramidCalculatorFilter();
            case HISTOPYRAMIDSUM_CALCULATOR_FILTER:
                return new HistoPyramidSumCalculatorFilter();
            case HISTOPYRAMID_DIVIDE_BY1K_FILTER:
                return new HistoPyramidDivideBy1kFilter();
            case POINT_GATHERING_FILTER:
                return new PointGatheringFilter();
            case CREATE_IMAGE_FILTER:
                return new CreateImageTestingFilter();
            case RED_HYPNO_FILTER:
                return new RedHypnoFilter();
            case PURPLE_HYPNO_FILTER:
                return new PurpleHypnoFilter();
            case RIPPLE_FILTER:
                return new RippleFilter();
            case BLUR_MOTION_FILTER:
                return new MotionBlurFilter();
            case RED_BLEND_HUE_FILTER:
                return new BlendHueFilter(RED_BLEND_HUE_FILTER, FragmentShaderArgsFunctions.red);
            case CYAN_BLEND_HUE_FILTER:
                return new BlendHueFilter(CYAN_BLEND_HUE_FILTER, FragmentShaderArgsFunctions.cyan);
            case YELLOW_BLEND_HUE_FILTER:
                return new BlendHueFilter(YELLOW_BLEND_HUE_FILTER, FragmentShaderArgsFunctions.yellow);
            case MAGENTA_BLEND_HUE_FILTER:
                return new BlendHueFilter(MAGENTA_BLEND_HUE_FILTER, FragmentShaderArgsFunctions.magenta);
            case HORIZONTAL_MIRROR_FILTER:
                return new HorizontalMirrorFilter();
            case RAINBOW_FILTER:
                return new RainbowFilter();
            default:
                if (!TextUtils.isEmpty(filterMode)) {
                    if (filterClassMap != null) {
                        if (filterClassMap.keySet().contains(filterMode)) {
                            Class filterClass = filterClassMap.get(filterMode);
                            try {
                                Log.d(TAG, "Creating filter for class: " + filterClass);
                                return (BaseFilter) filterClass.newInstance();
                            } catch (InstantiationException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return new ImageFilter();
        }
    }

    public void registerFilter(String filter, Class<? extends BaseFilter> filterClass) {
        filterClassMap.put(filter, filterClass);
    }
}
