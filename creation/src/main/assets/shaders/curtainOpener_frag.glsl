precision mediump float;
varying vec2 outTexCoordinates;
uniform float timeFactor;
uniform samplerExternalOES baseSampler;

void main() {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);

    vec4 outputPixels = vec4(vec3(0.0), 1.0);
    float left = step(0.5 * (sin(timeFactor)), outTexCoordinates.y);
    float right = step(0.5 * (sin(timeFactor)), 1.0 - outTexCoordinates.y);
    if ((left != 0.0 && outTexCoordinates.y < 0.5) ||
        (right != 0.0) && outTexCoordinates.y >= 0.5) {
        outputPixels = fragColor;
    }

    fragColor = outputPixels;
}