package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;

/**
 * Created by Tanvi on 20/12/17.
 */

public class ToonFilter extends ImageFilter {
    private volatile float hueRegions = 17f, saturationRegions = 7f, valueRegions = 9f;

    private static final String ARGS = "\n" +
            "uniform float hueRegions;\n" +
            "uniform float saturationRegions;\n" +
            "uniform float valueRegions;\n";
    
    private static final String MAIN = "vec2 texturecoordinates = outTexCoords.xy;\n" +
            "vec2 texeloffset = texelSize;\n" +
            "vec2 leftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, 0.0);\n" +
            "vec2 rightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, 0.0);\n" +
            "vec2 topTextureCoordinate = texturecoordinates + vec2(0.0, -texeloffset.y);\n" +
            "vec2 topLeftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, - texeloffset.y);\n" +
            "vec2 topRightTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, texeloffset.y);\n" +
            "vec2 bottomTextureCoordinate = texturecoordinates + vec2(0.0, texeloffset.y);\n" +
            "vec2 bottomLeftTextureCoordinate = texturecoordinates - vec2(texeloffset.x, -texeloffset.y);\n" +
            "vec2 bottomRightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, texeloffset.y);\n" +
            "\n" +
            "float bottomLeftIntensity = (texture2D(baseSampler, bottomLeftTextureCoordinate).r + texture2D(baseSampler, bottomLeftTextureCoordinate).g) / 2.0;\n" +
            "float topRightIntensity = (texture2D(baseSampler, topRightTextureCoordinate).r + texture2D(baseSampler, topRightTextureCoordinate).g) / 2.0;\n" +
            "float topLeftIntensity = (texture2D(baseSampler, topLeftTextureCoordinate).r + texture2D(baseSampler, topLeftTextureCoordinate).g) / 2.0;\n" +
            "float bottomRightIntensity = (texture2D(baseSampler, bottomRightTextureCoordinate).r + texture2D(baseSampler, bottomRightTextureCoordinate).g) / 2.0;\n" +
            "float leftIntensity = (texture2D(baseSampler, leftTextureCoordinate).r + texture2D(baseSampler, leftTextureCoordinate).g) / 2.0;\n" +
            "float rightIntensity = (texture2D(baseSampler, rightTextureCoordinate).r + texture2D(baseSampler, rightTextureCoordinate).g) / 2.0;\n" +
            "float bottomIntensity = (texture2D(baseSampler, bottomTextureCoordinate).r + texture2D(baseSampler, bottomTextureCoordinate).g) / 2.0;\n" +
            "float topIntensity = (texture2D(baseSampler, topTextureCoordinate).r + texture2D(baseSampler, topTextureCoordinate).g) / 2.0;\n" +
            "\n" +
            "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "float mag = length(vec2(h, v));\n" +
            "vec3 hsvColor = rgb2hsv(fragColor.rgb);\n" +
            "float hue = floor((hsvColor.r * hueRegions) + 0.5) / hueRegions;\n" +
            "float saturation = floor((hsvColor.g * saturationRegions) + 0.5) / saturationRegions;\n" +
            "float value = floor((hsvColor.b * valueRegions) + 0.5) / valueRegions;\n" +
            "saturation *= 0.7;\n" +
            "value = log(1.0 + value);\n" +
            "hsvColor.rgb = vec3(hue, saturation, value);\n" +
            "float thresholdTest = 1.0 - step(0.47, mag);\n" +
            "fragColor.rgb = hsv2rgb(hsvColor);\n" +
            "fragColor = vec4(fragColor.rgb * thresholdTest, fragColor.a);";

    public ToonFilter() {
        super();
        mFilterMode.add(FilterManager.TOON_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_ARGS.add(ProgramCache.gFS_Header_Func_HSV);
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        Log.d(TAG, String.format(Locale.getDefault(), "HueRegions(%f)SaturationRegions(%f)ValueRegions(%f)", hueRegions, saturationRegions, valueRegions));
        program.uniform1f("hueRegions", hueRegions);
        program.uniform1f("saturationRegions", saturationRegions);
        program.uniform1f("valueRegions", valueRegions);
    }

}
