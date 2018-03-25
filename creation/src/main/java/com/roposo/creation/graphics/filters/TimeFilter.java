package com.roposo.creation.graphics.filters;

import android.animation.ValueAnimator;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 29/11/17.
 */

public class TimeFilter extends ImageFilter{
    protected ValueAnimator timeAnimator;
    protected volatile float mTimeFactor = 1f;
    private final int duration;
    private final float fromValue;
    private final float toValue;
    private int repeatMode;
    private int repeatCount;

    public TimeFilter() {
        this(0.5f, 1.0f, 500);
    }

    public TimeFilter(float fromValue, float toValue, int duration) {
        this(fromValue, toValue, duration, ValueAnimator.REVERSE, ValueAnimator.INFINITE);
    }

    public TimeFilter(float fromValue, float toValue, int duration, int repeatMode, int repeatCount) {
        super();
        this.duration = duration;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.repeatMode = repeatMode;
        this.repeatCount = repeatCount;
        setUpAnimator();
    }

    protected void setUpAnimator() {
        //Time Animator that increases with time
        timeAnimator = ValueAnimator.ofFloat(fromValue, toValue);
        timeAnimator.setDuration(duration);
        timeAnimator.setRepeatMode(repeatMode);
        timeAnimator.setRepeatCount(repeatCount);
        timeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTimeFactor = (float) animation.getAnimatedValue();
            }
        });
    }

    @Override
    public void postSetup() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                timeAnimator.start();
            }
        });
        setUpAnimator();
    }

    @Override
    public void exportVariableValues(Program program) {
        program.uniform1f("timeFactor", mTimeFactor);
    }
}
