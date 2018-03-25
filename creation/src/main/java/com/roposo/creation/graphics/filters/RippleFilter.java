package com.roposo.creation.graphics.filters;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 26/02/18.
 */

public class RippleFilter extends TimeFilterFromScene {

    private static String ARGS = "uniform float timeFactor;\n";
    private static String MAIN = BaseFilter.loadShader("RippleFilter.txt", ContextHelper.getContext());

    public RippleFilter() {
        super();
        mFilterMode.add(FilterManager.RIPPLE_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
    }

}
