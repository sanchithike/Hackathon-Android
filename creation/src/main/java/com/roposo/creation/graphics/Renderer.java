package com.roposo.creation.graphics;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

/**
 *
 * @author bajaj
 * Abstract base class for all Renderers
 * <p>
 *     There can be instances of Renderers which don't render anything.
 *     for example, DummyRenderer just takes care of creating shared Textures/Surface textures \
 *     over a separate thread to be used by all existing Renderers supposed to share data.
 * </p>
 */
public abstract class Renderer {
    String TAG = "Renderer";
    Context mContext;

    /* Properties for External texture which need to persist */
    // TODO minor There needs to be support for multiple external texture based sources.
    SurfaceTexture mSurfaceTexture;

    // If using shared context it's the GLSurfaceView (if it exists) which owns and manages context/resources
    // Also irrespective of whether it's the GLSurfaceView exists, it's the DummyRenderer which \
    // creates and manages context/resources to be used by Video renderer
    volatile boolean mUseSharedContext;

    EGLContext mSharedEGLContext;

    ControllerInterface mController;

    // Used for sharing Caches
    Renderer mRendererInstance;

    Caches mCache;

    volatile boolean mRendererReady = false;

    public Renderer(ControllerInterface controller) {
        mController = controller;
    }

    public void setController(ControllerInterface controller) {
        mController = controller;
    }

    public void setRendererInstance(Renderer renderer) {
        mRendererInstance = renderer;
    }

    public void setUseSharedEGLContext(boolean useSharedContext) {
        mUseSharedContext = useSharedContext;
    }

    public EGLContext getEGLContext() {
        return mSharedEGLContext;
    }

    void onRendererPrepared() {
        mRendererReady = true;
        mController.onRendererPrepared(mSharedEGLContext, this);
    }

    void handleCreateSurfaceTexture() {

        SurfaceTexture prevSurfaceTexture = OpenGLRenderer.getSurfaceTexture(mRendererInstance);
        if(prevSurfaceTexture != null) {
            prevSurfaceTexture.release();
        }
        mSurfaceTexture = OpenGLRenderer.createSurfaceTexture(mRendererInstance);

        // Should never happen
        if (mSurfaceTexture == null) {
            Log.e(TAG, "Couldn't create surface texture");
            return;
        }
        // Pass it to the Render Controller so that it can register onFrameAvailable callbacks on ST
        // Also Render Controller will take care of sending the ST to anyone,
        // who may wish to make use of it, mostly by writing to it (Camera/decoders) for us.
        updateSurfaceTexture();
    }

    /**
     * Pass it to Camera as preview texture by calling setPreviewTexture
     */
    void updateSurfaceTexture() {
        mController.onSurfaceTextureAvailable(mSurfaceTexture);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public interface ControllerInterface {
        void onRendererPrepared(EGLContext sharedEGLContext, Renderer renderer);
        void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture);
        void onFrameAvailable();
        void onDisplaySurfaceChanged(int width, int height);
        void onFrameDisplayed(long timeStampUs);
        void onVideoFrameRendered(long timestampUs);
        void onAudioFrameRendered(long timestamp);
        void onAudioRecordingFinished();
        void onVideoRecordingFinished();
        void onAudioRecorderReady(boolean success);
        void onVideoRecorderReady(boolean success);
        void onRecordingFinished(boolean success);
    }
}
