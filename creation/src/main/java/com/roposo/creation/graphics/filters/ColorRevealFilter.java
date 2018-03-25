package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;
import android.util.Log;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.creation.R;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;

import java.util.Locale;

/**
 * This filter has three effects combined :
 *  1. Mono effect (black and white)
 *  2. Gradual Purple Color Overlay starting from mid of the photo towards the screen edges
 *  3. Flicker effect
 * Created by akshaychauhan on 12/6/17.
 */

public class ColorRevealFilter extends TimeFilterFromScene {
    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n"
            ;

    private static final String FRAG_SHADER_STRING = ""+
           "vec3 PURPLE_COLOR = vec3(0.867, 0.627, 0.867);\n" +
            "\n" +
            "float left = step(0.5 * timeFactor, outTexCoords.y);\n" +
            "if (left != 0.0 && outTexCoords.y < 0.5) { \n" +
            "  fragColor.rgb = max(fragColor.rgb + PURPLE_COLOR - 1.0, 0.0);\n" +
            "}\n" +
            "\n" +
            "float right = step(0.5 * timeFactor, 1.0 - outTexCoords.y);\n" +
            "if (right != 0.0 && outTexCoords.y >= 0.5) {\n" +
            "  fragColor.rgb = max(fragColor.rgb + PURPLE_COLOR - 1.0, 0.0);\n" +
            "}\n" +
            "\n" +
            "vec4 flickerMultiplier = vec4(vec3(step(0.5, fract(timeFactor * 1000.0)) + 0.5), 1.0);\n" +
            "fragColor *= flickerMultiplier;\n"
            ;

    public ColorRevealFilter() {
        super(0, 1, 6000, TimeAnimate.REVERSE, R.raw.color_reveal);
        mFilterMode.add(FilterManager.COLOR_REVEAL_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
