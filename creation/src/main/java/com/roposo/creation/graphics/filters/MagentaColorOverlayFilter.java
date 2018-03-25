package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 1/18/18.
 */

public class MagentaColorOverlayFilter extends ColorOverlayFilter {
    private static volatile float[] colorMultiplier = {0.5f, 0.25f, 0.58f, 0.5f};

    public MagentaColorOverlayFilter() {
        super(FilterManager.MAGENTA_COLOR_OVERLAY, colorMultiplier);
    }
}
