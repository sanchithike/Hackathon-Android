package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_FRAGCOLOR;

/**
 * Created by tajveer on 2/16/18.
 */

public class Blur1DFilter extends ImageFilter {

    private static final String MAIN_H =
            "vec4 blurredColor = vec4(0.0, 0.0, 0.0, 0.0);\n"
                    + "    blurredColor += 0.3 * fragColor;\n"
                    + "    blurredColor += 0.35 * sampleTexture(outTexCoords + vec2(-1.33, 0.0) * texelSize);\n"
                    + "    blurredColor += 0.35 * sampleTexture(outTexCoords + vec2(1.33, 0.0) * texelSize);\n"
                     + "       " + SHADER_VAR_FRAGCOLOR + " = blurredColor;\n"
                    /*+ "       " + SHADER_VAR_FRAGCOLOR + " = fragColor/fragColor.a;\n"*/;

    private static final String MAIN_V =
            "vec4 blurredColor = vec4(0.0, 0.0, 0.0, 0.0);\n"
                    + "    blurredColor += 0.3 * fragColor;\n"
                    + "    blurredColor += 0.35 * sampleTexture(outTexCoords + vec2(0.0, 1.33) * texelSize);\n"
                    + "    blurredColor += 0.35 * sampleTexture(outTexCoords + vec2(0.0, -1.33) * texelSize);\n"
                    + "       " + SHADER_VAR_FRAGCOLOR + " = blurredColor;\n"
                    /*+ "       " + SHADER_VAR_FRAGCOLOR + " = fragColor/fragColor.a;\n"*/;

    public Blur1DFilter(boolean horizontal) {
        super();
        mFilterMode.add(horizontal ? FilterManager.BLUR_H_FILTER : FilterManager.BLUR_V_FILTER);
        FRAG_SHADER_MAIN += horizontal ? MAIN_H : MAIN_V;

        registerShader();
    }
}
