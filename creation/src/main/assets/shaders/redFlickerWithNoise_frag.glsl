#define part 0.10
uniform samplerExternalOES baseSampler;
uniform float timeFactor;
varying vec2 outTexCoordinates;

float random(vec2 st) {
 return fract(sin(dot(st.xy, vec2(tan(timeFactor), 43758.0 * tan(timeFactor)))));
}

void main() {
    float t = fract(timeFactor);
    fragColor = texture2D(baseSampler, outTexCoordinates);
    vec3 redColor = vec3(0.75, 0.0, 0.0);
    float dist;
    if (t <= 2.0 * part || (t > 6.0 * part && t <= 8.0 * part)) {
       dist = distance(outTexCoords - sin(timeFactor), vec2(0.5));
       fragColor *= vec4(vec3((1.0 - vec3(dist)) * redColor),  1.0 + abs(tan(timeFactor)));
    }
    else if (t > 2.0 * part && t <= 4.0 * part) {
        fragColor.rgb *= vec3(random(outTexCoords.xy));
    }
    else if (t > 4.0 * part && t <= 5.0 * part) {
        fragColor.rgb *= redColor;
    }
}
