package com.roposo.creation.graphics.filters;


import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 09/01/18.
 */

public class PointGatheringFilter extends ImageFilter {
    public volatile int mNumberOfPoints;
    private static final String ARGS = BaseFilter.loadShader("PointGatheringArgs.txt", ContextHelper.getContext());
    private static final String MAIN = BaseFilter.loadShader("PointGatheringMain.txt", ContextHelper.getContext());

    public PointGatheringFilter() {
        super();
        mFilterMode.add(FilterManager.POINT_GATHERING_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        FRAG_SHADER_ARGS.add(ARGS);
        registerShader();
    }

}
