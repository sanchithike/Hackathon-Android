package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;

/**
 * Created by akshaychauhan on 1/23/18.
 */

public class ImageAdjustmentFilter extends ImageFilter {

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private float mSaturation = 0.5f;
    private float mBrightness = 0.5f;
    private float mContrast = 0.5f;

    public void setBrightness(float brightness) {
        mBrightness = brightness;
    }

    public void setContrast(float contrast) {
        mContrast = contrast;
    }

    public void setSaturation(float saturation) {
        mSaturation = saturation;
    }

    private static final String FRAG_SHADER_ARG = "\n" +
            "uniform float contrastStrength;\n" +
            "uniform float saturationStrength;\n" +
            "uniform float brightnessStrength;\n"
            ;

    private static final String FRAG_SHADER_STRING = "\n" +
            "vec3 hsv = rgb2hsv(fragColor.rgb);\n" +
            // Value of contrastStrength , saturationStrength , brightnessStrength will be obtained
            // in range of 0 to 1 , we will calculate the factor according to required range

            // Change in saturation (value should be from 0 to 2) and default value 1.0
            "float saturationFactor = 2.0 * saturationStrength;\n"+
            "hsv.y *= saturationFactor;\n" +
            "fragColor = vec4(hsv2rgb(hsv),1.0);\n" +

            // Change in contrast (0.25 to 1.50) and default as 1.0 &
            // brightness (brightness factor should be from -0.75 to 0.75) & default value 0.0
            "float contrastFactor = -0.5 * (contrastStrength * contrastStrength) + (1.75 * contrastStrength) + 0.25;\n"+
            "float brightnessFactor = (1.5 * brightnessStrength) - 0.75;\n"+
            "fragColor.r = ((contrastFactor) * (fragColor.r - 0.5) + 0.5) + brightnessFactor;\n"+
            "fragColor.g = ((contrastFactor) * (fragColor.g - 0.5) + 0.5) + brightnessFactor;\n"+
            "fragColor.b = ((contrastFactor) * (fragColor.b - 0.5) + 0.5) + brightnessFactor;\n"
            ;

    public ImageAdjustmentFilter() {
        super();
        mFilterMode.add(FilterManager.IMAGE_ADJUSTMENT_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_ARGS.add(ProgramCache.gFS_Header_Func_HSV);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        if (VERBOSE) {
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Saturation Strength(%f) , " +
                            "Contrast Strength(%f), " +
                            "Brightness Strength(%f) ",
                    mSaturation, mContrast, mBrightness));
        }
        program.uniform1f("saturationStrength", mSaturation);
        program.uniform1f("contrastStrength", mContrast);
        program.uniform1f("brightnessStrength", mBrightness);
    }
}
