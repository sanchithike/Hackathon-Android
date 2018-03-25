package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * @author by Muddassir on 16/10/17.
 */

public class RoposoMirror3Filter extends ImageFilter {

    private static final String FRAG_SHADER_COLOR = "" +
            "    float innerRect = 0.5;\n" +
            "    float outerRect = 0.75;\n" +
            "\n" +
            "    float innerRectFactorStart = (1.0 - innerRect) /2.0;\n" +
            "    float innerRectFactorEnd = 1.0 - innerRectFactorStart;\n" +
            "    float outerRectFactorStart = (1.0 - outerRect) / 2.0;\n" +
            "    float outerRectFactorEnd = 1.0 - outerRectFactorStart;\n" +
            "\n" +
            "    float x = " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".x;\n" +
            "    float y = " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ".y;\n" +
            "    if ((x < outerRectFactorStart || y < outerRectFactorStart)\n" +
            "            || (x > outerRectFactorEnd || y > outerRectFactorEnd)) {\n" +
            "        " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = texture2D(" + ProgramCache.SHADER_VAR_SAMPLER + ", " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ");\n" +
            "    } else {\n" +
            "        if ((x < innerRectFactorStart || y < innerRectFactorStart)\n" +
            "                || (x > innerRectFactorEnd || y > innerRectFactorEnd)) {\n" +
            "            " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = texture2D(" + ProgramCache.SHADER_VAR_SAMPLER + ", vec2(x - outerRectFactorStart\n" +
            "                    , y - outerRectFactorStart) * vec2(1.0 / outerRect, 1.0 / outerRect));\n" +
            "        } else {\n" +
            "            " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = texture2D(" + ProgramCache.SHADER_VAR_SAMPLER + ", vec2(x - innerRectFactorStart\n" +
            "                    , y - innerRectFactorStart) * vec2(1.0 / innerRect, 1.0 / innerRect));\n" +
            "        }\n" +
            "    }\n" +
            "    " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = blendFactor * " + ProgramCache.SHADER_VAR_FRAGCOLOR + ";\n" +
            "    gl_FragColor = vec4(" + ProgramCache.SHADER_VAR_FRAGCOLOR + ".rgba);";

    public RoposoMirror3Filter() {
        super();
        mFilterMode.add(FilterManager.ROPOSO_MIRROR_3_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_COLOR;
        registerShader();
    }

    @Override
    public void setup(Caches caches) {
        super.setup(caches);
    }
}
