package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 29/11/17.
 */

public class ColorMixFilter extends TimeFilter {
    private static final String ARGS = "uniform float timeFactor;";
    private static final String MAIN = "vec2 st = outTexCoords.xy/vec2(0.778, 1);\n" +
            "st.x *= 0.778;\n" +
            "vec3 color = vec3(0.);\n" +
            "color = vec3(st.x,st.y,abs(sin(timeFactor)));\n" +
            "vec3 mixed = mix(color, fragColor.rgb);" +
            "fragColor = vec4(mixed, 1.0);";


    public ColorMixFilter() {
        super();
        mFilterMode.add(FilterManager.GRAINY_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }
}
