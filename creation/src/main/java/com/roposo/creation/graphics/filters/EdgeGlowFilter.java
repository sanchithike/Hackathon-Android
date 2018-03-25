package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import static com.roposo.creation.graphics.gles.ProgramCache.gFS_Edge_Gradient;

/**
 * Created by akshaychauhan on 1/22/18.
 */

public class EdgeGlowFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "#define THRESHOLD 0.9 \n" +
            "uniform float timeFactor;\n" +
            "float strength = 1.0;\n" +
            "\n" +
            ProgramCache.gFS_Edge_Gradient
            ;

    private static final String FRAG_SHADER_STRING = ""+
            "vec3 sobelResult = sobel(strength * texelSize.x, strength * texelSize.y, outTexCoords);\n" +
            "if (sobelResult.x > THRESHOLD) {\n"+
                    "   float redValue = sobelResult.x * (sin(1.0 * timeFactor));\n"+
                    "   float greenValue = sobelResult.x * (sin(1.0 * timeFactor + 1.04));\n"+
                    "   float blueValue = sobelResult.x * (sin(1.0 * timeFactor + 2.08));\n"+
                    "   fragColor.rgb = vec3(redValue, greenValue, blueValue);\n"+
                    "}"
            ;

    public EdgeGlowFilter() {
        super(0.0f, 3.14f, 1000);
        mFilterMode.add(FilterManager.EDGE_GLOW_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
