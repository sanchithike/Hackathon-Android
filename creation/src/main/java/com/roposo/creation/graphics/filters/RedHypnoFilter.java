package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 20/02/18.
 */

public class RedHypnoFilter extends HypnoFilter {
    private static volatile float[] color1 = {0.9294f, 0.2392f, 0.0f};
    private static volatile float[] color2 = {0.19215f, 0.02745f, 0.258823f};

    public RedHypnoFilter() {
        super(FilterManager.RED_HYPNO_FILTER, color1, color2);
    }
}
