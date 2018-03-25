package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;

/**
 * Created by Tanvi on 21/12/17.
 */

public class ToonWithEdgeFilter extends ImageFilter{
    private volatile float hueRegions = 17f, saturationRegions = 7f, valueRegions = 9f;
    private static final String ARGS = ProgramCache.gFS_Header_Func_HSV + "\n" +
           "vec3 posterize(vec3 color) {\n" +
            "\tvec2 texturecoordinates = outTexCoords.xy;\n" +
            "\tvec2 texeloffset = texelSize;\n" +
            "\tvec2 leftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, 0.0);\n" +
            "\tvec2 rightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, 0.0);\n" +
            "\tvec2 topTextureCoordinate = texturecoordinates + vec2(0.0, -texeloffset.y);\n" +
            "\tvec2 topLeftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, - texeloffset.y);\n" +
            "\tvec2 topRightTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, texeloffset.y);\n" +
            "\tvec2 bottomTextureCoordinate = texturecoordinates + vec2(0.0, texeloffset.y);\n" +
            "\tvec2 bottomLeftTextureCoordinate = texturecoordinates - vec2(texeloffset.x, -texeloffset.y);\n" +
            "\tvec2 bottomRightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, texeloffset.y);\n" +
            "\n" +
            "\tfloat bottomLeftIntensity = (texture2D(baseSampler, bottomLeftTextureCoordinate).r + texture2D(baseSampler, bottomLeftTextureCoordinate).g) / 2.0;\n" +
            "\tfloat topRightIntensity = (texture2D(baseSampler, topRightTextureCoordinate).r + texture2D(baseSampler, topRightTextureCoordinate).g) / 2.0;\n" +
            "\tfloat topLeftIntensity = (texture2D(baseSampler, topLeftTextureCoordinate).r + texture2D(baseSampler, topLeftTextureCoordinate).g) / 2.0;\n" +
            "\tfloat bottomRightIntensity = (texture2D(baseSampler, bottomRightTextureCoordinate).r + texture2D(baseSampler, bottomRightTextureCoordinate).g) / 2.0;\n" +
            "\tfloat leftIntensity = (texture2D(baseSampler, leftTextureCoordinate).r + texture2D(baseSampler, leftTextureCoordinate).g) / 2.0;\n" +
            "\tfloat rightIntensity = (texture2D(baseSampler, rightTextureCoordinate).r + texture2D(baseSampler, rightTextureCoordinate).g) / 2.0;\n" +
            "\tfloat bottomIntensity = (texture2D(baseSampler, bottomTextureCoordinate).r + texture2D(baseSampler, bottomTextureCoordinate).g) / 2.0;\n" +
            "\tfloat topIntensity = (texture2D(baseSampler, topTextureCoordinate).r + texture2D(baseSampler, topTextureCoordinate).g) / 2.0;\n" +
            "\n" +
            "\tfloat h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "\tfloat v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "\tfloat mag = length(vec2(h, v));\n" +
            "\tvec3 hsv = rgb2hsv(color);\n" +
            "\tfloat hue = floor((hsv.r * 17.0) + 0.5) / 17.0;\n" +
            "\tfloat saturation = floor((hsv.g * 7.0) + 0.9) / 7.0;\n" +
            "\tfloat value = floor((hsv.b * 9.0) + 0.5) / 9.0;\n" +
            "\tsaturation *= 0.7;\n" +
            "\tvalue = log(1.0 + value);\n" +
            "\thsv.rgb = vec3(hue, saturation, value);\n" +
            "\tcolor = hsv2rgb(hsv);\n" +
            "\tfloat thresholdTest = 1.0 - step(0.47, mag);\n" +
            "\tcolor *= thresholdTest;\n" +
            "\treturn color;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "vec3 edge(vec2 uv)\n" +
            "{\n" +
            "\tvec4 lines= vec4(0.30, 0.59, 0.11, 1.0);\n" +
            "\n" +
            "\tlines.rgb = lines.rgb * LINES;\n" +
            "\tlines *= 1.5;\n" +
            "\n" +
            "\tfloat s11 = dot(texture2D(baseSampler, uv + vec2(-1.0/texelSize.x , -1.0/texelSize.y )), lines);   // LEFT\n" +
            "\tfloat s12 = dot(texture2D(baseSampler, uv + vec2(0, -1.0/texelSize.y )), lines);             // MIDDLE\n" +
            "\tfloat s13 = dot(texture2D(baseSampler, uv + vec2(1.0/texelSize.x , -1.0/texelSize.y )), lines);    // RIGHT\n" +
            "\n" +
            "\tfloat s21 = dot(texture2D(baseSampler, uv + vec2(-1.0/texelSize.x , 0.0)), lines);                // LEFT\n" +
            "\t// Omit center\n" +
            "\tfloat s23 = dot(texture2D(baseSampler, uv + vec2(-1.0/texelSize.x , 0.0)), lines);                // RIGHT\n" +
            "\n" +
            "\tfloat s31 = dot(texture2D(baseSampler, uv + vec2(-1.0/texelSize.x , 1.0/texelSize.y )), lines);    // LEFT\n" +
            "\tfloat s32 = dot(texture2D(baseSampler, uv + vec2(0, 1.0/texelSize.y )), lines);              // MIDDLE\n" +
            "\tfloat s33 = dot(texture2D(baseSampler, uv + vec2(1.0/texelSize.x , 1.0/texelSize.y )), lines); // RIGHT\n" +
            "\n" +
            "\tfloat t1 = s13 + s33 + (2.0 * s23) - s11 - (2.0 * s21) - s31;\n" +
            "\tfloat t2 = s31 + (2.0 * s32) + s33 - s11 - (2.0 * s12) - s13;\n" +
            "\n" +
            "\tvec3 col;\n" +
            "\n" +
            "\tif (((t1 * t1) + (t2* t2)) > 0.04) \n" +
            "\t{\n" +
            "\t\tcol = vec3(-1.,-1.,-1.);\n" +
            "\t}\n" +
            "\telse\n" +
            "\t{\n" +
            "\t\tcol = vec3(0.,0.,0.);\n" +
            "\t}\n" +
            "\n" +
            "\treturn col;\n" +
            "}";

    private static final String MAIN = "vec3 color = fragColor.rgb;\n" +
            "\tcolor = posterize(color);\n" +
            "\tcolor += edge(outTexCoords.xy);\n" +
            "\tfragColor = vec4(color, fragColor.a);";

    public ToonWithEdgeFilter() {
        super();
        mFilterMode.add(FilterManager.MONO_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        FRAG_SHADER_ARGS.add(ARGS);
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
