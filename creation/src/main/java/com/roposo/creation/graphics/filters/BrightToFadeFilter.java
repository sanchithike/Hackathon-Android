package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/6/18.
 */

public class BrightToFadeFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = ""+
            " fragColor *= vec4(vec3(abs(tan(timeFactor))), 1.0);"
            ;

    public BrightToFadeFilter() {
        super(1.57f, 1.0f, 1500, TimeAnimate.RESTART);
        mFilterMode.add(FilterManager.BRIGHT_TO_FADE_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

}
