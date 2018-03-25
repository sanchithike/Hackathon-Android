precision mediump float;
uniform float timeFactor;

void main() {
    if (fract(timeFactor) > 0.5) {
        float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;
        vec4 outputPixels = vec4(vec3(pixelLuminance), fragColor.a);
        fragColor *= vec4(vec3(clamp(abs(tan(timeFactor)), 1.75, 2.0)), 1.0);
    }
}