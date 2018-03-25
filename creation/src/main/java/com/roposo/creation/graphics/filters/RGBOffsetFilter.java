package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 18/12/17.
 */

public class RGBOffsetFilter extends TimeFilterFromScene {
    private static final String ARGS = "uniform float timeFactor;";
    private static final String MAIN = "float rgbOffsetOpt = timeFactor * timeFactor;\n"+
            "float y = mod(outTexCoords.y,1.0);\n" +
            "float red = texture2D(baseSampler, vec2(outTexCoords.x - 0.01 * rgbOffsetOpt,y - 0.01 * rgbOffsetOpt)).r;\n" +
            "float green = texture2D(baseSampler, vec2(outTexCoords.x, y)).g;\n" +
            "float blue = texture2D(baseSampler, vec2(outTexCoords.x +0.01*rgbOffsetOpt,y + 0.01 * rgbOffsetOpt)).b;\n" +
            "\n" +
            "vec3 color = vec3(red,green,blue);\n" +
            "fragColor = vec4(color,1.0);";

    public RGBOffsetFilter() {
        super(-1.8f, 1.8f, 2000, TimeAnimate.REVERSE);
        mFilterMode.add(FilterManager.RGBOFFSET_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }
}
