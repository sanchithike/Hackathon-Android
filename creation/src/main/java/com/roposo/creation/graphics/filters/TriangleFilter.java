package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;

/**
 * @author by Muddassir on 16/10/17.
 */

public class TriangleFilter extends ImageFilter {

    private float mAngleZero = (float) (60 * Math.PI / 180f),
            mAngleOne = (float) (30 * Math.PI / 180f),
            mAngleTwo = (float) (15 * Math.PI / 180f),
            mAngleThree = (float) (0 * Math.PI / 180f);

    private float mBlurRadius = (float) (5f * Math.PI / 180f);

    public TriangleFilter() {
        super();
/*        super(BaseFilter.loadShader("defaultImage_vert.glsl", ContextHelper.applicationContext),
                BaseFilter.loadShader("triangle_frag.glsl", ContextHelper.applicationContext));*/
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("blurRadius", mBlurRadius);
        program.uniform1f("angleZero", mAngleZero);
        program.uniform1f("angleOne", mAngleOne);
        program.uniform1f("angleTwo", mAngleTwo);
        program.uniform1f("angleThree", mAngleThree);
    }
}
