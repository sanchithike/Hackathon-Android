package com.visagetechnologies.visagetrackerdemo;

import java.util.Arrays;
import java.util.Comparator;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.fragments.RenderFragment;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.RenderManager;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.scenes.Scene3D;

/** Activity called to initiate and control tracking for either tracking from camera or image.
 */
public class TrackerActivity extends AppCompatActivity
{
	private static boolean licenseMessageShown = false;

	public static float AverageFPS = 0;
	public static int FPSCounter = 0;
	public static TrackerActivity instance;


	JavaCamTrackerView cpreview;
	int cameraId = -1;
	String sourceImagePath;
	String destinationImagePath;
	int sourceFaceIndex = 0;
	int destinationFaceIndex = 0;
	FaceData[] sourceFaces;
	FaceData[] destinationFaces;
	FaceRenderer renderer;

	public static Bitmap sourceBitmap;
	public static Bitmap destinationBitmap;
	@Override
    public void onCreate(Bundle savedInstanceState) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onCreate(savedInstanceState);
		ContextHelper.setContext(this);
		ContextHelper.setApplicationContext(getApplicationContext());
		Bundle bundle = getIntent().getExtras();
		sourceImagePath = bundle.getString("sourceImagePath");
		destinationImagePath = bundle.getString("destinationImagePath");
		int faceIndex = bundle.getInt("faceIndex");
		if(faceIndex != -1){
			destinationFaceIndex = faceIndex - 1;
		}
		track();
		TrackerActivity.instance = this;
		licenseMessageShown = false;
		AverageFPS = 0;
		FPSCounter = 0;
	}


	public void track(){
		TrackerInit(getFilesDir().getAbsolutePath() + "/Facial Features Tracker - High.cfg");
		Bitmap source = Utils.LoadBitmapFromFile(sourceImagePath);
		source = Utils.CreateOptimalBitmapSize(source,this);

		Bitmap destination = Utils.LoadBitmapFromFile(destinationImagePath);
		destination = Utils.CreateOptimalBitmapSize(destination,this);

		sourceBitmap = source;
		destinationBitmap = destination;
		Scene3D.sourceBitmap = sourceBitmap;
		Scene3D.destinationBitmap = destinationBitmap;
		WriteSouceFrameImage(Utils.ConvertToByte(sourceBitmap), sourceBitmap.getWidth(), sourceBitmap.getHeight());
		WriteDestinationFrameImage(Utils.ConvertToByte(destinationBitmap), destinationBitmap.getWidth(), destinationBitmap.getHeight());

		sourceFaces = TrackSource(sourceBitmap.getWidth(),sourceBitmap.getHeight());
		destinationFaces = TrackDestination(destinationBitmap.getWidth(),destinationBitmap.getHeight());
		Log.d("TrackerActivity","source faces length = "+sourceFaces.length);
		Log.d("TrackerActivity","destination faces length = "+destinationFaces.length);



		if(sourceFaces.length > 0 && destinationFaces.length > 0){


//			sourceFaceIndex = 0;
//			destinationFaceIndex = destinationFaces.length - 1;
			setBitmap(sourceFaces,sourceBitmap);
			setBitmap(destinationFaces,destinationBitmap);
//			calculateRect(sourceFaces);
//			calculateRect(destinationFaces);
//			calculateMedian(sourceFaces);
//			calculateMedian(destinationFaces);

			// sort arrays
//			Arrays.sort(sourceFaces, new Comparator<FaceData>() {
//				public int compare(FaceData idx1, FaceData idx2) {
//					return idx1.getFaceRect().left - idx2.getFaceRect().left;
//				}
//			});
//
//			Arrays.sort(destinationFaces, new Comparator<FaceData>() {
//				public int compare(FaceData idx1, FaceData idx2) {
//					return idx1.getFaceRect().left - idx2.getFaceRect().left;
//				}
//			});

			if(destinationFaceIndex > getDestinationFaces().length -1){
				destinationFaceIndex = getDestinationFaces().length -1;
			}

			Scene3D.verticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getFaceModelVertices());
			Scene3D.texCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getFaceModelTextureCoords());
			Scene3D.indicesBuffer = Utils.getShortBuffer(getDestinationFaces()[destinationFaceIndex].getCorrectedTriangles());
			Scene3D.leftEyeVerticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getLeftEyeVertices());
			Scene3D.leftEyeTexCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getLeftEyeTextureCoordinates());

			Scene3D.rightEyeVerticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getRightEyeVertices());
			Scene3D.rightEyeTexCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getRightEyeTextureCoordinates());

			Scene3D.eyeIndicesBuffer = Utils.getShortBuffer(getSourceFaces()[sourceFaceIndex].getEyeTriangles());

//			Scene3D.rect = convertRect(getSourceFaces()[sourceFaceIndex].getFaceRect());
			// Translation
			double[] translation = new double[3];
			float[] translationFace = getDestinationFaces()[destinationFaceIndex].getFaceTranslation();
			for(int i = 0; i < translationFace.length; i++){
				translation[i] = (double) translationFace[i];
			}
			Scene3D.translation = translation;
			Scene3D.destinationBitmap = destinationBitmap;
			// Rotation
			float[] rotationData = getDestinationFaces()[destinationFaceIndex].getFaceRotation();
			rotationData[0] = (float) Math.toDegrees(rotationData[0]);
			rotationData[1] = (float) Math.toDegrees(rotationData[1] + Math.PI);
			rotationData[2] = (float) Math.toDegrees(rotationData[2]);
			Scene3D.rotationAngles = rotationData;

			Scene3D.cameraFocus  = getDestinationFaces()[destinationFaceIndex].getCameraFocus();
			setContentView(R.layout.scenelayout);
			RenderFragment renderFragment = new RenderFragment();
			this.getSupportFragmentManager().beginTransaction().replace(R.id.scenelayout,renderFragment).commit();
			renderFragment.startPlayback(destinationImagePath, GraphicsConsts.MEDIA_TYPE_IMAGE, GraphicsConsts.RENDER_TARGET_DISPLAY, new RenderManager.AVComponentListener() {
				@Override
				public void onStarted(@Nullable SessionConfig config) {

				}

				@Override
				public void onPrepared(@Nullable SessionConfig config) {

				}

				@Override
				public void onProgressChanged(@Nullable SessionConfig config, long timestamp) {

				}

				@Override
				public void onCompleted(@Nullable SessionConfig config) {

				}

				@Override
				public void onCancelled(@Nullable SessionConfig config, boolean error) {

				}
			});
			renderFragment.invalidateScene(SceneManager.SceneName.SCENE_3D);
		}
		else{
			Log.d("TrackerActivity","Unable to detect faces in source or destination image");
		}
	}

	private RectF convertRect(Rect faceRect) {
		RectF result = new RectF();
		result.top = (float) faceRect.top / sourceBitmap.getHeight();
		result.bottom = (float) faceRect.bottom / sourceBitmap.getHeight();
		result.left = (float) faceRect.left / sourceBitmap.getWidth();
		result.right = (float) faceRect.right / sourceBitmap.getWidth();
		return result;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void setBitmap(FaceData[] faces, Bitmap bitmap){
		for(int i = 0; i < faces.length; i++){
			faces[i].setBitmap(bitmap);
		}
	}

//	private void calculateMedian(FaceData[] faces){
//		for(int i = 0; i < faces.length; i++){
//			faces[i].calculateMedian();
//		}
//	}
//
//	private void calculateRect(FaceData[] faces){
//		for(int i = 0; i < faces.length; i++){
//			faces[i].calculateRect();
//		}
//	}


	@Override
	public void onPause() {
		super.onPause();
		this.finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//Pause tracking while we change the orientation (image buffers, etc.)
		//NOTE: We will restart tracking in the wrapper when the camera send the first changed frame
		PauseTracker();
		//
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		int screenOrientation = display.getRotation();
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(cameraId, cameraInfo);
		int orientation = cameraInfo.orientation;
		//
		int finalOrientation = 0;
		// Calculate orientation value based on the screen and camera orientation
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			finalOrientation = (screenOrientation * 90 + orientation) % 360;
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			finalOrientation = (orientation - screenOrientation*90 + 360)%360;
		}

		setParameters(cpreview.previewWidth, cpreview.previewHeight, finalOrientation, 1);
	}

	/**
	 * Thread for tracking from image
	 */
	
	private final class ImageThread implements Runnable {
			
			private int Iwidth;
			private int Iheight;

		protected ImageThread(int width, int height) {
			Iwidth = width;
			Iheight = height;
		}

		@Override
		public void run() {
			TrackFromImage(Iwidth, Iheight);
		}
	}

	public FaceData[] getSourceFaces(){
		return sourceFaces;
	}

	public FaceData[] getDestinationFaces(){
		return destinationFaces;
	}

    public void ShowDialog()
    {
		Intent intent = new Intent(this, WarningActivity.class);
		startActivity(intent);
		this.finish();
	}
    
    /* 
     * Method that is called if the product is unlicensed
     */
    
    public void AlertDialogFunction(String message)
    {
//    	TextView title = new TextView(this);
//		title.setText("License warning");
//		title.setGravity(Gravity.CENTER);
//		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
//
//		TextView msg = new TextView(this);
//		msg.setText(message);
//		msg.setGravity(Gravity.CENTER);
//		msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
//
//		new AlertDialog.Builder(this)
//	    .setCustomTitle(title)
//	    .setView(msg)
//	    .setPositiveButton(android.R.string.yes, null)
//	     .setIcon(android.R.drawable.ic_dialog_alert)
//	     .show();
    }

	/** Interface to native method called for initializing tracker.
	 * 
	 * @param configFilename absolute path to tracker configuration file. 
	 */
	public  native void TrackerInit(String configFilename);

    /** Interface to native method called to start tracking from image.
	 * 
     * @param width width of image file used for tracking
     * @param height height of image used for tracking
	 */
	public native void TrackFromImage(int width, int height);

	/**
	 * Prepare raw image interface to track from camera.
	 */
    public native void TrackFromCam();

	/** Interface to native method called to obtain frame rate information from tracker.
	 * 
	 * @return frame rate value
	 */
	public static native float GetFps();

	/** Interface to native method called to obtain rendering frame rate information.
	 * 
	 * @return rendering frame rate value
	 */
	public static native float GetDisplayFps();

	/** Interface to native method called to obtain status of tracking information from tracker.
	 * 
	 * @return status of tracking information as text.
	 */
	public static native String GetStatus();

	/** Interface to native method called to obtain pure tracking time.
	 *
	 * @return status of tracking information as text.
	 */
	public static native int GetTrackTime();
	
	/**
	 * Interface to native method called to set camera frame parameteres
	 * 
	 */

	public static native void setParameters(int width, int height,
			int orientation, int flip);
	
	/** Interface to native method used to stop tracking.
	 */
	public native void TrackerStop();
	public native void PauseTracker();
	public native FaceData[] getFaces();

	/** Interface to native method used for passing raw pixel data to tracker. Called from surfaceCreated.
	 *
	 * @param frame raw pixel data of image used for tracking.
	 * @param width width of image used for tracking.
	 * @param height height of image used for tracking.
	 */

	public static native void WriteDestinationFrameImage(byte[] frame, int width, int height);
	public static native void WriteSouceFrameImage(byte[] frame, int width, int height);
	public static native void WriteFrameImage(byte[] frame, int width, int height);

	public static native FaceData[] Track(int width, int height);

	public static native FaceData[] TrackSource(int width, int height);
	public static native FaceData[] TrackDestination(int width, int height);

	static {
		System.loadLibrary("VisageVision");
		System.loadLibrary("VisageWrapper");
	}
}