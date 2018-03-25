package com.roposo.creation.camera;


/**
 * Created by Muddassir on 24/02/2016.
 * For custom camera
 * A basic Camera preview class
 */

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.MyLogger;
import com.roposo.creation.R;

import java.util.List;

public class CameraPreviewCompat extends SurfaceView implements SurfaceHolder.Callback {
    public static final int WIDTH_BACK = 1280;
    public static final int HEIGHT_BACK = 720;
    public static final int WIDTH_FRONT = 640;
    public static final int HEIGHT_FRONT = 480;
    private final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewRunning;
    private int camId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int displayOrientation;
    private Camera.Size preferredVideoSize;


    private float defaultPreferredAspectRatio = (float) 16 / 9;
    private int maxPreviewWidth = 1280;
    //  960 instead of 720, to handle 4:3 aspect ratio, in which case the preview size will be 1280x960
    private int maxPreviewHeight = 960;

    private int mFacingMaterial = CameraCharacteristics.LENS_FACING_BACK;

    public CameraPreviewCompat(Context context) {
        super(context);
    }

    public CameraPreviewCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreviewCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Initialize the preview with camera
     *
     * @param context with which preview is to be initialized
     * @param camera  to be attached
     */
    public CameraPreviewCompat(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    /**
     * Configure the camera and set the SurfaceHolder
     *
     * @param holder holder to be allotted to camera
     */
    private void configureCamera(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startFaceDetection();
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    // Set auto focus
    private void setAutoFocus() {
        try {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    try {
                        Parameters params = camera.getParameters();
                        if (params.getFocusMode() != null) {
                            /*if (!params.getFocusMode().equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                                params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                            }else*/
                            if (!params.getFocusMode().equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                            }
                        }
                        camera.setParameters(params);
                    } catch (Exception e) {
                        Toast.makeText(ContextHelper.getContext(), R.string.auto_focus_not_available, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(ContextHelper.getContext(), R.string.auto_focus_not_available, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        configureCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mHolder = holder;
        if (isPreviewRunning) {
            mCamera.stopPreview();
            isPreviewRunning = false;
        }
        setCameraDisplaySizeAndOrientation();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        this.getHolder().removeCallback(this);
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreviewRunning = false;
            mCamera.release();
            mCamera = null;
        }
    }

    // enable or disable flash mode
    public void enableFlash(boolean enable) {
        try {
            if (mCamera != null) {
                Parameters params = mCamera.getParameters();
                if (enable) {
                    if (params.getFlashMode() != null) {
                        if (!params.getFlashMode().equals(Parameters.FLASH_MODE_TORCH)) {
                            params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        }
                    }
                } else {
                    if (params.getFlashMode() != null) {
                        if (!params.getFlashMode().equals(Parameters.FLASH_MODE_OFF)) {
                            params.setFlashMode(Parameters.FLASH_MODE_OFF);
                        }
                    }
                }
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            Toast.makeText(ContextHelper.getContext(), R.string.flash_is_not_available, Toast.LENGTH_SHORT).show();
        }
    }


    // switch camera preview
    public Camera openFrontFacingCamera() {
        int numberOfCamera = Camera.getNumberOfCameras();
        if (numberOfCamera > 1) {
            if (camId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                mFacingMaterial = CameraCharacteristics.LENS_FACING_FRONT;
            } else if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFacingMaterial = CameraCharacteristics.LENS_FACING_BACK;
                camId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }

            try {
                mCamera.stopPreview();
                isPreviewRunning = false;
                mCamera.release();
                mCamera = Camera.open(camId);
                configureCamera(mHolder);
                setCameraDisplaySizeAndOrientation();
            } catch (RuntimeException e) {
                CrashlyticsWrapper.logException(e);
            }
        }
        return mCamera;
    }

    // Set camera preview size and rotation of the preview
    private void setCameraDisplaySizeAndOrientation() {
        setCameraDisplayOrientation();
        try {
            Parameters p = mCamera.getParameters();
            if (p != null) {
                cameraSetup(p, AndroidUtilities.displayMetrics.widthPixels, AndroidUtilities.displayMetrics.heightPixels);
                mCamera.setParameters(p);

                //cameraSetup(p, width, height);
                mCamera.setPreviewDisplay(mHolder);

                // always start preview after setting up parameters
                mCamera.startPreview();
                isPreviewRunning = true;

                // always set auto focus after starting preview
                setAutoFocus();
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
    }

    /**
     * Get the optimal preview size for camera
     *
     * @param sizes list of sizes available
     * @param w     width of the surface
     * @param h     height of the surface
     * @return optimal size
     */
    public Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null)
            return null;

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    // Set the camera display orientation
    public void setCameraDisplayOrientation() {
        if (mCamera == null) {
            return;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(camId, info);

        WindowManager winManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
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
            if (mFacingMaterial == CameraCharacteristics.LENS_FACING_FRONT) {
                displayOrientation = (info.orientation + degrees) % 360;
                displayOrientation = (360 - displayOrientation) % 360;  // compensate the mirror
            } else {
                if (info.orientation == 270) {
                    displayOrientation = (info.orientation - degrees + 360) % 360;
                } else {
                    displayOrientation = (info.orientation - degrees + 360) % 360;
                }
            }
        } else {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
        mCamera.setDisplayOrientation(displayOrientation);
    }

    // set camera to this surfaceView
    public void setmCamera(Camera mCamera) {
        this.mCamera = mCamera;
        this.getHolder().addCallback(this);
        configureCamera(mHolder);
        setCameraDisplaySizeAndOrientation();
    }

    public int getDisplayOrientation() {
        return displayOrientation;
    }

    public void setmFacingMaterial(int mFacingMaterial) {
        this.mFacingMaterial = mFacingMaterial;
    }

    public void setCamId(int camId) {
        this.camId = camId;
    }

    private void cameraSetup(Parameters p, int w, int h) {
        // set the camera parameters, including the preview size

        try {
            preferredVideoSize = p.getPreferredPreviewSizeForVideo();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            Camera.Size optimalSize = findOptimalPreviewSize(mCamera, camId); //getOptimalSize(p.getSupportedPreviewSizes(), preferredVideoSize);
            if (optimalSize != null) {
                double cameraAspectRatio = ((double) optimalSize.width) / optimalSize.height;
                p.setPreviewSize(optimalSize.width, optimalSize.height);
                if (((double) h) / w > cameraAspectRatio) {
                    lp.width = (int) (h / cameraAspectRatio);
                    lp.height = h;
                } else {
                    lp.height = (int) (w * cameraAspectRatio);
                    lp.width = w;
                    lp.topMargin = (h - lp.height) / 2;
                }
                lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

                setLayoutParams(lp);
                requestLayout();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getOptimalSize(List<Camera.Size> supportedPreviewSizes, Camera.Size preferredPreviewSizeForVideo) {
        return getOptimalSize(supportedPreviewSizes, preferredPreviewSizeForVideo.width, preferredPreviewSizeForVideo.height);
    }

    public Camera.Size getPreferredVideoSize() {
        return preferredVideoSize;
    }

    public Camera.Size findOptimalPreviewSize(Camera camera, int cameraId) {
        if (camera == null)
            return null;

        final float ASPECT_TOLERANCE = 0.05f;
        double minDiff = Float.MAX_VALUE;
        Camera.Parameters params = camera.getParameters();
        Camera.Size optimalSize = null;

        if (params != null) {
            // We can never go beyond preferred preview size which defines the max width/height.
            preferredVideoSize = params.getPreferredPreviewSizeForVideo();
            if (preferredVideoSize.width < maxPreviewWidth || preferredVideoSize.height < maxPreviewHeight) {
                maxPreviewWidth = preferredVideoSize.width;
                maxPreviewHeight = preferredVideoSize.height;
                defaultPreferredAspectRatio = ((float) maxPreviewWidth) / maxPreviewHeight;
            }
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            if (sizes != null) {
                // Try to find an size match aspect ratio and size
                for (Camera.Size size : sizes) {
                    double aspectRatio = (double) size.width / size.height;
                    float aspectDiff = (float) Math.abs(aspectRatio - defaultPreferredAspectRatio);
                    if (aspectDiff > ASPECT_TOLERANCE || size.width > maxPreviewWidth || size.height > maxPreviewHeight) {
                        MyLogger.d(TAG, "Rejecting preview size: " + size);
                        continue;
                    }
                    // Choose the size if aspect ratio diff is lesser than currently chosen one.
                    // If aspect ratio diff is not lesser, then choose the largest size closest to desired aspect ratio.
                    if ((aspectDiff < minDiff || Math.abs(aspectDiff - defaultPreferredAspectRatio)
                            < ASPECT_TOLERANCE) || ((aspectDiff == minDiff) &&
                            (optimalSize != null && size.width > optimalSize.width))) {
                        optimalSize = size;
                        minDiff = aspectDiff;
                    }
                }
            }
        }

        if (optimalSize != null) {
            preferredVideoSize = optimalSize;
        }
        return optimalSize;

    }
}
