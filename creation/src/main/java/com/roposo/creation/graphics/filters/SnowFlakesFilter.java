package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This code is referred from : http://www.glslsandbox.com/e#36547.0
 * Created by akshaychauhan on 1/9/18.
 */

public class SnowFlakesFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n" +
            "float snow(vec2 uv, float scale) {\n" +
            "  uv += timeFactor/scale;\n" +
            "  uv.y += timeFactor * 2.0/scale;\n" +
            "  uv.x += sin(uv.y + timeFactor * 0.5)/scale;\n" +
            "  uv *= scale;\n" +
            "  vec2 s = floor(uv), f=fract(uv), p;\n" +
            "  float k = 2.0 , d;\n" +
            "  p = 0.5 + 0.35 * sin(5.0 * fract(sin((s + p + scale) * mat2(7,3,6,5)) * 5.0)) - f;\n" +
            "  d = length(p);\n" +
            "  k = min(d,k);\n" +
            "  k = smoothstep(0.0, k, sin(f.x + f.y) * 0.008);\n" +
            "  return k;\n" +
            "}";

    private static final String FRAG_SHADER_STRING = "\n"+
            " vec2 uv = outTexCoords.xy;\n" +
            " vec3 finalColor = vec3(1.0);\n" +
            " float c = 0.0;\n" +
            " c += snow(uv, 10.0);\n" +
            " c += snow(uv, 5.0);\n" +
            " finalColor = (vec3(c));\n" +
            " fragColor += vec4(finalColor, 1.0);";

    public SnowFlakesFilter() {
        super(0.0f, 3.14f, 500, TimeAnimate.RESTART);
        mFilterMode.add(FilterManager.SNOW_FLAKES_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
