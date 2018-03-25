package com.roposo.creation.graphics;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by bajaj on 12/07/16.
 */
public class RGLSurfaceView extends GLSurfaceView {
    private static final String TAG = RGLSurfaceView.class.getSimpleName();

    Context mContext;
    SurfaceEventListener mSurfaceEventListener;

    private boolean mCompatMode = false;
    private boolean mRendering;

    public RGLSurfaceView(Context context) {
        super(context);
        Log.d(TAG, "RGLSurfaceView");
        mContext = context;
        configureRenderer();
    }

    public RGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "RGLSurfaceView");
        mContext = context;
        configureRenderer();
    }

    public void setCompatMode(boolean compatMode) {
        mCompatMode = compatMode;
    }

    private void configureRenderer() {
        setEGLContextClientVersion(2);     // select GLES 2.0
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setRenderer(Renderer renderer) {
        if (mRendering) return;
        super.setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mRendering = true;
//        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!mCompatMode && mRendering) {
            super.surfaceCreated(holder);
        }
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(!mCompatMode && mRendering) {
            super.surfaceDestroyed(holder);
        }
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceDestroyed(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(!mCompatMode && mRendering) {
            super.surfaceChanged(holder, format, w, h);
        }
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceChanged(holder, format, w, h);
        }
    }

    public void setEventListener(SurfaceEventListener eventListener) {
        mSurfaceEventListener = eventListener;
    }

    public interface SurfaceEventListener {
        void surfaceCreated(SurfaceHolder holder);
        void surfaceDestroyed(SurfaceHolder holder);
        void surfaceChanged(SurfaceHolder holder, int format, int w, int h);
    }
}
