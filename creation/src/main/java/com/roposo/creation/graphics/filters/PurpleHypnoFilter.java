package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 20/02/18.
 */

public class PurpleHypnoFilter extends HypnoFilter {
    private static volatile float[] color1 = {0.894f, 0.2196f, 0.3961f};
    private static volatile float[] color2 = {0.1137f, 0.03137f, 0.2706f};

    public PurpleHypnoFilter() {
        super(FilterManager.PURPLE_HYPNO_FILTER, color1, color2);
    }
}
