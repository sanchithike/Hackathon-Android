package com.roposo.creation.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.core.util.GraphicUtil.Size;

import java.util.List;

/**
 * Created by bajaj on 31/05/17.
 */

public abstract class BaseCameraSettings {
    static final String TAG = CameraSettings.class.getSimpleName();
    private static final String PREFERENCES_FILE = "videoeditor_preferences";

    private static final String CAMERA_PREFERENCES_KEY = "camerapref_v2";

    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_SIZE = "size";
    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_FPS = "fps";
    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_FPSRANGE = "fpsrange";

    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_WIDTH = "previewwidth";
    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_HEIGHT = "previewheight";
    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_ASPECT = "aspect";
    private static final String CAMERA_PREFERENCES_KEY_PREVIEW_FOCUS = "focus";

    private static final String CAMERA_PREFERENCES_KEY_PICTURE_WIDTH = "picturewidth";
    private static final String CAMERA_PREFERENCES_KEY_PICTURE_HEIGHT = "pictureheight";

    private static final String CAMERA_PREFERENCES_KEY_ID = "id";

    public static int MAX_PREVIEW_WIDTH = 1920;
    public static int MAX_PREVIEW_HEIGHT = 1440;

    public static int MAX_PICTURE_SIZE = 2800;

    private static final int PREFERRED_FPS = 30;

    public static final float DEFAULT_PREFERRED_BACK_INV_ASPECT_RATIO = (float) 16 / 9;
    public static final float DEFAULT_PREFERRED_FRONT_INV_ASPECT_RATIO = (float) 16 / 9;

    static SparseArray<CameraSettings.CameraProperties> mCameraProperties = new SparseArray<>();
    private static SharedPreferences mPreferences;

    static BaseCameraSettings mCameraSettings;

    BaseCameraSettings(Context context) {
        init(context);
    }

    public static void init(Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES_FILE, 0);
    }

    public static void configurePictureProperties(List<Size> supportedPictureSizes, CameraProperties cameraProperties) {
        Size selectedSize = null;
        Size maxSize = null;
        float PREFERRED_ASPECT_RATIO = (cameraProperties.isFrontCamera() ? DEFAULT_PREFERRED_FRONT_INV_ASPECT_RATIO : DEFAULT_PREFERRED_BACK_INV_ASPECT_RATIO);
        for (Size size : supportedPictureSizes) {
            float aspectRatio = (float) size.width / size.height;
            if (Math.abs(aspectRatio - PREFERRED_ASPECT_RATIO) < 0.05 && size.width <= MAX_PICTURE_SIZE) {
                if (maxSize == null || (size.width > maxSize.width)) {
                    maxSize = size;
                    Log.d(TAG, "current max picture size: " + maxSize.width + " x " + maxSize.height);
                }
                if (selectedSize == null || size.width > selectedSize.width) {
                    selectedSize = size;
                    Log.d(TAG, "current selected picture size: " + selectedSize.width + " x " + selectedSize.height);
                } else {
                    Log.d(TAG, "rejecting picture size: " + size.width + " x " + size.height);
                }
            } else {
                Log.d(TAG, "rejecting picture size: " + size.width + " x " + size.height);
            }
        }
        if (maxSize == null) {
            maxSize = supportedPictureSizes.get(0);
        }

        if (selectedSize == null) {
            selectedSize = maxSize;
        }
        Log.d(TAG, "final selected picture size: " + selectedSize.width + " x " + selectedSize.height);
        Log.d(TAG, "final max picture size: " + maxSize.width + " x " + maxSize.height);

        cameraProperties.mPictureWidth = selectedSize.width;
        cameraProperties.mPictureHeight = selectedSize.height;
    }

    static CameraProperties findOptimalPreviewSize(Size preferredSize, List<Size> supportedPreviewSizes, CameraProperties cameraProperties) {
        final float ASPECT_TOLERANCE = 0.05f;

        double minDiff = Float.MAX_VALUE;


        float PREFERRED_ASPECT_RATIO;

        if (preferredSize != null) {
            PREFERRED_ASPECT_RATIO = (float) preferredSize.width / preferredSize.height;
        } else {
            PREFERRED_ASPECT_RATIO = (cameraProperties.isFrontCamera() ? DEFAULT_PREFERRED_FRONT_INV_ASPECT_RATIO : DEFAULT_PREFERRED_BACK_INV_ASPECT_RATIO);
        }

        if (preferredSize == null) {
            preferredSize = new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
        }

        // We can never go beyond preferred preview size which defines the max width/height.
        Size prefSize = new Size(preferredSize.width, preferredSize.height);
        if (prefSize.width > MAX_PREVIEW_WIDTH || prefSize.height > MAX_PREVIEW_HEIGHT) {
            prefSize.width = MAX_PREVIEW_WIDTH;
            prefSize.height = MAX_PREVIEW_HEIGHT;
        }
        Size optimalSize = null;

        // Try to find an size match aspect ratio and size
        for (Size size : supportedPreviewSizes) {
            double aspectRatio = (double) size.width / size.height;
            float aspectDiff = (float) Math.abs(aspectRatio - PREFERRED_ASPECT_RATIO);
            if (aspectDiff > ASPECT_TOLERANCE || size.width > prefSize.width || size.height > prefSize.height) {
                Log.d(TAG, "Rejecting preview size: " + size.width + " x " + size.height);
                continue;
            }
            // Choose the size if aspect ratio diff is lesser than currently chosen one.
            // If aspect ratio diff is not lesser, then choose the largest size closest to desired aspect ratio.
            if ((aspectDiff < minDiff || Math.abs(aspectDiff - PREFERRED_ASPECT_RATIO) < ASPECT_TOLERANCE) || ((aspectDiff == minDiff) && (optimalSize != null && (size.width > optimalSize.width)))) {
                Log.d(TAG, "Accepting preview size: " + size.width + " x " + size.height);
                optimalSize = size;
                minDiff = aspectDiff;
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : supportedPreviewSizes) {
                if (Math.abs(size.height - prefSize.height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - prefSize.height);
                }
            }
        }
        if (optimalSize == null) {
            optimalSize = prefSize;
        }

        cameraProperties.mPreviewWidth = optimalSize.width;
        cameraProperties.mPreviewHeight = optimalSize.height;
        cameraProperties.mPreviewAspectRatio = ((float) cameraProperties.mPreviewWidth) / cameraProperties.mPreviewHeight;
        cameraProperties.mPreviewSize = cameraProperties.mPreviewWidth + "x" + cameraProperties.mPreviewHeight;

        return cameraProperties;
    }

    static void findSupportedFrameRate(int[] range, CameraProperties cameraProperties) {

        int fps = (range[0] + range[1]) / 2;
        int preferredFPS = PREFERRED_FPS * 1000;
        int fpsRange = (range[1] - range[0]) / 2;
        fpsRange = Math.min(5000, fpsRange);

        // TODO minor revise this logic before activating this function.
        while (preferredFPS < range[0] + fpsRange)

        {
            preferredFPS++;
        }

        while (preferredFPS < range[1] - fpsRange)

        {
            preferredFPS--;
        }

        cameraProperties.mPreviewFrameRate = preferredFPS;
        cameraProperties.mPreviewFPSRange = fpsRange;
        Log.d(TAG, "find supported frame rate: " + " fps: " + preferredFPS + " range: " + fpsRange);
    }

    public static class CameraProperties {

        // Preview params
        String mPreviewSize;
        int mPreviewFrameRate;
        int mPreviewFPSRange = 5000; // Camera fps params are scaled up by 1000

        public int mPreviewWidth;
        public int mPreviewHeight;

        public int mPictureWidth;
        public int mPictureHeight;

        double mPreviewAspectRatio;

        public boolean isFrontCamera() {
            return mIsFrontCamera;
        }

        // May be used to determine preview properties, say, to flip the preview/video or not.
        boolean mIsFrontCamera;

        int mCameraId;

        String mSmoothFocusMode;

        // Whether the settings have been stored in Shared Preferences
        boolean mStored;

        boolean isCameraCapableEnough;
        int allowedMeteringAreas;

        public double getPreviewAspectRatio() {
            return mPreviewAspectRatio;
        }

        @Override
        public String toString() {
            String str = "";
            str += "PreviewSize: " + mPreviewSize;
            str += "Preview Width: " + mPreviewWidth;
            str += "Preview Height: " + mPreviewHeight;
            str += "Preview FPS: " + mPreviewFrameRate;
            str += "Preview Aspect: " + mPreviewAspectRatio;
            str += "IsFront: " + mIsFrontCamera;
            str += "Stored: " + mStored;
            return str;
        }
    }

    static CameraProperties queryCameraProperties(int cameraId) {
        return queryCameraProperties(cameraId, true);
    }

    static CameraProperties queryCameraProperties(int cameraId, boolean getDefault) {
        CameraProperties cameraProperties = mCameraProperties.get(cameraId);

        if (cameraProperties == null) {
            cameraProperties = getCameraPropertiesFromSharedPreferences(cameraId, getDefault);
        }
        Log.d(TAG, "Setting camera props to " + cameraProperties);
        return cameraProperties;
    }

    private static CameraProperties getCameraPropertiesFromSharedPreferences(int cameraId, boolean getDefaultValue) {
        Log.d(TAG, "Getting camera " + cameraId + " properties from shared preferences");

        if (!mPreferences.contains(CAMERA_PREFERENCES_KEY)) {
            mPreferences.edit()
                    .clear()
                    .putString(CAMERA_PREFERENCES_KEY, CAMERA_PREFERENCES_KEY)
                    .commit();
        }
        if (!getDefaultValue && !mPreferences.contains(CAMERA_PREFERENCES_KEY + cameraId))
            return null;

        CameraProperties cameraProperties = new CameraProperties();

        cameraProperties.mPreviewSize = mPreferences.getString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_SIZE + cameraId, "640x480");
        cameraProperties.mPreviewFrameRate = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FPS + cameraId, 20);
        cameraProperties.mPreviewFPSRange = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FPSRANGE + cameraId, 0);

        cameraProperties.mPreviewAspectRatio = Double.parseDouble(mPreferences.getString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_ASPECT + cameraId, "1.3333"));
        cameraProperties.mPreviewWidth = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_WIDTH + cameraId, 1280);
        cameraProperties.mPreviewHeight = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_HEIGHT + cameraId, 720);

        cameraProperties.mPictureWidth = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PICTURE_WIDTH + cameraId, 1280);
        cameraProperties.mPictureHeight = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PICTURE_HEIGHT + cameraId, 720);

        cameraProperties.mCameraId = mPreferences.getInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_ID + cameraId, 0);
        cameraProperties.mSmoothFocusMode = mPreferences.getString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FOCUS + cameraId, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        cameraProperties.mStored = true;

        // Cache it locally for it to be directly retrievable using queryCameraProperties().
        mCameraProperties.put(cameraId, cameraProperties);

        return cameraProperties;
    }

    @SuppressLint("CommitPrefEdits")
    static void storeCameraPropertiesToSharedPreferences(int cameraId) {
        Log.d(TAG, "Storing properties for camera " + cameraId);

        SharedPreferences.Editor editor = mPreferences.edit();
        CameraProperties cameraProperties = mCameraProperties.get(cameraId);
        cameraProperties.mStored = true;

        editor.putString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_SIZE + cameraId, cameraProperties.mPreviewSize);
        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_WIDTH + cameraId, cameraProperties.mPreviewWidth);
        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_HEIGHT + cameraId, cameraProperties.mPreviewHeight);
        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FPS + cameraId, cameraProperties.mPreviewFrameRate);
        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FPSRANGE + cameraId, cameraProperties.mPreviewFPSRange);

        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PICTURE_WIDTH + cameraId, cameraProperties.mPictureWidth);
        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PICTURE_HEIGHT + cameraId, cameraProperties.mPictureHeight);

        editor.putString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_ASPECT + cameraId, Double.toString(cameraProperties.mPreviewAspectRatio));

        editor.putInt(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_ID + cameraId, cameraProperties.mCameraId);
        editor.putString(CAMERA_PREFERENCES_KEY + CAMERA_PREFERENCES_KEY_PREVIEW_FOCUS, cameraProperties.mSmoothFocusMode);
        editor.commit(); // commit instead of apply deliberate
    }
}
