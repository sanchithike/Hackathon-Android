package com.roposo.creation.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.GraphicUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bajaj on 01/06/17.
 */

@RequiresApi(21)
public class CameraSettings2 extends BaseCameraSettings {

    private CameraSettings2(Context context) {
        super(context);
    }

    public static BaseCameraSettings getInstance(Context context) {
        if (mCameraSettings == null) {
            mCameraSettings = new CameraSettings2(context);
        }
        return mCameraSettings;
    }

    static CameraProperties initCameraProperties(String cameraDeviceId, int cameraId) {
        Log.d(TAG, "Configuring camera " + cameraId + " for first time");
        CameraProperties cameraProperties = new CameraProperties();

        configureGenericCameraConfig(cameraDeviceId, cameraProperties);
        findOptimalPreviewSize(cameraDeviceId, cameraProperties);

        configurePictureProperties(cameraDeviceId, cameraProperties);

        findSupportedFocusMode(cameraDeviceId, cameraProperties);
//        findSupportedFrameRate(camera, cameraProperties);

        mCameraProperties.put(cameraId, cameraProperties);
        storeCameraPropertiesToSharedPreferences(cameraId);
        return cameraProperties;
    }

    private static List<GraphicUtil.Size> getSizeList(Size[] sizes) {
        List<GraphicUtil.Size> sizeList = new ArrayList<>(sizes.length);
        for (Size size : sizes) {
            sizeList.add(new GraphicUtil.Size(size.getWidth(), size.getHeight()));
        }
        return sizeList;
    }

    private static void configurePictureProperties(String cameraDeviceId, CameraProperties cameraProperties) {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraDeviceId);
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //typically these are identical

            configurePictureProperties(getSizeList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), cameraProperties);
//            chooseVideoSize(streamConfigurationMap.getOutputSizes(MediaRecorder.class));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static void findSupportedFocusMode(String cameraDeviceId, CameraProperties cameraProperties) {

    }

    private static void findOptimalPreviewSize(String cameraDeviceId, CameraProperties cameraProperties) {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraDeviceId);
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //typically these are identical

            findOptimalPreviewSize(null, getSizeList(streamConfigurationMap.getOutputSizes(SurfaceTexture.class)), cameraProperties);
//            chooseVideoSize(streamConfigurationMap.getOutputSizes(MediaRecorder.class));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static CameraProperties configureGenericCameraConfig(String cameraDeviceId, CameraProperties cameraProperties) {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraDeviceId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing == null) {
                Log.w(TAG, "Not able to query Lens facing");
                return cameraProperties;
            }
            cameraProperties.mIsFrontCamera = lensFacing.equals(CameraMetadata.LENS_FACING_FRONT);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return cameraProperties;
    }

    public static boolean isFrontCamera(String cameraDeviceId) {
        boolean result = false;
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDeviceId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing == null) return false;
            if (lensFacing.equals(CameraMetadata.LENS_FACING_FRONT)) {
                result = true;
            } else if (lensFacing.equals(CameraMetadata.LENS_FACING_BACK)) {
                result = false;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
