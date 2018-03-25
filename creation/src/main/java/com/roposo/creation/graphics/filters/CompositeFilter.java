package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Program;

/**
 * Created by tajveer on 11/28/17.
 */

public class CompositeFilter extends ImageFilter {

    private ImageFilter[] filters;

    public ImageFilter[] getFilters() {
        return filters;
    }

    public CompositeFilter(ImageFilter... filters) {
        super();
        this.filters = filters;
        for (ImageFilter filter : filters) {
            mFilterMode.addAll(filter.mFilterMode);
            VERT_SHADER_ARGS += filter.VERT_SHADER_ARGS;
            VERT_SHADER_MAIN += filter.VERT_SHADER_MAIN;
            FRAG_SHADER_MAIN += filter.FRAG_SHADER_MAIN;
            FRAG_SHADER_ARGS.addAll(filter.FRAG_SHADER_ARGS);
        }
        registerShader();
    }

    @Override
    void copy(BaseFilter baseFilter) {
        super.copy(baseFilter);

        CompositeFilter filter = (CompositeFilter) baseFilter;
        filters = filter.filters; // do not need a deep copy here
    }

    @Override
    public void postSetup() {
        for (ImageFilter filter : filters) {
            filter.postSetup();
        }
    }

    @Override
    public void setTimestamp(long timestamp) {
        for(ImageFilter filter : filters) {
            filter.setTimestamp(timestamp);
        }
    }

    @Override
    public void exportVariableValues(Program program) {
        for (ImageFilter filter : filters) {
            filter.exportVariableValues(program);
        }
    }

}
