package com.roposo.creation.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.GraphicUtil.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bajaj on 21/07/16.
 */
@SuppressWarnings("deprecation")
public class CameraSettings extends BaseCameraSettings {

    private CameraSettings(Context context) {
        super(context);
    }

    public static BaseCameraSettings getInstance(Context context) {
        if (mCameraSettings == null) {
            mCameraSettings = new CameraSettings(context);
        }
        return mCameraSettings;
    }

    private static void findSupportedFrameRate(Camera camera, CameraProperties cameraProperties) {
        Camera.Parameters params = camera.getParameters();
        int [] range = new int[2];
        params.getPreviewFpsRange(range);

        findSupportedFrameRate(range, cameraProperties);
    }

    private static void findSupportedFocusMode(Camera camera, CameraProperties cameraProperties) {
        Log.d(TAG, "find supported focus mode");
        Camera.Parameters params = camera.getParameters();
        if (params != null) {
            boolean continuousVideoFocusSupported = false;
            boolean autoFocusModeSupported = false;
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null) {
                for (String focus : focusModes) {
                    if (focus.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        cameraProperties.mSmoothFocusMode = focus;
                        continuousVideoFocusSupported = true;
                        break;
                    }
                    if (focus.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        autoFocusModeSupported = true;
                    }
                }
                if (!continuousVideoFocusSupported && autoFocusModeSupported) {
                    cameraProperties.mSmoothFocusMode = Camera.Parameters.FOCUS_MODE_AUTO;
                }
            }
        }
        Log.d(TAG, "find supported focus mode done");
    }

    public static boolean isFrontCamera(int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        try {
            Camera.getCameraInfo(cameraId, cameraInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    public static CameraProperties initCameraProperties(Camera camera, int cameraId) {
        Log.d(TAG, "Configuring camera " + cameraId + " for first time");
        CameraProperties cameraProperties = new CameraProperties();

        configureGenericCameraConfig(cameraId, cameraProperties);
        if (findOptimalPreviewSize(camera, cameraProperties) == null) {
            return null;
        }

        configurePictureProperties(camera, cameraProperties);

        findSupportedFocusMode(camera, cameraProperties);
//        findSupportedFrameRate(camera, cameraProperties);

        mCameraProperties.put(cameraId, cameraProperties);
        storeCameraPropertiesToSharedPreferences(cameraId);
        return cameraProperties;
    }

    private static List<Size> getSizeList(List<Camera.Size> sizes) {
        List<Size> sizeList = new ArrayList<>(sizes.size());
        for (Camera.Size size : sizes) {
            sizeList.add(new Size(size.width, size.height));
        }
        return sizeList;
    }

    private static void configurePictureProperties(Camera camera, CameraProperties cameraProperties) {
        try {
            Camera.Parameters params = camera.getParameters();
            List<Size> pictureSizes = getSizeList(params.getSupportedPictureSizes());

            configurePictureProperties(pictureSizes, cameraProperties);

            params.setPictureSize(cameraProperties.mPictureWidth, cameraProperties.mPictureHeight);
            camera.setParameters(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static CameraProperties configureGenericCameraConfig(int cameraId, CameraProperties cameraProperties) {

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

//        cameraProperties.mIsFrontCamera = ( (Build.VERSION.SDK_INT >= 23) ? (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) : (cameraInfo.facing == CameraCharacteristics.LENS_FACING_FRONT));
        cameraProperties.mIsFrontCamera = (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);

        cameraProperties.mCameraId = cameraId;
        return cameraProperties;
    }

    /**
     * Get the optimal preview size for camera
     */
    public static CameraProperties findOptimalPreviewSize(Camera camera, CameraProperties cameraProperties) {
        if (camera == null)
            return null;

        try {
            Camera.Parameters params = camera.getParameters();

            Camera.Size prefCameraSize = params.getPreferredPreviewSizeForVideo();
            Size prefSize;
            if (prefCameraSize != null) {
                prefSize = new Size(prefCameraSize.width, prefCameraSize.height);
            } else {
                prefSize = new Size(1280, 720);
            }
            return findOptimalPreviewSize(prefSize, getSizeList(params.getSupportedPreviewSizes()), cameraProperties);
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
        return null;
    }
}
