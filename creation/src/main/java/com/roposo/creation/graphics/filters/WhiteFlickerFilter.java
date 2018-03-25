package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This will add a gradual white flickering effect to the image or video
 * Created by akshaychauhan on 12/2/17.
 */

public class WhiteFlickerFilter extends TimeFilterFromScene {
    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING =
                    "   vec4 colorMultiplier;\n" +
                    // colorMultiplier will range from 1.0 to 2.0 - to increase brightness
                    "   if (timeFactor < 43.974 ) { \n" + //First 7 cycles
                    "       colorMultiplier = vec4(vec3(clamp(abs(tan(timeFactor)), 1.0, 2.0)), 1.0);\n" +
                    "   }\n" +
                    "   else {\n" +
                    // colorMultiplier will range from 0.4 to 1.0
                    "       colorMultiplier = vec4(vec3(clamp(smoothstep(0.1, 2.0, abs(tan(timeFactor))), 0.4 , 1.0)), 1.0);\n" +
                    "   }\n" +
                    "  fragColor  *=  colorMultiplier;\n";

    public WhiteFlickerFilter() {
        super(0.0f, 94.24f, 5000); //5400.0 degree , 15 complete cycles of 360 degree
        mFilterMode.add(FilterManager.WHITE_DISCO_LIGHTS_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
