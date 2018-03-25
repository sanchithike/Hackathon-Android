package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * This will create a time animated zoom out effect
 * Created by akshaychauhan on 12/4/17.
 */

public class ZoomOutFilter extends TimeFilterFromScene {

    private static final String FRAG_SHADER_ARG = "uniform float timeFactor;\n";

    private static final String FRAG_SHADER_STRING = "" +
            " vec2 inputCoords =  outTexCoords.xy;\n" +
            " vec4 outputPixels = vec4(0.0);\n"+
            " float zoomFactor = 0.5 + 0.5 * (sin(timeFactor));\n" +
            " vec2 scaleCenter = vec2(0.5);\n" +
            // Translate and scale down the coordinate values
            " inputCoords = ((inputCoords - scaleCenter)/zoomFactor) + scaleCenter;\n" +
            // outTexCoords is the coordinate we are going to assign the pixel to ,
            // we fetch the pixel coordinate from the sampler's inputCoords coordinate
            // Value of inputCoords is changed according to zoomFactor
            " outputPixels = texture2D(baseSampler, vec2(inputCoords.x, inputCoords.y));\n" +
            " vec2 leftBottom = step(vec2(0.001), inputCoords);\n" +
            " vec3 colorMultiplier = vec3(leftBottom.x * leftBottom.y);\n" +
            " vec2 topRight = step(vec2(0.001), 1.0 - inputCoords);\n" +
            " colorMultiplier *=  vec3(topRight.x * topRight.y);\n" +
            " fragColor =  outputPixels * vec4(colorMultiplier, 1.0);\n";

    public ZoomOutFilter() {
        // Taking value of angle from 90 to 240 degree as sine curve will decrease during that period
        // resulting in : decrease in zoom factor and increase in placement coordinates (in shader code)
        // This will eventually lead to a zoom out effect
        super(1.57f, 4.71f, 2000, TimeAnimate.REVERSE); //90 degree to 240 degree in 2000 milli-seconds
        mFilterMode.add(FilterManager.ZOOM_OUT_FILTER);
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        registerShader();
    }
}
