package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This will add red color overlay to the image/video
 * Created by akshaychauhan on 12/1/17.
 */

public class RedColorOverlayFilter extends ColorOverlayFilter {
    private static volatile float[] colorMultiplier = {0.9f, 0.25f, 0.35f, 0.5f};

    public RedColorOverlayFilter() {
        super(FilterManager.RED_COLOR_OVERLAY_FILTER, colorMultiplier);
    }

}
