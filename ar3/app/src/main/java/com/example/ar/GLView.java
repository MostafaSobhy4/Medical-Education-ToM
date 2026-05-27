package com.example.ar;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLView extends GLSurfaceView {

    private final GLRenderer renderer;

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);

        renderer = new GLRenderer();

        renderer.setGLView(this);

        setRenderer(renderer);

        // IMPORTANT: we control redraw manually via camera frames
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public GLRenderer getRenderer() {
        return renderer;
    }
}