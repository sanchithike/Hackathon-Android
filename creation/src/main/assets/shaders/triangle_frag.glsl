#extension GL_OES_EGL_image_external : require
precision highp float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform samplerExternalOES baseSampler;
varying vec2 outPosition;
uniform float angleZero;
uniform float angleOne;
uniform float angleTwo;
uniform float angleThree;
uniform float blurRadius;

void main(void) {
    highp vec4 fragColor;
    fragColor = vec4(1.0); //texture2D(baseSampler, outTexCoordinates);
    float x = (outPosition.x + 1.0) / 2.0;
    float y = (1.0 - outPosition.y) / 2.0;

    float slopeZero = tan(angleZero);
    float slopeOne = tan(angleOne);
    float slopeTwo = tan(angleTwo);
    float slopeThree = tan(angleThree);

    float c0 = y - x * slopeZero;
    float c1 = y - x * slopeOne;
    float c2 = y - x * slopeTwo;
    float c3 = y - x * slopeThree;

    float slope= tan(y / x);

    if (c0 < 0.0 && c1 > 0.0) { // Blue
        float cleft = tan(angleZero + blurRadius);
        float cmiddle = tan(angleZero - blurRadius);
        float cright = tan(angleOne + blurRadius);
        vec4 colorLeft = blendFactor * fragColor; // frag
        vec4 colorMiddle = blendFactor * vec4(fragColor.r * 0.03, fragColor.g * 1.0, fragColor.b * 0.92, fragColor.a); //
        vec4 colorRight = blendFactor * vec4(fragColor.r * 1.0, fragColor.g * 0.92, fragColor.b * 0.03, fragColor.a);
        float mixFactorLeft = smoothstep(cleft, cmiddle, slope);
        float mixFactorRight = smoothstep(cright, c1, slope);
        if (slope <= cleft && slope >= cmiddle) {
            fragColor = mix(colorLeft, colorMiddle, mixFactorLeft);
        } else if (slope <= cright && slope >= c1) {
            fragColor = mix(colorMiddle, colorRight, mixFactorRight);
        } else {
            fragColor = colorMiddle;
        }
    } else {
        if ((c1 < 0.0) && (c2 > 0.0)) { // Yellow
            float cleft = tan(angleOne + blurRadius);
            float cmiddle = tan(angleOne - blurRadius);
            float cright = tan(angleTwo + blurRadius);
            vec4 colorLeft = blendFactor * vec4(fragColor.r * 0.03, fragColor.g * 1.0, fragColor.b * 0.92, fragColor.a); // blue
            vec4 colorMiddle = blendFactor * vec4(fragColor.r * 1.0, fragColor.g * 0.92, fragColor.b * 0.03, fragColor.a); // yellow
            vec4 colorRight = blendFactor * vec4(fragColor.r * 1.0, fragColor.g * 0.03, fragColor.b * 0.5, fragColor.a); // red
            float mixFactorLeft = smoothstep(cleft, cmiddle, slope);
            float mixFactorRight = smoothstep(cright, c2, slope);
            if (slope < cleft && slope >= cmiddle) {
                fragColor = mix(colorLeft, colorMiddle, mixFactorLeft);
            } else if (slope <= cright && slope > c2) {
                fragColor = mix(colorMiddle, colorRight, mixFactorRight);
            } else {
                fragColor = colorMiddle;
            }
        } else {
            if ((c2 < 0.0) && (c3 > 0.0)) { // Red
                float cleft = tan(angleTwo + blurRadius);
                float cmiddle = tan(angleTwo - blurRadius);
                float cright = tan(angleThree + blurRadius);
                vec4 colorLeft = blendFactor * vec4(fragColor.r * 1.0, fragColor.g * 0.92, fragColor.b * 0.03, fragColor.a); // blue
                vec4 colorMiddle = blendFactor * vec4(fragColor.r * 1.0, fragColor.g * 0.03, fragColor.b * 0.5, fragColor.a); // yellow
                vec4 colorRight = blendFactor * fragColor; // frag
                float mixFactorLeft = smoothstep(cleft, cmiddle, slope);
                float mixFactorRight = smoothstep(cright, c3, slope);
                if (slope <= cleft && slope >= cmiddle) {
                    fragColor = mix(colorLeft, colorMiddle, mixFactorLeft);
                } else {
                    if (slope <= cright && slope <= c3) {
                        fragColor = mix(colorMiddle, colorRight, mixFactorRight);
                    } else {
                        fragColor = colorMiddle;
                    }
                }
            }
        }
    }
    gl_FragColor = vec4(fragColor.rgba);
}