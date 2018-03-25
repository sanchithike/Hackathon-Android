package com.roposo.creation.graphics;

import android.opengl.Matrix;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.RenderTarget;

import static com.roposo.creation.graphics.gles.GraphicsUtils.VERBOSE;

/**
 * Created by bajaj on 09/10/17.
 */
// TODO minor sahilbaja Camera abstraction
public class Camera {
    private static final String TAG = "Camera";

    static final int ROTATION_0 = 0;
    static final int ROTATION_90 = 90;
    static final int ROTATION_180 = 180;
    static final int ROTATION_270 = 270;

    private float mDistance = 0.0f;
    public float[] eye = {0.0f, 0.0f, 0.0f};
    private float[] center = {0.0f, 0.0f, 0.0f};
    private float[] up = {0.0f, 1.0f, 0.0f};

    float mAspectRatio;

    private int mCameraRotation;

    private float mCameraTranslation;
    private float mZoomLevel;

    private float[] mViewMatrix = new float[16];
    public float mPreScale;

    public Camera(float aspectRatio) {
        mAspectRatio = aspectRatio;

        mCameraRotation = 0;
        mZoomLevel = 1.0f;
        mCameraTranslation = 0.0f;
    }

    public void rotateCamera(int rotation) {
        mCameraRotation += rotation;
        setCameraRotation(mCameraRotation);
    }

    public float[] getView() {
        return mViewMatrix;
    }

    private void computeCameraParams() {
        if (VERBOSE) {
            Log.d(TAG, "new eye: " + eye[0] + " x " + eye[1] + " x " + eye[2]);
            Log.d(TAG, "new center: " + center[0] + " x " + center[1] + " x " + center[2]);
        }
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, eye[0], eye[1], eye[2], center[0], center[1], center[2], 0.0f, 1.0f, 0.0f); //, up[0], up[1], up[2]);
        Matrix.rotateM(mViewMatrix, 0, mCameraRotation, 0, 0, 1);
    }

    public void resetCamera() {
        eye = new float[]{0.0f, 0.0f, 0.0f};
        center = new float[]{0.0f, 0.0f, -Caches.mWallDist};
        up = new float[]{0.0f, 1.0f, 0.0f};

        mCameraRotation = 0;

        mZoomLevel = 1.0f;

        if (mAspectRatio == 0) return;
        float refDistance = (float) (1.0f / mAspectRatio / 2.0 / Math.tan(((double) RenderTarget.mFOV * Math.PI) / 360.0));
        mDistance = 1.0f * refDistance;

        Log.d(TAG, "resetCamera:: mDistance: " + mDistance);

        mCameraTranslation = mDistance + Caches.mWallDist - (mDistance + Caches.mWallDist) / mZoomLevel;
        setEye(eye[0], eye[1], mCameraTranslation);
        computeCameraParams();
    }

    private void setCameraRotation(int rotation) {
        mCameraRotation = rotation;
        switch (mCameraRotation) {
            case ROTATION_0:
                up[0] = 0.0f;
                up[1] = 1.0f;
                break;
            case ROTATION_90:
                up[0] = 1.0f;
                up[1] = 0.0f;
                break;
            case ROTATION_180:
                up[0] = 0.0f;
                up[1] = -1.0f;
                break;
            case ROTATION_270:
                up[0] = -1.0f;
                up[1] = 0.0f;
                break;
            default:
                up[0] = 0.0f;
                up[1] = 1.0f;
        }
        computeCameraParams();
    }

    private void setEye(float x, float y, float z) {
        eye[0] = x;
        eye[1] = y;
        eye[2] = mDistance - z;

        mPreScale = (mDistance + Caches.mWallDist) / mDistance / 2.0f;

        Log.d(TAG, "new Camera pos: " + eye[0] + " x " + eye[1] + " x " + eye[2]);
        computeCameraParams();
    }

    private void positionCamera(float posZ) {
        setEye(eye[0], eye[1], posZ);
        Log.d(TAG, "Camera pos: " + eye[0] + " x " + eye[1] + " x " + eye[2]);
    }

    public void positionCamera(float posX, float posY) {
        eye[0] = posX;
        eye[1] = posY;
        center[0] = posX;
        center[1] = posY;
        positionCamera(eye[0], eye[1], eye[2]);
    }

    public void positionCamera(float posX, float posY, float posZ) {
        computeCameraParams();
    }

    public void translateCamera(float distanceZ) {
        moveCameraBy(0.0f, 0.0f, distanceZ);
    }

    public void translateCamera(float distanceX, float distanceY) {
        moveCameraBy(distanceX, distanceY, 0.0f);
    }

    public void setCameraZoom(float zoomLevel) {
        mZoomLevel = zoomLevel;
        mZoomLevel = Math.max(0.25f, Math.min(4.0f, mZoomLevel));
        mCameraTranslation = mDistance + Caches.mWallDist - (mDistance + Caches.mWallDist) / mZoomLevel;
        positionCamera(mCameraTranslation);
    }

    public void zoomCamera(float zoomLevel) {
        mZoomLevel = mZoomLevel * (1 + zoomLevel);
        setCameraZoom(mZoomLevel);
//        translateCamera(mDistanceZ);
    }

    public void moveCameraBy(float distanceX, float distanceY, float distanceZ) {
        if (mAspectRatio == 0) return;
        float cameraScaleFactor = Math.abs((Caches.mWallDist + mDistance) / mDistance);
        eye[0] += cameraScaleFactor * distanceX;
        eye[1] += cameraScaleFactor * distanceY;
        eye[2] += distanceZ;

        center[0] += cameraScaleFactor * distanceX;
        center[1] += cameraScaleFactor * distanceY;

        float verticalScale = 1.0f / mAspectRatio * cameraScaleFactor / 2.0f;
        float horizontalScale = 1.0f; // (float) (mDrawableAspectRatio/mAspectRatio * cameraScaleFactor / 2.0f);

        eye[0] = Math.max(-horizontalScale, Math.min(horizontalScale, eye[0]));
        eye[1] = Math.max(-verticalScale, Math.min(verticalScale, eye[1]));
        center[0] = Math.max(-horizontalScale, Math.min(horizontalScale, center[0]));
        center[1] = Math.max(-verticalScale, Math.min(verticalScale, center[1]));

        if (VERBOSE) {
            Log.d(TAG, "vertical scale: " + verticalScale + "\t eye Y: " + eye[1]);
            Log.d(TAG, "horizontal scale: " + horizontalScale + "\t eye X: " + eye[0]);
        }

        computeCameraParams();
    }

    public float[] getCameraLookAt() {
        return center;
    }

    public float[] getCameraPosition() {
        return eye;
    }

    public float[] getViewMatrix() {
        return mViewMatrix;
    }
}
