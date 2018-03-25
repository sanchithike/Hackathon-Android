package com.roposo.creation.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.view.Surface;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.EventTrackUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by bajaj on 29/05/17.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraControl2 extends CameraPreview {
    private static final String TAG = "CameraControl2";

    static {
        final CameraManager manager = (CameraManager) ContextHelper.applicationContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String [] cameraIds = manager.getCameraIdList();
            numberOfCameras = cameraIds.length;
            if (numberOfCameras == 0) {
                EventTrackUtil.logDebug("NoCamerasFound", "QueryCameras", "CameraControl2", null, 4);
            }
            for (int i = 0; i < cameraIds.length; i++) {
                String cameraId = cameraIds[i];
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == null) {
                    EventTrackUtil.logDebug("InvalidLensFacingInfo", "QueryCamera", "CameraControl2", null, 4);
                    continue;

                }
                if (lensFacing.equals(CameraMetadata.LENS_FACING_FRONT)) {
                    if (CAMERA_FRONT < 0) {
                        CAMERA_FRONT = i;
                    }
                } else if (lensFacing.equals(CameraMetadata.LENS_FACING_BACK)) {
                    if (CAMERA_BACK < 0) {
                        CAMERA_BACK = i;
                    }
                }
            }
            if (numberOfCameras > 0 && CAMERA_BACK < 0) {
                CAMERA_BACK = 0;
            }
            mCameraId = (DEFAULT_CAMERA_FRONT && CAMERA_FRONT >= 0) ? CAMERA_FRONT : CAMERA_BACK;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for preview.
     */
    private CameraCaptureSession mCameraSession;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private static Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest.Builder mCaptureBuilder;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private static HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private static Handler mBackgroundHandler;
    private CameraPictureCallback mPictureCallback;
    private boolean mCameraSessionActive;
    private boolean[] mCameraRequested = new boolean[numberOfCameras];
    private boolean mManualFocusEngaged = false;
    private Integer afModeBeforeManualFocus;

    public CameraControl2(Context context) {
        super(context);
        mCameraSettings = CameraSettings2.getInstance(mContext);
        startBackgroundThread();
    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        if (mBackgroundThread != null && mBackgroundThread.isAlive()) return;
        mBackgroundThread = new HandlerThread("CameraBackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (1 == 1) return;
        if (mBackgroundThread == null) return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                mPictureCallback.onCameraCaptured(bytes, null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    public void takePicture(final CameraPictureCallback pictureCallback) {
        super.takePicture(pictureCallback);
//        setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
        if (null == mCameraDevice) {
            return;
        }
        try {
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            mCaptureBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON); // required for raw

            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, mFlashEnabled ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);

            mCaptureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CameraMetadata.CONTROL_EFFECT_MODE_OFF);

            mCameraSession.capture(mCaptureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    mPictureCallback = pictureCallback;
                    mPictureCallback.onShutter();
//                    mCaptureResult = result;
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openCamera() {
        final Activity activity = (ContextHelper.getContext() instanceof Activity) ? (Activity) ContextHelper.getContext() : null;
        if (null == activity || activity.isFinishing()) {
            return;
//            return null;
        }

        //sometimes openCamera gets called multiple times, so lets not get stuck in our semaphore lock
        if (mCameraDevice != null && isCameraAvailable) {
            return;
//            return null;
        }

        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            Log.d(TAG, "Opening camera");
            //mCameraOpenCloseLock.acquire();
//            mCameraOpenCloseLock.tryAcquire(500, TimeUnit.MILLISECONDS);

            String[] cameraList = manager.getCameraIdList();

            String cameraId = cameraList[mCameraId];

            mCameraRequested[mCameraId] = true;
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
            //mCameraOpenCloseLock.release();
        }
//        return null;
    }

    private int getCameraIndexFromId(String cameraId) {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraList = manager.getCameraIdList();
            for (int i = 0; i < cameraList.length; i++) {
                if (cameraList[i].equals(cameraId)) {
                    return i;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            isCameraAvailable = true;
            setCameraRequestedOnUI(mCameraDevice.getId(), false);
            mCameraProps = CameraSettings.queryCameraProperties(mCameraId, false);

            // Should happen only first time for each camera in a lifetime.
            // Once done, we store camera properties in SharedPreferences and retreive them later.
            if (mCameraProps == null) {
                mCameraProps = CameraSettings2.initCameraProperties(mCameraDevice.getId(), mCameraId);
            }
            configureSessionProperties();
//            startPreview();

            mCameraStateCallback.onCameraOpened();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            setCameraRequestedOnUI(mCameraDevice.getId(), false);
            cameraDevice.close();
            mCameraDevice = null;
            isCameraAvailable = false;

            mCameraStateCallback.onCameraDisconnected();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            setCameraRequestedOnUI(mCameraDevice.getId(), false);
            cameraDevice.close();
            mCameraDevice = null;
            isCameraAvailable = false;

            Log.e(TAG, "CameraDevice.StateCallback onCameraError() " + error);

            mCameraStateCallback.onCameraError(error);
        }
    };

    private void configureSessionProperties() {
        // We don't persist these properties across sessions. So, these need to be queried in each session.
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            mCameraProps.isCameraCapableEnough = isCamera2CapableEnough();
            mCameraProps.allowedMeteringAreas = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        } catch (Exception e) {
        }
    }

    private void refreshPreview() {
        if (mCameraSession == null) return;
        if (!mCameraProps.isCameraCapableEnough) return;
        try {
            if (mCameraSessionActive) {
                mCameraSession.stopRepeating();
            }
        } catch (CameraAccessException|IllegalStateException|IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resumePreview() {
        // Not a mistake. refreshPreview essentially does the same thing.
        startPreview();
    }

    @Override
    public void pausePreview() {
        super.pausePreview();
        if (mCameraSession == null) return;
        try {
            if (mCameraSessionActive) {
                mCameraSession.stopRepeating();
                mCameraSession.abortCaptures();
                Log.d(TAG, "Closing camera session");
//                mCameraSession.close();
                mCameraSession = null;
                mCameraSessionActive = false;
            }
        } catch (CameraAccessException|IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * close camera when not in use/pausing/leaving
     */
    public void stopPreview() {
        super.stopPreview();
        try {
            //mCameraOpenCloseLock.tryAcquire(200, TimeUnit.MILLISECONDS);
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
                isCameraAvailable = false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            //mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    public synchronized void startPreview() {

        mManualFocusEngaged = false;
        if (mCameraRequested[mCameraId]) {
            Log.d(TAG, "startPreview: camera already requested. Returning");
            return;
        }
        if (mCameraSessionActive) {
            Log.d(TAG, "startPreview: camera session active. Returning");
            return;
        }
        if (mCameraPreviewOngoing) {
            Log.d(TAG, "startPreview: camera preview already ongoing. Returning");
            return;
        }
        if (null == mCameraDevice) {
            openCamera();
            return;
        }

        if (mPreviewSurface == null) {
            Log.d(TAG, "Surface Texture not ready yet");
            return;
        }
        if (mCameraProps == null) {
            Log.d(TAG, "Cannot start camera. Camera not expected to behave properly");
            return;
        }
        mCameraRequested[mCameraId] = true;

        try {
            mPreviewSurface.setDefaultBufferSize(mCameraProps.mPreviewWidth, mCameraProps.mPreviewHeight);

            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            List<Surface> surfaces = new ArrayList<>();

            final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
            if (manager != null) {
                try {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
                    Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    displayOrientation = orientation != null ? orientation : ExifInterface.ORIENTATION_UNDEFINED;
                    Range<Integer> exposureRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                    if (exposureRange != null) {
                        int minExposureCompensation = exposureRange.getLower();
                        int maxExposureCompensation = exposureRange.getUpper();
                        float exposureCompensation = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
                        brightness = (exposureCompensation - minExposureCompensation) / (maxExposureCompensation - minExposureCompensation);
                    }
                    Float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                    if (maxZoom == null) {
                        maxZoomLevel = 1;
                    } else {
                        maxZoomLevel = maxZoom.intValue();
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }


            assert mPreviewSurface != null;

/*            ImageReader reader=ImageReader.newInstance(mCameraProps.mPictureWidth, mCameraProps.mPictureHeight, ImageFormat.JPEG, 1);
            reader.setOnImageAvailableListener(imageAvailableListener, mBackgroundHandler);*/

            Surface previewSurface = new Surface(mPreviewSurface);

            // Add both surfaces
            surfaces.add(previewSurface);
//            surfaces.add(reader.getSurface());

            // Set capture Surface
//            mCaptureBuilder.addTarget(reader.getSurface());

            // Set Preview Surface
            mPreviewBuilder.addTarget(previewSurface);

            Log.d(TAG, "createCaptureSession");
            // Start Preview session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    synchronized (CameraControl2.this) {
                        mCameraRequested[getCameraIndexFromId(mCameraDevice.getId())] = false;
                        mCameraSession = session;
                        mCameraSessionActive = true;
                    }
                    updatePreview();
                    Log.d(TAG, "onConfigured: " + session);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    synchronized (CameraControl2.this) {
                        mCameraRequested[getCameraIndexFromId(mCameraDevice.getId())] = false;
                        Log.e(TAG, "config failed: " + session);
                    }
                    AndroidUtilities.showShortToast("CaptureSession Config Failed");
                    Log.d(TAG, "onConfigureFailed: " + session);
                }

                @Override
                public void onReady(@NonNull CameraCaptureSession session) {
                    super.onReady(session);
                    synchronized (CameraControl2.this) {
                        mCameraRequested[getCameraIndexFromId(mCameraDevice.getId())] = false;
                    }
                    if (mCameraSession == null) return;
                    try {
                        mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), captureCallback, mBackgroundHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onActive(@NonNull CameraCaptureSession session) {
                    super.onActive(session);
                    onStartPreview();
                    Log.d(TAG, "onActive: " + session);
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    synchronized (CameraControl2.this) {
                        if (session.equals(mCameraSession)) {
                            mCameraSessionActive = false;
                            mCameraSession = null;
                        }
                    }
                    super.onClosed(session);
                    Log.d(TAG, "onClosed: " + session);
                }

                @Override
                public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
                    super.onSurfacePrepared(session, surface);
                }
            }, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
            mCameraRequested[mCameraId] = false;
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        mManualFocusEngaged = false;
        if (null == mCameraDevice) {
            return;
        }
        if (mPreviewBuilder == null || mCameraSession == null) {
            return;
        }
        try {

            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), captureCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            CrashlyticsWrapper.logException(e);
        }
    }

    public void destroyPreview() {
        super.destroyPreview();
        stopBackgroundThread();
    }

    private void setCameraRequestedOnUI(final String cameraId, boolean cameraRequested) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mCameraRequested[getCameraIndexFromId(cameraId)] = false;
                //mCameraOpenCloseLock.release();
            }
        });
    }

    @Override
    public boolean hasFlash() {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return false;
        }
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            return hasFlash != null && hasFlash;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void forceFlash() {
        if (mPreviewBuilder == null) return;
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        refreshPreview();
    }

    // enable or disable flash mode
    @Override
    public void enableFlash(boolean enable) {
        if (mPreviewBuilder == null) return;
        super.enableFlash(enable);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        mPreviewBuilder.set(CaptureRequest.FLASH_MODE, enable ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
        refreshPreview();
    }

    @Override
    public void setFocusMode(@FocusMode String focusMode) {
        if (mPreviewBuilder == null || mCameraSession == null) {
            return;
        }
        Log.d(TAG, "setFocusMode : " + focusMode);
        int afMode;
        switch (focusMode) {
            case FOCUS_MODE_AUTO:
                afMode = CaptureRequest.CONTROL_AF_MODE_AUTO;
                break;
            case FOCUS_MODE_FIXED:
                afMode = CaptureRequest.CONTROL_AF_MODE_EDOF;
                break;
            case FOCUS_MODE_PICTURE:
                afMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                break;
            case FOCUS_MODE_VIDEO:
                afMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                break;
            default:
                afMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
        }
        Integer currentAfMode = mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE);
        if (currentAfMode != null && currentAfMode == afMode) {
            return;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        refreshPreview();
        try {
            mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), captureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            updatePreview();
        }
    }

    @Override
    public void fixedFocus() {
        if (mPreviewBuilder == null) return;
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_EDOF);
        refreshPreview();
    }

    @Override
    public void manualFocus(@FloatRange(from = 0.0, to = 1.0) float x,
                            @FloatRange(from = 0.0, to = 1.0) float y) {

        if (mPreviewBuilder == null) return;


        if (!mCameraProps.isCameraCapableEnough || mCameraProps.allowedMeteringAreas < 1 || mManualFocusEngaged) {
            return;
        }

        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return;
        }

        afModeBeforeManualFocus = mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE);

        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        cancelCurrentFocus();

        prepareFocus(x, y, manager);

    }

    private void prepareFocus(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y, CameraManager manager) {
        if (mPreviewBuilder == null) {
            resetPreviewBuilderAfterManualFocus();
            return;
        }
        try {

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (sensorArraySize == null) {
                return;
            }


            Pair<Float, Float> pair = getOrientedFocusPoints(x, y);
            x = pair.first * sensorArraySize.width();
            y = pair.second * sensorArraySize.height();

            int focusRectSize = Math.min(sensorArraySize.width(), sensorArraySize.height()) / 20;

            Rect focusArea = getFocusRect((int)x, (int)y, focusRectSize, focusRectSize, sensorArraySize);


            // TODO: 20/01/18 Don't remove commented lines. Under Review

/*
            int left = focusArea.left;
            int right = focusArea.right;
            focusArea.left = sensorArraySize.right - right;
            focusArea.right = sensorArraySize.right - left;
*/


//            refreshPreview();

            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    new MeteringRectangle[] { new MeteringRectangle(focusArea, MeteringRectangle.METERING_WEIGHT_MAX - 1) });

//            mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), captureCallback, mBackgroundHandler);

            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);

//            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,
//                    new MeteringRectangle[] { new MeteringRectangle(focusArea, MeteringRectangle.METERING_WEIGHT_MAX / 2) });

//            refreshPreview();

        } catch (CameraAccessException e) {
            e.printStackTrace();
            resetPreviewBuilderAfterManualFocus();
        }
    }

    private void cancelCurrentFocus() {

        try {

            refreshPreview();
//            mCameraSession.stopRepeating();

            //cancel any existing AF trigger (repeated touches, etc.)
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);

            if (mCameraSession == null) {
                resetPreviewBuilderAfterManualFocus();
            } else {
                mCameraSession.capture(mPreviewBuilder.build(), focusCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            resetPreviewBuilderAfterManualFocus();
        }
    }

    private void resetPreviewBuilderAfterManualFocus() {
        mManualFocusEngaged = false;
        if (mPreviewBuilder == null) return;

        refreshPreview();
        if (afModeBeforeManualFocus == null) {
            afModeBeforeManualFocus = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, afModeBeforeManualFocus);
        if (mCameraSession != null) {
            try {
                mCameraSession.setRepeatingRequest(mPreviewBuilder.build(), captureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraCaptureSession.CaptureCallback focusCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Integer afTrigger = request.get(CaptureRequest.CONTROL_AF_TRIGGER);

            Log.d(TAG, "mode: " + afTrigger);


            if (afTrigger == null || afTrigger == CaptureRequest.CONTROL_AF_TRIGGER_START) {
                Log.d(TAG, "mode: start");
                //the focus trigger is complete -
                //resume repeating (preview surface will get frames), clear AF trigger
//                mManualFocusEngaged = false;
//                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
//                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                resetPreviewBuilderAfterManualFocus();
            } else if (afTrigger == CaptureRequest.CONTROL_AF_TRIGGER_CANCEL) {
                Log.d(TAG, "mode: off");
                startManualFocus();
            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "Manual AF failure: " + failure);
            mManualFocusEngaged = false;
            resetPreviewBuilderAfterManualFocus();
        }
    };

    private void startManualFocus() {
        if (!mCameraSessionActive) return;
        try {
            mCameraSession.capture(mPreviewBuilder.build(), focusCallback, mBackgroundHandler);
            mManualFocusEngaged = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            updatePreview();
        }
    }


    @Override
    public void setBrightness(@FloatRange(from = 0F, to = 1F) float value) {
        if (value > 1F || value < 0F) return;

        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return;
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Range<Integer> exposureRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            if (exposureRange == null) {
                return;
            }
            int minExposureCompensation = exposureRange.getLower();
            int maxExposureCompensation = exposureRange.getUpper();
            int normalizedVal = (int) (minExposureCompensation + (maxExposureCompensation - minExposureCompensation) * value);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, normalizedVal);
            super.setBrightness(value);
            refreshPreview();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    public int getCaptureOrientation() {
        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        int orientationVal = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Integer rotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (rotation == null) return orientationVal;
            return getCaptureOrientation(rotation);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return orientationVal;
    }

    public static boolean isCamera2CapableEnough() {
        boolean isCameraCapableEnough = false;

        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String [] cameraIds = manager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer supportedHardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (supportedHardwareLevel != null) {
                    if (supportedHardwareLevel.equals(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) || supportedHardwareLevel.equals(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)) {
                        isCameraCapableEnough = true;
                    } else {
                        isCameraCapableEnough = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isCameraCapableEnough;
    }

    @Override
    public boolean zoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        if(zoom < 0F || maxZoomLevel <= 1 || zoom > 1F || mPreviewBuilder == null || presentZoomLevel == zoom) {
            return false;
        }


        final CameraManager manager = (CameraManager) ContextHelper.getContext().getSystemService(Context.CAMERA_SERVICE);

        if (manager == null) return false;

        try {
            presentZoomLevel = zoom;

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());

            float newZoomLevel = (float) (Math.pow(zoom, 2) * (maxZoomLevel - 1) + 1F);
            Log.d(TAG, "zoom: " + zoom + "  normalised: " + newZoomLevel + " max: " + maxZoomLevel);

            Rect zoomRect = cropRegionForZoom(characteristics, newZoomLevel);

            mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

            updatePreview();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Rect cropRegionForZoom(@NonNull CameraCharacteristics cc, float zoomTo) {

        Rect sensor = cc.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        int sensorCenterX = sensor.width()/2;
        int sensorCenterY = sensor.height()/2;
        int zoomAreaHalfWidth = (int)(0.5f * sensor.width() / zoomTo);
        int zoomAreaHalfHeight = (int)(0.5f * sensor.height() / zoomTo);

        return(new Rect(
                sensorCenterX - zoomAreaHalfWidth,
                sensorCenterY - zoomAreaHalfHeight,
                sensorCenterX + zoomAreaHalfWidth,
                sensorCenterY + zoomAreaHalfHeight));
    }

    @Override
    public void smoothZoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        zoomTo(zoom);
    }
}
