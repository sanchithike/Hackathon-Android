package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Continuously switch between mono to color values of image
 * Created by akshaychauhan on 12/7/17.
 */

public class MonoToColorAnimateFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = ""+
            "float switchFactor = step(0.5, fract(timeFactor));\n"+
            "if (switchFactor == 0.0) {\n"+
            "  float monoFactor = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;\n" +
            "  fragColor = vec4(vec3(monoFactor), fragColor.a);\n"+
            "}\n"
            ;

    public MonoToColorAnimateFilter() {
        super(0.0f, 10.0f, 10000);
        mFilterMode.add(FilterManager.MONO_TO_COLOR_ANIMATE_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
