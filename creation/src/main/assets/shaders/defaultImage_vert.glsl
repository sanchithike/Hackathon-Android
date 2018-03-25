precision mediump float;
attribute vec3 position;
attribute vec2 texCoords;
uniform mat4 uMVPMatrix;
uniform mat4 mainTextureTransform;
varying vec2 outTexCoordinates;
varying vec2 outPosition;

void main() {
    outTexCoordinates = (mainTextureTransform * vec4(texCoords, 0.0, 1.0)).xy;
    vec4 transformedPosition = uMVPMatrix * vec4(position, 1.0);
    outPosition = position.xy;
    gl_Position = transformedPosition;
}