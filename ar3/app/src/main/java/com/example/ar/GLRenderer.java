package com.example.ar;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLRenderer implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    public interface GLReadyCallback {
        void onReady(SurfaceTexture surfaceTexture);
    }

    private final GLView glView;
    private GLReadyCallback callback;
    private SurfaceTexture surfaceTexture;
    private int program, textureId;
    private int filterMode = 0;
    private boolean frameAvailable = false;

    // Viewport size — set in onSurfaceChanged, passed to shader as uResolution
    private float viewportW = 1080f, viewportH = 1920f;

    // Face landmark positions (UV space 0–1)
    private float leftCheekX = 0.35f, leftCheekY = 0.52f;
    private float rightCheekX = 0.65f, rightCheekY = 0.52f;
    private float noseX = 0.5f, noseY = 0.48f;

    private static final float[] QUAD = {
            -1f, -1f,  0f, 0f,
            1f, -1f,  1f, 0f,
            -1f,  1f,  0f, 1f,
            1f,  1f,  1f, 1f,
    };
    private final FloatBuffer quadBuf;

    public GLRenderer(GLView view) {
        glView = view;
        quadBuf = ByteBuffer.allocateDirect(QUAD.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        quadBuf.put(QUAD).position(0);
    }

    public void setGLReadyCallback(GLReadyCallback cb) { callback = cb; }

    public void setFilterMode(int mode) {
        filterMode = mode;
        glView.requestRender();
    }

    public void updateFaceLandmarks(float lcX, float lcY,
                                    float rcX, float rcY,
                                    float nX,  float nY) {
        leftCheekX = lcX; leftCheekY = lcY;
        rightCheekX = rcX; rightCheekY = rcY;
        noseX = nX; noseY = nY;
        glView.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = ShaderUtils.createProgram(
                ShaderUtils.VERTEX_SHADER, ShaderUtils.FRAGMENT_SHADER);

        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        if (callback != null) callback.onReady(surfaceTexture);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        viewportW = w;
        viewportH = h;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (frameAvailable) {
                surfaceTexture.updateTexImage();
                frameAvailable = false;
            }
        }

        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);

        int aPos = GLES20.glGetAttribLocation(program, "position");
        int aTex = GLES20.glGetAttribLocation(program, "texCoord");

        quadBuf.position(0);
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quadBuf);
        GLES20.glEnableVertexAttribArray(aPos);

        quadBuf.position(2);
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 16, quadBuf);
        GLES20.glEnableVertexAttribArray(aTex);

        float[] stMatrix = new float[16];
        surfaceTexture.getTransformMatrix(stMatrix);
        GLES20.glUniformMatrix4fv(
                GLES20.glGetUniformLocation(program, "uSTMatrix"), 1, false, stMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uFilterMode"), filterMode);

        // Pass actual viewport dimensions so shader works in pixel space
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uResolution"),
                viewportW, viewportH);

        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uLeftCheek"),
                leftCheekX, leftCheekY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uRightCheek"),
                rightCheekX, rightCheekY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uNose"),
                noseX, noseY);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        synchronized (this) { frameAvailable = true; }
        glView.requestRender();
    }
}