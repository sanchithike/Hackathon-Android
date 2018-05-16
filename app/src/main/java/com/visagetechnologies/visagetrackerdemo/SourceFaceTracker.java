package com.visagetechnologies.visagetrackerdemo;

import android.graphics.Bitmap;

public class SourceFaceTracker implements Runnable{

    private FaceActivity faceActivity;
    private Bitmap bitmap;

    public SourceFaceTracker(Bitmap bitmap, FaceActivity faceActivity){
        this.bitmap = bitmap;
        this.faceActivity = faceActivity;
    }

    @Override
    public void run() {
        WriteSouceFrameImage(Utils.ConvertToByte(bitmap), bitmap.getWidth(), bitmap.getHeight());
        faceActivity.setSourceFaces(TrackSource(bitmap.getWidth(),bitmap.getHeight()));
    }

    public static native FaceData[] TrackSource(int width, int height);
    public static native void WriteSouceFrameImage(byte[] frame, int width, int height);

}
