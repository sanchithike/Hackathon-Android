package com.visagetechnologies.visagetrackerdemo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageButton;

public class DestinationFaceTracker extends Thread implements Runnable{

    private ImageButton candidate;
    private FaceActivity faceActivity;
    private int index;


    public DestinationFaceTracker(ImageButton candidate,FaceActivity faceActivity, int index){
        this.candidate = candidate;
        this.faceActivity = faceActivity;
        this.index = index;
    }
    @Override
    public void run() {
        Bitmap bitmap = ((BitmapDrawable)this.candidate.getDrawable()).getBitmap();
        WriteDestinationFrameImage(Utils.ConvertToByte(bitmap), bitmap.getWidth(), bitmap.getHeight());
        FaceData[] faces = TrackDestination(bitmap.getWidth(),bitmap.getHeight());
        faceActivity.setDestinationFaces(faces,this.index);
    }


    static {
        System.loadLibrary("VisageVision");
        System.loadLibrary("VisageWrapper");
    }

    public static native void WriteDestinationFrameImage(byte[] frame, int width, int height);
    public static native FaceData[] TrackDestination(int width, int height);
}
