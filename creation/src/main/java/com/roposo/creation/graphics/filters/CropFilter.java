package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Program;

/**
 * @author muddassir on 11/7/17.
 */

public class CropFilter extends ImageFilter {
    private static final String gFS_Main_ColorFilter_Mirror_Args =
              "uniform vec2 minTexCoord;\n"
            + "uniform vec2 maxTexCoord;\n";


    public CropFilter() {
        super();
        FRAG_SHADER_ARGS.add(gFS_Main_ColorFilter_Mirror_Args);
    }

    @Override
    void copy(BaseFilter baseFilter) {
        super.copy(baseFilter);
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
/*        program.uniform2fv("minTexCoord", mMinTexCoord);
        program.uniform2fv("maxTexCoord", mMaxTexCoord);*/
    }

/*    @Override
    public void setup(Caches caches) {
        super.setup(caches);
        setScaleFactors(mWidthScale, mHeightScale);
    }

    @Override
    public void setScaleFactors(float widthScale, float heightScale) {
        super.setScaleFactors(widthScale, heightScale);
        mMinTexCoord[0] = 0f;
        mMinTexCoord[1] = (1f - mHeightScale) / 2f;

        mMaxTexCoord[0] = 1f;
        mMaxTexCoord[1] = (1f + mHeightScale) / 2f;
    }*/
}
