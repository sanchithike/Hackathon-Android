package com.roposo.creation.graphics.gles;

import android.graphics.Rect;
import android.opengl.GLES20;

/**
 * Created by bajaj on 05/09/17.
 */

public class FBObject extends RenderTarget {
    final int mTextureId;
    private final int mDepthBufferId;

    private final int mFrameBufferId;

    float[] mProjectionMatrix = new float[16];

    private FBObject(int frameBufferId, int textureId, int depthBufferId) {
        mFrameBufferId = frameBufferId;
        mTextureId = textureId;
        mDepthBufferId = depthBufferId;
    }

    static FBObject createFrameBufferObject(int textureId, int width, int height, boolean needDepthBuffer) {
        int[] frameBuffers = new int[1];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        int frameBufferId = frameBuffers[0];

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        int depthBufferId = 0;
        if (needDepthBuffer) {
            // Create a depth buffer and bind it.
            int[] values = new int[1];
            GLES20.glGenRenderbuffers(1, values, 0);
            Caches.checkGlError("glGenRenderbuffers");
            depthBufferId = values[0];    // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBufferId);
            Caches.checkGlError("glBindRenderbuffer " + depthBufferId);

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                    width, height);
            Caches.checkGlError("glRenderbufferStorage");

            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_RENDERBUFFER, depthBufferId);
            Caches.checkGlError("glFramebufferRenderbuffer");
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        FBObject fbObject = new FBObject(frameBufferId, textureId, depthBufferId);
        fbObject.mWidth = width;
        fbObject.mHeight = height;
        fbObject.setExtent(new Rect(0, 0, fbObject.mWidth, fbObject.mHeight));
        fbObject.computeProjection();
        return fbObject;
    }

    @Override
    public void setScaleParams(float width, float height) {

    }

    @Override
    public int getTargetID() {
        return TARGET_FBO;
    }

    public int getFrameBufferId() {
        return mFrameBufferId;
    }

    public void cleanup() {
        // TODO critical
    }
}
