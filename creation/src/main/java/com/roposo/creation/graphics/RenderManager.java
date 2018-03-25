package com.roposo.creation.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.ActionListener;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.EventTrackUtil;
import com.roposo.core.util.FileUtilities;
import com.roposo.creation.R;
import com.roposo.creation.av.AVMediaExtractor;
import com.roposo.creation.av.AVUtils;
import com.roposo.creation.av.AndroidMuxer;
import com.roposo.creation.av.AudioEncoder;
import com.roposo.creation.av.MediaExtractorCallback;
import com.roposo.creation.av.MediaFeeder;
import com.roposo.creation.av.Muxer;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.av.mediaplayer.MediaPlayer;
import com.roposo.creation.av.mediaplayer.MediaPlayerUtils;
import com.roposo.creation.av.mediaplayer.MediaPlayerUtils.MediaPlayerImpl;
import com.roposo.creation.av.mediaplayer.MediaSource;
import com.roposo.creation.av.mediaplayer.UriSource;
import com.roposo.creation.camera.BaseCameraSettings;
import com.roposo.creation.camera.CameraControl;
import com.roposo.creation.camera.CameraControl2;
import com.roposo.creation.camera.CameraPreview;
import com.roposo.creation.camera.CameraPreview.CameraCaptureCallback;
import com.roposo.creation.camera.CameraStateCallback;
import com.roposo.creation.graphics.SceneManager.SceneDescription;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.scenes.ISceneAdjustments;
import com.roposo.creation.graphics.scenes.Scene;
import com.roposo.creation.listeners.CameraEventListener;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.roposo.creation.camera.CameraPreview.CAPTURE_MODE_ONESHOT;
import static com.roposo.creation.camera.CameraPreview.CAPTURE_MODE_PICTURE;
import static com.roposo.creation.camera.CameraPreview.CAPTURE_MODE_RENDER_OFFSCREEN;
import static com.roposo.creation.camera.CameraPreview.FOCUS_MODE_PICTURE;
import static com.roposo.creation.camera.CameraPreview.FOCUS_MODE_VIDEO;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_AUDIO;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_BLANK_AUDIO;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_CAMERA;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_IMAGE;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_MIC;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_VIDEO;
import static com.roposo.creation.graphics.GraphicsConsts.RENDER_TARGET_DISPLAY;
import static com.roposo.creation.graphics.GraphicsConsts.RENDER_TARGET_IMAGE;
import static com.roposo.creation.graphics.GraphicsConsts.RENDER_TARGET_VIDEO;

/**
 * @author Sahil Bajaj on 12/10/2017.
 */

@RequiresApi(17)
public class RenderManager implements SurfaceTexture.OnFrameAvailableListener, BaseRenderer
        .ControllerInterface, MediaExtractorCallback, CameraStateCallback, MediaPlayerUtils
        .OnPreparedListener, MediaPlayerUtils.OnSeekCompleteListener, MediaPlayerUtils.OnSeekListener,
        MediaPlayerUtils.OnInfoListener, MediaPlayerUtils.OnCompletionListener, MediaPlayerUtils.OnErrorListener
        , MediaPlayerUtils.OnProgressListener, RGLSurfaceView.SurfaceEventListener, ISceneAdjustments {

    private static final String TAG = "RenderManager";

    private static final boolean VERBOSE = true || AVUtils.VERBOSE;

    private static final boolean SHOW_FRAME_LOGS = false;
    private final static boolean EXIF_ROTATION_SUPPORTED = true;
    private static final int PREFERRED_VIDEO_FPS = 30;
    private static final boolean USE_MEDIA_PLAYER = false;
    private final boolean CAPTURE_BLIT_DISPLAY = false;
    public Context mContext;
    private final int maxDuration = 500 * 60 * 1000; // Only being used by Compat mode right now.
    private AudioEncoder mAudioEncoder;
    private CameraPreview mCameraPreview;
    private Handler mUiHandler;
    private Handler mBackgroundHandler;
    private int TOTAL_TRACK_COUNT = 1;
    private boolean cameraCompat;
    private float finalZoom;
    private GLSVRenderer mDisplayRenderer;
    private VideoEncoder mVideoRenderer;
    private DummyRenderer mDummyRenderer;
    // Used when frames have to be redirected directly from MediaExtractor to Muxer
    private MediaFeeder mAudioFeeder;

    //TODO major for 2nd pass video rendering after recording.
    // Pass this somehow from Extractor/decoder   VideoConfig mSourceVideoConfig;
    private MediaFeeder mVideoFeeder;
    //AVMediaExtractor mMediaPlayer;
    private MediaPlayerImpl mMediaPlayer;
    private int mRenderTarget; //bit masks, to accommodate multiple render targets
    // There is always some kind of sharing. This flag signifies whether all renderers use display's context
    private boolean mUseDisplayContext;
    private EGLContext mSharedEGLContext;
    // Depending on there's a camera/video source involved, mSourceMedia type will change.
    private int mSourceMedia = MEDIA_TYPE_IMAGE;
    private SessionConfig mVideoConfig;
    private CameraEventListener mMediaEventListener;
    private AVRenderListener mAVRenderListener;
    private SurfaceView mGLSurfaceView;
    private ScaleGestureDetector mScaleListener = null;
    private GestureDetector mGestureListener = null;
    private float mZoomLevel = 1f;
    private int mPreviewWidth, mPreviewHeight;
    private boolean mCameraSwapped;
    private boolean mIsRecording;
    private boolean mPrepared;
    /**
     * true only if both audio and video have been paused.
     */
    private boolean mEncodersPaused;
    private boolean mPlaybackPaused;
    private boolean mAudioPaused, mVideoPaused;
    private int kbps = (int) (6.0f * 1024); // 3.2Mbps by default
    private boolean mRequestVideoRecordingStop;
    private String mSourceFilePath;
    private long mStartTime, mEndTime; // microseconds
    private int mVideoFrameCount;
    private int mAudioFrameCount;
    private SessionConfig mSourceConfig;
    private int mTrackCount;
    private boolean mRecorderReady, mRendererReady, mExtractorReady;
    private long mFirstDisplayFrameTs, mFirstVideoFrameTs;

    private int frameInterval; //ms
    private boolean mAudioStopped, mVideoStopped;
    private Renderer mRefRenderer;
    private MediaRecorder mMediaRecorder;
    private CameraCaptureCallback mDefaultCameraCaptureCallback = new CameraCaptureCallback() {
        @Override
        public void onShutter() {

        }

        @Override
        public void onCameraCapture(Bitmap bitmap, String path, int orientation, long maxFileSizeBytes) {
            Log.d(TAG, "Image saved... as backup for video: " + path);
            mCameraPreview.startPreview();
        }
    };
    private String mOutputFilePath;
    private volatile SurfaceTexture mSurfaceTexture;
    private float audioVolume = 1.0f;
    private volatile boolean mRendering = true;
    private List<String> mFilter;
    private CaptureRenderer mCaptureRenderer;
    private boolean doLoop;
    private long mSeekTime = -1, mSeekReqTime = -1;
    private long mDuration;
    private int mCurrentPosition = -1;
    private float mPlaybackSpeed = 1.0f;
    private boolean isCamera2;
    private Runnable mRenderRequestRunnable;
    private Scene mScene;
    private int mDisplayFrameReqCount, mVideoFrameReqCount;
    private int mDisplayFrameCount;
    private int mExtractionMode;
    private boolean isSeeking;
    private long mWaitTime;

    // This is the last timestamp that's gonna be received (whatever the mDuration be)
    private long mLastTimestamp = -1;
    private long mPauseTime = -1;
    private ActionListener displayRenderCreationListener;
    private boolean mMuxerDone;
    private Drawable mOverlayDrawable;

    private long mVideoCompatStartTime;
    private long mVideoCompatTimestamp;
    private Timer mVideoCompatTimer;
    private TimerTask mVideoCompatTimerTask;
    private SceneDescription mSceneDesc = new SceneDescription(SceneManager.SceneName.SCENE_DEFAULT);
    private boolean mIsCamera;
    private String mScenePath;
    private ArrayList<ImageSource> mImageSources = new ArrayList<>();

    private boolean mIsConcat;
    private int mAudioInputType;
    private long mPrevDisplayTs;
    private long mPrevVideoTs = -1;
    private boolean isCompat;
    private boolean mUsePreviousSceneDescription = false;
    private boolean USE_PREFERRED_TIMESTAMP = false;
    private int mSceneFrameCount;
    private long mFirstSceneFrameTs;
    private boolean mExtractionFinished;
    private Runnable mStartCameraPreviewRunnable;
    private String storyId;

    public RenderManager(Context context) {
        this(context, 0, 0, null);
    }

    public RenderManager(Context context, int sourceMedia, int renderTarget) {
        this(context, sourceMedia, renderTarget, null);
    }

    public RenderManager(Context context, int sourceMedia, int renderTarget, SurfaceView sv) {
        init(context, sourceMedia, renderTarget, sv);
    }

    public void init(Context context, int sourceMedia, int renderTarget, SurfaceView sv) {
        this.mContext = context;
        this.mGLSurfaceView = sv;

        CameraPreview.updateCameraCompat();
        cameraCompat = CameraPreview.cameraCompat;

        setSourceType(sourceMedia);
        setRenderTargetType(renderTarget);

        mUiHandler = new UiHandler(this);
        HandlerThread imageThread = new HandlerThread("ImageThread");
        imageThread.start();
        mBackgroundHandler = new Handler(imageThread.getLooper());

        mRenderRequestRunnable = new Runnable() {
            @Override
            public void run() {
//                mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
                requestRender();
            }
        };

        mStartCameraPreviewRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasCameraSource()) {
                    startCameraPreview();
                }
            }
        };
    }

    private void reset() {
        mTrackCount = 0;
        mRecorderReady = false;
        mRendererReady = false;
        mExtractorReady = false;

        mExtractionFinished = false;

        mPrepared = false;
        frameInterval = (int) (1.0f / PREFERRED_VIDEO_FPS * 1000); // ms

        if (isRenderTargetDisplay()) {
            mDisplayFrameReqCount = 0;
            mDisplayFrameCount = 0;
        }

        if (isRenderTargetVideo()) {
            mVideoFrameReqCount = 0;
        }

        mLastTimestamp = -1;
        mVideoCompatTimestamp = 0;

        mAudioEncoder = null;
    }

    public void configure() {
        reset();

        Log.d(TAG, "configure: " + " sourcePath: " + mSourceFilePath + " sourceType: " + mSourceMedia + " targetType: " + mRenderTarget);
        mImageSources = new ArrayList<>();
        if (mSourceFilePath != null) {
            mImageSources.add(new ImageSource(mSourceFilePath));
        }
        if (hasCameraSource()) {
            mImageSources.add(CameraPreview.sImageSource);
        }
        createScene();
        if (hasCameraSource()) {
            configureCamera();
        }
        if (mGLSurfaceView instanceof RGLSurfaceView) {
            ((RGLSurfaceView) mGLSurfaceView).setEventListener(mCameraPreview);
        } else if (mGLSurfaceView instanceof RSurfaceView) {
            ((RSurfaceView) mGLSurfaceView).setEventListener(mCameraPreview);
        }

        if (!isCompat) {
            configureRenderer();
            try {
                initCodecs();
            } catch (IOException e) {
                Log.e(TAG, "Not able to initialize codecs");
                mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.RENDERER_ERROR, mVideoConfig));
            }
        }
    }

    private void configureGestures() {
        mScaleListener = new ScaleGestureDetector(mContext, new ScaleDetectorListener());
        mGestureListener = new GestureDetector(mContext, new CustomGestureListener());
    }

    private void configureCamera() {
        if (!hasCameraSource()) {
            mCameraPreview = null;
            return;
        }
        if (mCameraPreview == null) {
            if (!cameraCompat && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CameraControl2.isCamera2CapableEnough()) {
                mCameraPreview = new CameraControl2(mContext);
                isCamera2 = true;
            } else {
                mCameraPreview = new CameraControl(mContext);
                isCamera2 = false;
            }
        }
        mCameraPreview.setCameraStateCallback(this);
        mCameraPreview.setPreview(mGLSurfaceView);
        mCameraPreview.startPreview();
    }

    public void setSourceType(int sourceMedia) {
        mSourceMedia = sourceMedia;

        TOTAL_TRACK_COUNT = 0;
        if (hasAnyAudioSource())
            TOTAL_TRACK_COUNT++;
        if (hasVisualSource())
            TOTAL_TRACK_COUNT++;

        if (hasExternalAVSource()) {
            computeMediaExtractionMode();
        }
        isCompat = hasCameraSource() && CameraPreview.cameraCompat;
    }

    private void computeMediaExtractionMode() {
        int extractionMode = 0;
        boolean extractAudio = hasAudioSource();
        boolean extractVideo = hasVideoSource();
        extractionMode |= extractAudio ? AVMediaExtractor.EXTRACT_TYPE_AUDIO_DECODED : 0;
        extractionMode |= extractVideo ? AVMediaExtractor.EXTRACT_TYPE_VIDEO_DECODED : 0;
        mExtractionMode = extractionMode;
    }

    public void setRenderTargetType(int renderTargetType) {
        mRenderTarget = renderTargetType;

        // Use shared context only when rendering to both display and video simultaneously
        // This happens only during recording.
        // When playing back a video and simultaneously re-encoding it,
        // we will anyway use 2 different RenderManager instances.
        mUseDisplayContext = (mRenderTarget & GraphicsConsts.RENDER_TARGET_DISPLAY) > 0;

        handleSetUseSharedContext();
    }

    private void handleSetUseSharedContext() {
        if (mDisplayRenderer != null) {
            mDisplayRenderer.setUseSharedEGLContext(mUseDisplayContext);
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setUseSharedEGLContext(true);
        }
        if (mDummyRenderer != null) {
            mDummyRenderer.setUseSharedEGLContext(mUseDisplayContext);
        }
    }

    private void configureRenderer() {
        if (isRenderTargetDisplay()) {
            if (mDisplayRenderer == null && hasVisualSource()) {
                prepareDisplayRenderer();
            } else {
                // onSurfaceCreated is not going to be called again. So fake a onRendererPrepared call.
                onRendererPrepared(mSharedEGLContext, mDisplayRenderer);
            }
        }

        // If we are not rendering to display, onSurfaceCreated etc are not there to help us.
        if (isRenderTargetOnlyVideo()) {
            prepareDummyRenderer();
        }

        handleSetUseSharedContext();
    }

    private void initCodecs() throws IOException {
        // mVideoRenderer = new VideoEncoder(mVideoConfig, mContext, this));
        // Since Our Encoder is a renderer first, so it has been created as a part of creation of Renderers.

        mIsRecording = false;
        mRequestVideoRecordingStop = false;

        mAudioStopped = true;
        mVideoStopped = true;

        if (hasAnyAudioSource()) {
            int audioInputType = 0;
            if (hasMicAudioSource()) {
                audioInputType = AudioEncoder.MIC;
            } else if (hasAudioSource()) {
                audioInputType = AudioEncoder.AUDIO;
            } else if (hasBlankAudioSource()) {
                audioInputType = AudioEncoder.BLANK_AUDIO;
            }
            mAudioInputType = audioInputType;
            mAudioEncoder = new AudioEncoder(this, audioInputType);
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setPauseEnabled(false);
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.setPauseEnabled(false);
        }
        if (mAudioFeeder != null) {
            mAudioFeeder.setPauseEnabled(false);
        }
    }

    public void startResumeRendering() throws IOException {
        if (!isCompat) {
            if (mPlaybackPaused) {
                resumeRendering();
            } else {
                mPlaybackPaused = false;
                mVideoFrameCount = 0;
                mAudioFrameCount = 0;

                if (isRenderTargetDisplay()) {
                    if (mDisplayRenderer != null) {
                        mDisplayRenderer.reset();
                    }
                }
                if (isRenderTargetVideo()) {
                    startRecording(); // Do not start recording yet. Wait for mediaExtractor to get
                    // prepared and then let it start the recording
                }
                if ((mRenderTarget & RENDER_TARGET_IMAGE) > 0) {
//                    if (hasCameraSource()) {
                    prepareCaptureRenderer();
//                    }
                    captureImage(new CameraCaptureCallback() {
                        @Override
                        public void onShutter() {

                        }

                        @Override
                        public void onCameraCapture(Bitmap bitmap, String path, int orientation
                                , long maxFileSizeBytes) {
                            path = FileUtilities.generateMediaFile(FileUtilities.FILE_TYPE_JPG
                                    , FileUtilities.MEDIA_DIR_IMAGE).getAbsolutePath();
                            try {
                                AndroidUtilities.writeImage(bitmap, path, Bitmap.CompressFormat.JPEG, 100);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            SessionConfig.Builder configBuilder = new SessionConfig.Builder(path)
                                    .withVideoBitrate(kbps * 1024);
                            SessionConfig config = configBuilder.build();
                            publishOnComplete(config);
                        }
                    });
                }
            }
        } else {
            if (isRenderTargetVideo()) {
                if (prepareVideoRecorderCompat()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    try {
                        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                            @Override
                            public void onInfo(MediaRecorder mr, int what, int extra) {

                            }
                        });
                        configureCameraForRecording();
                        scheduleVideoCompatTimer();
                        mMediaRecorder.start();
                        mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.RECORDING_STARTED, mVideoConfig));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorderCompat();
                    // inform user
                }
            }
        }

    }

    private void scheduleVideoCompatTimer() {
        mVideoCompatTimer = new Timer();
        mVideoCompatStartTime = System.currentTimeMillis();
        mVideoCompatTimerTask = new TimerTask() {
            @Override
            public void run() {
                mVideoCompatTimestamp = System.currentTimeMillis() - mVideoCompatStartTime;
                publishProgress(mVideoConfig, mVideoCompatTimestamp * 1000);
            }
        };
        mVideoCompatTimestamp = 0;
        mVideoCompatTimer.schedule(mVideoCompatTimerTask, 0, frameInterval);
    }

    private void stopVideoCompatTimer() {
        if (mVideoCompatTimerTask != null) {
            mVideoCompatTimerTask.cancel();
        }
        if (mVideoCompatTimer != null) {
            mVideoCompatTimer.cancel();
            mVideoCompatTimer.purge();
        }
    }

    public boolean hasCameraSource() {
        return (mSourceMedia & MEDIA_TYPE_CAMERA) > 0;
    }

    private void releaseMediaExtractor() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void startPlayback() {
        mMuxerDone = false;
        Log.d(TAG, "startPlayback: " + " sourcePath: " + mSourceFilePath + " sourceType: " + mSourceMedia + " targetType: " + mRenderTarget);
        if (containsCameraScene() && mCameraPreview != null && mCameraPreview.isCameraAvailable()) {
            setCameraPreviewSize(mCameraPreview.getCameraProperties().mPreviewWidth
                    , mCameraPreview.getCameraProperties().mPreviewHeight);
        } else {
            if (mSourceConfig != null) {
                setCameraPreviewSize(mSourceConfig.getVideoWidth(), mSourceConfig.getVideoHeight());
            }
        }
        setPreviewRenderParams();
        setRenderParams();

        readyForFrames();
    }

    private void startRecording() throws IOException {
        if (!isRenderTargetVideo()) {
            Log.w(TAG, "startRendering:: renderTarget should be Video for recording");
            return;
        }
        File outputFile = FileUtilities.generateMediaFileForThisStory(storyId, FileUtilities.FILE_TYPE_MP4, FileUtilities.MEDIA_DIR_CREATION);
        if (null == outputFile) {
            return;
        }
        mOutputFilePath = outputFile.getAbsolutePath();
        Log.d(TAG, "Recording to file: " + mOutputFilePath);
        SessionConfig.Builder configBuilder = new SessionConfig.Builder(mOutputFilePath)
                .withVideoBitrate(kbps * 1024);
        int videoWidth = 720;
        int videoHeight = 1280;
        videoWidth = ((videoWidth + 15) / 16) * 16;
        videoHeight = ((videoHeight + 15) / 16) * 16;

        configBuilder = configBuilder
                .withVideoResolution(videoWidth, videoHeight);

        if (mSourceConfig != null) {
            configBuilder.withAudioBitrate(Math.min(mSourceConfig.getAudioBitrate(), 128000))
                    .withAudioChannels(Math.min(mSourceConfig.getNumAudioChannels(), 2))
                    .withAudioSampleRate(Math.min(mSourceConfig.getAudioSamplerate(), 48000));
        }

        if (hasVisualSource()) {
            configBuilder.withVideoDecoded();
        }
        if (hasAnyAudioSource()) {
            configBuilder.withAudioDecoded();
        }

        mVideoConfig = configBuilder.build();
        mVideoConfig.getMuxer().setIsAudioSourceMic(hasMicAudioSource());
        if (((AndroidMuxer) mVideoConfig.getMuxer()).getMuxer() == null) {
            publishOnComplete(null);
            return;
        }
        configureCameraForRecording();
        prepareVideoRenderer();
        handleSetUseSharedContext();

        if (containsCameraScene() && mCameraPreview != null && mCameraPreview.isCameraAvailable()) {
            setCameraPreviewSize(mCameraPreview.getCameraProperties().mPreviewWidth
                    , mCameraPreview.getCameraProperties().mPreviewHeight);
        } else {
            if (mSourceConfig != null) {
                setCameraPreviewSize(mSourceConfig.getVideoWidth(), mSourceConfig.getVideoHeight());
            }
        }
        setPreviewRenderParams();
        setRenderParams();

        if (mVideoRenderer != null) {
            mVideoRenderer.setVideoConfig(mVideoConfig);
        }

        if (mAudioFeeder != null) {
            mAudioFeeder.setVideoConfig(mVideoConfig);
        }

        reset(mVideoConfig);

        if (isRenderTargetVideo()) {
            if (!hasAnyAudioSource() || !hasCameraSource()) {
                if (mVideoRenderer != null) {
                    mVideoRenderer.setCanStartRecording(true);
                }
            }
        }

        if (isRenderTargetVideo()) {
            mIsRecording = true;
            if (hasAnyAudioSource()) {
                if (mAudioEncoder != null) {
                    mAudioEncoder.startRecording();
                }
            }
            if (hasVisualSource()) {
                mVideoRenderer.startRecording();
            }
        }
    }

    private boolean hasImageSource() {
        return (mSourceMedia & MEDIA_TYPE_IMAGE) > 0;
    }

    private void configureCameraForRecording() {
        if (mCameraPreview != null) {
            mCameraPreview.setFocusMode(FOCUS_MODE_VIDEO);
        }
    }

    public void resumeRendering() {
        resumeRendering(true);
    }

    public void resumeRendering(boolean forceResume) {
        if (!forceResume) return;
        if (hasImageSource()) {
            if (mPauseTime > 0) {
                mWaitTime += (System.currentTimeMillis() - mPauseTime);
                mPauseTime = -1;
            }
            postRequestRender();
        }
        changePauseState(false);
    }

    public void pauseRecording() {
        if (hasImageSource()) {
            mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
        }
        changePauseState(true);

        // Don't update the pause time, if we're already paused.
        //if (mPlaybackPaused && isRenderTargetDisplay() || mEncodersPaused && isRenderTargetVideo()) return;

        mPauseTime = System.currentTimeMillis();
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void stopRecording() {
        stopRecording(false);
    }

    public void stopRecording(boolean kill) {
        Log.d(TAG, "stopRecording: " + kill);
        if (kill) {
            mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
        }
        if (isRenderTargetVideo()) {
            if (!isCompat) {
                mIsRecording = false;

                // Reset the pause state... for two reasons
                // 1) For the next cycle
                // 2) We might still choose to render something (eos, for instance) as a part of wrapping up
                // the encoding, pause state will not let us do that.
                stopAudioRecording();
                stopVideoRecording(kill);
            } else {
                stopVideoCompatTimer();
                releaseMediaRecorderCompat();
                publishOnComplete(mVideoConfig);
            }
        }
    }

    private void stopAudioRecording() {
        Log.d(TAG, "stop audio recorder");
        changeAudioPauseState(false);
        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
        }
    }

    private void stopVideoRecording(boolean kill) {
        Log.d(TAG, "stop video recorder: " + kill);
        mRequestVideoRecordingStop = true;

        changeVideoPauseState(false);
        if (mVideoRenderer != null) {
            mVideoRenderer.stopRecording();
        }
    }

    private void checkRecorderDone() {
        mIsRecording = !(mAudioStopped && mVideoStopped);
        if (isRenderTargetVideo() && (!mIsRecording && mMuxerDone)) {
            publishOnComplete(mVideoConfig);
        } else if ((isRenderTargetOnlyDisplay() && mExtractionFinished)) {
            publishOnComplete(mSourceConfig);
        }
    }

    @Override
    public void onRecordingFinished(boolean success) {
        Log.d(TAG, "recording status update: onMuxerFinished: " + success);
        if (success) {
            onRecordingFinished(mVideoConfig);
        } else {
            onRecordingFinished(null);
        }
    }

    private void onRecordingFinished(SessionConfig config) {
        mMuxerDone = true;
        checkRecorderDone();
    }

    public boolean containsCameraScene() {
        return (hasCameraSource());
    }

    public boolean isCameraAvailable() {
        return mCameraPreview != null && mCameraPreview.isCameraAvailable();
    }

    public boolean isCameraCompat() {
        return cameraCompat;
    }

    public boolean isCompat() {
        return isCompat;
    }

    @Override
    public void onCameraOpened() {
        Log.d(TAG, "onCameraOpened");
        if (cameraCompat) {
            if (mGLSurfaceView != null)
                mCameraPreview.startPreview();
        } else {
            startCameraPreviewSync();
        }

        if (mMediaEventListener != null) {
            mMediaEventListener.onCameraOpened();
        } else {
            Log.e(TAG, "Camera event listener not set");
        }
    }

    @Override
    public void onCameraDisconnected() {
        if (mMediaEventListener != null) {
            mMediaEventListener.onCameraDisconnected();
        } else {
            Log.e(TAG, "Camera event listener not set");
        }
    }

    @Override
    public void onCameraError(int error) {
        if (mMediaEventListener != null) {
            mMediaEventListener.onCameraError(error);
        } else {
            Log.e(TAG, "Camera event listener not set");
        }
    }

    public void setAudioVolume(float audioVolume) {
        this.audioVolume = audioVolume;
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(audioVolume);
        }
    }

    public void setRenderListener(AVRenderListener renderListener) {
        mAVRenderListener = renderListener;
    }

    public void addDrawable(Drawable drawable) {
        if (mScene == null || mScene.mRootDrawables == null || mScene.mRootDrawables.get(0) == null)
            return;
        mScene.mRootDrawables.get(0).addChild(drawable);
        if (mVideoRenderer != null) {
            mVideoRenderer.addDrawable(drawable);
        }
        if (mCaptureRenderer != null) {
            mCaptureRenderer.addDrawable(drawable);
        }
    }

    public synchronized void seekTo(long time) {
        //pauseRecording();
        mSeekReqTime = time;
        mLastTimestamp = -1;
        if (mMediaPlayer != null && mMediaPlayer.isPrepared()) {
            mMediaPlayer.seekTo(time);
        }
    }

    @Override
    public void onPrepared(MediaPlayerImpl mp) {
        if (mSeekReqTime > 0) {
            seekTo(mSeekReqTime);
        }
//        if (mPlaybackPaused || isSeeking) return;

        mMediaPlayer.setMediaExtractionListener(this);

        onMediaExtractorPrepared(mSourceConfig);
    }

    @Override
    public void onSeekComplete(MediaPlayerImpl mp) {

    }

    @Override
    public void onSeek(MediaPlayerImpl mp) {

    }

    @Override
    public boolean onInfo(MediaPlayerImpl mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayerImpl mp) {
        mLastTimestamp = mp.getCurrentPosition();
        mExtractorReady = false;
        onMediaExtractionFinished();
    }

    @Override
    public boolean onError(MediaPlayerImpl mp, int what, int extra) {
        return true;
    }

    public int getSourceMediaType() {
        return mSourceMedia;
    }

    public void handlePrepare(int sourceMedia, int renderTarget) {
        setSourceType(sourceMedia);
        setRenderTargetType(renderTarget);

        configure();
    }

    @Override
    public void onProgress(MediaPlayerImpl mp, long timeInUs) {
    }

    private void resetZoom(boolean animate) {
/*        zoomStep = (finalZoom - mZoomLevel) / durationms;
        Runnable zoomRunnable = new ZoomRunnable(mZoomLevel, finalZoom, durationms);*/
        if (mCameraPreview != null) {
            if (animate) {
                mCameraPreview.smoothZoomTo(0);
            } else {
                mCameraPreview.zoomTo(0);
            }
        }
    }

    /**
     * Prepare for a subsequent recording. Must be called after {@link #stopRecording()}
     * and before {@link #release()}
     *
     * @param config
     */
    public void reset(SessionConfig config) throws IOException {
        if (mVideoRenderer != null && mVideoRenderer.isReusing()) {
            mVideoRenderer.reset(config);
        }
        if (mAudioEncoder != null /*&& mAudioEncoder.isReusing()*/) {
            mAudioEncoder.setAudioInputType(mAudioInputType);
            mAudioEncoder.reset(config);
        }
        mIsRecording = false;
    }

    /**
     * Release resources. Must be called after {@link #stopRecording()} After this call
     * this instance may no longer be used.
     */
    public void release() {
        if (mVideoRenderer != null) {
            mVideoRenderer.release();
            mVideoRenderer = null;
        }
        // AudioEncoder releases all it's resources when stopRecording is called
        // because it doesn't have any meaningful state
        // between recordings. It might someday if we decide to present
        // persistent audio audioVolume meters etc.
        // Until then, we don't need to write AudioEncoder.release()
    }

    public void onHostActivityResumed() {
// TODO major       mVideoRenderer.onHostActivityResumed();
    }

    public void onHostActivityPaused() {
// TODO major       mVideoRenderer.onHostActivityPaused();
    }

    public void onHostActivityStopped() {
// TODO major       mVideoRenderer.onHostActivityPaused();
    }

    public void onHostActivityDestroyed() {
        release();
    }

    private void changePauseState(boolean isPause) {
        mPlaybackPaused = isPause;
        if (!isPause) {
            isSeeking = false;
        }
        if (mMediaPlayer != null && mMediaPlayer.isPrepared()) {
            if (isPause) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.resume();
            }
        }
        changeVideoPauseState(isPause);
        changeAudioPauseState(isPause);
    }

    private void changeAudioPauseState(boolean isPaused) {
        if (mAudioEncoder != null) {
            mAudioEncoder.setPauseEnabled(isPaused);
        }
        mAudioPaused = isPaused;
        mEncodersPaused = mAudioPaused && mVideoPaused;
    }

    private void changeVideoPauseState(boolean isPaused) {
        if (mVideoRenderer != null) {
            mVideoRenderer.setPauseEnabled(isPaused);
        }
        mVideoPaused = isPaused;
        mEncodersPaused = mAudioPaused && mVideoPaused;
    }

    private void prepareMediaExtractor() {
        if (hasExternalAVSource()) {
            String id = generateMediaPlayerId();
            boolean isNewInstance = needsNewMediaPlayer();
            if (isNewInstance) {
                releaseMediaExtractor();
/*                if (USE_MEDIA_PLAYER || isRenderTargetOnlyDisplay()) {
                    mMediaPlayer = new MediaPlayer();
                } else {*/
                    mMediaPlayer = new AVMediaExtractor();
/*                }*/
                mMediaPlayer.setId(id);
            }

            mMediaPlayer.setExtractionMode(mExtractionMode);

            if (doLoop) {
                mMediaPlayer.setLooping(isRenderTargetDisplay() && (!isRenderTargetVideo()));
            }
            setAudioVolume(audioVolume);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnSeekListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnProgressListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setSeekMode(MediaPlayer.SeekMode.PRECISE);
            mMediaPlayer.setPlayAudio((hasAudioSource()) && (isRenderTargetDisplay()));
            mMediaPlayer.setRealTimeExtract(isRenderTargetDisplay());
            mMediaPlayer.setClipSize(mStartTime, mEndTime);
            mMediaPlayer.setPlaybackSpeed(mPlaybackSpeed);

            if (isNewInstance) {
                try {
                    mMediaPlayer.setDataSource(getSource(mSourceFilePath));
                } catch (IOException e) {
                    CrashlyticsWrapper.logException(e);
                    return;
                }
                mMediaPlayer.setSurface(new Surface(mDummyRenderer.getSurfaceTexture()));

                try {
                    mMediaPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                onMediaExtractorPrepared(mSourceConfig);
            }
        } else {
        }
    }

    private boolean needsNewMediaPlayer() {
        String id = generateMediaPlayerId();
        return mMediaPlayer == null || !id.equals(mMediaPlayer.getId());
    }

    private String generateMediaPlayerId() {
        return "s:" + mStartTime + ",e:" + mEndTime + ";p:" + mSourceFilePath;
    }

    private boolean isRenderTargetVideo() {
        return (mRenderTarget & RENDER_TARGET_VIDEO) > 0;
    }

    private boolean isRenderTargetImage() {
        return (mRenderTarget & RENDER_TARGET_IMAGE) > 0;
    }

    private boolean isRenderTargetDisplay() {
        return (mRenderTarget & RENDER_TARGET_DISPLAY) > 0;
    }

    private boolean isRenderTargetOnlyVideo() {
        return !isRenderTargetDisplay() && isRenderTargetVideo();
    }

    private boolean isRenderTargetOnlyDisplay() {
        return isRenderTargetDisplay() && !isRenderTargetVideo();
    }

    private MediaSource getSource(String mSourceFilePath) {
        // TODO add uri source also
        return new UriSource(mContext, Uri.parse(mSourceFilePath));
    }

    private void prepareDummyRenderer() {
        Log.d(TAG, "Preparing Dummy Renderer to handle textures.");
        if (mDummyRenderer == null) {
            mDummyRenderer = new DummyRenderer(this);
            mDummyRenderer.setRendererInstance(mRefRenderer);
            mDummyRenderer.setUseSharedEGLContext(mUseDisplayContext);
            mDummyRenderer.prepareRenderer(mUseDisplayContext ? mSharedEGLContext : null);

            // If we're rendering to display, treat its renderer as owner of caches, else Dummy renderer
            mRefRenderer = (mUseDisplayContext) ? mDisplayRenderer : mDummyRenderer;

            mDummyRenderer.setRendererInstance(mRefRenderer);
        }
    }

    private synchronized void prepareVideoRenderer() {
        // It is the Video Renderer which needs to use the shared EGL context
        // If Display renderer is in picture, then it's the owner of the original EGLContext.
        if (mVideoRenderer == null) {
            mVideoRenderer = new VideoEncoder(mVideoConfig, this);
        }
        Log.d(TAG, "set egl context for video: " + mSharedEGLContext);
        mVideoRenderer.setUseSharedEGLContext(true);
        mVideoRenderer.setSharedEGLContext(mSharedEGLContext);
        mVideoRenderer.setRendererInstance(mRefRenderer);
    }

    private void prepareCaptureRenderer() {
        // It is the Capture Renderer which needs to use the shared EGL context
        // If Display renderer is in picture, then it's the owner of the original EGLContext.
        if (mCaptureRenderer == null) {
            mCaptureRenderer = new CaptureRenderer(this);
        }
        int displayWidth = AndroidUtilities.widthInPixel();
        int captureWidth = Math.max(720, Math.min(1080, Math.min(BaseCameraSettings.MAX_PICTURE_SIZE, displayWidth)));
        int captureHeight = (int) (captureWidth * BaseCameraSettings.DEFAULT_PREFERRED_BACK_INV_ASPECT_RATIO);
        mCaptureRenderer.setCameraParams(mCameraPreviewWidth, mCameraPreviewHeight);
        mCaptureRenderer.setRendererInstance(mRefRenderer);
        Log.d(TAG, "set egl context for capturerenderer: " + mSharedEGLContext);
        mCaptureRenderer.setSharedEGLContext(mSharedEGLContext);
        if (mSourceConfig != null) {
            mCaptureRenderer.prepareRenderer(mSharedEGLContext, mSourceConfig.getVideoWidth(), mSourceConfig.getVideoHeight());
        } else {
            mCaptureRenderer.prepareRenderer(mSharedEGLContext, captureWidth, captureHeight);
        }
        mCaptureRenderer.invalidateScene(mScene);
    }

    private synchronized void prepareDisplayRenderer() {
        if (mDisplayRenderer == null && hasVisualSource()) {
            mDisplayRenderer = new GLSVRenderer(this);
            setTargetView(mGLSurfaceView);
            mDisplayRenderer.setDestinationView((RGLSurfaceView) mGLSurfaceView);
            mDisplayRenderer.setRendererInstance(mDisplayRenderer);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        mSurfaceTexture = surfaceTexture;
        surfaceTexture.setOnFrameAvailableListener(this);
        startCameraPreviewSync();
    }

    private void startCameraPreviewSync() {
        if (isCamera2) {
            AndroidUtilities.runOnUIThread(mStartCameraPreviewRunnable);
        } else {
            mBackgroundHandler.post(mStartCameraPreviewRunnable);
        }
    }

    private synchronized void startCameraPreview() {
        if (mCameraPreview.isPreviewOngoing()) return;
        if (mSurfaceTexture == null) {
            Log.d(TAG, "Surface Texture Not Ready Yet");
            return;
        }
        Log.d(TAG, "Surface Texture Ready");
        mCameraPreview.handleSetSurfaceTexture(mSurfaceTexture);
        mCameraPreview.startPreview();
        BaseCameraSettings.CameraProperties cameraProps = mCameraPreview.getCameraProperties();
        if (cameraProps == null) return;
        setCameraPreviewSize(cameraProps.mPreviewWidth, cameraProps.mPreviewHeight);
        setPreviewRenderParams();
        setRenderParams();
    }

    private void setPreviewRenderParams() {
        if (mDisplayRenderer != null) {
            mDisplayRenderer.setPreviewRenderParams(mPreviewWidth, mPreviewHeight);
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setPreviewRenderParams(mPreviewWidth, mPreviewHeight);
        }
        if (mCaptureRenderer != null) {
            mCaptureRenderer.setPreviewRenderParams(mPreviewWidth, mPreviewHeight);
        }
    }

    public BaseRenderer getVideoRenderer() {
        if (isRenderTargetVideo()) return mVideoRenderer;
        return mDisplayRenderer;
    }

    private void setRenderParams() {
        if (mDisplayRenderer != null) {
            mDisplayRenderer.setRenderParams();
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setRenderParams();
        }
        if (mCaptureRenderer != null) {
            mCaptureRenderer.setRenderParams();
        }
    }

    int mCameraPreviewWidth, mCameraPreviewHeight;

    private void setCameraPreviewSize(int previewWidth, int previewHeight) {
        mCameraPreviewWidth = previewWidth;
        mCameraPreviewHeight = previewHeight;
        if (mDisplayRenderer != null) {
            mDisplayRenderer.setCameraParams(previewWidth, previewHeight);
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setCameraParams(previewWidth, previewHeight);
        }
        if (mCaptureRenderer != null) {
            mCaptureRenderer.setCameraParams(previewWidth, previewHeight);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (VERBOSE && (SHOW_FRAME_LOGS || ((mVideoFrameCount % 30) == 0)))
            Log.d(TAG, "Frame available");

        if (!mRendering) return;

        processFrameAvailableEvent(surfaceTexture.getTimestamp() / 1000);
        // Send a request to Dummy Renderer to updateTexImage
        if (mDummyRenderer != null) {
            mDummyRenderer.requestRender();
        }
        if (VERBOSE) Log.d(TAG, "OnFrameAvailable: " + surfaceTexture.getTimestamp() / 1000L);
    }

    private void processFrameAvailableEvent(long timestamp) {
        if (mCameraSwapped) {
            mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.CAMERA_SWAP));
            mCameraSwapped = false;
        }
    }

    @Override
    public void onRendererPrepared(EGLContext sharedEGLContext, Renderer renderer) {
        if (sharedEGLContext == null) {
            Log.d(TAG, " why the fuck is the context null");
        }

        if (renderer instanceof GLSVRenderer || renderer instanceof DummyRenderer) {
            mSharedEGLContext = sharedEGLContext;
        }

        if (renderer instanceof GLSVRenderer) {
            if (mDummyRenderer == null) {
                prepareDummyRenderer();
            } else {
                onRendererPrepared(mSharedEGLContext, mDummyRenderer);
            }
            mDisplayRenderer.invalidateScene(mScene);
            if (displayRenderCreationListener != null) {
                displayRenderCreationListener.onSuccess();
            }
        } else if (renderer instanceof VideoEncoder) {
            mVideoRenderer.invalidateScene(mScene);
            if (displayRenderCreationListener != null) {
                displayRenderCreationListener.onSuccess();
            }
        } else if (renderer instanceof DummyRenderer) {
            mRendererReady = true;
            prepareMediaExtractor();
            prepareForRendering();
            checkReadyToRender();
        } // No need of the final else
    }

    @Override
    public void onDisplaySurfaceChanged(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        setPreviewRenderParams();
    }

    @Override
    public void onFrameDisplayed(long timestampUs) {
        if (!isRenderTargetVideo()) {
            if (!hasCameraSource()) {
                if (mLastTimestamp == -1) {
                    // Progress is determined by display timing on the screen (when display's the only target)
                    publishProgress(mSourceConfig, timestampUs);
                }
            }
            if (mMediaPlayer != null) {
                mMediaPlayer.setPrevVideoFrameTs(timestampUs);
            }
        }

        if (isRenderTargetDisplay() && !isRenderTargetVideo()) {
            if (hasImageSource()) {
                long delay = mDisplayFrameCount * frameInterval * 1000L - timestampUs;
                if (VERBOSE)
                    Log.d(TAG, "onFrameDisplayed: timeStamp: " + timestampUs + " duration: " + mDuration + " requesting next in : " + delay);
                if (timestampUs >= mDuration) {
                    mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
                    publishOnComplete(mSourceConfig);
                } else if (!isSeeking) {
                    postRequestRender(frameInterval);
                }
            }
        }
        mDisplayFrameCount++;
    }

    private void publishOnComplete(SessionConfig config) {
        Log.d(TAG, "publishOnComplete: " + config);
        mExtractionFinished = false;
        mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.PUBLISH_ON_COMPLETE, config));
    }

    @Override
    public void onVideoFrameRendered(final long timestampUs) {
        if (VERBOSE && (SHOW_FRAME_LOGS || ((mVideoFrameCount % 30) == 0)))
            Log.d(TAG, "video timestamp: " + timestampUs + " mEndtime: " + mEndTime + " frameCount: " + mVideoFrameCount);

        if (mVideoFrameCount == 2) {
            mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.RECORDING_STARTED, mVideoConfig));
        } else if (mVideoFrameCount > 2) {
            publishProgress(mVideoConfig, timestampUs);
        }
        mVideoFrameCount++;

        if (mMediaPlayer != null) {
            mMediaPlayer.setPrevVideoFrameTs(timestampUs);
        }

        if (hasImageSource()) {
            if (isRenderTargetVideo()) {
                if (timestampUs > mDuration) {
                    stopRecording();
                    mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
                } else {
                    postRequestRender(5);
                }
            }
        }
    }

    private void publishProgress(SessionConfig config, long timestampUs) {
        if (VERBOSE) Log.d(TAG, "publishProgress: timeStamp = " + timestampUs);
        mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.PUBLISH_PROGRESS, timestampUs));
    }

    @Override
    public void onAudioFrameRendered(long timestamp) {
        if (VERBOSE && (SHOW_FRAME_LOGS || ((mAudioFrameCount % 30) == 0))) {
            Log.d(TAG, "audio timestamp: " + timestamp + " duration: " + (mEndTime - mStartTime));
        }
        if (hasCameraSource()) {
            if (mVideoRenderer != null && !mVideoRenderer.canStartRecording()) {
                mVideoRenderer.setCanStartRecording(true);
            }
        }
    }

    @Override
    public void onAudioRecordingFinished() {
        mAudioStopped = true;
        Log.d(TAG, "recording status update: onAudioStopped");
        checkRecorderDone();
    }

    @Override
    public void onVideoRecordingFinished() {
        mVideoStopped = true;
        mRequestVideoRecordingStop = false;
        Log.d(TAG, "recording status update: onVideoStopped");
        checkRecorderDone();
    }

    @Override
    public void onAudioRecorderReady(boolean success) {
        if (!success) {
            CrashlyticsWrapper.logException(new RuntimeException("AudioRecorder initialization failed"));
            publishOnComplete(null);
            return;
        }
        mTrackCount++;
        mAudioStopped = false;
        checkRecorderReady();
    }

    @Override
    public void onVideoRecorderReady(boolean success) {
        if (!success) {
            CrashlyticsWrapper.logException(new RuntimeException("AudioRecorder initialization failed"));
            publishOnComplete(null);
            return;
        }
        mTrackCount++;
        mVideoStopped = false;
        checkRecorderReady();
    }

    private void checkRecorderReady() {
        if (mTrackCount > TOTAL_TRACK_COUNT) {
            CrashlyticsWrapper.logException(new IllegalStateException("Current track count cannot be greater than total track count"));
        }
        mRecorderReady = (mTrackCount == TOTAL_TRACK_COUNT);

        checkReadyToRender();
    }

    private void readyForFrames() {
        mRendering = true;

        if (hasExternalAVSource()) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPrepared()) {
                    mMediaPlayer.start();
                }
            }
        }
        mAVRenderListener.readyForFrames();

        if (hasImageSource()) {
            postRequestRender();
        }
    }

    private boolean hasVisualSource() {
        return (mSourceMedia & (MEDIA_TYPE_VIDEO | MEDIA_TYPE_CAMERA | MEDIA_TYPE_IMAGE)) > 0;
    }

    private void postRequestRender() {
        postRequestRender(0);
    }

    private void postRequestRender(int delayMs) {
//        mBackgroundHandler.removeCallbacks(mRenderRequestRunnable);
        mBackgroundHandler.postDelayed(mRenderRequestRunnable, delayMs - 1);
    }

    private Scene createScene(String scenePath, boolean isCamera) {
        mIsCamera = isCamera;
        mScenePath = scenePath;
        return createScene();
    }

    private Scene createScene() {
        ArrayList<ImageSource> imageSources = new ArrayList<>(mImageSources.size());
        for (ImageSource imageSource : mImageSources) {
            imageSources.add(imageSource.clone());
        }
        try {
            mScene = SceneManager.createScene(mSceneDesc, imageSources);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mScene == null) return null;
        if (mOverlayDrawable != null) {
            Drawable root = mScene.mRootDrawables.get(0);
            Drawable subRoot = (Drawable) root.getChild(0);
            if (subRoot != null) {
                root = subRoot;
            }
            root.addChild(mOverlayDrawable);
        }
        return mScene;
    }

    private void prepareForRendering() {
        if (!cameraCompat && isRenderTargetVideo()) {
            prepareVideoRenderer();
        }
        if (isRenderTargetDisplay()) {
            prepareDisplayRenderer();
        }
        if (!cameraCompat && /*isRenderTargetDisplay() &&*/ hasCameraSource()) {
            prepareCaptureRenderer();
        }

        if (!mPrepared) {
            setRenderParams();
        }
        mPrepared = true;
    }

    @Override
    public void onFrameAvailable() {
        // updateTexImage done. Finally can proceed to render.
        if (!hasImageSource()) {
            requestRender();
        }
    }

    private void requestRender() {
        if (mRequestVideoRecordingStop) {
            Log.d(TAG, "Rendering the last fucking frame");
        }
        synchronized (this) {
            if (!mRendering) return;
        }

        if (mSurfaceTexture == null) {
            Log.e(TAG, "requestRender: surfaceTexture is null");
            return;
        }
        // Proceed for rendering during pause, only if we're seeking. Else return.
        if ((mPlaybackPaused && isRenderTargetDisplay()) && !isSeeking && hasImageSource()) return;

        long currentTimeUs = System.currentTimeMillis() * 1000L;

        long timeStampUs = 0;

        // Generate timestamps ourselves when it's NOT the camera which is feeding frames and timestamps
        if (hasImageSource()) {
            if (isRenderTargetDisplay()) {
                if (mDisplayFrameReqCount == 0) {
                    mFirstDisplayFrameTs = currentTimeUs;
                }
                synchronized (this) {
                    if (mSeekReqTime >= 0) {
                        // While seeking the only requestRender is triggered by seek, hence positive mSeekReqTime
                        mSeekTime = mSeekReqTime;
                        mFirstDisplayFrameTs = currentTimeUs - mSeekTime;
                        mSeekReqTime = -1;
                        // Resumption of playback after seek should start from the exact seektime
                        // regardless of when that happens.
                        mWaitTime = 0;
                    } else if (mSeekTime >= 0) {
                        // The very first frame rendering after resuming play after seek
                        mFirstDisplayFrameTs = currentTimeUs - mSeekTime;
                        mSeekTime = -1;

                        // Resumption of playback after seek should start from the exact seektime
                        // regardless of when that happens.
                        mWaitTime = 0;
                    } else if (mWaitTime > 0) {
                        // The very first frame after resuming play (after pause)
                        mFirstDisplayFrameTs += mWaitTime * 1000;
                        mWaitTime = 0;
                    }
                }
                timeStampUs = currentTimeUs;
            } else if (isRenderTargetVideo()) {
                timeStampUs = mVideoFrameReqCount * frameInterval * 1000L;
            }
        } else if (hasVideoSource()) {
            timeStampUs = mSurfaceTexture.getTimestamp() / 1000L;
        } else if (hasCameraSource()) {
            timeStampUs = mSurfaceTexture.getTimestamp() / 1000L;
            if (timeStampUs <= 0) {
                return;
            }
        }

        long displayTs = 0, videoTs = 0;

        if (isRenderTargetDisplay()) {
            if (((mPrevDisplayTs > timeStampUs && ((mPrevDisplayTs - timeStampUs) > 200 * 1000L)) || timeStampUs == 0 || mDisplayFrameReqCount == 0 || mSeekReqTime > 0) && !hasImageSource()) {
                mFirstDisplayFrameTs = timeStampUs - (mSeekReqTime > 0 ? mSeekReqTime : 0);
                mSeekReqTime = -1;
            }
            displayTs = timeStampUs - mFirstDisplayFrameTs;

            mPrevDisplayTs = timeStampUs;
            mDisplayRenderer.setFrameTimestamp(displayTs * 1000L);
            mDisplayRenderer.requestRender();
            ++mDisplayFrameReqCount;

            if (mSceneFrameCount == 0) {
                mFirstSceneFrameTs = displayTs;
            }
            mSceneFrameCount++;
        }

        if (isRenderTargetVideo() && hasVisualSource() && mIsRecording) {
            if (mVideoRenderer == null || !mVideoRenderer.canStartRecording()) {
                Log.w(TAG, "Frame render request in invalid state");
                return;
            }

            if ((mVideoFrameReqCount == 0) && !hasImageSource()) {
                mFirstVideoFrameTs = timeStampUs;
            }

            videoTs = timeStampUs - mFirstVideoFrameTs;

            if (videoTs == mPrevVideoTs) return;
            mPrevVideoTs = videoTs;

            if (VERBOSE)
                Log.d(TAG, "video: " + "frame count: " + mVideoFrameReqCount + " firstFrameTs: " + mFirstVideoFrameTs + " ts: " + timeStampUs + " final video ts: " + videoTs);

            mVideoRenderer.setFrameTimestamp(videoTs * 1000L);
            mVideoRenderer.setDisplayOffset((mFirstDisplayFrameTs - mFirstVideoFrameTs) / 1000L);

            mVideoRenderer.requestRender();
            // Render atleast one frame after recording is stopped,
            // so that VideoEncoder drains the encoder and passes EOS and finally stops it
            if (mRequestVideoRecordingStop) {
                mRequestVideoRecordingStop = false;
            }
            ++mVideoFrameReqCount;
        }

        if (isRenderTargetImage()) {
            mRenderTarget &= ~RENDER_TARGET_IMAGE;
            mCaptureRenderer.setFrameTimestamp(USE_PREFERRED_TIMESTAMP ? mScene.getCaptureTimestamp() * 1000L * 1000L : (displayTs - mFirstSceneFrameTs) * 1000L);
            mCaptureRenderer.requestRender();
        }
    }

    private boolean hasAnyAudioSource() {
        return hasMicAudioSource() || hasAudioSource() || hasBlankAudioSource();
    }

    private boolean hasMicAudioSource() {
        return (mSourceMedia & MEDIA_TYPE_MIC) > 0;
    }

    private boolean hasBlankAudioSource() {
        return (mSourceMedia & MEDIA_TYPE_BLANK_AUDIO) > 0;
    }

    public boolean zoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        return mCameraPreview != null && mCameraPreview.zoomTo(zoom);
    }

    private void setZoom(float zoom) {
        mZoomLevel = zoom;
        if (mDisplayRenderer != null) {
            mDisplayRenderer.setZoom(zoom);
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.setZoom(zoom);
        }
    }

/*    public boolean handleOnTouch(MotionEvent event) {
        mScaleListener.onTouchEvent(event);
        mGestureListener.onTouchEvent(event);
        return true;
    }*/

    public void resumeView() throws Exception {
        if (mGLSurfaceView != null && mGLSurfaceView instanceof RGLSurfaceView) {
            ((RGLSurfaceView) mGLSurfaceView).onResume();
        }

        Log.d(TAG, "resumeView complete: ");
    }

    public void pauseView() {
        if (mGLSurfaceView != null && mGLSurfaceView instanceof RGLSurfaceView) {
            ((RGLSurfaceView) mGLSurfaceView).onPause();
        }

        Log.d(TAG, "pauseView complete ");
    }

    public void swapCamera() {
        mMediaEventListener.onRequestCameraSwap();
        stopCameraPreview();

        mCameraSwapped = true;
        mCameraPreview.swapAndOpenCamera();
    }

    private void resumeCameraPreview() {
        if (mCameraPreview != null) {
            mCameraPreview.resumePreview();
        }
    }

    private void pauseCameraPreview() {
        if (mCameraPreview != null) {
            mCameraPreview.pausePreview();
        }
    }

    private void stopCameraPreview() {
        if (mCameraPreview != null) {
            mCameraPreview.pausePreview();
            mCameraPreview.stopPreview();
        }
    }

    public void destroy() {
//        mRenderManager.destroy();
        this.mGLSurfaceView = null;
        this.mContext = null;
    }

    public void renderBitmap(Bitmap bitmap) {
        //mRenderManager.flagForReset();
    }

    private int[] queryBitmapSize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int[] size = new int[2];
        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return size;
    }

    private void setTargetView(SurfaceView glSurfaceView) {
        if (glSurfaceView instanceof RGLSurfaceView) {

            if (mDisplayRenderer != null) {
                mDisplayRenderer.setDestinationView((GLSurfaceView) glSurfaceView);
                // Bind GLSurfaceView with Renderer
                ((RGLSurfaceView) glSurfaceView).setRenderer(mDisplayRenderer);
                ((RGLSurfaceView) glSurfaceView).setCompatMode(isCompat);
            }
        }
    }

    public void setMediaEventListener(CameraEventListener cameraEventListener) {
        mMediaEventListener = cameraEventListener;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    @Override
    public void onMediaExtractorPrepared(final SessionConfig config) {
        mSourceConfig = config;
        mExtractorReady = true;
        checkReadyToRender();

/*        // Let the upper layer call startPlayback or startRendering
        // unless it's auto
        if (isAutoPlay) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) && ((mRenderTarget & RENDER_TARGET_VIDEO) == 0)) {
                        startPlayback();
                    } else {
                        try {
                            startRecording();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }*/
    }

    private synchronized void checkReadyToRender() {
        if ((!hasVisualSource() || mRendererReady)
                && (!hasExternalAVSource() || mExtractorReady)
                && (!isRenderTargetVideo() || mRecorderReady)) {
            mUiHandler.sendMessage(Message.obtain(mUiHandler, UiHandler.RENDERER_PREPARED, mSourceConfig));
        }
    }

    private boolean hasExternalAVSource() {
        return (mSourceMedia & (MEDIA_TYPE_VIDEO | MEDIA_TYPE_AUDIO)) > 0;
    }

    @Override
    public void onMediaExtractionFinished() {
        // Check if media extractor is NULL, because onMediaExtractionFinished can be a dummy call as well.
        if (mMediaPlayer != null && hasAudioSource() && hasVideoSource()) {
            stopRecording();
        } else if (mMediaPlayer != null && (hasAudioSource())) {
            stopAudioRecording();
        } else if (mMediaPlayer != null && (hasVideoSource())) {
            stopVideoRecording(true);
        }

        mExtractionFinished = true;
        checkRecorderDone();
    }

    private boolean hasVideoSource() {
        return ((mSourceMedia & MEDIA_TYPE_VIDEO)) > 0;
    }

    private boolean hasAudioSource() {
        return (mSourceMedia & MEDIA_TYPE_AUDIO) > 0;
    }

    @Override
    public boolean onFrameAvailable(long presentationTimeUs) {
        return true; //(isRenderTargetOnlyVideo());
    }

    @Override
    public void sendAudioFrame(ByteBuffer data, MediaCodec.BufferInfo bufferInfo, boolean endOfStream) {
        synchronized (this) {
            if (!mRendering) {
                Log.d(TAG, "sendAudioFrame: mRendering: false. Returning.");
                return;
            }
        }
        if (isRenderTargetVideo() && hasAnyAudioSource()) {
            mAudioFrameCount++;
            mAudioEncoder.sendAudioToEncoder(data, bufferInfo, endOfStream);
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.setPrevAudioFrameTs(bufferInfo.presentationTimeUs);
        }
        if ((isRenderTargetDisplay()) && !isRenderTargetVideo()) {
//            mMediaPlayer.setPrevAudioFrameTs(bufferInfo.presentationTimeUs);
        }
    }

    @Override
    public void sendDirectAudioFrame(Muxer.MediaFrame mediaFrame) {
        synchronized (this) {
            if (!mRendering) return;
        }
        mAudioFrameCount++;
        mAudioFeeder.writeFrame(mediaFrame);
    }

    @Override
    public void addTrack(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("audio/")) {
            Log.d(TAG, "Adding audio track from extractor");
            try {
                mAudioFeeder.addTrack(format);
            } catch (Exception e) {
                Log.w(TAG, "Unsupported format: " + format + ". Trying audio AAC");
                format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
                mAudioFeeder.addTrack(format);
            }
        } else {
            Log.d(TAG, "Ignoring video track from extractor");
//            mVideoFeeder.addTrack(format);
        }
    }

    public void setSourcePath(String path) {
        mSourceFilePath = path;
        releaseMediaExtractor();
        if (FileUtilities.isFileTypeImage(path)) {
            SessionConfig.Builder configBuilder = new SessionConfig.Builder(mSourceFilePath);
            int[] size = new int[2];
            int status = AndroidUtilities.getMediaSize(path, size);
            if (status >= 0) {
                configBuilder.withVideoResolution(size[0], size[1]);
            }
            mSourceConfig = configBuilder.build();
        } else if (FileUtilities.isFileTypeVideo(path)) {
            SessionConfig.Builder configBuilder = new SessionConfig.Builder(mSourceFilePath)
                    .withVideoBitrate(kbps * 1024);

            int[] size = new int[2];

            int status = AndroidUtilities.getMediaSize(path, size);

            int videoBitrate = AndroidUtilities.getVideoBitrate(path);
            int audioBitrate = AndroidUtilities.getAudioBitrate(path);
            int trackCount = AndroidUtilities.getAudioChannelCount(path);
            int audioSampleRate = AndroidUtilities.getAudioSampleRate(path);

            int videoWidth, videoHeight;
            if (status >= 0) {
                videoWidth = size[0];
                videoHeight = size[1];
            } else {
                videoWidth = 720;
                videoHeight = 1280;
            }
            configBuilder = configBuilder
                    .withVideoResolution(videoWidth, videoHeight)
                    .withVideoBitrate(videoBitrate);

            configBuilder.withAudioBitrate(Math.min(audioBitrate, 96000))
                    .withAudioChannels(Math.min(trackCount, 2))
                    .withAudioSampleRate(Math.min(audioSampleRate, 48000));

            mSourceConfig = configBuilder.build();
        }
    }

    public void setClipSize(long startTime, long endTime) {
        mStartTime = startTime * 1000;
        mEndTime = endTime * 1000;
        mDuration = mEndTime - mStartTime;
        if (mMediaPlayer != null) {
            mMediaPlayer.setClipSize(mStartTime, mEndTime); //us
        }
    }

    public void onStart() {
        startCameraPreviewSync();
    }

    public void onResume() {
        resumeCameraPreview();
        if (mMediaPlayer != null && mMediaPlayer.isPrepared()) {
            mMediaPlayer.start();
        }
    }

    public void onPause() {
        pauseCameraPreview();
    }

    public void onStop() {
        if (mMediaPlayer != null && mMediaPlayer.isPrepared()) {
            mMediaPlayer.pause();
        }
        stopCameraPreview();
        if (isRenderTargetVideo()) {
            stopRecording(true);
        }
    }

    public void resetPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        stopCameraPreview();
    }

    public synchronized void onDestroy() {
        reset();
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mUiHandler.removeCallbacksAndMessages(null);

        mRendering = false;

        releaseMediaExtractor();
        if (mCaptureRenderer != null) {
            mCaptureRenderer.destroy();
            mCaptureRenderer = null;
        }
        if (mDummyRenderer != null) {
            mDummyRenderer.release();
            mDummyRenderer = null;
        }
        // TODO critical fix before any release
        // TODO either shouldn't happen from non-GL thread or should be synchronized.
        if (mDisplayRenderer != null) {
            mDisplayRenderer.destroy();
            mDisplayRenderer = null;
        }
        if (mVideoRenderer != null) {
            mVideoRenderer.stopRecording();
            mVideoRenderer = null;
        }
        mSurfaceTexture = null;
        if (mCameraPreview != null) {
            mCameraPreview.destroyPreview();
        }
    }

    public void captureImage(final CameraCaptureCallback captureCallback) {
        captureImage(captureCallback, !isCameraCompat() ? CAPTURE_MODE_RENDER_OFFSCREEN : CAPTURE_MODE_PICTURE);
    }

    public void captureImage(final CameraCaptureCallback captureCallback, int captureMode) {
        switch (captureMode) {
            case CAPTURE_MODE_ONESHOT:
                captureImageOneShot(captureCallback);
                break;
            case CAPTURE_MODE_RENDER_OFFSCREEN:
                captureImageUsingRenderer(captureCallback);
                break;
            case CAPTURE_MODE_PICTURE:
            default:
//                takeFocusPicture(captureCallback);
                takePicture(captureCallback);
        }
    }

    public void captureImageOneShot(final CameraCaptureCallback captureCallback) {
        Log.d(TAG, "capture Image");
        mCameraPreview.takePictureUsingOneshotcallback(new CameraCaptureCallback() {
            @Override
            public void onShutter() {
                if (captureCallback != null) {
                    captureCallback.onShutter();
                }
            }

            @Override
            public void onCameraCapture(Bitmap bitmap, String dummy, int orientation, long maxFileSizeBytes) {
                mCameraPreview.startPreview();
                String path = null;
                try {
                    path = saveBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (captureCallback != null) {
                    captureCallback.onCameraCapture(bitmap, path, orientation, maxFileSizeBytes);
                }
            }
        });
    }

    public void captureImageUsingRenderer(final CameraCaptureCallback captureCallback) {
        if (CAPTURE_BLIT_DISPLAY) {
            final Bitmap bitmap = mDisplayRenderer.captureFrameBuffer();
            String path = null;
            if (captureCallback != null) {
                AndroidUtilities.runOnUIThread(new CameraCaptureRunnable(captureCallback, bitmap, path, ExifInterface.ORIENTATION_NORMAL));
            }
        } else {
            mCaptureRenderer.setFilter(mFilter);
            mCaptureRenderer.setCaptureCallback(new CameraCaptureCallback() {
                @Override
                public void onShutter() {
                    /*if (captureMode == CAPTURE_MODE_RENDER_OFFSCREEN) {
                        if (isFlashEnabled() && !isFrontCamera()) {
                            forceFlash();
                        }
                    }*/
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            captureCallback.onShutter();
                        }
                    });
                }

                @Override
                public void onCameraCapture(final Bitmap bitmap, final String path, final int orientation, final long maxFileSizeBytes) {
                   /* if (isFlashEnabled() && !isFrontCamera()) {
                        enableFlash(false);
                    }*/
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            captureCallback.onCameraCapture(bitmap, path, orientation, maxFileSizeBytes);
                        }
                    });
                }
            });
            mRenderTarget |= RENDER_TARGET_IMAGE;
            requestRender();
        }
    }

    public void takePicture(final CameraCaptureCallback captureCallback) {
        mCameraPreview.takePicture(new CameraPreview.CameraPictureCallback() {

            @Override
            public void onShutter() {
                if (captureCallback != null) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            captureCallback.onShutter();
                        }
                    });
                }
            }

            @Override
            public void onCameraCaptured(final byte[] data, String path) {
                mCameraPreview.startPreview();
                handleCameraCapture(captureCallback, data, path, true);
            }
        });
    }

    public void setCameraBrightness(@FloatRange(from = 0F, to = 1F) float val) {
        if (val > 1F || val < 0F) return;
        mCameraPreview.setBrightness(val);

    }

    public float getCameraBrightness() {
        return mCameraPreview.getBrightness();
    }

    @Override
    public void setSaturation(float saturation) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setSaturation(saturation);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setSaturation(saturation);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setSaturation(saturation);
        }
        requestRender();
    }

    @Override
    public void setContrast(float contrast) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setContrast(contrast);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setContrast(contrast);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setContrast(contrast);
        }
        requestRender();
    }

    @Override
    public void setBrightness(float brightness) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setBrightness(brightness);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setBrightness(brightness);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setBrightness(brightness);
        }
        requestRender();
    }

    @Override
    public void setSceneRotateInZ(double sceneRotateInZ) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setSceneRotateInZ(sceneRotateInZ);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setSceneRotateInZ(sceneRotateInZ);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setSceneRotateInZ(sceneRotateInZ);
        }
        requestRender();
    }

    @Override
    public void setSceneZoom(double[] sceneZoom) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setSceneZoom(sceneZoom);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setSceneZoom(sceneZoom);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setSceneZoom(sceneZoom);
        }
        requestRender();
    }

    @Override
    public void setSceneTranslateByInfo(double[] sceneTranslateByInfo) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setSceneTranslateByInfo(sceneTranslateByInfo);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setSceneTranslateByInfo(sceneTranslateByInfo);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setSceneTranslateByInfo(sceneTranslateByInfo);
        }
        requestRender();
    }

    @Override
    public void setSceneRectFInfo(RectF rectFInfo) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().setSceneRectFInfo(rectFInfo);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().setSceneRectFInfo(rectFInfo);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().setSceneRectFInfo(rectFInfo);
        }
        requestRender();
    }

    @Nullable
    public SceneDescription cropMedia() {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            return mDisplayRenderer.getSceneAdjustmentsInterface().cropMedia();
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            return mVideoRenderer.getSceneAdjustmentsInterface().cropMedia();
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            return mCaptureRenderer.getSceneAdjustmentsInterface().cropMedia();
        }
        return null;
    }

    @Override
    public void addLut(SceneManager.LutDescription leftLut, SceneManager.LutDescription rightLut, float transitionPoint, float intensity) {
        if (mDisplayRenderer != null && mDisplayRenderer.getSceneAdjustmentsInterface() != null) {
            mDisplayRenderer.getSceneAdjustmentsInterface().addLut(leftLut, rightLut, transitionPoint, intensity);
        }
        if (mVideoRenderer != null && mVideoRenderer.getSceneAdjustmentsInterface() != null) {
            mVideoRenderer.getSceneAdjustmentsInterface().addLut(leftLut, rightLut, transitionPoint, intensity);
        }
        if (mCaptureRenderer != null && mCaptureRenderer.getSceneAdjustmentsInterface() != null) {
            mCaptureRenderer.getSceneAdjustmentsInterface().addLut(leftLut, rightLut, transitionPoint, intensity);
        }
        requestRender();
    }

    private void handleCameraCapture(final CameraCaptureCallback captureCallback, final byte[] data
            , String path, boolean retry) {
        if ((data == null || data.length == 0) && (path == null || path.isEmpty())) {
            AndroidUtilities.showShortToast("Unable to capture!\n Please try again");
            return;
        }

        int exifOrientation = mCameraPreview.getCaptureOrientation();
        int rotation = AndroidUtilities.getRotationFromOrientation(exifOrientation);

        try {
            Matrix matrix = new Matrix();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.showShortToast(AndroidUtilities.getString(R.string
                                .something_went_wrong));
                        HashMap<String, String> map = new HashMap<>(2);
                        map.put("data", String.valueOf(data));
                        map.put("length", String.valueOf(data.length));
                        EventTrackUtil.logDebug("cannotdecodecaptured", "onCameraCaptured", RenderManager.class.getName(), map, 4);
                    }
                });
                return;
            }
            Bitmap outBitmap = bitmap;
            if (EXIF_ROTATION_SUPPORTED) {
                if (AndroidUtilities.isFlipVertical(exifOrientation)) {
                    matrix.setScale(1, -1);
                    rotation = 360 - rotation;
                } else if (AndroidUtilities.isFlipHorizontal(exifOrientation)) {
                    matrix.setScale(-1, 1);
                    rotation = 360 - rotation;
                }
            } else {
                matrix = AndroidUtilities.getImageMatrixFromExif(exifOrientation);
                rotation = 0;
            }

            if (!matrix.isIdentity()) {
                outBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                bitmap.recycle();
            }
            mCameraPreview.setFocusMode(FOCUS_MODE_PICTURE);
            if (captureCallback != null) {
                AndroidUtilities.runOnUIThread(new CameraCaptureRunnable(captureCallback, outBitmap, path, rotation));
            }
        } catch (OutOfMemoryError e) {
            if (retry) {
                CrashlyticsWrapper.log(4, "RenderManager", "OutofMemory: Trying again");
                CrashlyticsWrapper.logException(e);
                HashMap<String, String> map = new HashMap<>(2);
                map.put("failure", "OOM");
                EventTrackUtil.logDebug("handleCameraCapture", "OOM, trying again", RenderManager.class.getName(), map, 4);
                System.gc();
                handleCameraCapture(captureCallback, data, path, false);
            } else {
                CrashlyticsWrapper.log(4, "handleCameraCapture", "OutofMemory again. Quitting");
                CrashlyticsWrapper.logException(e);
                EventTrackUtil.logDebug("handleCameraCapture", "OOM again, quitting", RenderManager.class.getName(), null, 4);
            }
        }
    }
/*
    public void takeFocusPicture(final CameraCaptureCallback captureCallback) {
        mCameraPreview.manualFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    if (VERBOSE) Log.d(TAG, "auto focus success. Going to capture");
                    takePicture(captureCallback);
                } else {
                    mCameraPreview.manualFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                Log.d(TAG, "auto focus 2nd try success");
                            } else {
                                if (VERBOSE)
                                    Log.d(TAG, "auto focus 2nd try failed");
                            }
                            Log.d(TAG, "auto focus 2nd try. Going to capture");
                            takePicture(captureCallback);
                        }
                    }, null, 0);
                    if (VERBOSE) Log.w(TAG, "auto focus failed");
                }
            }
        }, null, 0);
    }*/

    private String saveRawImage(byte[] data) throws IOException {
        File file = FileUtilities.generateMediaFile(FileUtilities.FILE_TYPE_JPG, FileUtilities.MEDIA_DIR_IMAGE);
        if (null != file) {
            AndroidUtilities.writeRawData(data, file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        return null;
    }

    private String saveBitmap(Bitmap bitmap) throws IOException {
        File file = FileUtilities.generateMediaFile(FileUtilities.FILE_TYPE_WEBP, FileUtilities.MEDIA_DIR_IMAGE);
        if (null != file) {
            AndroidUtilities.writeImage(bitmap, file.getAbsolutePath(), Bitmap.CompressFormat.WEBP, 100);
            return file.getAbsolutePath();
        }

        return null;
    }

    public void setAutoFocus() {
        if (mCameraPreview != null) {
            mCameraPreview.autoFocus();
        }
    }

    public void manualFocus(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y) {
        /*RectF transformedRect;
        if (!cameraCompat && mDisplayRenderer != null) {
            transformedRect = mDisplayRenderer.getTransformedCoord(x, y);
        } else {
            transformedRect = mCameraPreview.getTransformedCordCompat(x, y);
        }
        if (transformedRect == null) {
            CrashlyticsWrapper.log("manualfocus: transformed rect is null");
            return;
        }
*/
        mCameraPreview.manualFocus(x, y);
    }

    public boolean hasFlash() {
        return mCameraPreview.hasFlash();
    }

    private void forceFlash() {
        mCameraPreview.forceFlash();
    }

    public boolean isFlashEnabled() {
        return mCameraPreview.isFlashEnabled();
    }

    public void enableFlash(boolean flag) {
        mCameraPreview.enableFlash(flag);
    }

    public boolean isFrontCamera() {
        return mCameraPreview != null && mCameraPreview.mCameraProps != null && mCameraPreview.mCameraProps.isFrontCamera();
    }

    private boolean isImageOnlySource() {
        return mSourceMedia == MEDIA_TYPE_IMAGE;
    }

    // release the media recorder
    public void releaseMediaRecorderCompat() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();  // stop the recording
                mMediaRecorder.reset();   // clear recorder configuration
                mMediaRecorder.release(); // release the recorder object
                mMediaRecorder = null;
            } catch (Exception e) {
                //Crashlytics.logException(e);
            }
        }
    }

    // prepare video media recorder
    private boolean prepareVideoRecorderCompat() {
        try {
            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            mCameraPreview.unlock();
            android.hardware.Camera camera = (mCameraPreview instanceof CameraControl) ? ((CameraControl) mCameraPreview).mCamera : null;
            mMediaRecorder.setCamera(camera);

            // Step 2: Set sources
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3(a): Set the recorder orientation
            if (isFrontCamera()) {
                mMediaRecorder.setOrientationHint(360 - mCameraPreview.getDisplayOrientation());
            } else {
                mMediaRecorder.setOrientationHint(mCameraPreview.getDisplayOrientation());
            }

            // Step 3(b): Setup the recorder profile and other configuration
            boolean recorderConfigured = setRecorderProfileCompat();
            if (!recorderConfigured) {
                EventTrackUtil.logDebug("VideoCompat", "PrepareMediaRecorder", "RenderManager", null, 4);
                return false;
            }
            mMediaRecorder.setMaxDuration(maxDuration); // 500 seconds

            // Step 4: Set output file
            mMediaRecorder.setOutputFile(mOutputFilePath);

            // Step 5: Set the preview output
            if (camera == null) {
                mMediaRecorder.setPreviewDisplay(mGLSurfaceView.getHolder().getSurface());
            }

            // Step 6: Prepare configured MediaRecorder
            mMediaRecorder.prepare();

        } catch (IllegalStateException e) {
            releaseMediaRecorderCompat();
            return false;
        } catch (Exception e) {
            releaseMediaRecorderCompat();
            return false;
        }
        return true;
    }

    public void setDoLoop(boolean doLoop) {
        this.doLoop = doLoop; //((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) && ((mRenderTarget & RENDER_TARGET_VIDEO) == 0)
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(this.doLoop);
        }
    }

    // Set the media recorder profile
    private boolean setRecorderProfileCompat() {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        int width = 1280;
        mMediaRecorder.setVideoSize(width, (int) (width / mCameraPreview.mCameraProps.getPreviewAspectRatio()));

        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(profile.videoCodec);
        if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
                profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
            // Nothing needs to be done. Call to setCaptureRate() enables
            // time lapse video recording.
        } else {
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioChannels(profile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(profile.audioCodec);
        }

        File output = FileUtilities.generateMediaFile(FileUtilities.FILE_TYPE_MP4, FileUtilities.MEDIA_DIR_VIDEO);
        if (null != output) {
            mOutputFilePath = output.getAbsolutePath();
        } else {
            publishOnComplete(null);
            return false;
        }
        Log.d(TAG, "Recording to: " + mOutputFilePath);
        SessionConfig.Builder configBuilder = new SessionConfig.Builder(mOutputFilePath)
                .withVideoBitrate(profile.videoBitRate);
        int videoWidth = profile.videoFrameWidth;
        int videoHeight = profile.videoFrameHeight;

        configBuilder = configBuilder
                .withVideoResolution(videoWidth, videoHeight);

        configBuilder.withAudioBitrate(profile.audioBitRate)
                .withAudioChannels(profile.audioChannels)
                .withAudioSampleRate(profile.audioSampleRate);

        if (hasVisualSource()) {
            configBuilder.withVideoDecoded();
        }
        if (hasAnyAudioSource()) {
            configBuilder.withAudioDecoded();
        }

        mVideoConfig = configBuilder.build();
        return true;
    }

    public void setSpeed(float speed) {
        mPlaybackSpeed = speed;
        if (mMediaPlayer != null) {
            mMediaPlayer.setPlaybackSpeed(speed);
        }
    }

    public void setDisplayRenderCreationListener(ActionListener displayRenderCreationListener) {
        this.displayRenderCreationListener = displayRenderCreationListener;
    }

    public ActionListener getDisplayRenderCreationListener() {
        return displayRenderCreationListener;
    }

    public void setOverlayDrawable(Drawable drawable) {
        mOverlayDrawable = drawable;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCameraPreview != null) {
            mCameraPreview.surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCameraPreview != null) {
            mCameraPreview.surfaceDestroyed(holder);
        }
//        synchronized (this) {
//            mRendering = false;
//            mDisplayRenderer = null;
//        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCameraPreview != null) {
            mCameraPreview.surfaceChanged(holder, format, w, h);
        }
    }

    public void cancelCaptureRequest() {
        if (mCaptureRenderer != null) {
            mCaptureRenderer.cancelCaptureRequest();
        }
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public String getStoryId() {
        return storyId;
    }

    public interface AVRenderListener extends AVComponentListener {
        void readyForFrames();
    }

    public interface AVComponentListener {
        void onStarted(@Nullable final SessionConfig config);

        void onPrepared(@Nullable final SessionConfig config);

        void onProgressChanged(@Nullable final SessionConfig config, long timestamp);

        void onCompleted(@Nullable final SessionConfig config);

        void onCancelled(@Nullable final SessionConfig config, boolean error);
    }

    private class ZoomRunnable implements Runnable {

        float mInitialZoom, mFinalZoom;
        float zoomStep;
        int mDuration;

        float mZoomLevel;

        ZoomRunnable(float initialZoom, float finalZoom, int duration) {
            mInitialZoom = initialZoom;
            mFinalZoom = finalZoom;
            mDuration = duration;
        }

        @Override
        public void run() {
            mZoomLevel += zoomStep;
            setZoom(mZoomLevel);
            if (Math.abs(mZoomLevel - finalZoom) > zoomStep) {
                if (mGLSurfaceView instanceof RGLSurfaceView) {
                    ((RGLSurfaceView) mGLSurfaceView).queueEvent(this);
                }
            }
        }
    }

    private class ScaleDetectorListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        float scaleFocusX = 0;
        float scaleFocusY = 0;

        @Override
        public boolean onScale(ScaleGestureDetector arg0) {
            float scale = arg0.getScaleFactor() * mZoomLevel;

            mZoomLevel = scale;
            mZoomLevel = Math.max(1.0f, Math.min(mZoomLevel, 3.0f));
//            setScale(scale);
            return true;
        }
    }

    private final class CustomGestureListener implements GestureDetector.OnGestureListener {
        private static final int SWIPE_THRESHOLD = 500;
        private static final int SWIPE_VELOCITY_THRESHOLD = 500;

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean result = false;
/*            if(e.getX() > 3*mPreviewWidth/4) {
                nextSlide();
                result = true;
            } else if(e.getX() < mPreviewWidth/4) {
                prevSlide();
                result = true;
            } else {*/
//            toggleSlideshow();
            result = true;
/*            }*/
            return result;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //setTranslate(distanceX, distanceY);
//            moveCamera(distanceX*mZoomLevel, distanceY*mZoomLevel);
            //Passing negative of the values because the distance(X/Y) passed has been measured from dest to source
            mDisplayRenderer.translateCanvas(-distanceX / mPreviewWidth, -distanceY / mPreviewWidth);
            if (mVideoRenderer != null) {
                mVideoRenderer.translateCanvas(-distanceX / mPreviewWidth, -distanceY / mPreviewWidth);
            }
            requestRender();
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
//                            onSwipeRight();
                        } else {
//                            onSwipeLeft();
                        }
                    }
                    result = true;
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
//                        onSwipeBottom();
                    } else {
//                        onSwipeTop();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    public boolean isCamera2() {
        return isCamera2;
    }

    private class CameraCaptureRunnable implements Runnable {
        private String mPath;
        private Bitmap mBitmap;
        private int rotation;

        private CameraCaptureCallback mCaptureCallback;

        CameraCaptureRunnable(CameraCaptureCallback captureCallback, Bitmap bitmap, String path, int rotation) {
            mCaptureCallback = captureCallback;
            mBitmap = bitmap;
            mPath = path;
            this.rotation = rotation;
        }

        @Override
        public void run() {
            // TODO send the correct size of image
            mCaptureCallback.onCameraCapture(mBitmap, mPath, rotation, 0);
        }
    }

    public void setUsePreviousSceneDescription(boolean mUsePreviousSceneDescription) {
        this.mUsePreviousSceneDescription = mUsePreviousSceneDescription;
    }

    public void invalidateScene(SceneManager.SceneName sceneName) {
        SceneDescription sceneDescription = null;
        if (mUsePreviousSceneDescription && mSceneDesc != null) {
            sceneDescription = mSceneDesc.clone();
            sceneDescription.sceneName = sceneName;
        }
        invalidateScene(sceneName, sceneDescription);
    }

    public void invalidateScene(SceneManager.SceneDescription sceneDescription) {
        mSceneDesc = sceneDescription;
        invalidateScene(null, sceneDescription);
    }

    private void invalidateScene(SceneManager.SceneName sceneName, SceneManager.SceneDescription sceneDescription) {
        if (sceneName == null) {
            sceneName = SceneManager.SceneName.SCENE_DEFAULT;
        }

        if (sceneDescription == null) {
            sceneDescription = new SceneDescription(sceneName);
        }

        mSceneDesc = sceneDescription;
        Scene scene = createScene();
        invalidateScene(scene);
    }

    private void invalidateScene(Scene scene) {
        mScene = scene;
        if (scene == null) return;
        if (mDisplayRenderer != null) {
            mDisplayRenderer.invalidateScene(mScene);
        }

        if (mVideoRenderer != null) {
            mVideoRenderer.invalidateScene(mScene);
        }

        if (mCaptureRenderer != null) {
            mCaptureRenderer.invalidateScene(mScene);
        }
        mSceneFrameCount = 0;
        requestRender();
    }

    @NonNull
    public SceneDescription getSceneDesc() {
        return mSceneDesc;
    }

    public void setFilter(List<String> filter) {
        mFilter = filter != null ? filter : Collections.singletonList(FilterManager.IMAGE_FILTER);
/*        BaseFilter baseFilter = FilterManager.createFilter(filter);
        setFilter(baseFilter);*/
    }

    private static class UiHandler extends Handler {
        static final int PUBLISH_PROGRESS = 0;
        static final int PUBLISH_ON_COMPLETE = 1;
        static final int RECORDING_STARTED = 2;
        static final int RENDERER_PREPARED = 3;
        static final int RENDERER_ERROR = 4;
        static final int CAMERA_SWAP = 5;

        private WeakReference<RenderManager> renderManager;

        UiHandler(RenderManager renderManager) {
            super(Looper.getMainLooper());
            this.renderManager = new WeakReference<>(renderManager);
        }

        @Override
        public void handleMessage(Message msg) {
            RenderManager mRenderManager = renderManager.get();
            if (mRenderManager == null) {
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {
                case PUBLISH_PROGRESS:
                    mRenderManager.handleProgress((long) msg.obj);
                    break;
                case PUBLISH_ON_COMPLETE:
                    mRenderManager.handleOnCompletion((SessionConfig) msg.obj);
                    break;
                case RECORDING_STARTED:
                    mRenderManager.handleRecordingStarted((SessionConfig) msg.obj);
                    break;
                case RENDERER_PREPARED:
                    mRenderManager.handleRenderPrepared((SessionConfig) msg.obj);
                    break;
                case RENDERER_ERROR:
                    mRenderManager.handleRenderError((SessionConfig) msg.obj);
                    break;
                case CAMERA_SWAP:
                    mRenderManager.handleCameraSwap();
                    break;
                default:
                    super.handleMessage(msg);
                    break;

            }
        }
    }

    private void handleCameraSwap() {
        mGLSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                mMediaEventListener.onCameraSwapped();
            }
        });

        Log.d(TAG, "handleCameraSwap");
        if (mCameraPreview.isCameraAvailable()) {
            setCameraPreviewSize(mCameraPreview.getCameraProperties().mPreviewWidth, mCameraPreview.getCameraProperties().mPreviewHeight);
        }
        setRenderParams();
    }

    private void handleRenderError(SessionConfig config) {
        if (mAVRenderListener != null) {
            mAVRenderListener.onCancelled(config, true);
        }
    }

    private void handleRenderPrepared(SessionConfig config) {
        if (mAVRenderListener != null) {
            mAVRenderListener.onPrepared(config);
        }
    }

    private void handleRecordingStarted(SessionConfig config) {
        if (mAVRenderListener != null) {
            mAVRenderListener.onStarted(config);
        }
    }

    private void handleOnCompletion(SessionConfig config) {
        if (!doLoop && isRenderTargetVideo()) {
            releaseMediaExtractor();
        }
        if (mAVRenderListener != null) {
            mAVRenderListener.onCompleted(config);
        }
        mRenderTarget &= ~RENDER_TARGET_VIDEO;
    }

    private void handleProgress(long timeStampUs) {
        if (mAVRenderListener != null) {
            mAVRenderListener.onProgressChanged(mSourceConfig, timeStampUs);
        }
        if (mScene != null) {
            mScene.onProgress(timeStampUs);
        }
    }
}
