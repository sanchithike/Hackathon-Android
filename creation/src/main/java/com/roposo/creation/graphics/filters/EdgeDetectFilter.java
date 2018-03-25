package com.roposo.creation.graphics.filters;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 22/11/17.
 */

public class EdgeDetectFilter extends ImageFilter {
//    private static final String FRAG_SHADER_ARGS_STRING = BaseFilter.loadShader();
    private static final String FRAG_SHADER_STRING = "vec4 pixel = fragColor;\n" +
        "    float y = (pixel.r + 16.0) / 116.0;\n" +
        "    float x = y + (pixel.g/500.0);\n" +
        "    float z = y - (pixel.b/200.0);\n" +
        "\n" +
        "    // D65 standard referent\n" +
        "    float X = 0.950470, Y = 1.0, Z = 1.088830;\n" +
        "\n" +
        "    x = X * (x > 0.206893034 ? x*x*x : (x - 4.0/29.0) / 7.787037);\n" +
        "    y = Y * (y > 0.206893034 ? y*y*y : (y - 4.0/29.0) / 7.787037);\n" +
        "    z = Z * (z > 0.206893034 ? z*z*z : (z - 4.0/29.0) / 7.787037);\n" +
        "\n" +
        "    // second, map CIE XYZ to sRGB\n" +
        "    float r =  3.2404542*x - 1.5371385*y - 0.4985314*z;\n" +
        "    float g = -0.9692660*x + 1.8760108*y + 0.0415560*z;\n" +
        "    float b =  0.0556434*x - 0.2040259*y + 1.0572252*z;\n" +
        "\n" +
        "    r = (r <= 0.00304) ? 12.92*r : 1.055 * pow(r,1.0/2.4) - 0.055;\n" +
        "    g = (g <= 0.00304) ? 12.92*g : 1.055 * pow(g,1.0/2.4) - 0.055;\n" +
        "    b = (b <= 0.00304) ? 12.92*b : 1.055 * pow(b,1.0/2.4) - 0.055;\n" +
        "\n" +
        "    // third, get sRGB values\n" +
        "    float ir = r; ir = max(0.0, min(ir, 1.0));\n" +
        "    float ig = g; ig = max(0.0, min(ig, 1.0));\n" +
        "    float ib = b; ib = max(0.0, min(ib, 1.0));\n" +
        "\n" +
        "    fragColor = vec4(ir, ig, ib, 1.0);";

    public EdgeDetectFilter() {
        super();
        mFilterMode.add(FilterManager.GRAINY_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
//        FRAG_SHADER_ARGS += FRAG_SHADER_ARGS_STRING;
        registerShader();
    }

    /*
    @Override
    public boolean onDraw() {
        if (!isInitialized()) return false;
//        uniform1f("alphax", mAlphaX);
//        uniform1f("alphay", mAlphaY);
        uniform1f("strength", 3.0f);
        uniform1i("KERNEL_SIZE", 1);
        return super.onDraw();
    }
    */
}
