package com.example.ar;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class ShaderUtils {

    // Add this constant alongside your existing shader string constants
    public static final String FILTER_MODE_UNIFORM = "filterMode";

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
                    "uniform sampler2D maskTexture;\n" +
                    "uniform int filterMode;\n" +
                    "varying vec2 vTexCoord;\n" +

                    "vec3 rgb2hsv(vec3 c) {\n" +
                    "    float maxC = max(c.r, max(c.g, c.b));\n" +
                    "    float minC = min(c.r, min(c.g, c.b));\n" +
                    "    float delta = maxC - minC;\n" +
                    "    float h = 0.0;\n" +
                    "    if (delta > 0.0001) {\n" +
                    "        if (maxC == c.r)      h = mod((c.g - c.b) / delta, 6.0);\n" +
                    "        else if (maxC == c.g) h = (c.b - c.r) / delta + 2.0;\n" +
                    "        else                  h = (c.r - c.g) / delta + 4.0;\n" +
                    "        h /= 6.0;\n" +
                    "        if (h < 0.0) h += 1.0;\n" +
                    "    }\n" +
                    "    float s = (maxC < 0.0001) ? 0.0 : delta / maxC;\n" +
                    "    return vec3(h, s, maxC);\n" +
                    "}\n" +

                    "vec3 hsv2rgb(vec3 c) {\n" +
                    "    float h = c.x * 6.0;\n" +
                    "    float s = c.y;\n" +
                    "    float v = c.z;\n" +
                    "    float i = floor(h);\n" +
                    "    float f = h - i;\n" +
                    "    float p = v * (1.0 - s);\n" +
                    "    float q = v * (1.0 - s * f);\n" +
                    "    float t = v * (1.0 - s * (1.0 - f));\n" +
                    "    if      (i == 0.0) return vec3(v, t, p);\n" +
                    "    else if (i == 1.0) return vec3(q, v, p);\n" +
                    "    else if (i == 2.0) return vec3(p, v, t);\n" +
                    "    else if (i == 3.0) return vec3(p, q, v);\n" +
                    "    else if (i == 4.0) return vec3(t, p, v);\n" +
                    "    else               return vec3(v, p, q);\n" +
                    "}\n" +

                    "vec3 applyJaundice(vec3 rgb) {\n"+

                    "    // warm yellow tint\n"+
                    "rgb.r *= 1.45;\n"+
                    "rgb.g *= 1.25;\n"+
                    "rgb.b *= 0.35;\n"+

                    "// slightly desaturate blue tones\n"+
                    "float luminance =\n"+
                    "dot(rgb, vec3(0.299, 0.587, 0.114));\n"+

                    "rgb = mix(\n"+
                    "vec3(luminance),\n"+
                    "rgb,\n"+
                    "1.15\n"+
                    ");\n"+

                    "return clamp(rgb, 0.0, 1.0);\n"+
                            "}\n"+

                    "void main() {\n" +
                    "    vec4 color = texture2D(texture, vTexCoord);\n" +

                    // ✅ Flip mask UV vertically to match camera orientation
                    "    vec2 maskCoord = vec2(1.0 - vTexCoord.x, 1.0 - vTexCoord.y);\n" +
                    // In your FRAGMENT_SHADER main():
                    "    float personMask = texture2D(maskTexture, maskCoord).r;\n" +  // no flip here

                    "    vec3 filtered = color.rgb;\n" +
                    "    if (filterMode == 1) {\n" +
                    "        vec3 jaundiced = applyJaundice(color.rgb);\n" +
                    "        filtered = mix(color.rgb, jaundiced, 1.0 - personMask);\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(filtered, 1.0);\n" +
                    "}\n";

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