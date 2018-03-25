package com.roposo.creation.graphics.filters;

import com.roposo.core.util.ContextHelper;

/**
 * Created by Tanvi on 09/02/18.
 *
 * This class contains the functions that are used by multiple shaders.
 * If you are using composite filters, the functions of the child filters should be used from here so they are not repeated.
 */

public class FragmentShaderArgsFunctions {
    public final static float[] red = {0.9f, 0.25f, 0.35f, 0.5f};
    public final static float[] cyan = {0.0f, 1.0f, 1.0f, 1.0f};
    public final static float[] yellow = {0.9f, 0.68f, 0.16f, 0.5f};
    public final static float[] magenta = {0.5f, 0.25f, 0.58f, 0.5f};

    public final static String RGB_HSL = BaseFilter.loadShader("RGBHSL.txt", ContextHelper.getContext());
    public final static String BLEND_HUE = "vec3 BlendHue(vec3 base, vec3 blend)\n" +
            "{\n" +
            "\tvec3 baseHSL = RGBToHSL(base);\n" +
            "\treturn HSLToRGB(vec3(RGBToHSL(blend).r, baseHSL.g, baseHSL.b));\n" +
            "}\n";

    public final static String CONTRAST_SATURATION_BRIGHTNESS = BaseFilter.loadShader("ContrastSaturationBrightness.txt", ContextHelper.getContext());
    public final static String DESATURATE = BaseFilter.loadShader("Desaturate.txt", ContextHelper.getContext());

    public final static String FLOAT_BLENDING_MODES = BaseFilter.loadShader("FloatBlendingModes.txt", ContextHelper.getContext());   //helper functions for real blending functions
    public final static String BLEND_DECLARATION = "uniform vec3 blend;\n";
    public final static String BLEND = "#define Blend(base, blend, funcf) \t\tvec3(funcf(base.r, blend.r), funcf(base.g, blend.g), funcf(base.b, blend.b))\n";

}
