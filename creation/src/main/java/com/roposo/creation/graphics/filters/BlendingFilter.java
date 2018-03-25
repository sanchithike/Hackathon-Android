package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;
import java.util.logging.Filter;

/**
 * Created by Tanvi on 07/02/18.
 */

public class BlendingFilter extends ImageFilter {

    private volatile float[] blendColor = {0.9f, 0.25f, 0.35f};

    public enum BlendingMode {
        //EXPORT VARIABLES LEFT
        NORMAL(FilterManager.NORMAL_BLEND, "BlendNormal", "(blend)"),
        LIGHTEN(FilterManager.LIGHTEN_BLEND, "BlendLighten", "BlendLightenf"),
        DARKEN(FilterManager.DARKEN_BLEND, "BlendDarken", "BlendDarkenf"),
        MULTIPLY(FilterManager.MULTIPLY_BLEND, "BlendMultiply", "(base * blend)"),
        AVERAGE(FilterManager.AVERAGE_BLEND, "BlendAverage", "((base + blend) / 2.0)"),
        ADD(FilterManager.ADD_BLEND, "BlendAdd", "min(base + blend, vec3(1.0))"),
        SUBSTRACT(FilterManager.SUBSTRACT_BLEND, "BlendSubstract", "max(base + blend - vec3(1.0), vec3(0.0))"),
        DIFFERENCE(FilterManager.DIFFERENCE_BLEND, "BlendDifference", "abs(base - blend)"),
        NEGATION(FilterManager.NEGATION_BLEND, "BlendNegation", "(vec3(1.0) - abs(vec3(1.0) - base - blend))"),
        EXCLUSION(FilterManager.EXCLUSION_BLEND, "BlendExclusion", "(base + blend - 2.0 * base * blend)"),
        SCREEN(FilterManager.SCREEN_BLEND, "BlendScreen", "Blend(base, blend, BlendScreenf)"),
        OVERLAY(FilterManager.OVERLAY_BLEND, "BlendOverlay", "Blend(base, blend, BlendOverlayf)"),
        SOFT_LIGHT(FilterManager.SOFT_LIGHT_BLEND, "BlendSoftLight", "Blend(base, blend, BlendSoftLightf)"),
        HARD_LIGHT(FilterManager.HARD_LIGHT_BLEND, "BlendHardLight", "BlendOverlay(blend, base)"),
        COLOR_DODGE(FilterManager.COLOR_DODGE_BLEND, "BlendColorDodge", "Blend(base, blend, BlendColorDodgef)"),
        COLOR_BURN(FilterManager.COLOR_BURN_BLEND, "BlendColorBurn", "Blend(base, blend, BlendColorBurnf)"),
        LINEAR_DODGE(FilterManager.LINEAR_DODGE_BLEND, "BlendLinearDodge", "BlendAdd"),
        LINEAR_BURN(FilterManager.LINEAR_BURN_BLEND, "BlendLinearBurn", "BlendSubstract"),
        LINEAR_LIGHT(FilterManager.LINEAR_LIGHT_BLEND, "BlendLinearLight", "Blend(base, blend, BlendLinearLightf)"),
        VIVID_LIGHT(FilterManager.VIVID_LIGHT_BLEND, "BlendVividLight", "Blend(base, blend, BlendVividLightf)"),
        PIN_LIGHT(FilterManager.PIN_LIGHT_BLEND, "BlendPinLight", "Blend(base, blend, BlendPinLightf)"),
        HARD_MIX(FilterManager.HARD_MIX_BLEND, "BlendHardMix", "Blend(base, blend, BlendHardMixf)"),
        REFLECT(FilterManager.REFLECT_BLEND, "BlendReflect", "Blend(base, blend, BlendReflectf)"),
        GLOW(FilterManager.GLOW_BLEND, "BlendGlow", "BlendReflect(blend, base)"),
        PHOENIX(FilterManager.PHOENIX_BLEND, "BlendPhoenix", "(min(base, blend) - max(base, blend) + vec3(1.0))"),
        ;

        String filterMode;
        String glFunctionName;
        String glFunctionImpl;

        BlendingMode(String filterMode, String glFunctionName, String glFunctionImpl) {
            this.filterMode = filterMode;
            this.glFunctionName = glFunctionName;
            this.glFunctionImpl = glFunctionImpl;
        }

        public String getArgs() {
            return "#define " + glFunctionName + "(base, blend)\t\t" + glFunctionImpl + ";\n";
        }

        public String getMain() {
            return "vec3 temp = " + glFunctionName + "(fragColor.rgb, blend);" + "\n" +
                    "fragColor = vec4(temp, 1.0);\n";
        }
    }

    public BlendingFilter(BlendingMode blendingMode, float[] blendColor) {
        super();
        mFilterMode.add(blendingMode.filterMode);
        if (blendColor != null) {
            this.blendColor = blendColor;
        }
        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.BLEND_DECLARATION);
//        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.RGB_HSL);
        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.FLOAT_BLENDING_MODES);
        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.BLEND);
        FRAG_SHADER_ARGS.add(blendingMode.getArgs());
        FRAG_SHADER_MAIN += blendingMode.getMain();
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform3fv("blend", this.blendColor);
    }
}
