precision mediump float;
varying vec2 outTexCoordinates;
uniform float timeFactor;
uniform samplerExternalOES baseSampler;

void mainImage() {
    mediump vec4 fragColor;
    fragColor = texture2D(baseSampler, outTexCoordinates);

	vec2 inputCoords = outTexCoordinates.xy;
    float zoomFactor = (0.5 + 0.5 * ((sin(timeFactor))));

    vec2 scaleCenter = vec2(0.5);
    inputCoords = ((inputCoords - scaleCenter) / zoomFactor) + scaleCenter;

    vec2 leftBottom = step(vec2(0.001), inputCoords);
    vec3 colorMultiplier = vec3(leftBottom.x * leftBottom.y);
    vec2 topRight = step(vec2(0.001), 1.0- inputCoords);
    colorMultiplier *= topRight.x * topRight.y;

    fragColor = ouputPixels * vec4(colorMultiplier, 1.0);
}