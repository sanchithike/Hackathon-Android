#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform samplerExternalOES baseSampler;
varying vec2 outPosition;

void main(void) {
    mediump vec4 fragColor;
    fragColor = blendFactor * texture2D(baseSampler, outTexCoordinates);
    gl_FragColor = vec4(fragColor.rgba);
}