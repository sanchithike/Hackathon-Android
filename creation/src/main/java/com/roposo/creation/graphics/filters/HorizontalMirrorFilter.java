package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 3/9/18.
 */

public class HorizontalMirrorFilter extends ImageFilter {

    private static final String FRAG_SHADER_STRING = "\n"+
            "fragColor = texture2D(baseSampler, vec2(1.0 - outTexCoords.x, outTexCoords.y));"
            ;


    public HorizontalMirrorFilter() {
        super();
        mFilterMode.add(FilterManager.HORIZONTAL_MIRROR_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
