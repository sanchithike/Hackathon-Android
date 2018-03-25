package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 14/12/17.
 * Blurs around a given center
 */

public class RadialBlurFilter extends ImageFilter{
    private static final String ARGS_STRING = "vec3 deform(vec2 p)\n" +
            "{\n" +
            "    vec2 q = sin(vec2(1.1,1.2) + p );\n" +
            "\n" +
            "    float a = atan(q.y, q.x);\n" +
            "    float r = sqrt(dot(q,q));\n" +
            "\n" +
            "    vec2 uv = p*sqrt(1.0+r*r);\n" +
            "    uv += sin( vec2(0.0,0.6) + vec2(1.0,1.1));\n" +
            "         \n" +
            "    return texture2D( baseSampler, uv*0.3).xyz;\n" +
            "}\n";

    private static final String SHADER_STRING = "vec2 p = -1.0 + 2.0*outTexCoords;\n" +
            "\n" +
            "vec3  col = vec3(0.0);\n" +
            "vec2  distance = (vec2(0.0,0.0)-p)/32.0;\n" +
            "float w = 1.0;\n" +
            "vec2  s = p;\n" +
            "for( int i=0; i<8; i++ )\n" +
            "{\n" +
            "    vec3 res = deform( s );\n" +
            "    col += w*smoothstep( 0.0, 1.0, res );\n" +
            "    w *= .99;\n" +
            "    s += distance;\n" +
            "}\n" +
            "col = col * 2.5 / 8.0;\n" +
            "\n" +
            "fragColor = vec4( col, 1.0 );";

    public RadialBlurFilter() {
        super();
        mFilterMode.add(FilterManager.RADIAL_BLUR_FILTER);
        FRAG_SHADER_MAIN += SHADER_STRING;
        FRAG_SHADER_ARGS.add(ARGS_STRING);
        registerShader();
    }

}
