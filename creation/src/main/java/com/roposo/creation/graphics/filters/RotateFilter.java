package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 12/18/17.
 */

public class RotateFilter extends TimeFilterFromScene {
    private static final String FRAG_SHADER_ARG = "#define PI 3.14159265359\n"+
            " uniform float timeFactor;\n"+
            " mat2 rotate2d(in float angle) {\n" +
            "    return mat2(cos(angle),-sin(angle),\n" +
            "                sin(angle),cos(angle));\n" +
            "}\n"
            ;

    private static final String FRAG_SHADER_STRING = "\n"+
            " vec3 color = vec3(0.0);\n" +
            " vec2 st = outTexCoords.xy - vec2(0.5);\n" +
            " st = rotate2d(sin(timeFactor) * PI) * st;\n" +
            " st += vec2(0.5);\n" +
            " color = texture2D(baseSampler, st).rgb;\n" +
            " fragColor = vec4(color,1.0);\n"
            ;

    public RotateFilter() {
        super(0.0f, 6.28f, 4000);
        mFilterMode.add(FilterManager.ROTATE_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
