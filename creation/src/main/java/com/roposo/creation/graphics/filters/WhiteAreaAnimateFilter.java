package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * Reference : https://sensing.konicaminolta.us/blog/identifying-color-differences-using-l-a-b-or-l-c-h-coordinates/
 * Created by akshaychauhan on 1/22/18.
 */

public class WhiteAreaAnimateFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n"+
            ProgramCache.gFS_Header_Func_CIE_LAB;

    private static final String FRAG_SHADER_STRING = "" +
            "vec4 imgColorInCie = cieLAB(fragColor);\n" +
            "vec4 whiteColorInCie = cieLAB(vec4(1.0));\n" +
            "float deltaL = imgColorInCie.x - whiteColorInCie.x;\n" +
            "float deltaA = imgColorInCie.y - whiteColorInCie.y;\n" +
            "float deltaB = imgColorInCie.z - whiteColorInCie.z;\n" +
            "float colorDifference = ceil(abs(deltaL * 2.0) + abs(deltaA * 2.0) + abs(deltaB * 2.0))/2.0;\n" +
            "if (colorDifference < 30.0) {"+
            "   float timeFactor = fract(timeFactor);\n" +
            "   vec3 tcolorMultiplier;\n" +
            "   if (timeFactor <= 0.33) {\n" +
            "       tcolorMultiplier = vec3(1.0, 0.0, 0.0);\n" +
            "   }\n"+
            "   else if (timeFactor <= 0.66) {\n" +
            "       tcolorMultiplier = vec3(0.0, 1.0, 0.0);\n" +
            "   }\n"+
            "   else {\n" +
            "       tcolorMultiplier = vec3(0.0, 0.0, 1.0);\n" +
            "   }\n"+
            "   fragColor.rgb = tcolorMultiplier;\n"+
            "}\n"
            ;

    public WhiteAreaAnimateFilter() {
        super(0.0f, 3.14f, 1000);
        mFilterMode.add(FilterManager.WHITE_AREA_ANIMATE_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
