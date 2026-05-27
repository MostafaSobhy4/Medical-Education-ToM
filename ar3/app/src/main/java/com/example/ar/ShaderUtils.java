package com.example.ar;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class ShaderUtils {

    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec2 texCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = position;\n" +
                    "    vTexCoord = texCoord;\n" +
                    "}";

    public static final String FRAGMENT_SHADER =

            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +

                    "uniform samplerExternalOES texture;\n" +
                    "varying vec2 vTexCoord;\n" +

                    // =====================================
                    // RGB -> HSV
                    // =====================================

                    "vec3 rgb2hsv(vec3 c) {\n" +

                    "    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);\n" +

                    "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +

                    "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +

                    "    float d = q.x - min(q.w, q.y);\n" +
                    "    float e = 1.0e-10;\n" +

                    "    return vec3(abs(q.z + (q.w - q.y)/(6.0*d + e)), d/(q.x + e), q.x);\n" +
                    "}\n" +

                    // =====================================
                    // SKIN MASK FUNCTION
                    // =====================================

                    "float skinMaskAt(vec2 uv) {\n" +

                    "    vec3 rgb = texture2D(texture, uv).rgb;\n" +
                    "    vec3 hsv = rgb2hsv(rgb);\n" +

                    "    float hueMask = smoothstep(0.01, 0.03, hsv.x) *\n" +
                    "                    (1.0 - smoothstep(0.14, 0.18, hsv.x));\n" +

                    "    float satMask = smoothstep(0.10, 0.18, hsv.y) *\n" +
                    "                    (1.0 - smoothstep(0.65, 0.80, hsv.y));\n" +

                    "    float valMask = smoothstep(0.15, 0.25, hsv.z);\n" +

                    "    return hueMask * satMask * valMask;\n" +
                    "}\n" +

                    // =====================================
                    // MAIN
                    // =====================================

                    "void main() {\n" +

                    "    vec4 color = texture2D(texture, vTexCoord);\n" +

                    // =====================================
                    // NEIGHBOR SAMPLING
                    // =====================================

                    // small blur radius

                    "    float offset = 0.0035;\n" +

                    "    float mask = 0.0;\n" +

                    // center
                    "    mask += skinMaskAt(vTexCoord) * 4.0;\n" +

                    // neighbors
                    "    mask += skinMaskAt(vTexCoord + vec2(offset, 0.0));\n" +
                    "    mask += skinMaskAt(vTexCoord - vec2(offset, 0.0));\n" +
                    "    mask += skinMaskAt(vTexCoord + vec2(0.0, offset));\n" +
                    "    mask += skinMaskAt(vTexCoord - vec2(0.0, offset));\n" +

                    // diagonals
                    "    mask += skinMaskAt(vTexCoord + vec2(offset, offset));\n" +
                    "    mask += skinMaskAt(vTexCoord + vec2(-offset, offset));\n" +
                    "    mask += skinMaskAt(vTexCoord + vec2(offset, -offset));\n" +
                    "    mask += skinMaskAt(vTexCoord + vec2(-offset, -offset));\n" +

                    // normalize
                    "    mask /= 12.0;\n" +

                    // extra smoothing
                    "    mask = smoothstep(0.15, 0.75, mask);\n" +

                    // =====================================
                    // JAUNDICE TINT
                    // =====================================

                    "    vec3 tinted = color.rgb;\n" +

                    "    tinted.r += 0.10;\n" +
                    "    tinted.g += 0.08;\n" +
                    "    tinted.b -= 0.04;\n" +

                    "    tinted *= vec3(1.05, 1.03, 0.97);\n" +

                    // =====================================
                    // FINAL BLEND
                    // =====================================

                    "    vec3 finalColor = mix(color.rgb, tinted, mask * 0.72);\n" +

                    "    gl_FragColor = vec4(finalColor, 1.0);\n" +
                    "}";

    public static int createProgram(String vertexSource, String fragmentSource) {

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES20.glCreateProgram();

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            Log.e("ShaderUtils", "Program linking failed:");
            Log.e("ShaderUtils", GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }

        return program;
    }

    private static int loadShader(int type, String source) {

        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e("ShaderUtils", "Shader compile failed:");
            Log.e("ShaderUtils", GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}