package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.Program;

import java.util.Locale;

/**
 * Created by akshaychauhan on 1/31/18.
 */

public class LutFilter extends ImageFilter {

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private float intensity = 1.0f;
    private float transitionPoint = 1.0f;
    private float leftLutFlag = 1.0f;
    private float rightLutFlag = 0.0f;

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setTransitionPoint(float transitionPoint) {
        this.transitionPoint = transitionPoint;
    }

    public void setLeftLutFlag(float leftLutFlag) {
        this.leftLutFlag = leftLutFlag;
    }

    public void setRightLutFlag(float rightLutFlag) {
        this.rightLutFlag = rightLutFlag;
    }

    private static final String FRAG_SHADER_ARG = "uniform float intensity;\n"+
            "uniform float transitionPt;\n"+
            "uniform float llf;\n"+
            "uniform float rlf;\n"
            ;

    private static final String FRAG_SHADER_STRING =
            "vec4 inputColor = clamp(fragColor.rgba, vec4(0.0), vec4(1.0));"+
            "highp float blueColor = inputColor.b;\n" +
            "highp vec2 box1;\n" +
            "box1.y = floor(blueColor * 32.0);\n" +
            "box1.x = 0.0;\n" +

            "highp vec2 box2;\n" +
            "box2.y = box1.y + 1.0;\n" +
            "box2.x = 0.0;\n" +

            "highp vec2 texPos1;\n" +
            "texPos1.x = clamp((inputColor.g * (32.0/33.0))+ 0.5/33.0, 0.0, 1.0);\n" +
            // 33 are the number of rows in one small box,
            // 32 are number of regions in one row in small box
            // 0.5/1089 is the distance to centre of pixel from start of box
            "texPos1.y = clamp(((box1.y * 33.0)  + (inputColor.r * 32.0))/(1089.0) + 0.5/(1089.0), 0.0, 1.0);\n" +

            "highp vec2 texPos2;\n" +
            "texPos2.x = clamp((inputColor.g * (32.0/33.0))+ 0.5/33.0, 0.0, 1.0);\n" +
            "texPos2.y = clamp(((box2.y * 33.0)  + (inputColor.r * 32.0))/(1089.0) + 0.5/(1089.0), 0.0, 1.0);\n" +

            "highp vec4 newColor1;\n" +
            "highp vec4 newColor2;\n" +

            "highp vec4 newColor = inputColor;\n" +
            "if (llf >= 1.0 && originalTexCoords.x < transitionPt) {\n"+
            "    newColor1 = sampleLutTexture(texPos1);\n"+
            "    newColor2 = sampleLutTexture(texPos2);\n"+
            "    newColor = mix(newColor1, newColor2, fract(blueColor));\n"+
            "}\n"+
            "else if (rlf >= 1.0 && originalTexCoords.x > transitionPt) {\n"+
            "    newColor1 = sampleLutTexture2(texPos1);\n"+
            "    newColor2 = sampleLutTexture2(texPos2);\n"+
            "    newColor = mix(newColor1, newColor2, fract(blueColor));\n"+
            "}\n"+
            "fragColor = mix(fragColor, vec4(newColor.rgb, inputColor.a), intensity);\n"
            ;

    public LutFilter() {
        super();
        mFilterMode.add(FilterManager.LUT_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        if (VERBOSE) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Intensity Value (%f), Transition Point (%f) , LLF (%f) , RLF (%f) ",
                    intensity, transitionPoint, leftLutFlag, rightLutFlag));
        }
        program.uniform1f("intensity", intensity);
        program.uniform1f("transitionPt", transitionPoint);
        program.uniform1f("llf", leftLutFlag);
        program.uniform1f("rlf", rightLutFlag);
    }
}
