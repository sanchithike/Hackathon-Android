package com.roposo.creation.graphics;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by bajaj on 12/07/16.
 */
public class RSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "RSurfaceView";

    Context mContext;
    RGLSurfaceView.SurfaceEventListener mSurfaceEventListener;

    private boolean mCompatMode = false;
    private boolean mRendering;

    public RSurfaceView(Context context) {
        super(context);
        Log.d(TAG, "RGLSurfaceView");
        mContext = context;
        configureRenderer();
    }

    public RSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "RGLSurfaceView");
        mContext = context;
        configureRenderer();
    }

    public void setCompatMode(boolean compatMode) {
        mCompatMode = compatMode;
    }

    private void configureRenderer() {
        getHolder().addCallback(this);
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setRenderer(Renderer renderer) {
        if (mRendering) return;
        mRendering = true;
//        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceDestroyed(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mSurfaceEventListener != null) {
            mSurfaceEventListener.surfaceChanged(holder, format, w, h);
        }
    }

    public void setEventListener(RGLSurfaceView.SurfaceEventListener eventListener) {
        mSurfaceEventListener = eventListener;
    }
}
