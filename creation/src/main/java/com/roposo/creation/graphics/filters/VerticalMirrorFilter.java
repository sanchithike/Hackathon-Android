package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/16/18.
 */

public class VerticalMirrorFilter extends ImageFilter {

    private static final String FRAG_SHADER_STRING = "\n"+
            "fragColor = texture2D(baseSampler, vec2(outTexCoords.x, 1.0 - outTexCoords.y));"
            ;


    public VerticalMirrorFilter() {
        super();
        mFilterMode.add(FilterManager.VERTICAL_MIRROR_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
