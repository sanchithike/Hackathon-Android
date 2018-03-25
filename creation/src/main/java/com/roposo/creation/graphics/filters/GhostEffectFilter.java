package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 12/18/17.
 */

public class GhostEffectFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "" +
            "if (fract(timeFactor) > 0.5) {\n"+
            "   fragColor *= vec4(1.0 - fragColor.r , 1.0 - fragColor.g, 1.0 - fragColor.b , 1.0);\n"+
            " }\n"
            ;

    public GhostEffectFilter() {
        super(0.0f, 1.0f, 1000);
        mFilterMode.add(FilterManager.GHOST_EFFECT_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
