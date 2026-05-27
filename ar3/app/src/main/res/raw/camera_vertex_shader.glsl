#extension GL_OES_EGL_image_external : require

attribute vec4 position;
attribute vec2 texCoord;
varying vec2 vTexCoord;

void main() {
    gl_Position = position;
    vTexCoord = texCoord;
}
