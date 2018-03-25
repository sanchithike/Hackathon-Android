package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/13/18.
 */

public class WhiteFilter extends ImageFilter {

    private static final String FRAG_SHADER_STRING = "" +
            "   fragColor = vec4(vec3(0.98, 0.92, 0.84),1.0);"
            ;

    public WhiteFilter() {
        mFilterMode.add(FilterManager.WHITE_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
