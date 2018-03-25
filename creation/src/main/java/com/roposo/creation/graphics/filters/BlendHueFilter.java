package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 07/03/18.
 */

public class BlendHueFilter extends ImageFilter {
    private volatile float[] hue;
    private static final String FRAG_SHADER_ARG = "uniform vec4 hue;\n";
    private static final String FRAG_SHADER_STRING = "fragColor.rgb = BlendHue(fragColor.rgb, hue.rgb);\n";

    public BlendHueFilter(String filterMode, float[] hue) {
        super();
        this.hue = hue;
        mFilterMode.add(filterMode);
        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.RGB_HSL);
        FRAG_SHADER_ARGS.add(FragmentShaderArgsFunctions.BLEND_HUE);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform4fv("hue", this.hue);
    }

    @Override
    void copy(BaseFilter baseFilter) {
        BlendHueFilter filter = (BlendHueFilter) baseFilter;
        super.copy(baseFilter);
        hue = filter.hue.clone();
    }
}
