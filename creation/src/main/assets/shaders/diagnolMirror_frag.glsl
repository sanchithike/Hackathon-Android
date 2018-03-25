precision mediump float;
varying vec2 outTexCoordinates;
uniform samplerExternalOES baseSampler;

void mainImage() {
     mediump vec4 fragColor;
     fragColor = texture2D(baseSampler, outTexCoordinates);

     vec2 inputCoords = outTexCoordinates.xy;
     vec4 outputPixels = fragColor;

     //For all the points across diagnol assign the transposed image
     if (inputCoords.x + inputCoords.y > 1.0) {
       vec2 outputCoords = vec2(1.0 - inputCoords.y, 1.0 - inputCoords.x);
       vec4 mirroredAcrossDiagnol = texture2D(baseSampler, outputCoords);
       outputPixels = mirroredAcrossDiagnol.bgra;
     }

     gl_FragColor = outputPixels;
}