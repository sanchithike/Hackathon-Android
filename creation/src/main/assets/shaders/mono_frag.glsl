precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform sampler2D baseSampler;

void main(void) {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);

    float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;
    fragColor = blendFactor * vec4(pixelLuminance, pixelLuminance, pixelLuminance, fragColor.a);

    gl_FragColor = vec4(fragColor.rgba);
}