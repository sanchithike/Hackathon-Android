package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/6/18.
 */

public class PhotoFlickerFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "\n" +
            "float timeFact = fract(timeFactor);\n" +
            "\n" +
            "if ((timeFact > 0.4 && timeFact < 0.45 ) || (timeFact > 0.55 && timeFact < 0.6)) {\n" +
            " fragColor *= vec4(vec3(clamp(abs(tan(1.57)), 1.75, 2.0)), 1.0);\n" +
            "}\n"+
            "else if (timeFact < 0.2) {"+
            " fragColor.a = 0.4;"+
            "}"
            ;

    public PhotoFlickerFilter() {
        super(1.57f, 1.0f, 4000, TimeAnimate.RESTART);
        mFilterMode.add(FilterManager.PHOTO_FLICKER_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

}
