package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 17/01/18.
 */
public class CreateImageTestingFilter extends ImageFilter {
    private static final String MAIN = "float a = outTexCoords.x;\n" +
            "\tfloat b = outTexCoords.y;\n" +
            "\n" +
            /*"\tif (a > (texelSize.x * 10.0 + texelSize.x / 2.0) && b > (texelSize.y * 10.0 + texelSize.y / 2.0) && a < (texelSize.x * 14.0 + texelSize.x / 2.0) && b < (texelSize.y * 14.0 + texelSize.y / 2.0)) {\n" +
            "\t\tfragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "\t}\n" +*/
            "\tif (a > (texelSize.x * 2.0 + texelSize.x / 2.0) && b > (texelSize.y * 2.0 + texelSize.y / 2.0) && a < (texelSize.x * 5.0 + texelSize.x / 2.0) && b < (texelSize.y * 5.0 + texelSize.y / 2.0)) {\n" +
            "\t\tfragColor = vec4(0.01, 0.0, 0.0, 1.0);\n" +
            "\t}\n" +
            "\telse {\n" +
            "\t\tfragColor = vec4(vec3(0.0, 0.0, 1.0), 1.0);\n" +
            "\t}";

    public CreateImageTestingFilter() {
        super();
        mFilterMode.add(FilterManager.CREATE_IMAGE_FILTER);
        FRAG_SHADER_MAIN += MAIN;
        registerShader();
    }

}
