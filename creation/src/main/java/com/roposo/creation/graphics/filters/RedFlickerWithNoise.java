package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This will add following effects :
 * - Moving red color overlay
 * - Noise
 * - Flicker
 * Created by akshaychauhan on 12/21/17.
 */

public class RedFlickerWithNoise extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "\n"
            + "# define part 0.10 \n"
            + "uniform float timeFactor;\n"
            + "float random(vec2 st) {\n"
            + "   return fract(sin(dot(st.xy, vec2(tan(timeFactor), 43758.0 * tan(timeFactor))))); \n"
            + "}\n"
            ;

    private static final String FRAG_SHADER_STRING = ""+
            "    float t = fract(timeFactor);\n" +
            "    fragColor = texture2D(baseSampler, outTexCoords);\n" +
            "    vec3 redColor = vec3(0.75, 0.0, 0.0);\n" +
            "    float dist;\n" +
            "    if (t <= 2.0 * part || (t > 6.0 * part && t <= 8.0 * part)) {\n" +
            "        dist = distance(outTexCoords - sin(timeFactor), vec2(0.5));\n" +
            "        fragColor *= vec4(vec3((1.0 - vec3(dist)) * redColor),  1.0 + abs(tan(timeFactor)));\n" +
            "    }\n" +
            "    else if (t > 2.0 * part && t <= 4.0 * part) {\n" +
            "        fragColor.rgb *= max(fragColor.rgb, vec3(random(outTexCoords.xy)));\n" +
            "    }\n" +
            "    else if (t > 4.0 * part && t <= 5.0 * part) {\n" +
            "        fragColor.rgb = (1.0 - ((1.0 - fragColor.rgb) * (1.0 - redColor)));"+
            "    }\n"
            ;

    public RedFlickerWithNoise() {
        super(0.0f, 6.28f, 6000);
        mFilterMode.add(FilterManager.RED_FLICKER_WITH_NOISE_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
