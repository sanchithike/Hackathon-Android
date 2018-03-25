precision mediump float;
varying vec2 outTexCoordinates;
uniform float timeFactor;

void main() {
    mediump vec4 fragColor;

    float switchFactor = step(0.5, fract(timeFactor));
    if (switchFactor == 0.0) {
        float monoFactor = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;
        fragColor = vec4(vec3(monoFactor), fragColor.a);
    }

    gl_FragColor = fragColor;
}