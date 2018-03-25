package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/18/18.
 */

public class YellowColorOverlayFilter extends ColorOverlayFilter {
    private static volatile float[] colorMultiplier = {0.9f, 0.68f, 0.16f, 0.5f};

    public YellowColorOverlayFilter() {
        super(FilterManager.YELLOW_COLOR_OVERLAY, colorMultiplier);
    }
}
