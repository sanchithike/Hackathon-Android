package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.Program;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshaychauhan on 1/23/18.
 */

public class TimeFilterFromScene extends ImageFilter {
    private float mTimeFactor = 1f;
    private final int duration;
    private final float fromValue;
    private final float toValue;
    private final boolean dontCompute;
    private TimeAnimate repeatMode;
    private List<Float> animatedValues = null;
    private static boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    private void setmTimeFactor(float mTimeFactor) {
        this.mTimeFactor = mTimeFactor;
    }

    public int getDuration() {
        return duration;
    }

    public enum TimeAnimate {
        RESTART,
        REVERSE
    }

    TimeFilterFromScene() {
        this(0.0f, 1.0f, 500, TimeAnimate.RESTART);
    }

    /**
     * @param dontCompute Set this true to set timeFactor as the timestamp
     */
    TimeFilterFromScene(boolean dontCompute) {
        this(0.0f, 1.0f, 500, TimeAnimate.RESTART, null, dontCompute);
    }

    TimeFilterFromScene(float fromValue, float toValue, int duration) {
        this(fromValue, toValue, duration, TimeAnimate.RESTART);
    }

    TimeFilterFromScene(float fromValue, float toValue, int duration, TimeAnimate timeAnimate) {
        this(fromValue, toValue, duration, timeAnimate, null);
    }

    TimeFilterFromScene(float fromValue, float toValue, int duration, TimeAnimate timeAnimate, Integer resourceId) {
        this(fromValue, toValue, duration, timeAnimate, resourceId, false);
    }

    TimeFilterFromScene(float fromValue, float toValue, int duration, TimeAnimate timeAnimate, Integer resourceId, boolean dontCompute) {
        super();
        this.duration = duration;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.repeatMode = timeAnimate;
        this.resourceId = resourceId;
        this.dontCompute = dontCompute;
    }

    @Override
    void generateVariableValues() {
        if (dontCompute) {
            mTimeFactor = mTimestamp;
        }
        else {
            if (resourceId != null) {
                int timeDuration = getDuration();
                float usableTimeValue = readTimeValueFromCSV(resourceId, mTimestamp, timeDuration);
                if (usableTimeValue != -1.0f) {
                    setmTimeFactor(usableTimeValue);
                } else {
                    Log.e(TAG, "Missing correct values for curtain slider effect");
                }
            } else {
                int timeDuration;
                float animatedTimeValue;
                if (this.repeatMode.equals(TimeAnimate.REVERSE)) {
                    timeDuration = duration * 2;
                } else {
                    timeDuration = duration;
                }
                animatedTimeValue = Math.abs((Math.abs(duration - (mTimestamp % (timeDuration))) - duration));
                mTimeFactor = fromValue + ((toValue - fromValue) * (animatedTimeValue / duration));
            }
        }
    }

    private float readTimeValueFromCSV(Integer resourceId, long timeStamp, int timeDuration) {
        if (animatedValues == null) {
            InputStream inputStream = ContextHelper.getContext().getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            try {
                line = reader.readLine();
                if (line != null) {
                    animatedValues = new ArrayList<>();
                    String[] timeValues = line.split(",");
                    //Since for loop is expected to be faster than for-each in android using it instead of latter
                    for (int i = 0; i < timeValues.length; i++) {
                        animatedValues.add(Float.parseFloat(timeValues[i]));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        float requiredValue = -1.0f;

        if (animatedValues != null && animatedValues.size() > 0) {
            double usableIndex = (((double) timeStamp / timeDuration) * animatedValues.size()) % animatedValues.size();
            if (usableIndex < 0) {
                usableIndex = 0;
                Log.e(TAG, "Receiving negative timestamp " + timeStamp);
            }

            requiredValue = getIndexValueFromAnimatedTime(usableIndex);
            if (VERBOSE) {
                Log.d(TAG, "Usable t - " + requiredValue + " and index value " + usableIndex % timeDuration);
            }
        }
        return requiredValue;
    }

    private float getIndexValueFromAnimatedTime(double usableIndex) {
        double decimalPart = usableIndex - (int) usableIndex;

        //Interpolating between two values
        int maxIndex = (int) Math.ceil(usableIndex);
        int minIndex = (int) Math.floor(usableIndex);

        float requiredValue;
        if (maxIndex < animatedValues.size()) {
            requiredValue = (float) ((decimalPart) * animatedValues.get(maxIndex) +
                    (1.0 - decimalPart) * animatedValues.get(minIndex));
            if (VERBOSE) {
                Log.d(TAG, "Usable Index : " + usableIndex + ", Decimal Part " + decimalPart + ", Required Val " + requiredValue +
                        " and normal " + animatedValues.get((int) usableIndex));
            }
        } else {
            requiredValue = animatedValues.get(Math.min(animatedValues.size() - 1, (int) usableIndex));
        }
        return requiredValue;
    }

    @Override
    public void exportVariableValues(Program program) {
        program.uniform1f("timeFactor", mTimeFactor);
    }
}
