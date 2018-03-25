package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 31/01/18.
 */

public class HistoPyramidDivideBy1kFilter extends ImageFilter {
    private static final String MAIN =
                    "\tfragColor = vec4(fragColor.x/1.0, fragColor.y, fragColor.z, 1.0);\n";

    public HistoPyramidDivideBy1kFilter() {
        super();
        mFilterMode.add(FilterManager.HISTOPYRAMIDSUM_CALCULATOR_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();

    }
}
