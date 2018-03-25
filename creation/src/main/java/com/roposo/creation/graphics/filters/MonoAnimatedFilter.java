package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;
import android.util.Log;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;

/**
 * @author  by Muddassir on 16/10/17.
 */

public class MonoAnimatedFilter extends ImageFilter {
    public MonoAnimatedFilter() {
        super();
/*        super(BaseFilter.loadShader("defaultImage_vert.glsl", ContextHelper.applicationContext),
                BaseFilter.loadShader("mono_frag.glsl", ContextHelper.applicationContext));*/
    }

    @Override
    public void setup(Caches caches) {
        super.setup(caches);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                ValueAnimator animator = ValueAnimator.ofFloat(0.75f, 2.0f);
                animator.setDuration(2000);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mBlendFactor = (float) animation.getAnimatedValue();
                    }
                });
                animator.start();
            }
        });
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("blendFactor", mBlendFactor);
    }
}
