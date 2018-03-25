package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 08/01/18.
 */

public class HistoPyramidCalculatorFilter extends ImageFilter {

    /*private static final String MAIN = "float pointX = outTexCoords.x * 2.0;\n" +
            "\tfloat pointY = outTexCoords.y * 2.0;\n" +
            "\tvec4 color1 = texture2D(baseSampler, vec2(pointX, pointY));\n" +
            "\tvec4 color2 = texture2D(baseSampler, vec2(pointX + 0.5 * texelSize.x, pointY));\n" +
            "\tvec4 color3 = texture2D(baseSampler, vec2(pointX, pointY + 0.5 * texelSize.y));\n" +
            "\tvec4 color4 = texture2D(baseSampler, vec2(pointX + 0.5 * texelSize.x, pointY + 0.5 * texelSize.y));\n" +
            "\n" +
            "\t//cannot store the exact value - so we have to store the avg\n" +
            "\tfragColor = vec4( vec3((color1.x + color2.x + color3.x + color4.x)/4.0), 1.0);\n";*/

    public HistoPyramidCalculatorFilter() {
        super();
        mFilterMode.add(FilterManager.HISTOPYRAMID_CALCULATOR_FILTER);
        registerShader();
    }
}
