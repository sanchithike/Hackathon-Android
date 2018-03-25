precision mediump float;
varying vec2 outTexCoordinates;
uniform float timeFactor;
uniform samplerExternalOES baseSampler;

void mainImage() {
     mediump vec4 fragColor;
     fragColor = texture2D(baseSampler, outTexCoordinates);

     vec4 colorMultiplier;
     // timeFactor value will oscillate between 0 to 94.24 radians
     // i.e. 0 to 5400 degree (15 cycles)

     //For first 7 cycles
     if (timeFactor < 43.974) {
        colorMultiplier = vec4(vec3(clamp(abs(tan(timeFactor)), 1.0, 2.0)), 1.0);
     }
     //For remaining 8 cycles
     else {
       colorMultiplier = vec4(clamp(smoothstep(0.1, 2.0, abs(tan(timeFactor))), 0.4, 1.0),
                              clamp(smoothstep(0.1, 2.0, abs(tan(timeFactor))), 0.4, 1.0),
                              clamp(smoothstep(0.1, 2.0, abs(tan(timeFactor))), 0.4, 1.0),
                              1.0);
     }

     gl_FragColor = fragColor * colorMultiplier;
}