package com.roposo.creation.graphics.filters;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 12/16/17.
 */

public class GrayScaleFlickerFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "" +
            " if ( timeFactor > 10.0 && timeFactor < 90.0 ) {\n"+
            "   fragColor.rgb = vec3(dot(vec3(0.3,0.59,0.11), fragColor.rgb));\n"+
            "   fragColor *= vec4(vec3(step(0.5, fract(timeFactor))), 1.0);\n"+
            " }\n"
            ;

    public GrayScaleFlickerFilter() {
        super(0.0f, 100.0f, 8000, TimeAnimate.REVERSE);
        mFilterMode.add(FilterManager.GRAY_SCALE_FLICKER_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

}
