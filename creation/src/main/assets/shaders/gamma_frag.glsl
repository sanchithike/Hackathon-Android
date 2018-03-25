#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform float powerFactor;
uniform samplerExternalOES baseSampler;

void main(void) {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);
    if(outTexCoordinates.x > outTexCoordinates.y) {
        float redPow = pow(fragColor.r, powerFactor);
        float greenPow = pow(fragColor.g, powerFactor);
        float bluePow = pow(fragColor.b, powerFactor);
        fragColor = blendFactor * vec4(redPow, greenPow, bluePow, fragColor.a);
    }
    gl_FragColor = vec4(fragColor.rgba);
}