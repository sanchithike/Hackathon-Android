package com.roposo.creation.graphics.filters;

import com.roposo.creation.R;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 12/18/17.
 */

public class TriColorFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n" +
            "#define BlendColorDodgef(base, blend) \t((blend == 1.0) ? blend : min(base / (1.0 - blend), 1.0))\n" +
            "#define BlendColorBurnf(base, blend) \t((blend == 0.0) ? blend : max((1.0 - ((1.0 - base) / blend)), 0.0))\n" +
            "#define BlendVividLightf(base, blend) \t((blend < 0.5) ? BlendColorBurnf(base, (2.0 * blend)) : BlendColorDodgef(base, (2.0 * (blend - 0.5))))\n" +
            "#define Blend(base, blend, funcf) \t\tvec3(funcf(base.r, blend.r), funcf(base.g, blend.g), funcf(base.b, blend.b))\n" +
            "#define BlendVividLight(base, blend) \tBlend(base, blend, BlendVividLightf)\n";

    private static final String FRAG_SHADER_STRING = "vec3 SAFFRON = vec3(221., 79., 18.) / 255.;\n" +
            "vec3 GREEN = vec3(21., 181., 25.) / 255.;\n" +
            "\n" +
            "float bottom = step(0.5 * timeFactor, originalTexCoords.y);\n" +
            "\n" +
            "if (bottom != 0.0 && originalTexCoords.y <= 0.5) { \n" +
            "\tif (originalTexCoords.y < 0.35) { \n" +
            "\t\tfragColor.rgb *= BlendVividLight(fragColor.rgb, GREEN);\n" +
            "\t}\n" +
            "}\n" +
            "\n" +
            "float top = step(0.5 * timeFactor, 1.0 - originalTexCoords.y);\n" +
            "\n" +
            "if (top != 0.0 && originalTexCoords.y > 0.5) {\n" +
            "\tif (originalTexCoords.y >= 0.65) {\n" +
            "\t\tfragColor.rgb *= BlendVividLight(fragColor.rgb, SAFFRON);\n" +
            "\t}\n" +
            "}\n"
            ;

    public TriColorFilter() {
        super(0, 1, 6666, TimeAnimate.REVERSE, R.raw.tri_color);
        mFilterMode.add(FilterManager.TRICOLOR_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }


}
