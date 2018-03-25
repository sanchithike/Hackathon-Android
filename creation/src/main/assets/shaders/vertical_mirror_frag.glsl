precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform sampler2D baseSampler;

void main(void) {
    mediump vec4 fragColor;

    vec2 focusCoord = vec2(0.0, 1.0);
    vec2 texCoord = 2.0 * outTexCoordinates - vec2(1.0, 1.0);
    texCoord = texCoord * sign(texCoord + focusCoord);
    texCoord = texCoord * 0.5 + vec2(0.5, 0.5);
    fragColor = blendFactor * texture2D(baseSampler, texCoord);

    gl_FragColor = vec4(fragColor.bgra);
}