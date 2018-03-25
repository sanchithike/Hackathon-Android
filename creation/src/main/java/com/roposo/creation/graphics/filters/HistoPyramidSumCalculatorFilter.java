package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 15/01/18.
 */

public class HistoPyramidSumCalculatorFilter extends ImageFilter {
    /*private static final String MAIN =
        "float pointX = outTexCoords.x;\n" +
        "float pointY = outTexCoords.y;\n" +
        "\n" +
        "vec4 color1 = texture2D(baseSampler, vec2(pointX - texelSize.x * 0.5, pointY - texelSize.y * 0.5));\n" +
        "vec4 color2 = texture2D(baseSampler, vec2(pointX - texelSize.x * 0.5, pointY + texelSize.y * 0.5));\n" +
        "vec4 color3 = texture2D(baseSampler, vec2(pointX + texelSize.x * 0.5, pointY - texelSize.y * 0.5));\n" +
        "vec4 color4 = texture2D(baseSampler, vec2(pointX + texelSize.x * 0.5, pointY + texelSize.y * 0.5));\n" +
        "\n" +
        "fragColor = vec4(color1.x + color2.x + color3.x + color4.x, fragColor.y, fragColor.z, 1.0);\n";
        */

    private static final String MAIN = "fragColor = vec4(fragColor.x * 4.0, fragColor.y, fragColor.z, 1.0);\n";

    public HistoPyramidSumCalculatorFilter() {
        super();
        mFilterMode.add(FilterManager.HISTOPYRAMIDSUM_CALCULATOR_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
        
    }
}
