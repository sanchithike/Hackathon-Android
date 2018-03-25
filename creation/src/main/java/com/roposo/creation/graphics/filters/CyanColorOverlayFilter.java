package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/18/18.
 */

public class CyanColorOverlayFilter extends ColorOverlayFilter {
    private static volatile float[] colorMultiplier = {0.0f, 1.0f, 1.0f, 1.0f};

    public CyanColorOverlayFilter() {
        super(FilterManager.CYAN_COLOR_OVERLAY, colorMultiplier);
    }
}
