package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;

/**
 * @author by Muddassir on 16/10/17.
 */

public class MonoStripeFilter extends ImageFilter {

    private float startX;
    private static final float mDistance = 1 / 3f;

    public MonoStripeFilter() {
        super();
/*        super(BaseFilter.loadShader("defaultImage_vert.glsl", ContextHelper.applicationContext),
                BaseFilter.loadShader("mono_stripe_frag.glsl", ContextHelper.applicationContext));*/
    }

    @Override
    public void setup(Caches caches) {
        super.setup(caches);
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        startX += 0.01f;
        if (startX >= 1f) {
            startX = -mDistance;
        }

        float endX = startX + mDistance;
        Log.d(TAG, String.format("xStart = %f, xEnd = %f", startX, endX));
        program.uniform1f("xStart", startX);
        program.uniform1f("xEnd", endX);
    }
}
