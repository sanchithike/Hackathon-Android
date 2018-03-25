package com.roposo.creation.graphics.gles;

import android.graphics.Rect;
import android.opengl.Matrix;
import android.support.annotation.IntDef;

import com.roposo.creation.graphics.Camera;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by tajveer on 12/16/17.
 */

public abstract class RenderTarget {
    // OpenGL Camera Parameters
    public static float mFOV = 50.0f; // degrees
    static float zNear = 0.3f;
    static float zFar = 50.0f;
    //    float mRefDistance = 1.0; //(float) (1.0f/mAspectRatio * Math.tan((double)mFOV/2.0));
    float mAspectRatio = 0.0f;

    static final int TARGET_DEFAULT = 1;
    static final int TARGET_FBO = 2;
    private int mTargetID;
    Rect mExtent = new Rect();
    private float[] mProjectionMatrix = new float[16];
    public Camera mCamera;
    public int mWidth, mHeight;

    private void computeCamera() {
        mCamera = new Camera(mAspectRatio);
        mCamera.resetCamera();
    }

    void computeProjection() {
        Matrix.setIdentityM(mProjectionMatrix, 0);
//        Matrix.perspectiveM(mProjectionMatrix, 0, (float) ((float) (Math.atan((float) Math.tan(mFOV/2 * Math.PI/180f) * mAspectRatio)*2f) * 180/Math.PI), mAspectRatio, zNear, zFar);
        Matrix.perspectiveM(mProjectionMatrix, 0, mFOV, mAspectRatio, zNear, zFar);
    }

    public abstract void setScaleParams(float width, float height);

    public void setExtent(Rect rect) {
        mExtent.set(rect);
        mAspectRatio = (float) rect.width() / rect.height();
        computeCamera();
        computeProjection();
    }

    public float[] getView() {
        return mCamera.getView();
    }

    public float[] getProjection() {
        return mProjectionMatrix;
    }

    public float getPreScale() {
        return mCamera.mPreScale;
    }

    public void resetCamera() {
        computeCamera();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @IntDef({TARGET_DEFAULT, TARGET_FBO})
    @Retention(RetentionPolicy.SOURCE)
    @interface TargetID{

    }

    @TargetID int getTargetID() {
        return mTargetID;
    }

    Rect getExtent() {
        return mExtent;
    }
}
