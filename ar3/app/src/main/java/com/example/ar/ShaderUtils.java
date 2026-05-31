package com.example.ar;

import android.opengl.GLES20;

public class ShaderUtils {

    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec2 texCoord;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = position;\n" +
                    "    vTexCoord = (uSTMatrix * vec4(texCoord, 0.0, 1.0)).xy;\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;\n" +
                    "uniform samplerExternalOES uTexture;\n" +
                    "uniform int uFilterMode;\n" +
                    "uniform vec2 uResolution;\n" +          // NEW: actual viewport size in pixels
                    "varying vec2 vTexCoord;\n" +
                    "uniform vec2 uLeftCheek;\n" +
                    "uniform vec2 uRightCheek;\n" +
                    "uniform vec2 uNose;\n" +

                    // ── Skin detection ──────────────────────────────────────────────
                    "bool isSkin(vec3 c) {\n" +
                    "    return (c.r > 0.35 && c.g > 0.2 && c.b > 0.1 &&\n" +
                    "            c.r > c.b && c.r > c.g &&\n" +
                    "            abs(c.r - c.g) > 0.03 &&\n" +
                    "            (max(c.r,max(c.g,c.b)) - min(c.r,min(c.g,c.b))) > 0.08);\n" +
                    "}\n" +

                    // ── Jaundice ────────────────────────────────────────────────────
                    "vec3 jaundice(vec3 c) {\n" +
                    "    return clamp(vec3(c.r*1.08, c.g*1.04, c.b*0.82), 0.0, 1.0);\n" +
                    "}\n" +

                    // ── Noise ───────────────────────────────────────────────────────
                    "float hash(vec2 p) {\n" +
                    "    return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453);\n" +
                    "}\n" +
                    "float noise(vec2 p) {\n" +
                    "    vec2 i=floor(p); vec2 f=fract(p);\n" +
                    "    vec2 u=f*f*(3.0-2.0*f);\n" +
                    "    return mix(mix(hash(i),hash(i+vec2(1,0)),u.x),\n" +
                    "               mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),u.x),u.y);\n" +
                    "}\n" +

                    // ── Butterfly rash ───────────────────────────────────────────────
                    "vec3 butterflyRash(vec3 c, vec2 uv) {\n" +

                    // Convert UV → pixel coords so distances are measured in pixels,
                    // making the ellipse shape independent of aspect ratio entirely.
                    "    vec2 px = uv * uResolution;\n" +

                    // Landmark centres in pixel space
                    "    vec2 lCenter = uLeftCheek  * uResolution;\n" +
                    "    vec2 rCenter = uRightCheek * uResolution;\n" +
                    "    vec2 nCenter = uNose       * uResolution;\n" +

                    // Cheek ellipse radii IN PIXELS — tune these freely
                    //   halfW = half-width  (horizontal spread)
                    //   halfH = half-height (vertical spread, intentionally smaller)
                    "    float cheekW = uResolution.x * 0.22;\n" +   // 22% of screen width
                    "    float cheekH = uResolution.y * 0.09;\n" +   // 9%  of screen height

                    // Ellipse signed-distance: (dx/rx)^2 + (dy/ry)^2, compare to 1
                    "    vec2 lOff = px - lCenter;\n" +
                    "    float lEllipse = (lOff.x*lOff.x)/(cheekW*cheekW) +\n" +
                    "                     (lOff.y*lOff.y)/(cheekH*cheekH);\n" +
                    "    float leftMask = smoothstep(1.0, 0.1, lEllipse);\n" +

                    "    vec2 rOff = px - rCenter;\n" +
                    "    float rEllipse = (rOff.x*rOff.x)/(cheekW*cheekW) +\n" +
                    "                     (rOff.y*rOff.y)/(cheekH*cheekH);\n" +
                    "    float rightMask = smoothstep(1.0, 0.1, rEllipse);\n" +

                    // Nose bridge — narrower ellipse connecting the cheeks
                    "    float noseW = uResolution.x * 0.10;\n" +
                    "    float noseH = uResolution.y * 0.07;\n" +
                    "    vec2 nOff = px - nCenter;\n" +
                    "    float nEllipse = (nOff.x*nOff.x)/(noseW*noseW) +\n" +
                    "                     (nOff.y*nOff.y)/(noseH*noseH);\n" +
                    "    float noseMask = smoothstep(1.0, 0.1, nEllipse);\n" +

                    "    float mask = clamp(leftMask + rightMask + noseMask, 0.0, 1.0);\n" +

                    // Subtle blotchy texture
                    "    float mottle = noise(uv * 24.0);\n" +
                    "    float spot   = smoothstep(0.40, 0.70, mottle);\n" +

                    // Soft pale pink-red — like a real skin flush
                    "    vec3 baseTint = clamp(vec3(c.r*1.15+0.05, c.g*0.84, c.b*0.84), 0.0, 1.0);\n" +
                    "    vec3 spotTint = clamp(vec3(c.r*1.22+0.07, c.g*0.76, c.b*0.76), 0.0, 1.0);\n" +
                    "    vec3 rashColor = mix(baseTint, spotTint, spot * 0.5);\n" +

                    "    float opacity = mask * (0.40 + spot * 0.15);\n" +
                    "    return mix(c, rashColor, clamp(opacity, 0.0, 1.0));\n" +
                    "}\n" +

                    // ── Main ────────────────────────────────────────────────────────
                    "void main() {\n" +
                    "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
                    "    vec3 out3  = color.rgb;\n" +
                    "    if (isSkin(color.rgb)) {\n" +
                    "        if      (uFilterMode == 1) out3 = jaundice(color.rgb);\n" +
                    "        else if (uFilterMode == 3) out3 = butterflyRash(color.rgb, vTexCoord);\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(out3, 1.0);\n" +
                    "}\n";

    public static int createProgram(String vert, String frag) {
        int vs = compile(GLES20.GL_VERTEX_SHADER,   vert);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, frag);
        int p  = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }
}