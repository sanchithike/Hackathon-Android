package com.roposo.creation.camera;

/**
 * Created by bajaj on 02/06/17.
 */

public interface CameraStateCallback {
    void onCameraOpened();

    void onCameraDisconnected();

    void onCameraError(int error);
}
