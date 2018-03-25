package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by bajaj on 15/10/17.
 */

public class MonoFilter extends ImageFilter {
    private static final String FRAG_SHADER_STRING = "" +
            "   float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;\n" +
            "   fragColor = vec4(pixelLuminance, pixelLuminance, pixelLuminance, fragColor.a);\n";

    public MonoFilter() {
        super();
        mFilterMode.add(FilterManager.MONO_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
