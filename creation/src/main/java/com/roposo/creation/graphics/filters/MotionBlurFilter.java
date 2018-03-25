package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_FRAGCOLOR;

/**
 * Created by tajveer on 2/16/18.
 */

public class MotionBlurFilter extends ImageFilter {

    private static final String MAIN =
            "vec4 blurredColor = vec4(0.0, 0.0, 0.0, 0.0);\n"
                    + "    blurredColor += 0.5 * fragColor;\n"
                    + "    blurredColor += 0.3 * fragColor2;\n"
                    + "    blurredColor += 0.15 * fragColor3;\n"
                    + "    blurredColor += 0.05 * fragColor4;\n"
                     + "       " + SHADER_VAR_FRAGCOLOR + " = blurredColor;\n";

    public MotionBlurFilter() {
        super();
        mFilterMode.add(FilterManager.BLUR_MOTION_FILTER);
        FRAG_SHADER_MAIN += MAIN;

        registerShader();
    }
}
