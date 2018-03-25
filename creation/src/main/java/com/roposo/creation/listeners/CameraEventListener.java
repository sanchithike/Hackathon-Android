package com.roposo.creation.listeners;

import com.roposo.creation.camera.CameraStateCallback;

/**
 * Created by bajaj on 08/08/16.
 */
public interface CameraEventListener extends CameraStateCallback {
    void onRequestCameraSwap();
    void onCameraSwapped();
}
