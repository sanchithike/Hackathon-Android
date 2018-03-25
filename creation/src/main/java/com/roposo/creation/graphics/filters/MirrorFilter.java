package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_FRAGCOLOR;

/**
 * @author  bajaj on 15/10/17.
 */

public class MirrorFilter extends ImageFilter {

    private static final String gFS_Main_ColorFilter_Mirror =
            "    vec2 focusCoord = vec2(0.0, 1.0);\n" +
                    "    vec2 texCoord = 2.0 * outTexCoords - vec2(1.0, 1.0);\n" +
                    "    texCoord = texCoord * sign(texCoord + focusCoord);\n" +
                    "    texCoord = texCoord * 0.5 + vec2(0.5, 0.5);\n" +
                    "    " + SHADER_VAR_FRAGCOLOR + " = texture2D(baseSampler, texCoord);\n";


    public MirrorFilter() {
        super();
        mFilterMode.add(FilterManager.MIRROR_FILTER);
        FRAG_SHADER_MAIN += gFS_Main_ColorFilter_Mirror;
        registerShader();
    }
}