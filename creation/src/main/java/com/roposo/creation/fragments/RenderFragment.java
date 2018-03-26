package com.roposo.creation.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.roposo.core.customInjections.CrashlyticsWrapper;
import com.roposo.core.util.ActionListener;
import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.MyLogger;
import com.roposo.creation.R;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.camera.CameraPreview;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.RGLSurfaceView;
import com.roposo.creation.graphics.RSurfaceView;
import com.roposo.creation.graphics.RenderManager;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.scenes.ISceneAdjustments;
import com.roposo.creation.listeners.CameraEventListener;

import java.io.IOException;
import java.util.List;

import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_CAMERA;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_IMAGE;
import static com.roposo.creation.graphics.GraphicsConsts.MEDIA_TYPE_VIDEO;
import static com.roposo.creation.graphics.GraphicsConsts.RENDER_TARGET_DISPLAY;
import static com.roposo.creation.graphics.GraphicsConsts.RENDER_TARGET_VIDEO;

/**
 * @author bajaj on 16/08/17.
 */

@RequiresApi(17)
public class RenderFragment extends Fragment implements CameraEventListener, RenderManager.AVRenderListener {
    private static final String TAG = "RenderFragment";
    Context mContext;

    View rootView;
    private Drawable mOverlayDrawable;
    private String mStoryId;
    private SceneManager.SceneName mScene;

    public RGLSurfaceView getmGLSurfaceView() {
        if (!(mGLSurfaceView instanceof RGLSurfaceView)) return null;
        return (RGLSurfaceView) mGLSurfaceView;
    }

    SurfaceView mGLSurfaceView;
    RenderManager mRenderManager;


    // Rm can have recoder and player
    // different interface

    CameraEventListener mCameraEventListener;
    RenderManager.AVComponentListener avComponentListener;

    private static final int RENDER_STATE_WAITING = 0;
    private static final int RENDER_STATE_READY = 1;
    private static final int RENDER_STATE_REQUESTED = 2;
    private static final int RENDER_STATE_STARTED = 3;

    private int mRenderState = RENDER_STATE_WAITING;

    private boolean mIsAutoPlay;
    private boolean mIsLoop;

    // Whether we're capturing a video or an image (applying filters etc to an image)
    private int mRenderTarget = MEDIA_TYPE_VIDEO;

    // Depending on there's a camera/video source involved, mSourceMedia type will change.
    private int mSourceMedia = MEDIA_TYPE_IMAGE;
    private String mVideoPath;
    private float mPlaybackVolume = 1.0f;
    private Drawable mRootDrawable;
    private long mStartTime;
    private long mEndTime;
    private float mPlaybackSpeed = 1.0f;
    private ActionListener displayRenderCreationListener;

    public static RenderFragment newInstance(CameraEventListener eventListener, int renderTargetType) {
        RenderFragment fragment = new RenderFragment();
        fragment.setCameraEventListener(eventListener);
        fragment.setRenderTargetType(renderTargetType);
        return fragment;
    }

    private void setRenderTargetType(int renderTargetType) {
        mRenderTarget = renderTargetType;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getActivity();

        // Should only happen for download case (renderTarget == RENDER_TARGET_VIDEO)
        if (mContext == null) {
            mContext = ContextHelper.applicationContext;
        }

        int prevRenderState = mRenderState;
        mRenderState = RENDER_STATE_READY;

        rootView = null;
        if ((mRenderTarget & RENDER_TARGET_DISPLAY) == 0 || mSourceMedia == GraphicsConsts.MEDIA_TYPE_AUDIO) {
            // No view required.

        } else {
            rootView = inflater.inflate(R.layout.fragment_render, container, false);

            if (isCompat() && ((mSourceMedia & MEDIA_TYPE_CAMERA) > 0)) {
                mGLSurfaceView = new RSurfaceView(mContext);
            } else {
                mGLSurfaceView = new RGLSurfaceView(mContext);
            }
            ((FrameLayout)rootView).addView(mGLSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (prevRenderState == RENDER_STATE_REQUESTED) {
                startPlayback(mVideoPath, mSourceMedia, mRenderTarget, avComponentListener,
                        mStartTime, mEndTime);
        }

        // Prepare for rendering Camera or Video
        return rootView;
    }

    @Override
    public void onStart() {
        if (!isRenderTargetOnlyVideo()) {
            super.onStart();
        }

        if (mRenderManager != null) {
            if (!mRenderManager.isCameraAvailable()) {
/*            mRenderManager.configure();
            rootView.findViewById(R.id.camera_info_box).setVisibility(mRenderManager.isCameraAvailable() ? View.GONE : View.VISIBLE);*/
            }
        }
    }

    @Override
    public void onResume() {
        if (!isRenderTargetOnlyVideo()) {
            super.onResume();
        }
        if (mRenderManager != null && isRenderTargetDisplay() && !isRenderTargetVideo()) {
            mRenderManager.onResume();
        }
    }

    @Override
    public void onPause() {
        if (!isRenderTargetOnlyVideo()) {
            super.onPause();
        }
        if (mRenderManager != null && isRenderTargetDisplay() && !isRenderTargetVideo()) {
            mRenderManager.onPause();
        }

    }

    @Override
    public void onStop() {
        if (!isRenderTargetOnlyVideo()) {
            super.onStop();
        }
        handleStop();
    }

    public void startPlayback(String path,
                              int mediaType,
                              int renderTargetType,
                              RenderManager.AVComponentListener listener,
                              long startTime, long endTime) throws IllegalStateException {
        this.mStartTime = startTime;
        this.mEndTime = endTime;
        avComponentListener = listener;
        switch (mRenderState) {
            case RENDER_STATE_READY: {
                mSourceMedia = mediaType;
                mRenderTarget = renderTargetType;
                // Temporary. Fix tonight.
                mVideoPath = path;
                Log.d(TAG, "startPlayback: " + " state: " + mRenderState);
                prepareRenderManager(mSourceMedia, mRenderTarget);
                mRenderManager.setSourcePath(mVideoPath);
                mRenderManager.handlePrepare(mSourceMedia, mRenderTarget);
                if (startTime < 0) {
                    startTime = 0;
                }
                if (endTime < 0) {
                    endTime = AndroidUtilities.getMediaDuration(mVideoPath);
                    if (mSourceMedia == GraphicsConsts.MEDIA_TYPE_IMAGE) {
                        endTime = 10000; //UIPref.getInstance().minMediaItemDuration;
                    }
                }
                mRenderManager.setClipSize(startTime, endTime);
                startRendering();
                break;
            }
            case RENDER_STATE_WAITING: {
                mSourceMedia = mediaType;
                mRenderTarget = renderTargetType;
                // Temporary. Fix tonight.
                mVideoPath = path;
                mRenderState = RENDER_STATE_REQUESTED;
                Log.d(TAG, "startPlayback: " + " state: " + mRenderState);
                if (isRenderTargetOnlyVideo()) {
                    forceStartLifecycle();
                }
                break;
            }
            case RENDER_STATE_REQUESTED: {
                MyLogger.w(TAG, "Ignoring! startPlayback already called");
                break;
            }
            case RENDER_STATE_STARTED: {
                IllegalStateException exception = new IllegalStateException("Already playing");
                CrashlyticsWrapper.logException(exception);
                throw exception;
            }
        }
    }

    private void forceStartLifecycle() {
        onCreateView(null, null, null);
        onStart();
        onResume();
    }

    private void forceStopLifecycle() {
        onPause();
        onStop();
        onDestroy();
    }

    public void startPlayback(String path,
                              int mediaType,
                              int renderTargetType,
                              RenderManager.AVComponentListener listener) throws IllegalStateException {
        startPlayback(path, mediaType, renderTargetType, listener, -1, -1);
    }

    public void setUsePreviousSceneDescription(boolean mUsePreviousSceneDescription) {
        if (mRenderManager != null) {
            mRenderManager.setUsePreviousSceneDescription(mUsePreviousSceneDescription);
        }
    }

    public void invalidateScene(SceneManager.SceneName sceneName) {
        mScene = sceneName;
        if (mRenderManager != null) {
            mRenderManager.invalidateScene(sceneName);
        }
    }

    public void invalidateSceneWith(SceneManager.SceneDescription sceneDescription) {
        if (mRenderManager != null) {
            mRenderManager.invalidateScene(sceneDescription);
        }
    }

    public SceneManager.SceneDescription getCurrentSceneDescription() {
        return mRenderManager != null ? mRenderManager.getSceneDesc() : null;
    }

    public void pausePlayback() {
        setKeepScreenOn(false);
        if (mRenderManager != null) {
            mRenderManager.pauseRecording();
        }
    }

    public void resumePlayback() {
        setKeepScreenOn(true);
        if (mRenderManager != null) {
            mRenderManager.resumeRendering();
        }
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.setKeepScreenOn(keepScreenOn);
        }
    }

    public void startCameraDisplay() {
        mSourceMedia = MEDIA_TYPE_CAMERA;
        mRenderTarget = RENDER_TARGET_DISPLAY;
        prepareRenderManager(mSourceMedia, mRenderTarget);
    }

    public void setVolume(float volumeLevel) {
        mPlaybackVolume = volumeLevel;
        if (mRenderManager != null) {
            mRenderManager.setAudioVolume(volumeLevel);
        }
    }

    public void setIsLoop(boolean isLoop) {
        mIsLoop = isLoop;
        if (mRenderManager != null) {
            mRenderManager.setDoLoop(isLoop);
        }
    }


    @Override
    public void onDestroy() {
        if (!isRenderTargetOnlyVideo()) {
            super.onDestroy();
        }
        release();
    }

    private void checkAndShowInfoBox() {
        View cameraInfoBox = rootView.findViewById(R.id.camera_info_box);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(AndroidUtilities.dp(2));
        drawable.setColor(Color.WHITE);
        cameraInfoBox.setBackground(drawable);
        if (mRenderManager.isCameraAvailable()) {
            cameraInfoBox.setVisibility(View.GONE);
        } else {
            cameraInfoBox.findViewById(R.id.permission_box).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent appSettingsIntent = AndroidUtilities.getInstalledAppDetailsIntent();
                    getActivity().startActivity(appSettingsIntent);
                }
            });
            cameraInfoBox.setVisibility(View.VISIBLE);
        }
        cameraInfoBox.setVisibility(mRenderManager.isCameraAvailable() ? View.GONE : View.VISIBLE);
    }

    public boolean isCameraAvailable() {
        return mRenderManager != null && mRenderManager.isCameraAvailable();
    }

    public boolean isFrontCamera() {
        return mRenderManager != null && mRenderManager.isFrontCamera();
    }

    protected void prepareRenderManager(int sourceMedia, int renderTarget) {
        if (mRenderManager == null) {
            mRenderManager = new RenderManager(mContext, sourceMedia, renderTarget, mGLSurfaceView);
            mRenderManager.setDisplayRenderCreationListener(this.displayRenderCreationListener);
        }

        if (mRootDrawable != null) {
            mRenderManager.addDrawable(mRootDrawable);
        }
        mRenderManager.setRenderListener(this);
        mRenderManager.setMediaEventListener(mCameraEventListener);

        mRenderManager.setDoLoop(mIsLoop);
        mRenderManager.setAudioVolume(mPlaybackVolume);
        mRenderManager.setSpeed(mPlaybackSpeed);
        if (mOverlayDrawable != null) {
            mRenderManager.setOverlayDrawable(mOverlayDrawable);
        }

        mRenderManager.setStoryId(mStoryId);
        if (mScene != null) {
            mRenderManager.invalidateScene(new SceneManager.SceneDescription(mScene));
        }
    }

    public void captureImage(final CameraPreview.CameraCaptureCallback captureCallback) {
        if (!mRenderManager.isCameraAvailable()) {
            return;
        }
        mRenderManager.captureImage(captureCallback);
    }

    public void swapCamera() {
        mRenderManager.swapCamera();
    }

    public boolean isCamera2() {
        return mRenderManager.isCamera2();
    }

    public void autoFocus() {
        mRenderManager.setAutoFocus();
    }

    public boolean isCompat() {
        return hasCameraSource() && isCameraCompat();
    }

    public boolean isCameraCompat() {
        return CameraPreview.cameraCompat;
    }

    private boolean hasCameraSource() {
        return (mSourceMedia & MEDIA_TYPE_CAMERA) > 0;
    }

    public float getCameraBrightness() {
        return mRenderManager.getCameraBrightness();
    }

    public void setCameraBrightness(@FloatRange(from = 0F, to = 1F) float val) {
        mRenderManager.setCameraBrightness(val);
    }

    ISceneAdjustments getSceneAdjustmentsInterface() {
        return mRenderManager;
    }

    public void runCameraCaptureAnimation() {
        mGLSurfaceView.setBackgroundColor(Color.BLACK);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGLSurfaceView.setBackground(null);
            }
        }, 40);
    }

    public boolean cannotStartRecording() {
        return mRenderManager != null && mRenderManager.containsCameraScene() && !mRenderManager.isCameraAvailable();
    }

    public void startRendering() {
        setKeepScreenOn(true);
        try {
            mRenderManager.startResumeRendering();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording(boolean kill) {
        setKeepScreenOn(false);
        if (mRenderManager == null) return;
        mRenderManager.zoomTo(0);
        mRenderManager.stopRecording(kill);
    }

    public boolean canFocus() {
        if (mRenderManager.isCameraAvailable() && !mRenderManager.isFrontCamera()) {
            return true;
        }
        return false;
    }

    public void manualFocus(@FloatRange(from = 0.0, to = 1.0) float x, @FloatRange(from = 0.0, to = 1.0) float y) {
        mRenderManager.manualFocus(x, y);
    }

    public boolean hasFlash() {
        return mRenderManager.hasFlash();
    }

    public void enableFlash(boolean flashEnable) {
        mRenderManager.enableFlash(flashEnable);
    }

    public boolean zoomTo(@FloatRange(from = 0F, to = 1F) float zoom) {
        if (mRenderManager == null) return false;
        return mRenderManager.zoomTo(zoom);
    }

    @Override
    public void onCameraOpened() {
        MyLogger.d(TAG, "onCameraOpened");
        checkAndShowInfoBox();
        if (mCameraEventListener != null) {
            mCameraEventListener.onCameraOpened();
        }
    }

    @Override
    public void onCameraDisconnected() {
        MyLogger.d(TAG, "onCameraDisconnected");
        checkAndShowInfoBox();
        if (mCameraEventListener != null) {
            mCameraEventListener.onCameraDisconnected();
        }
    }

    @Override
    public void onCameraError(int error) {
        MyLogger.d(TAG, "onCameraError");
        checkAndShowInfoBox();
        if (mCameraEventListener != null) {
            mCameraEventListener.onCameraError(error);
        }
    }

    @Override
    public void onRequestCameraSwap() {
        if (mCameraEventListener != null) {
            mCameraEventListener.onRequestCameraSwap();
        }
    }

    @Override
    public void onCameraSwapped() {
        if (mCameraEventListener != null) {
            mCameraEventListener.onCameraSwapped();
        }
    }

    @Override
    public void onStarted(SessionConfig config) {
        if (rootView != null) {
            rootView.setKeepScreenOn(true);
        }
        mRenderState = RENDER_STATE_STARTED;
        if (avComponentListener != null) {
            avComponentListener.onStarted(config);
        }
    }

    @Override
    public void onPrepared(SessionConfig config) {
        if (avComponentListener != null) {
            avComponentListener.onPrepared(config);
        }
        mRenderManager.startPlayback();
    }

    @Override
    public void onProgressChanged(SessionConfig config, long timestamp) {
        if (avComponentListener != null) {
            avComponentListener.onProgressChanged(config, timestamp);
        }
    }

    @Override
    public void readyForFrames() {

    }

    @Override
    public void onCompleted(SessionConfig config) {
        if (rootView != null) {
            rootView.setKeepScreenOn(false);
        }

        if (!isRenderTargetOnlyVideo() && (isRemoving() || isDetached() || !isAdded())) {
            return;
        }
        if (mRenderManager != null && mRenderManager.hasCameraSource()) {
            mRenderManager.zoomTo(0);
            mRenderManager.setAutoFocus();
        }

        if (isRenderTargetOnlyVideo()) {
            mRenderState = RENDER_STATE_WAITING;
        } else {
            mRenderState = RENDER_STATE_READY;
        }

        if (avComponentListener != null) {
            avComponentListener.onCompleted(config);
        }
        if (isRenderTargetOnlyVideo()) {
            forceStopLifecycle();
        }
        mRenderTarget &= ~RENDER_TARGET_VIDEO;
    }

    @Override
    public void onCancelled(SessionConfig config, boolean error) {
        if (!isRenderTargetOnlyVideo() && (isRemoving() || isDetached() || !isAdded())) {
            return;
        }
        if (avComponentListener != null) {
            avComponentListener.onCancelled(config, error);
        }
    }

    public void setCameraEventListener(CameraEventListener cameraEventListener) {
        mCameraEventListener = cameraEventListener;
    }

    public void setAVListener(RenderManager.AVComponentListener avListener) {
        avComponentListener = avListener;
    }

    public void addDrawable(Drawable drawable) {
        mRootDrawable = drawable;
        if (mRenderManager != null) {
            mRenderManager.addDrawable(drawable);
        }
    }

    public void seekTo(long time) {
        if (mRenderManager != null) {
            mRenderManager.seekTo(time);
        }
    }

    public void release() {
        if (avComponentListener != null && mRenderState > RENDER_STATE_READY) {
            avComponentListener.onCancelled(null, false);
        }
        if (null != mRenderManager) {
            mRenderManager.onDestroy();
            mRenderManager.releaseMediaRecorderCompat();
            mRenderManager = null;
        }
    }

    public void handleStop() {
        setKeepScreenOn(false);
        if (mRenderManager != null) {
            mRenderManager.onStop();
        }
    }

    public void setSpeed(float speed) {
        mPlaybackSpeed = speed;
        if (mRenderManager != null) {
            mRenderManager.setSpeed(speed);
        }
    }

    public void setFilter(List<String> filter) {
        if (mRenderManager != null) {
            mRenderManager.setFilter(filter);
        }
    }

    public void resetPlayer() {
        setKeepScreenOn(false);
        if (mRenderManager != null) {
            mRenderManager.resetPlayer();
        }
    }

    public void setOnDisplayRenderCreationListener(ActionListener displayRenderCreationListener) {
        this.displayRenderCreationListener = displayRenderCreationListener;
    }

    public void setOverlayDrawable(Drawable drawable) {
        mOverlayDrawable = drawable;
        if (mRenderManager != null) {
            mRenderManager.setOverlayDrawable(drawable);
        }
    }

    private boolean isRenderTargetVideo() {
        return (mRenderTarget & RENDER_TARGET_VIDEO) > 0;
    }

    private boolean isRenderTargetOnlyVideo() {
        return !isRenderTargetDisplay() && isRenderTargetVideo();
    }

    private boolean isRenderTargetDisplay() {
        return (mRenderTarget & RENDER_TARGET_DISPLAY) > 0;
    }

    public void cancelCaptureRequest() {
        if (mRenderManager != null) {
            mRenderManager.cancelCaptureRequest();
        }
    }

    public void setStoryId(String storyId) {
        this.mStoryId = storyId;
    }
}
