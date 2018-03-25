package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;
import android.util.Log;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.ContextHelper;
import com.roposo.creation.av.AVUtils;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Locale;

/**
 * @author by Muddassir on 16/10/17.
 */

public class RGBAnimatedFilter extends ImageFilter {
    private ValueAnimator redAnimator, greenAnimator, blueAnimator;
    private volatile float mRedFactor = 1f, mBlueFactor = 1f, mGreenFactor = 1f;

    private static final String FRAG_SHADER_ARG = BaseFilter.loadShader("ShadesArgs.txt", ContextHelper.getContext());
    private static final String FRAG_SHADER_COLOR = BaseFilter.loadShader("ShadesMain.txt", ContextHelper.getContext());

    public RGBAnimatedFilter() {
        super();
        mFilterMode.add(FilterManager.RGB_ANIMATED_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_COLOR;
        registerShader();
        setUpAnimator();
    }

    private void setUpAnimator() {
        float fromValue = 0.5f;
        float toValue = 1.0f;
        int duration = 1000;

        // Red animator to change mRedFactor
        redAnimator = ValueAnimator.ofFloat(fromValue, toValue);
        redAnimator.setDuration(duration);
        redAnimator.setRepeatMode(ValueAnimator.REVERSE);
        redAnimator.setRepeatCount(ValueAnimator.INFINITE);
        redAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRedFactor = (float) animation.getAnimatedValue();
            }
        });

        // Green animator to change mGreenFactor
        greenAnimator = ValueAnimator.ofFloat(fromValue, toValue);
        greenAnimator.setDuration(duration);
        greenAnimator.setStartDelay(duration / 2);
        greenAnimator.setRepeatMode(ValueAnimator.REVERSE);
        greenAnimator.setRepeatCount(ValueAnimator.INFINITE);
        greenAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mGreenFactor = (float) animation.getAnimatedValue();
            }
        });

        // Blue animator to change mBlueFactor
        blueAnimator = ValueAnimator.ofFloat(fromValue, toValue);
        blueAnimator.setDuration(duration);
        blueAnimator.setStartDelay(duration);
        blueAnimator.setRepeatMode(ValueAnimator.REVERSE);
        blueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBlueFactor = (float) animation.getAnimatedValue();
//                setFloat("blueFactor", (Float) animation.getAnimatedValue());
            }
        });
    }

    @Override
    public void postSetup() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                redAnimator.start();
                blueAnimator.start();
                greenAnimator.start();
            }
        });
    }

    @Override
    void copy(BaseFilter baseFilter) {
        super.copy(baseFilter);
        setUpAnimator();
    }

    @Override
    public void exportVariableValues(Program program) {
        if (AVUtils.VERBOSE) Log.d(TAG, String.format(Locale.getDefault(), "RGB(r=%f,g=%f,b=%f)", mRedFactor,
                mGreenFactor, mBlueFactor));
        program.uniform1f("redFactor", mRedFactor);
        program.uniform1f("greenFactor", mGreenFactor);
        program.uniform1f("blueFactor", mBlueFactor);
    }
}
