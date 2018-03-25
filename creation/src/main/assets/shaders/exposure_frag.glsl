#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 outTexCoordinates;
uniform float blendFactor;
uniform float exposure;
uniform samplerExternalOES baseSampler;

void main(void) {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);
    if(outTexCoordinates.x > outTexCoordinates.y) {
        float powerFactor = pow(2.0, exposure);
        float redPow = fragColor.r * powerFactor;
        float greenPow = fragColor.g * powerFactor;
        float bluePow = fragColor.b * powerFactor;
        fragColor = blendFactor * vec4(redPow, greenPow, bluePow, fragColor.a);
    }
    gl_FragColor = vec4(fragColor.rgba);
}