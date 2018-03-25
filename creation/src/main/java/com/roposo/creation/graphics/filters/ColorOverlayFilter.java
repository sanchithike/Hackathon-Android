package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;

/**
 * A basic color overlay filter which expects the filterMode value and
 * overlay rgb values during filter object construction
 * Created by akshaychauhan on 12/5/17.
 */

public abstract class ColorOverlayFilter extends ImageFilter {
    private volatile float[] colorMultiplier;
    private static final String FRAG_SHADER_ARG = "uniform vec4 colorMultiplier;\n"
            ;

    private static final String FRAG_SHADER_STRING =
            "fragColor.rgb = vec3(fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11);\n" +
            "vec3 blendColor = fragColor.rgb * colorMultiplier.rgb;\n" +
            " fragColor = vec4(blendColor, fragColor.a);\n"
            ;


    ColorOverlayFilter(String filterMode, float[] colorMultiplier) {
        super();
        this.colorMultiplier = colorMultiplier;
        mFilterMode.add(filterMode);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform4fv("colorMultiplier", this.colorMultiplier);
    }

    @Override
    void copy(BaseFilter baseFilter) {
        ColorOverlayFilter filter = (ColorOverlayFilter) baseFilter;
        super.copy(baseFilter);
        colorMultiplier = filter.colorMultiplier.clone();
    }
}
