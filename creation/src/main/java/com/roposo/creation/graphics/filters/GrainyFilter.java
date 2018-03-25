package com.roposo.creation.graphics.filters;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 27/11/17.
 * Creates an image like this -
 * https://realbusiness.co.uk/wp-content/uploads/2015/01/media.caspianmedia.comimage2350a25c9394327464158eb91d245542-9c21fe7dd04426762982e38386f9de780acd1b49.jpg
 *
 * TODO
 * Size of the noise can be adjusted
 */

public class GrainyFilter extends TimeFilterFromScene {
    private static final String ARGS = BaseFilter.loadShader("FilmGrainArgs.txt", ContextHelper.getContext());
    private static final String MAIN = BaseFilter.loadShader("FilmGrainMain.txt", ContextHelper.getContext());

    public GrainyFilter() {
        super();
        FRAG_SHADER_ARGS.add(ARGS);
        mFilterMode.add(FilterManager.GRAINY_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }
}
