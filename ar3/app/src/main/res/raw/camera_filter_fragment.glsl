#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform sampler2D texture;
uniform sampler2D maskTexture;
uniform int filterMode;

varying vec2 vTexCoord;

void main() {

    vec4 camera = texture2D(texture, vTexCoord);
    float mask = texture2D(maskTexture, vTexCoord).r;

    vec3 color = camera.rgb;

    if (filterMode == 1) {
        vec3 jaundice = vec3(1.0, 0.9, 0.6);
        color = mix(camera.rgb, jaundice, mask);
    }

    gl_FragColor = vec4(color, 1.0);
}