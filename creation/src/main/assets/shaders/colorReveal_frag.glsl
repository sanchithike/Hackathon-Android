precision mediump float;
varying vec2 outTexCoordinates;
uniform float timeFactor;
uniform float flickerTimeFactor;
uniform samplerExternalOES baseSampler;

void main() {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);
    vec3 PURPLE_COLOR = vec3(0.867, 0.627, 0.867);

    float monoFactor = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;
    fragColor = vec4(vec3(monoFactor), fragColor.a);
    vec3 outputPixels = fragColor.rgb;

    float left = step(0.5 * sin(timeFactor), outTexCoordinates.x);
    if (left != 0.0 && outTexCoordinates.x < 0.5) {
        outputPixels *= PURPLE_COLOR;
    }

    float right = step(0.5 * sin(timeFactor), 1.0 - outTexCoordinates.x);
    if (right != 0.0 && outTexCoordinates.x > 0.5) {
        outputPixels *= PURPLE_COLOR;
    }

    fragColor *= vec4(vec3(outputPixels), 1.0);
    vec4 flickerMultiplier = vec4(vec3(step(0.5, fract(flickerTimeFactor))), 1.0);
    fragColor *= flickerMultiplier;
}
