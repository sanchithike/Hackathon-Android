precision mediump float;
uniform float timeFactor;

void main() {
    if ( timeFactor > 10.0 && timeFactor < 90.0 ) {
        fragColor.rgb = vec3(dot(vec3(0.3,0.59,0.11), fragColor.rgb));
        fragColor *= vec4(vec3(step(0.5, fract(timeFactor))), 1.0);
    }
    gl_FragColor = vec4(fragColor.rgba);
}