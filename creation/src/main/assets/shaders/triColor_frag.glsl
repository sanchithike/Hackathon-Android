precision mediump float;
varying vec2 outTexCoords;
uniform float timeFactor;

void main() {
    vec3 SAFFRON = vec3(1.0, 0.6, 0.2);
    vec3 WHITE = vec3(1.0);
    vec3 GREEN = vec3(0.0745, 0.5333, 0.0314);
    vec3 outputPixels = vec3(1.0);
    float top = step(0.5 * (sin(timeFactor)), outTexCoords.x);
    if (top != 0.0 && outTexCoords.x <= 0.5) {
        if (outTexCoords.x < 0.35) {
           outputPixels *= SAFFRON;
        }
        else {
           outputPixels *= WHITE;
        }
    }

    float bottom = step(0.5 * (sin(timeFactor)), 1.0 - outTexCoords.x);
    if (bottom != 0.0 && outTexCoords.x > 0.5) {
        if (outTexCoords.x >= 0.65) {
            outputPixels *= GREEN;
        }
        else {
            outputPixels *= WHITE;
        }
    }
    fragColor *= vec4(outputPixels, 1.0);
}
