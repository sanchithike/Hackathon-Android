package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 12/02/18.
 */

public class SpotlightFilter extends ImageFilter{
    private static final String ARGS = "uniform float timeFactor;";
    private static final String MAIN = "vec2 light = vec2(.5, (cos(2.0 * timeFactor)));\n" +
            "    \n" +
            "vec3 finalColor = vec3(.8,.8,.0) * pow(max(dot(normalize(light),normalize(outTexCoords - vec2(0., .5))),0.9),90.0);\n" +
            "vec3 bg = fragColor.xyz;\n" +
            "\n" +
            "fragColor = vec4(bg + finalColor.xyz,1.);";

    public SpotlightFilter() {
        super();
        mFilterMode.add(FilterManager.RGBOFFSET_FILTER);
        FRAG_SHADER_ARGS.add(ARGS);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        program.uniform1f("timeFactor", mTimestamp/1000.0f);
    }
}
