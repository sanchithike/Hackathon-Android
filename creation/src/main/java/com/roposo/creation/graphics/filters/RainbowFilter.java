package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 3/9/18.
 */

public class RainbowFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "" +
            "vec3 col = 0.5 + (0.5 * cos(timeFactor + originalTexCoords.xyx + vec3(0,2,4)));\n" +
            "fragColor *= vec4(col,1.0);";

    public RainbowFilter() {
        super(0.0f, 10.0f, 6000, TimeAnimate.REVERSE);
        TAG = "RainbowFilter";
        mFilterMode.add(FilterManager.RAINBOW_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
