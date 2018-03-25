package com.roposo.creation.graphics.filters;


import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by Tanvi on 09/12/17.
 */

public class BlackWhiteNoiseFilter extends TimeFilterFromScene {
    private static final String FRAG_SHADER_ARGS_STRING = "vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }\n" +
            "\n" +
            "float snoise(vec2 v){\n" +
            "  const vec4 C = vec4(0.211324865405187, 0.366025403784439,\n" +
            "           -0.577350269189626, 0.024390243902439);\n" +
            "  vec2 i  = floor(v + dot(v, C.yy) );\n" +
            "  vec2 x0 = v -   i + dot(i, C.xx);\n" +
            "  vec2 i1;\n" +
            "  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +
            "  vec4 x12 = x0.xyxy + C.xxzz;\n" +
            "  x12.xy -= i1;\n" +
            "  i = mod(i, 289.0);\n" +
            "  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))\n" +
            "  + i.x + vec3(0.0, i1.x, 1.0 ));\n" +
            "  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),\n" +
            "    dot(x12.zw,x12.zw)), 0.0);\n" +
            "  m = m*m ;\n" +
            "  m = m*m ;\n" +
            "  vec3 x = 2.0 * fract(p * C.www) - 1.0;\n" +
            "  vec3 h = abs(x) - 0.5;\n" +
            "  vec3 ox = floor(x + 0.5);\n" +
            "  vec3 a0 = x - ox;\n" +
            "  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );\n" +
            "  vec3 g;\n" +
            "  g.x  = a0.x  * x0.x  + h.x  * x0.y;\n" +
            "  g.yz = a0.yz * x12.xz + h.yz * x12.yw;\n" +
            "  return 130.0 * dot(m, g);\n" +
            "}";

    private static final String FRAG_SHADER_STRING = "vec2 texCoord = outTexCoords.xy;\n" +
            "\n" +
            "\tvec3 noise = vec3(snoise(texCoord));\n" +
            "  \n" +
            "\tvec3 col = texture2D(baseSampler, texCoord).rgb;\n" +
            "  \n" +
            "\tcol = col+noise;\n" +
            "   \n" +
            "\tfragColor =  vec4(col,1.0);";

    public BlackWhiteNoiseFilter() {
        super();
        mFilterMode.add(FilterManager.BLACK_WHITE_NOISE_FILTER);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARGS_STRING);
        registerShader();
    }

}
