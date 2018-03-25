precision mediump float;
varying vec2 outTexCoords;
uniform float timeFactor;
#define PI 3.14159265359

mat2 rotate2d(in float angle) {
    return mat2(cos(angle),-sin(angle),
                sin(angle),cos(angle));
}

void main() {
  vec3 color = vec3(0.0);
  vec2 st = outTexCoords.xy - vec2(0.5);
  st = rotate2d(sin(timeFactor) * PI) * st;
  st += vec2(0.5);
  color = texture2D(baseSampler, st).rgb;
  fragColor = vec4(color,1.0);
}
