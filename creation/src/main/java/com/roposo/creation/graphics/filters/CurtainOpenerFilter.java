package com.roposo.creation.graphics.filters;

import com.roposo.creation.R;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * The image/video will gradually open from the middle of the screen outwards
 * Created by akshaychauhan on 12/9/17.
 */

public class CurtainOpenerFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "" +
            " fragColor = texture2D(baseSampler, outTexCoords.xy);\n" +
            " vec4 outputPixels = vec4(vec3(0.0), 1.0);\n" +

            " float left = step(0.5 * timeFactor, 1.0 - outTexCoords.y);\n" +
            " float right = step(0.5 * timeFactor, outTexCoords.y);\n" +

            // Since we have image as rotated in the screen ,
            // that's why left and right curtain checks are opposite
            " if ((left != 0.0 && outTexCoords.y >= 0.5) || " +
            "     (right != 0.0 && outTexCoords.y < 0.5)) {\n" +
            "      outputPixels = fragColor;\n" +
            " }\n" +

            " fragColor = outputPixels;\n";

    public CurtainOpenerFilter() {
        super(0.0f, 1.0f, 6000, TimeAnimate.REVERSE, R.raw.curtain_slider);
        TAG = "CurtainOpenerFilter";
        mFilterMode.add(FilterManager.CURTAIN_OPENER_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

}
