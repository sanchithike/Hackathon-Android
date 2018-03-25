uniform float timeFactor;
precision mediump float;

void main() {
   if (fract(timeFactor) > 0.5) {
    fragColor *= vec4(1.0 - fragColor.r , 1.0 - fragColor.g, 1.0 - fragColor.b , fragColor.a);
   }
}
