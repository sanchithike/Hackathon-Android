precision mediump float;
uniform float saturationAmt;

vec3 saturate(vec3 color, float amount) {
  vec3 gray = vec3(dot(vec3(0.3,0.59,0.11), color));
  return vec3(mix(color, gray, -amount));
}

void main() {
    fragColor = vec4(saturate(fragColor.rgb , saturationAmt), fragColor.a);
    gl_FragColor = vec4(fragColor.rgba);
}