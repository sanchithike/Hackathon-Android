package com.roposo.creation.graphics.filters;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 20/02/18.
 */

public abstract class HypnoFilter extends TimeFilterFromScene {

    private volatile float[] color1;
    private volatile float[] color2;
    private static String ARGS = "uniform float timeFactor;\n"
            + "uniform vec3 color1;\n"
            + "uniform vec3 color2;\n"
            ;
    private static String MAIN = BaseFilter.loadShader("HypnoMain.txt", ContextHelper.getContext());

    HypnoFilter(String filterMode, float[] color1, float[] color2) {
        super(true);
        this.color1 = color1;
        this.color2 = color2;
        mFilterMode.add(filterMode);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform3fv("color1", this.color1);
        program.uniform3fv("color2", this.color2);
    }

    @Override
    void copy(BaseFilter baseFilter) {
        HypnoFilter filter = (HypnoFilter) baseFilter;
        super.copy(baseFilter);
        color1 = filter.color1.clone();
        color1 = filter.color2.clone();
    }


}
