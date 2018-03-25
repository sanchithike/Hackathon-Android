package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This will create a diagonal mirror effect across the image/video diagonal (from bottom left to top right)
 * // TODO :  - While taking the camera input it takes opposite diagonal : probably because the origin
 *              in that case lies at bottom left (not sure)
 *            - Aspect ratio of reflected image
 * Created by akshaychauhan on 11/29/17.
 */

public class DiagonalMirrorFilter extends ImageFilter {
    private static final String FRAG_SHADER_STRING = "" +
            " vec2 inputCoords = outTexCoords.xy;\n" +
            " vec4 outputPixels = fragColor;\n" +
            " if (inputCoords.x + inputCoords.y > 1.0) {\n" +
            "   vec4 mirroredAcrossDiagnol = texture2D(baseSampler, vec2(1.0 - inputCoords.y, 1.0 - inputCoords.x));\n"+
            "   outputPixels = mirroredAcrossDiagnol;\n"+
            "}\n" +
            " fragColor  = outputPixels;\n";


    public DiagonalMirrorFilter() {
        super();
        mFilterMode.add(FilterManager.DIAGONAL_MIRROR_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
