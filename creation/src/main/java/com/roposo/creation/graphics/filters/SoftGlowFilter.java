package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import java.util.Locale;

/**
 * Created by akshaychauhan on 12/16/17.
 */

public class SoftGlowFilter extends ImageFilter {

    private float mSaturationAmount = 1.25f;

    private static final String FRAG_SHADER_ARG = "uniform float saturationAmt;\n"+
            " vec3 saturate(vec3 color, float amount) {\n" +
            " vec3 gray = vec3(dot(vec3(0.3,0.59,0.11), color));\n"+
            " return vec3(mix(color, gray, -amount));\n"+
            "}\n"
            ;

    private static final String FRAG_SHADER_COLOR = ""+
                " fragColor = vec4(saturate(fragColor.rgb , saturationAmt), fragColor.a);\n"
                ;

    public SoftGlowFilter() {
        super();
        mFilterMode.add(FilterManager.SOFT_GLOW_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_COLOR;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        Log.d(TAG, String.format(Locale.getDefault(), "Saturation Amount (%f)", mSaturationAmount));
        program.uniform1f("saturationAmt", mSaturationAmount);
    }

}
