

#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES texture;
varying vec2 vTexCoord;

void main() {

    vec4 color = texture2D(texture, vTexCoord);

    // simple "jaundice bias"
    color.r += 0.08;
    color.g += 0.05;
    color.b -= 0.03;

    gl_FragColor = color;
}