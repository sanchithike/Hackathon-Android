package com.roposo.creation.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.roposo.core.events.EventManager;
import com.roposo.core.events.EventTypes;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.EventTrackUtil;
import com.roposo.core.util.MyLogger;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.RGLSurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

import kotlin.ranges.RangesKt;

/**
 * Created by bajaj on 21/07/16.
 */
@SuppressWarnings({"deprecation", "PointlessBooleanExpression"})
public abstract class CameraPreview implements RGLSurfaceView.SurfaceEventListener {
    static final String TAG = "CameraPreview";
    private static final boolean VERBOSE = false || CameraUtils.VERBOSE;
    public static ImageSource sImageSource = new ImageSource(true);

    Context mContext;

    boolean mIsPreviewRunning;
    boolean isCameraAvailable;

    private static final String CAMERA_OPEN = "open";
    private static final String CAMERA_CLOSE = "close";
    private static final String CAMERA_CONFIGURE = "configure";

    public static boolean cameraCompat;

    public static boolean isCameraCompat() {
        return cameraCompat;
    }

    private static EventManager.EventManagerDelegate eventManagerDelegate = new EventManager.EventManagerDelegate() {
        @Override
        public boolean didReceivedEvent(int id, Object... args) {
            updateCameraCompat();
            return false;
        }
    };
    static {
        updateCameraCompat();
        EventManager.getInstance().addObserver(eventManagerDelegate, EventTypes.creationConfigUpdated);
    }

    boolean mFlashEnabled;
    float brightness;

    public static void updateCameraCompat() {
        cameraCompat = false; //(UIPref.getInstance().mSubCreationMode == UIPref.COMPAT);
        EventManager.getInstance().removeObserver(eventManagerDelegate, EventTypes.creationConfigUpdated);
    }

    // Camera ids start from 0
    // Names can be misleading, as there is no guarantee for 0th camera to be the back camera.
    // So we don't use it for any calculations etc here and elsewhere.
    static int CAMERA_BACK = -1;
    static int CAMERA_FRONT = -1;

    // TODO sahil :: this static variable is causing bug :: use storyObject.isFrontCamera
    // state gets saved across session in creation
    protected static int mCameraId;

    public CameraSettings.CameraProperties mCameraProps;

    SurfaceTexture mPreviewSurface;

    private String mSmoothFocusMode;

    boolean mCameraPreviewOngoing;
    private CameraCaptureCallback mCameraCaptureCallback;
    private CameraPictureCallback mCameraPictureCallback;

    CameraStateCallback mCameraStateCallback;

    int mCaptureQuality = 100;
    SurfaceHolder mHolder;

    // New creation never uses this. Only old creation needs to relayout the View. New creation, instead scales the content.
    SurfaceView mSurfaceView;

    public static int numberOfCameras;

    static final boolean DEFAULT_CAMERA_FRONT = false;

    public static final int CAPTURE_MODE_PICTURE = 1;
    public static final int CAPTURE_MODE_ONESHOT = 2;
    public static final int CAPTURE_MODE_RENDER_OFFSCREEN = 3;

    public static int CAPTURE_MODE_DEFAULT = CAPTURE_MODE_PICTURE;

    public static final String FOCUS_MODE_PICTURE = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
    public static final String FOCUS_MODE_VIDEO = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
    public static final String FOCUS_MODE_AUTO = Camera.Parameters.FOCUS_MODE_AUTO;
    public static final String FOCUS_MODE_FIXED = Camera.Parameters.FOCUS_MODE_FIXED;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef ({FOCUS_MODE_PICTURE, FOCUS_MODE_VIDEO, FOCUS_MODE_AUTO, FOCUS_MODE_FIXED})
    @interface FocusMode {}

    static final boolean FLIP_FRONT_CAPTURE = false;

    Camera.PreviewCallback mOneShotPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            int format = parameters.getPreviewFormat();
            byte[] byteArray = null;
            //YUV formats require more conversion
            if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {

                int w = parameters.getPreviewSize().width;
                int h = parameters.getPreviewSize().height;
                Log.d(TAG, "preview width : " + w + " height " + h);

                YuvImage yuv_image = new YuvImage(data, format, w, h, null);

                Rect rect = new Rect(0, 0, w, h);
                ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                yuv_image.compressToJpeg(rect, mCaptureQuality, output_stream);
                byteArray = output_stream.toByteArray();

                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

                output_stream.reset();

                float scaleFactor = 1.0f;
                Matrix matrix = new Matrix();
                matrix.postScale(1.0f, 1.0f);
                if (mCameraProps.mIsFrontCamera) {
                    // Front camera is flipped as well as landscape
                    matrix.postRotate(-90);
                    matrix.postScale(-1, 1);
                } else {
                    matrix.postRotate(90);
                }
                matrix.postScale(scaleFactor, scaleFactor);

                Bitmap outputBitmap;

                outputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

                // TODO send the correct size
                mCameraCaptureCallback.onCameraCapture(outputBitmap, null, ExifInterface.ORIENTATION_NORMAL, 0);

                mCameraCaptureCallback = null;

                bitmap.recycle();

            }
        }
    };


    BaseCameraSettings mCameraSettings;

    public CameraPreview(Context context) {
        mContext = context;
        destroyPreview();
    }

    public abstract void openCamera();

    static void findFrontCamera() {
        for(int i = 0; i < numberOfCameras; i++) {
            if(CameraSettings.isFrontCamera(i)) {
                if (CAMERA_FRONT < 0) {
                    CAMERA_FRONT = i;
                }
            } else if (CAMERA_BACK < 0) {
                CAMERA_BACK = i;
            }
        }
    }

    private void trackCamera(boolean isCameraAvailable, String operation, boolean isCompat, Map<String, String> bundle) {
        Map<String, String> map = new HashMap<>();
        map.put("nc", String.valueOf(true));
        map.put("subcrm", String.valueOf(isCompat));
        map.put("camval", String.valueOf(isCameraAvailable));
        if (bundle != null) {
            map.putAll(bundle);
        }
        EventTrackUtil.logDebug("CameraPreview", operation, "CameraPreview", map, -1);
    }

    private void trackCamera(boolean isCameraAvailable, String operation, boolean isCompat, boolean opStatus, Map<String, String> bundle) {
        Map<String, String> map = new HashMap<>();
        map.put("nc", String.valueOf(true));
        map.put("subcrm", String.valueOf(isCompat));
        map.put("camval", String.valueOf(isCameraAvailable));
        map.put("camstatus", String.valueOf(!opStatus));
        if (bundle != null) {
            map.putAll(bundle);
        }
        EventTrackUtil.logDebug("CameraPreview", operation, "CameraPreview", map, -1);
    }

    public boolean swapCamera() {
        if(numberOfCameras <= 1) return false;
        mCameraId = ++mCameraId % numberOfCameras;
        return true;
    }

    public void swapAndOpenCamera() {
        if(!swapCamera()) {
            MyLogger.w(TAG, "Camera swap not possible");
            return;
        }
        startPreview();
    }

    public void startPreview() {
        HashMap<String, String> map = new HashMap<>(2);
        map.put("operation", "startPreview");
        map.put("surfacetexture", String.valueOf(mPreviewSurface));
        trackCamera(isCameraAvailable, CAMERA_CONFIGURE, CameraPreview.isCameraCompat(), mIsPreviewRunning, map);
    }

    final void onStartPreview() {
        mCameraPreviewOngoing = true;

        HashMap<String, String> map = new HashMap<>(2);
        map.put("operation", "startPreview");
        map.put("status", "success");
        trackCamera(isCameraAvailable, CAMERA_CONFIGURE, CameraPreview.isCameraCompat(), mIsPreviewRunning, map);
    }

    public void resumePreview() {

    }

    void startPreviewCompat() {

    }

    public void pausePreview() {
        mCameraPreviewOngoing = false;
    }

    public void stopPreview() {
        trackCamera(isCameraAvailable, CAMERA_CLOSE, cameraCompat, null);
    }

    public void destroyPreview() {
        mPreviewSurface = null;
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    public void handleSetSurfaceTexture(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "handleSetSurfaceTexture");

        mPreviewSurface = surfaceTexture;
    }

    public int getDisplayOrientation() {
        return displayOrientation;
    }

    int displayOrientation = 0;
    boolean isCameraInverted;

    public void setCameraDisplayOrientation() {

    }

    public abstract boolean hasFlash();

    public boolean isFlashEnabled() {
        return mFlashEnabled;
    }

    public abstract void forceFlash();

    // enable or disable flash mode
    public void enableFlash(boolean enable) {
        mFlashEnabled = enable;
    }

    public void autoFocus() {
        Log.d(TAG, "Auto focus :: mode continuous video");
        setFocusMode(FOCUS_MODE_VIDEO);
    }

    public abstract void setFocusMode(@FocusMode String focusMode);

    public void setBrightness(@FloatRange(from = 0F, to = 1F) float value){
        brightness = value;
    }

    public float getBrightness() {
        return brightness;
    }

    public void fixedFocus() {

    }

    public abstract void manualFocus(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y);

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            mCameraPictureCallback.onShutter();
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken:: jpegCallback");
            mCameraPictureCallback.onCameraCaptured(data, null);
        }
    };

 /*   public void prepareTakePicture(CameraPictureCallback pictureCallback) {
        mCameraPictureCallback = pictureCallback;

//        setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
        checkAutoFocus(FOCUS_MODE_PICTURE, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success) {
                    cancelAutoFocus();
                    takePicture(mCameraPictureCallback);
                } else {
                    manualFocus(this, null, 0);
                }
            }
        });
    }*/

    // In case current focus mode is continuous picture/video, cancelling autofocus stops the focus
    // In current focus mode is autofocus, cancelling autofocus stops any focus in progress as well.
    // Doesn't claim any f**king exceptions that could be thrown. Let's hope that's the case indeed.
    void cancelAutoFocus() {
    }

    void checkAutoFocus(final String focusMode, Camera.AutoFocusCallback focusCallback) {
    }

    public void takePicture(final CameraPictureCallback pictureCallback) {
        mCameraPictureCallback = pictureCallback;
    }

    public void takePictureUsingOneshotcallback(CameraCaptureCallback captureCallback) {
        mCameraCaptureCallback = captureCallback;
    }

    float presentZoomLevel = 0F;
    int maxZoomLevel = 1;

    public CameraSettings.CameraProperties getCameraProperties() {
        return mCameraProps;
    }

    public void setPreviewDisplay (SurfaceHolder holder) throws IOException {
    }

    public void unlock() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!cameraCompat) return;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(!cameraCompat) return;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(!cameraCompat) return;
        mHolder = holder;
        pausePreview();
        startPreviewCompat();
    }

    // Set camera preview size and rotation of the preview
    public void setCameraDisplaySizeAndOrientationCompat() {
        HashMap<String, String> map = new HashMap<>(2);
        map.put("operation", "setCameraDisplaySizeAndOrientationCompat");
        map.put("holder", String.valueOf(mHolder));
        trackCamera(isCameraAvailable, CAMERA_CONFIGURE, cameraCompat, map);
    }

    @Deprecated
    public void setPreview(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public abstract boolean zoomTo(@FloatRange(from = 0F, to = 1F) float zoom);

    public abstract void smoothZoomTo(@FloatRange(from = 0F, to = 1F) float zoom);

    public boolean isCameraAvailable() {
        return isCameraAvailable;
    }

    public void setCameraStateCallback(CameraStateCallback cameraStateCallback) {
        mCameraStateCallback = cameraStateCallback;
    }

    public boolean isPreviewOngoing() {
        return mCameraPreviewOngoing;
    }

    public interface CameraPictureCallback {
        void onCameraCaptured(byte[] data, String path);

        void onShutter();
    }

    public interface CameraCaptureCallback {
        void onShutter();
        void onCameraCapture(Bitmap bitmap, String path, int orientation, long maxFileSizeBytes);
    }

    @NonNull
    protected Pair<Float, Float> getOrientedFocusPoints(@FloatRange(from = 0.0, to = 1.0) float x,
                                                        @FloatRange(from = 0.0, to = 1.0) float y) {
        if (displayOrientation != 0) {

            double radians = Math.toRadians(displayOrientation);
            double sinX = Math.sin(radians);
            double cosX = Math.cos(radians);

            float tempX = x - 0.5F;
            float tempY = y - 0.5F;

            x = (float) (tempX * cosX + tempY * sinX) + 0.5F;
            y = (float) (-tempX * sinX + tempY * cosX) + 0.5F;
        }

        if(isCameraInverted) {
            x = 1F - x;
            y = 1F - y;
        }

        return new Pair<>(x, y);
    }

    @NonNull
    protected Rect getFocusRect(int centerX, int centerY, int width, int height, @NonNull Rect bounds) {
        width = RangesKt.coerceIn(width, 0, bounds.width());
        height = RangesKt.coerceIn(height, 0, bounds.height());

        int left = centerX - (width / 2);
        int top = centerY - (height / 2);
        int right = centerX + (width / 2);
        int bottom = centerY + (height / 2);

        if (left < bounds.left) {
            left = bounds.left;
            right = left + width;
        }

        if (top < bounds.top) {
            top = bounds.top;
            bottom = top + height;
        }

        if (right > bounds.right) {
            right = bounds.right;
            left = right - width;
        }

        if (bottom > bounds.bottom) {
            bottom = bounds.bottom;
            top = bottom - height;
        }

        return new Rect(left, top, right, bottom);
    }

    public RectF getTransformedCordCompat(int x, int y) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        int screenWidth = AndroidUtilities.displayMetrics.widthPixels;
        int screenHeight =  AndroidUtilities.displayMetrics.heightPixels;

        float yRelative = (layoutParams.height - screenHeight) / (2);
        float xRelative = (layoutParams.width - screenWidth) / (2);


        return new RectF((x + xRelative) / layoutParams.width, (y + yRelative) / layoutParams.height, 4.0f, 4.0f);
    }

    public abstract int getCaptureOrientation();

    int getCaptureOrientation(int rotation) {
        boolean frontFacing = mCameraProps.isFrontCamera();

        int orientationVal = AndroidUtilities.getOrientationFromRotation(rotation);
        if (frontFacing && !FLIP_FRONT_CAPTURE) {
            switch (orientationVal) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientationVal = isCameraInverted ? AndroidUtilities.CH_ORIENTATION_TRANSPOSE : ExifInterface.ORIENTATION_TRANSPOSE;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientationVal = ExifInterface.ORIENTATION_FLIP_VERTICAL;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientationVal = isCameraInverted ? AndroidUtilities.CH_ORIENTATION_TRANSVERSE : ExifInterface.ORIENTATION_TRANSVERSE;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    orientationVal = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
                    break;
            }
        }
        return orientationVal;
    }
}