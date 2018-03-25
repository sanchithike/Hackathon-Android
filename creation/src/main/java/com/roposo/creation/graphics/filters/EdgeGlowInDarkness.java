package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.ProgramCache;

import static com.roposo.creation.graphics.gles.ProgramCache.gFS_Edge_Gradient;

/**
 * Created by akshaychauhan on 1/23/18.
 */

public class EdgeGlowInDarkness extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG =
            "uniform float timeFactor;\n" +
            "float strength = .5;\n" +
            gFS_Edge_Gradient
            ;

    private static final String FRAG_SHADER_STRING = ""+
            "vec3 sobelResult = sobel(strength * texelSize.x, strength * texelSize.y, outTexCoords);\n" +
            "fragColor.rgb *= sobelResult;\n"
            ;

    public EdgeGlowInDarkness() {
        super(0.5f, 1.5f, 1000);
        mFilterMode.add(FilterManager.EDGE_GLOW_IN_DARKNESS_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
