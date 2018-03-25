package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * @author by Muddassir on 16/10/17.
 */

public class RoposoQuadrantFilter extends ImageFilter {

    private static final String FRAG_SHADER_STRING = "" +
            "vec4 RPS_CYAN = vec4(0.12, 0.8, 0.8, 1.0);\n"
            + "vec4 RPS_YELLOW = vec4(0.9, 0.68, 0.16, 1.0);\n"
            + "vec4 RPS_RED = vec4(0.9, 0.25, 0.35, 1.0);\n"
            + "vec4 RPS_MAGENTA = vec4(0.5, 0.25, 0.58, 1.0);\n"
            + "\n"
            + "vec4 screenColor;\n"
            + "\n"
            + "if(" + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".x < 0.5 && " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".y < 0.5) {\n"
            + "    screenColor = RPS_MAGENTA;\n"
            + "} else if(" + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".x < 0.5 && " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".y > 0.5) {\n"
            + "    screenColor = RPS_YELLOW;\n"
            + "} else if(" + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".x > 0.5 && " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".y < 0.5) {\n"
            + "    screenColor = RPS_CYAN;\n"
            + "} else {\n"
            + "    screenColor = RPS_RED;\n"
            + "}\n"
            + ProgramCache.SHADER_VAR_FRAGCOLOR + " = screenColor;\n"
            ;

    public RoposoQuadrantFilter() {
        super();
        mFilterMode.add(FilterManager.ROPOSO_QUADRANT_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
