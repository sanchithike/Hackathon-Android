package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;

/**
 * Created by Tanvi on 13/12/17.
 */

public class ContrastFilter extends ImageFilter {
    private static final String ARGS = "uniform float contrastStrength;\n" +
            ProgramCache.gFS_Header_Func_HSV;
    private static final String FRAG_SHADER_STRING = "vec3 hsv = rgb2hsv(fragColor.rgb);\n" +
            "hsv.y = hsv.y*contrastStrength;\n" +
            "fragColor = vec4(hsv2rgb(hsv),1.0);\n";
    private static float mContrastStrength = 1.6f;

    public ContrastFilter() {
        super();
        mFilterMode.add(FilterManager.CONTRAST_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        Log.d(TAG, String.format(Locale.getDefault(), "Constrast Strength(%f)", mContrastStrength));
        program.uniform1f("contrastStrength", mContrastStrength);
    }
}
