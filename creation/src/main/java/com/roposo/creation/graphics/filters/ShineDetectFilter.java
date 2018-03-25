package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * Created by Tanvi on 22/11/17.
 */

public class ShineDetectFilter extends ImageFilter {
    private static final String FRAG_SHADER_ARGS_STRING =
            "uniform float strength;\n" +
            "\n" + ProgramCache.gFS_Header_Func_CIE_LAB;

    private static final String FRAG_SHADER_STRING = /*" if (outTexCoords.x > 0.5) { fragColor = fragColor2; }";*/
            "vec4 inputColors = fragColor.rgba;\n" +
            "vec4 pixelAVG = cieLAB(fragColor2);\n" +
            "vec4 pixel = cieLAB(inputColors);\n" +
            "\n" +
            "if (pixel.r >= 100.0 && pixel.r >= (1.5 - (strength-1.0)* 0.2)*pixelAVG.r)\n" +
            "{\n" +
            "    fragColor = vec4(1.0/100.0, 1.0, 1.0, 1.0);\n" +
            "}\n" +
            "else\n" +
            "{\n" +
                "    fragColor = vec4(0.0 , pixel.g, pixel.b, 1.0);\n" +
            "}\n";

    public ShineDetectFilter() {
        super();
        mFilterMode.add(FilterManager.SHINE_DETECT_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARGS_STRING);
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("strength", 1.0f);
    }
}
