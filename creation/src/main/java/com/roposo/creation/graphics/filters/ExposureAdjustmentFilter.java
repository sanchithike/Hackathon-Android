package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;

/**
 * @author by Muddassir on 16/10/17.
 */

public class ExposureAdjustmentFilter extends ImageFilter {
    private float mExposure;

    public ExposureAdjustmentFilter() {
        super();
/*        super(BaseFilter.loadShader("defaultImage_vert.glsl", ContextHelper.applicationContext),
                BaseFilter.loadShader("exposure_frag.glsl", ContextHelper.applicationContext));*/
        mExposure = 0.5f;
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("exposure", mExposure);
    }

    public void setExposure(float mExposure) {
        this.mExposure = mExposure;
    }
}
