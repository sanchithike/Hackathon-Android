package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 06/12/17.
 */

public class RectangularCurtainFilter extends TimeFilterFromScene {
    private static final String ARGS = "uniform float timeFactor;\n";
    private static final String MAIN = "vec2 coordsToUse = outTexCoords - vec2(0.5);\n" +
            "float a = atan(coordsToUse.x, coordsToUse.y);\n" +
            "float val = step(timeFactor,cos(floor(a*.636+.5)* 1.57-a)*length(coordsToUse.xy));\n" +
            "if (val == 1.0) {\n" +
            "\tfragColor = vec4(vec3(0.0),1.0);\n" +
            "}\n";

    public RectangularCurtainFilter() {
        super(0.0f, 1.0f, 1000, TimeAnimate.REVERSE);
        mFilterMode.add(FilterManager.RECTANGULAR_CURTAIN_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }
}
