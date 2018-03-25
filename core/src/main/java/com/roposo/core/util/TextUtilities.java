package com.roposo.core.util;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * Created by bajaj on 27/07/17.
 */

public class TextUtilities {

    public static Rect measureTextSingleLine(String text, int textSize) {
        return measureTextSingleLine(text, textSize, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), Paint.Align.LEFT);
    }

    public static Rect measureTextSingleLine(String text, int textSize, Typeface typeface, Paint.Align alignment) {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        paint.setTextAlign(alignment);
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }
}
