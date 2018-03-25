precision mediump float;
varying vec2 outTexCoordinates;
uniform vec4 colorMultiplier;
uniform samplerExternalOES baseSampler;

void main() {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);

    //Value of color multiplier should be passed according to the color overlay required
    gl_FragColor = fragColor * colorMultiplier;
}