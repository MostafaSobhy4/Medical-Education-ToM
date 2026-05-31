package com.example.ar;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLView extends GLSurfaceView {
    private GLRenderer renderer;

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new GLRenderer(this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setFilterMode(int mode) {
        if (renderer != null) renderer.setFilterMode(mode);
    }

    public void setGLReadyCallback(GLRenderer.GLReadyCallback cb) {
        if (renderer != null) renderer.setGLReadyCallback(cb);
    }

    // Updated to support full face landmarks (cheeks + nose)
    public void updateFaceLandmarks(float leftCheekX, float leftCheekY,
                                    float rightCheekX, float rightCheekY,
                                    float noseX, float noseY) {
        if (renderer != null) {
            renderer.updateFaceLandmarks(leftCheekX, leftCheekY,
                    rightCheekX, rightCheekY,
                    noseX, noseY);
        }
    }
}