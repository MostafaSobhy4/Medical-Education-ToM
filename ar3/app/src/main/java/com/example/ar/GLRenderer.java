package com.example.ar;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

public class GLRenderer implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    public interface GLReadyCallback {
        void onReady(SurfaceTexture surfaceTexture);
    }

    private GLReadyCallback callback;
    private GLSurfaceView glView;

    private int textureId;
    private SurfaceTexture surfaceTexture;

    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int textureUniform;

    private int oesTextureLocation = 0;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texBuffer;

    private int maskTextureId;

    private int maskUniform;

    private int maskWidth = 256;
    private int maskHeight = 256;

    private ByteBuffer testMaskBuffer;

    // Add these fields at the top of the class
    private int filterModeUniform;
    private int currentFilterMode = 0; // 0=none, 1=jaundice, 2=edema, 3=butterfly

    public void setFilterMode(int mode) {
        this.currentFilterMode = mode;
    }

    private final float[] vertices = {

            // slight zoom/crop for AR debugging

            -1.15f, -1.15f,
            1.15f, -1.15f,
            -1.15f,  1.15f,
            1.15f,  1.15f
    };



    private final float[] texCoords = {

            // correct portrait front camera

            0f, 1f,
            0f, 0f,
            1f, 1f,
            1f, 0f
    };

    public void setGLView(GLSurfaceView view) {
        this.glView = view;
    }

    public void setGLReadyCallback(GLReadyCallback callback) {
        this.callback = callback;
    }

    private int createMaskTexture() {

        int[] tex = new int[1];

        GLES20.glGenTextures(1, tex, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
        );

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
        );

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
        );

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
        );

        return tex[0];
    }

    private void createTestMask() {

        byte[] data = new byte[maskWidth * maskHeight];

        for (int y = 0; y < maskHeight; y++) {

            for (int x = 0; x < maskWidth; x++) {

                float dx = x - maskWidth / 2f;
                float dy = y - maskHeight / 2f;

                float dist = (float)Math.sqrt(dx * dx + dy * dy);

                int index = y * maskWidth + x;

                if (dist < 80) {
                    data[index] = (byte)255;
                } else {
                    data[index] = 0;
                }
            }
        }

        testMaskBuffer = ByteBuffer.allocateDirect(data.length);
        testMaskBuffer.put(data);
        testMaskBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        textureId = createOESTexture();



        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        texBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texBuffer.put(texCoords).position(0);

        // compile shaders HERE (correct place)
        program = ShaderUtils.createProgram(
                ShaderUtils.VERTEX_SHADER,
                ShaderUtils.FRAGMENT_SHADER
        );

        positionHandle = GLES20.glGetAttribLocation(program, "position");
        texCoordHandle = GLES20.glGetAttribLocation(program, "texCoord");
        textureUniform = GLES20.glGetUniformLocation(program, "texture");
        filterModeUniform = GLES20.glGetUniformLocation(program, "filterMode");

// Also initialize the mask texture here — it's currently never created!
        maskTextureId = createMaskTexture();

        // Upload a blank white mask so the shader doesn't sample garbage
        byte[] blank = new byte[4 * 4];

        // ✅ Fix — fill with 0 (black = no filter until real mask comes in)
        java.util.Arrays.fill(blank, (byte) 0);
        ByteBuffer blankBuf = ByteBuffer.wrap(blank);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                4, 4, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, blankBuf);
        maskUniform =
                GLES20.glGetUniformLocation(program, "maskTexture");

        if (callback != null) {
            callback.onReady(surfaceTexture);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }

        GLES20.glUseProgram(program);
        GLES20.glUniform1i(filterModeUniform, currentFilterMode);

        // IMPORTANT: bind camera texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(textureUniform, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

        GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D,
                maskTextureId
        );

        GLES20.glUniform1i(maskUniform, 1);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d("GL", "Frame available");
        if (glView != null) {
            glView.requestRender();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    private int createOESTexture() {
        int[] tex = new int[1];

        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return tex[0];
    }

    public void updateMask(Bitmap bitmap) {
        Log.d("MASK", "Updating mask texture");

        if (bitmap == null || maskTextureId == 0) return;

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTextureId);

        GLUtils.texImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                bitmap,
                0
        );
    }
}