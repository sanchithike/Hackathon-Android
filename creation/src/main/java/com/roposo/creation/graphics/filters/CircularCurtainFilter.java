package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 01/12/17.
 */

public class CircularCurtainFilter extends TimeFilterFromScene {

    private static final String ARGS = "uniform float timeFactor;\n" +
            "float circle(in vec2 _st, in float _radius){\n" +
            "    vec2 dist = _st-vec2(0.5);\n" +
            "\treturn 1.-smoothstep(_radius-(_radius*0.01),\n" +
            "                         _radius+(_radius*0.01),\n" +
            "                         dot(dist,dist)*4.0);\n" +
            "}\n";

    private static final String MAIN = " fragColor = texture2D(baseSampler, outTexCoords);\n" +
            " vec2 st = vec2(outTexCoords.x, (outTexCoords.y-0.5)*0.6 + 0.5);\n" +
            " vec3 color = fragColor.rgb;\n" +
            " if (circle(st, timeFactor) == 0.0) {\n" +
            " \tcolor = vec3(0.0);\n" +
            " }\n" +
            "\n" +
            " fragColor = vec4( color, 1.0);\n" +
            " gl_FragColor = vec4(fragColor.rgba);\n";



    public CircularCurtainFilter() {
        super(0.0f, 1.5f, 1500, TimeAnimate.REVERSE);
        mFilterMode.add(FilterManager.CIRCULAR_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }
}
