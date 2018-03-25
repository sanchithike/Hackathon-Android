package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by akshaychauhan on 2/5/18.
 */

public class HexaFilter extends ImageFilter {

    private static final String FRAG_SHADER_ARG = "#define H 0.258\n" +
            "#define S ( H/sqrt(3.))\n" +
            "\n" +
            "vec2 hexCoord(vec2 hexIndex, vec2 uv) {\n" +
            "\tfloat i = hexIndex.x;\n" +
            "\tfloat j = hexIndex.y;\t\n" +
            "\tvec2 r;\n" +
            "\n" +
            "\tr.x = (i) * S;\n" +
            "\tr.y = ((j) * H + (mod(i,2.0)) * H/2.);\n" +
            "\t    //if (i!=2.0 || j!=2.0) return r;\n" +
            "    return ((uv-r)/vec2(1.5*S,H))   ;\n" +
            "}\n" +
            "\n" +
            "vec2 hexIndex(vec2 coord) {\n" +
            "\tvec2 r;\n" +
            "\tfloat x = coord.x;\n" +
            "\tfloat y = coord.y;\n" +
            "\tfloat it = float(floor(x/S));\n" +
            "\tfloat yts = y - (mod(it,2.0)) * H/2.;\n" +
            "\tfloat jt = float(floor((1./H) * yts));\n" +
            "\tfloat xt = x - it * S;\n" +
            "\tfloat yt = yts - jt * H;\n" +
            "\tfloat deltaj = (yt > H/2.)? 1.0:0.0;\n" +
            "\tfloat fcond = S * (2./3.) * abs(0.5 - yt/H);\n" +
            "\n" +
            "\tif (xt > fcond) {\n" +
            "\t\tr.x = it;\n" +
            "\t\tr.y = jt;\n" +
            "\t}\n" +
            "\telse {\n" +
            "\t\tr.x = it - 1.0;\n" +
            "\t\tr.y = jt - (mod(r.x,2.0)) + deltaj;\n" +
            "\t}\n" +
            "\n" +
            "\treturn r;\n" +
            "}";

    private static final String FRAG_SHADER_STRING = "vec2 uv = outTexCoords.xy;\n" +
                    "\tvec2 hexIx = hexIndex(uv);\n" +
                    "\tvec2 hexXy = hexCoord(hexIx, uv);\n" +
                    "\tvec4 fcol = texture2D(baseSampler, hexXy);\n" +
                    "\tfragColor = fcol;";

    public HexaFilter() {
        super();
        mFilterMode.add(FilterManager.HEXA_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }

}
