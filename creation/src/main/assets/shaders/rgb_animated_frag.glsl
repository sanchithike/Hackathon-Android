#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform float redFactor;
uniform float greenFactor;
uniform float blueFactor;
uniform samplerExternalOES baseSampler;

void main(void) {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);

    fragColor = blendFactor * vec4(fragColor.r * redFactor, fragColor.g * greenFactor
    , fragColor.b *  blueFactor, fragColor.a);

    gl_FragColor = vec4(fragColor.rgba);
}