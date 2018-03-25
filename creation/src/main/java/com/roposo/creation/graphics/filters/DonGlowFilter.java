package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 12/18/17.
 */

public class DonGlowFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "\n" +
            " if (fract(timeFactor) > 0.5) {\n" +
            "   float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;\n" +
            "   vec4 outputPixels = vec4(vec3(pixelLuminance), fragColor.a);\n" +
            "   fragColor = outputPixels * vec4(vec3(clamp(abs(tan(timeFactor)), 1.75, 2.0)), 1.0);\n" +
            " }"
            ;


    public DonGlowFilter() {
        super(0.0f, 3.14f, 3000);
        mFilterMode.add(FilterManager.DON_GLOW_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
