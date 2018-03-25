package com.roposo.creation.camera;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.MyLogger;
import com.roposo.creation.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kotlin.ranges.RangesKt;

/**
 * Created by bajaj on 29/05/17.
 */

public class CameraControl extends CameraPreview {
    private static final String TAG = "CameraControl";

    static {
        numberOfCameras = Camera.getNumberOfCameras();
        findFrontCamera();
        mCameraId = (DEFAULT_CAMERA_FRONT && CAMERA_FRONT >= 0) ? CAMERA_FRONT : CAMERA_BACK;
    }

    private String currentFocusMode;

    public CameraControl(Context context) {
        super(context);
        mCameraSettings = CameraSettings.getInstance(mContext);
    }

    public Camera mCamera;

    public void takePicture(final CameraPictureCallback pictureCallback) {
        super.takePicture(pictureCallback);
//        setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);

        try {
            Camera.Parameters params = mCamera.getParameters();
//      params.setPictureSize(1280, 720);
            params.setJpegQuality(mCaptureQuality);
            mCamera.setParameters(params);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        try {
            mCamera.takePicture(shutterCallback, null, null, jpegCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takePictureUsingOneshotcallback(CameraCaptureCallback captureCallback) {
        super.takePictureUsingOneshotcallback(captureCallback);
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(mOneShotPreviewCallback);
        }
    }

    @Override
    public void openCamera() {
        if (mCamera != null) {
            return;
//            return mCamera;
        }
        mCameraProps = CameraSettings.queryCameraProperties(mCameraId, false);

        boolean isCameraDisabled;
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        isCameraDisabled = dpm.getCameraDisabled(null);
        if (!isCameraDisabled) {
            try {
                mCamera = Camera.open(mCameraId);
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }

            if (mCamera == null) {
                MyLogger.w(TAG, "Not able to open camera");
                Toast.makeText(mContext, R.string.not_able_to_open_camera, Toast.LENGTH_SHORT).show();
            } else {
                // Should happen only first time for each camera in a lifetime.
                // Once done, we store camera properties in SharedPreferences and retreive them later.
                if (mCameraProps == null) {
                    mCameraProps = CameraSettings.initCameraProperties(mCamera, mCameraId);
                }

                if (mCameraProps == null) {
                    MyLogger.w(TAG, "Camera behaving unexpectedly.");
                    CrashlyticsWrapper.logException(new Exception("Camera behaving unexpectedly"));
                    Toast.makeText(mContext, R.string.camera_disabled_not_able_to_open_camera, Toast.LENGTH_SHORT).show();
                    return;
//                    return null;
                }
                try {
                    setCameraConfiguration();
                } catch (Exception e) {
                    CrashlyticsWrapper.logException(e);
                }
            }
        } else {
            MyLogger.w(TAG, "Camera is disabled.");
            Toast.makeText(mContext, R.string.camera_disabled_not_able_to_open_camera, Toast.LENGTH_SHORT).show();
        }
        isCameraAvailable = mCamera != null;

        if (isCameraAvailable) {
            mCameraStateCallback.onCameraOpened();
        } else {
            mCameraStateCallback.onCameraError(-1);
        }

//        return mCamera;
    }

    public void startPreview() {
        super.startPreview();
        if (cameraCompat) {
            startPreviewCompat();
            return;
        }
        if (mCamera == null) {
            openCamera();
        }
        boolean cameraConfigured = true;
        if (mCamera != null) {
            mCameraProps = CameraSettings.queryCameraProperties(mCameraId);
            if (mPreviewSurface == null) return;
            try {
                setCameraConfiguration();
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
            try {
                mCamera.setPreviewTexture(mPreviewSurface);
                mCamera.startPreview();
                onStartPreview();
            } catch (IOException e) {
                cameraConfigured = false;
                CrashlyticsWrapper.logException(e);
                MyLogger.w(TAG, "handleSetSurfaceTexture :: Camera startPreview() failed.");
            } catch (Exception e) {
                cameraConfigured = false;
                MyLogger.w(TAG, "handleSetSurfaceTexture :: Camera startPreview() failed.");
                CrashlyticsWrapper.logException(e);
            }
        }
    }

    void startPreviewCompat() {
        super.startPreviewCompat();
        if (mCamera == null) {
            openCamera();
        }
        boolean cameraConfigured = true;
        if (mCamera != null) {
            // Expresses the intent, even when other conditions are not suitable.
            mCameraProps = CameraSettings.queryCameraProperties(mCameraId);
            try {
                setCameraConfiguration();
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
            try {
                setPreviewDisplay(mHolder);
                setCameraDisplaySizeAndOrientationCompat();
                onStartPreview();
            } catch (IOException e) {
                cameraConfigured = false;
                CrashlyticsWrapper.logException(e);
            } catch (Exception e) {
                cameraConfigured = false;
                CrashlyticsWrapper.logException(e);
            }
        }
    }

    @Override
    public void resumePreview() {
        super.resumePreview();
        try {
            if (mCamera == null) {
                startPreview();
            } else {
                mCamera.startPreview();
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    public void pausePreview() {
        super.pausePreview();
        if (mCamera == null) return;
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    public void stopPreview() {
        super.stopPreview();
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
        mCamera = null;
    }

    public void destroyPreview() {
        super.destroyPreview();
    }

    private void setCameraConfiguration() throws Exception {
        int previewWidth = mCameraProps.mPreviewWidth;
        int previewHeight = mCameraProps.mPreviewHeight;

        int pictureWidth = mCameraProps.mPictureWidth;
        int pictureHeight = mCameraProps.mPictureHeight;

        String focusMode = mCameraProps.mSmoothFocusMode;
        int fps = mCameraProps.mPreviewFrameRate;
        int fpsRange = mCameraProps.mPreviewFPSRange;
        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params == null) return;
            if (!params.isZoomSupported()) {
                maxZoomLevel = 1;
            } else {
                maxZoomLevel = params.getMaxZoom();
            }

            int minExposureCompensation = params.getMinExposureCompensation();
            int maxExposureCompensation = params.getMaxExposureCompensation();
            float exposureCompensation = params.getExposureCompensation();
            brightness = (exposureCompensation - minExposureCompensation) / (maxExposureCompensation - minExposureCompensation);

            params.setRecordingHint(true); // False for now. Some cameras screw up
            mCamera.setParameters(params);
            params.setPreviewSize(previewWidth, previewHeight);
            try {
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            params.setPictureSize(pictureWidth, pictureHeight);
            try {
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null && !focusModes.isEmpty()) {
                if (!focusModes.contains(focusMode)) {
                    focusMode = focusModes.get(0);
                }

                try {
                    params.setFocusMode(focusMode);
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCameraDisplayOrientation();
    }

    // Set the camera display orientation
    public void setCameraDisplayOrientation() throws RuntimeException {
        if (mCamera == null || mCameraProps == null) {
            return;
        }

        int cameraId = mCameraProps.mCameraId;

        boolean frontFacing = mCameraProps.mIsFrontCamera;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        WindowManager winManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = winManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (frontFacing) {
                displayOrientation = (info.orientation + degrees) % 360;
                displayOrientation = (360 - displayOrientation) % 360;  // compensate the mirror
            } else {
                if (info.orientation == 270) {
                    //displayOrientation = (360 - info.orientation - degrees) % 360;
                    displayOrientation = (info.orientation - degrees + 360) % 360;
                } else {
                    displayOrientation = (info.orientation - degrees + 360) % 360;
                }
            }
        } else {
            if (frontFacing) {
                if (info.orientation == 270) {
                    displayOrientation = (info.orientation + degrees) % 360;
                    displayOrientation = (360 - displayOrientation) % 360;  // compensate the mirror
                } else {
                    displayOrientation = (info.orientation + degrees) % 360;
                }
            } else {  // back-facing
                displayOrientation = (info.orientation - degrees + 360) % 360;
            }
        }
        // sahil.bajaj Hack for inverted camera sensor Nexus 5s and 6
        if ((degrees < 90) && frontFacing && (info.orientation < 180)) {
            isCameraInverted = true;
        } else {
            isCameraInverted = false;
        }
        try {
            mCamera.setDisplayOrientation(displayOrientation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set camera preview size and rotation of the preview
    public void setCameraDisplaySizeAndOrientationCompat() {
        super.setCameraDisplaySizeAndOrientationCompat();
        setCameraDisplayOrientation();
        try {
            if (null == mCamera) {
                CrashlyticsWrapper.log(3, "setCameraDisplaySizeAndOrientationCompat", "Camera is null");
                return;
            }
            Camera.Parameters p = mCamera.getParameters();
            if (p != null) {
                cameraSetupCompat(p, AndroidUtilities.displayMetrics.widthPixels, AndroidUtilities.displayMetrics.heightPixels);
                mCamera.setParameters(p);

                //cameraSetup(p, width, height);
                setPreviewDisplay(mHolder);

                // always start preview after setting up parameters
                mCamera.startPreview();
                // always set auto focus after starting preview
                autoFocus();
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    private void cameraSetupCompat(Camera.Parameters p, int w, int h) {
        // set the camera parameters, including the preview size

        try {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
            double cameraAspectRatio = ((double) mCameraProps.mPreviewAspectRatio);
            p.setPreviewSize(mCameraProps.mPreviewWidth, mCameraProps.mPreviewHeight);
            if (((double) h) / w > cameraAspectRatio) {
                lp.width = (int) (h / cameraAspectRatio);
                lp.height = h;

            } else {
                lp.height = (int) (w * cameraAspectRatio);
                lp.width = w;
                lp.topMargin = (h - lp.height) / 2;

            }
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

            mSurfaceView.setLayoutParams(lp);
            mSurfaceView.requestLayout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        super.setPreviewDisplay(holder);
        if (mCamera != null) {
            mCamera.setPreviewDisplay(holder);
        }
    }

    @Override
    public void unlock() {
        mCamera.unlock();
    }

    @Override
    public boolean hasFlash() {
        try {
            return mCamera.getParameters().getFlashMode() != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void forceFlash() {
        if (mCamera != null) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                if (hasFlash()) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void enableFlash(boolean enable) {
        super.enableFlash(enable);
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                if (enable) {
                    if (hasFlash()) {
                        if (!params.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        }
                    }
                } else {
                    if (params.getFlashMode() != null) {
                        if (!params.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }
                    }
                }
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            Toast.makeText(mContext, R.string.flash_is_not_available, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void setFocusMode(@FocusMode String focusMode) {
        Log.d(TAG, "Auto focus");
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus();
                Camera.Parameters params = mCamera.getParameters();
                if (!params.getSupportedFocusModes().contains(focusMode)) {
                    return;
                }
                params.setFocusAreas(null);
                params.setFocusMode(focusMode);  //The sensor calls come for back-camera only
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fixedFocus() {
        Log.d(TAG, "Fixed focus");
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus();
                Camera.Parameters params = mCamera.getParameters();
                if (!params.getSupportedFocusModes().contains(FOCUS_MODE_FIXED)) {
                    return;
                }
                params.setFocusMode(FOCUS_MODE_FIXED);  //The sensor calls come for back-camera only
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void manualFocus(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y) {
        Log.d(TAG, "Smooth focus");

        x = RangesKt.coerceIn(x, 0, 1);
        y = RangesKt.coerceIn(y, 0, 1);

        if (mCamera != null) {

            try {

                mCamera.cancelAutoFocus();
                Camera.Parameters params = mCamera.getParameters();
//                Camera.Size pictureSize = params.getPictureSize();
//                x *= pictureSize.width;
//                y *= pictureSize.height;


                if (params.getMaxNumFocusAreas() < 1) return;

                List<String> supportedFocusModes = params.getSupportedFocusModes();
                currentFocusMode = params.getFocusMode();
                if (supportedFocusModes == null || supportedFocusModes.isEmpty() || !supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
                    return;
                } else {
                    params.setFocusMode(FOCUS_MODE_AUTO);
                }


                Pair<Float, Float> pair = getOrientedFocusPoints(x, y);
                x = pair.first;
                y = pair.second;

                Log.d(TAG, "Trying to auto focus");

                Rect focusArea = getFocusRect(x, y);

                Log.d(TAG, "focus Rect: " + focusArea.toString());

                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(focusArea, 1000));
                params.setFocusAreas(focusAreas);

                mCamera.setParameters(params);

                String focusMode = mCamera.getParameters().getFocusMode();
                Log.d(TAG, "focussing in focus mode: " + focusMode);
                mCamera.autoFocus(focusCallback);
            } catch (Exception e) {
                e.printStackTrace();
                resetFocusMode(mCamera);
            }
        }
    }

    @NonNull
    private Rect getFocusRect(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y) {
        int centerX = (int) (2000 * x) - 1000;
        int centerY = (int) (2000 * y) - 1000;

        Rect bounds = new Rect(-1000, -1000, 1000, 1000);

        return getFocusRect(centerX, centerY, 100, 100, bounds);
    }

    private Camera.AutoFocusCallback focusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                camera.cancelAutoFocus();
            }
            resetFocusMode(camera);
        }
    };

    private void resetFocusMode(Camera camera) {
        if (camera == null) return;
        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(currentFocusMode);
            camera.setParameters(parameters);
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    void cancelAutoFocus() {
        if (mCamera != null) {
            mCamera.cancelAutoFocus();
        }
    }

    // Checks if focus is sharp.... If it is or is going to be. Dynamic focus will be blocked.
    void checkAutoFocus(final String focusMode, Camera.AutoFocusCallback focusCallback) {
        if (mCamera != null) {
            mCamera.autoFocus(focusCallback);
        }
    }

    @Override
    public void setBrightness(@FloatRange(from = 0F, to = 1F) float value) {
        if (value > 1F || value < 0F) return;
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                int minExposureCompensation = parameters.getMinExposureCompensation();
                int maxExposureCompensation = parameters.getMaxExposureCompensation();
                int normalizedVal = (int) (minExposureCompensation + (maxExposureCompensation - minExposureCompensation) * value);
                parameters.setExposureCompensation(normalizedVal);
                mCamera.setParameters(parameters);
                super.setBrightness(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void smoothZoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        if (maxZoomLevel <= 1) return;

        zoom = RangesKt.coerceIn(zoom, 0F, 1F);

        if (mCamera != null) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                if (params.isSmoothZoomSupported()) {
                    mCamera.startSmoothZoom((int) (zoom * maxZoomLevel));
                } else {
                    zoomTo(zoom);
                }
            } catch (IllegalArgumentException e) {
                zoomTo(0F);
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
        }
    }

    @Override
    public boolean zoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        if (zoom < 0F || maxZoomLevel <= 1 || zoom > 1F || presentZoomLevel == zoom || mCamera == null) {
            return false;
        }

        presentZoomLevel = zoom;

        int newZoomLevel = (int) (zoom * maxZoomLevel);

        Log.d(TAG, "Camera newZoom: " + newZoomLevel);

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setZoom(newZoomLevel);
            mCamera.setParameters(parameters);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getCaptureOrientation() {
        boolean frontFacing = mCameraProps.mIsFrontCamera;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int orientationVal = info.orientation;
        return getCaptureOrientation(orientationVal);
    }
}
