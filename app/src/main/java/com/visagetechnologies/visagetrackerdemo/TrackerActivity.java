package com.visagetechnologies.visagetrackerdemo;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.FaceDetector;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Toast;

import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.FileUtilities;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.fragments.RenderFragment;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.RenderManager;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.scenes.Scene3D;

import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

import static android.content.ContentValues.TAG;


/** Activity called to initiate and control tracking for either tracking from camera or image.
 */
public class TrackerActivity extends AppCompatActivity
{
	final int TRACK_GREYSCALE = 2;
	final int TRACK_RGB = 0;

	final int VISAGE_CAMERA_UP = 0;
	final int VISAGE_CAMERA_DOWN = 1;
	final int VISAGE_CAMERA_LEFT = 2;
	final int VISAGE_CAMERA_RIGHT = 3;

	private Handler mHandler = new Handler();
	private TextView tv;
	private static boolean licenseMessageShown = false;

	public static float AverageFPS = 0;
	public static int FPSCounter = 0;
	public static TrackerActivity instance;


	public static boolean wait = false;
	JavaCamTrackerView cpreview;
	ImageTrackerView ipreview;
	String imagePath;
	int cameraId = -1;
	int orientation;
	int type;
	String sourceImagePath;
	String destinationImagePath;
	int sourceFaceIndex = 0;
	int destinationFaceIndex = 0;
	FaceData[] sourceFaces;
	FaceData[] destinationFaces;
	FaceRenderer renderer;
	Bitmap faceTexture;

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
			calculateRect(sourceFaces);
			calculateRect(destinationFaces);
			calculateMedian(sourceFaces);
			calculateMedian(destinationFaces);

			// sort arrays
			Arrays.sort(sourceFaces, new Comparator<FaceData>() {
				public int compare(FaceData idx1, FaceData idx2) {
					return idx1.getFaceRect().left - idx2.getFaceRect().left;
				}
			});

			Arrays.sort(destinationFaces, new Comparator<FaceData>() {
				public int compare(FaceData idx1, FaceData idx2) {
					return idx1.getFaceRect().left - idx2.getFaceRect().left;
				}
			});
			Scene3D.verticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getFaceModelVertices());
			Scene3D.texCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getFaceModelTextureCoords());
			Scene3D.indicesBuffer = Utils.getShortBuffer(getDestinationFaces()[destinationFaceIndex].getCorrectedTriangles());
			Scene3D.leftEyeVerticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getLeftEyeVertices());
			Scene3D.leftEyeTexCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getLeftEyeTextureCoordinates());

			Scene3D.rightEyeVerticesBuffer = Utils.getFloatBuffer(getDestinationFaces()[destinationFaceIndex].getRightEyeVertices());
			Scene3D.rightEyeTexCoordBuffer = Utils.getFloatBuffer(getSourceFaces()[sourceFaceIndex].getRightEyeTextureCoordinates());

			Scene3D.eyeIndicesBuffer = Utils.getShortBuffer(getSourceFaces()[sourceFaceIndex].getEyeTriangles());

			Scene3D.rect = convertRect(getSourceFaces()[sourceFaceIndex].getFaceRect());
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

	/*public void setFaces(FaceData[] faces){
		if(faces != null){
			this.faces = faces;
//			if(faces.length > 0)
//				renderFace();
		}
	}*/

	private void createGLES20Surface(){
		final GLSurfaceView surfaceView = new GLSurfaceView(this);
		surfaceView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
		RelativeLayout layout = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layout.setLayoutParams(params);
		setContentView(layout);
		// Add mSurface to your root view
		addContentView(surfaceView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
		renderer = new FaceRenderer(this,this,sourceFaceIndex,destinationFaceIndex,destinationFaces[destinationFaceIndex].getMedianColor());
		surfaceView.setRenderer(renderer);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (cpreview != null && type == 0)
			cpreview.closeCamera();
		TrackerStop();
		mHandler.removeCallbacksAndMessages(null);
	}

	private void setBitmap(FaceData[] faces, Bitmap bitmap){
		for(int i = 0; i < faces.length; i++){
			faces[i].setBitmap(bitmap);
		}
	}

	private void calculateMedian(FaceData[] faces){
		for(int i = 0; i < faces.length; i++){
			faces[i].calculateMedian();
		}
	}

	private void calculateRect(FaceData[] faces){
		for(int i = 0; i < faces.length; i++){
			faces[i].calculateRect();
		}
	}


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
	 * Thread for tracking from camera
	 */
	
	private final class CameraThread implements Runnable {
		private int Cwidth;
		private int Cheight;
		private int Corientation;
		private int Cflip;

		protected CameraThread(int width, int height, int flip, int orientation) {

			Cwidth = width;
			Cheight = height;
			Corientation = orientation;
			Cflip = flip;
		}

		@Override
		public void run() {
			setParameters(Cwidth, Cheight, Corientation, Cflip);
			TrackFromCam();
			return;
		}
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

	public void renderFace(){
		setContentView(R.layout.dummy);
		final SurfaceView surface = new SurfaceView(this);
		surface.setFrameRate(60.0);
		surface.setKeepScreenOn(true);
		surface.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
		// Add mSurface to your root view
		addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
		renderer = new FaceRenderer(this,0,0,new int[3]);
		surface.setRenderer(renderer);
	}

	/*private static final class GetFaces implements Runnable{
		private final WeakReference<Handler> mHandlerRef;

		protected GetFaces(Handler handler){
			mHandlerRef = new WeakReference<Handler>(handler);
		}
		@Override
		public void run() {
			if(TrackerActivity.instance.faces == null || TrackerActivity.instance.faces.length == 0){
				TrackerActivity.instance.setFaces(TrackerActivity.instance.getFaces());
				if(TrackerActivity.instance.faces == null || TrackerActivity.instance.faces.length == 0){
					final Handler handler = mHandlerRef.get();
					if (handler != null)
						handler.postDelayed(this, 450);
				}
			}
		}
	}*/

	private static final class DoneRunnable implements Runnable {
		private final WeakReference<TextView> mTextViewRef;
		private final WeakReference<Handler> mHandlerRef;

		protected DoneRunnable(TextView textView, Handler handler) {
			mTextViewRef = new WeakReference<TextView>(textView);
			mHandlerRef = new WeakReference<Handler>(handler);
		}
		@Override
		public void run() {
			final TextView tv = mTextViewRef.get();
			if (tv!=null)
			{
				float fps = GetFps();
				float displayFps = GetDisplayFps();
				String state = GetStatus();

				int trackTime = GetTrackTime();

				if (!state.equals("OFF")) {
					if (TrackerActivity.AverageFPS == 0) {
						if (fps > 0 && fps < 100) {
							TrackerActivity.AverageFPS = fps;
							TrackerActivity.FPSCounter++;
						}
					}
					else {
						if (fps > 0 && fps < 80) {
							TrackerActivity.FPSCounter++;
							TrackerActivity.AverageFPS = TrackerActivity.AverageFPS + ((fps - TrackerActivity.AverageFPS) / TrackerActivity.FPSCounter);
						}

					}

					tv.setText("FPS: " + String.format("%.2f", fps) + " (track " + String.format("%d", trackTime) + " ms)" + "\nDISPLAY FPS: " + String.format("%.2f", displayFps) + "\nStatus: " + state + "\nAvgFPS: " + String.format("%.2f", TrackerActivity.AverageFPS));
				} else if (state.equals("OFF") && FPSCounter > 100 && licenseMessageShown == false) {
					new AlertDialog.Builder(TrackerActivity.instance)
							.setTitle("Warning")
						.setMessage("You are using an unlicensed copy of visage|SDK\nTracking time is limited to one minute.\nPlease contact Visage Technologies to obtain a license key.\n30-day trial licenses are available.\nTracking will now stop.")
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) { 
										}
									})
					    .setIcon(android.R.drawable.ic_dialog_alert)
					    .show();
					tv.setText("");
					licenseMessageShown = true;
				}
			}
			final Handler handler = mHandlerRef.get();
			if (handler != null)
				handler.postDelayed(this, 200);
		}
	}

	/**
	 * Starts tracking from camera and initializes surface for displaying tracking results.
	 */
	public void StartCam(){		
		TrackerInit(getFilesDir().getAbsolutePath() + "/Facial Features Tracker - High.cfg");
		cameraId = getCameraId();
		cpreview = new JavaCamTrackerView(this, this, cameraId);
    	RelativeLayout layout = new RelativeLayout(this);
    	layout.addView(cpreview);
    	
    	TrackerGLSurfaceView tGLView = new TrackerGLSurfaceView(this,imagePath);
    	cpreview.setGLView(tGLView);
    	layout.addView(tGLView);
    	
    	final TextView tv = new TextView(this);
    	tv.setTextColor(Color.GREEN);
    	tv.setBackgroundColor(Color.BLACK);
    	layout.addView(tv);
    	mHandler.postDelayed(new DoneRunnable(tv, mHandler), 200);
    	
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    	
    	Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
    	int screenOrientation = display.getRotation();
    	
    	layout.setLayoutParams(params);
    	setContentView(layout);    	
    	CameraInfo cameraInfo = new CameraInfo();
    	Camera.getCameraInfo(cameraId, cameraInfo);
		int orientation = cameraInfo.orientation;
    	int flip = 0;		
		if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
			flip = 1; // Mirror image from frontal camera
		}
		Thread CamThread;
		if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
			CamThread = new Thread(new CameraThread(cpreview.previewWidth, cpreview.previewHeight, flip, (screenOrientation*90 + orientation)%360));
    	else
    		CamThread = new Thread(new CameraThread(cpreview.previewWidth, cpreview.previewHeight, flip, (orientation - screenOrientation*90 + 360)%360));
		
    	CamThread.start();

	}

	int getCameraId(){
		int cameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			cameraId = i;
	    	if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
	    		break;
		}
	}
		return cameraId;
	}
	  
	
    /** Called to initialize and start tracking for image. Also initializes surface for displaying tracking results.
     * 
     * @param path absolute path to image file used for tracking. Sent from ImagesActivity using Bundle class provided by Android. 
     */
    /**
    public void StartImage(String path)
    {
    	TrackerInit(getFilesDir().getAbsolutePath() + "/Facial Features Tracker - High.cfg");


    	Bitmap bitmap = Utils.LoadBitmapFromFile(path);
		bitmap = Utils.CreateOptimalBitmapSize(bitmap,this);
//		Log.d("TrackerActivity","width and height is "+bitmap.getWidth()+" , "+bitmap.getHeight());
//		WriteFrameImage(Utils.ConvertToByte(bitmap), bitmap.getWidth(), bitmap.getHeight());
		faces = Track(bitmap.getWidth(),bitmap.getHeight());
		final SurfaceView surface = new SurfaceView(this);
		surface.setFrameRate(60.0);
		surface.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
		RelativeLayout layout = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layout.setLayoutParams(params);
		setContentView(layout);
		// Add mSurface to your root view
		addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
		renderer = new FaceRenderer(this);
		surface.setRenderer(renderer);
//		for(int i = 0; i < faces.length; i++){
//			Utils.saveFaceDataToFile(this,faces[i],""+i+".obj");
//		}


//		ipreview = new ImageTrackerView(this, path, this);
//		RelativeLayout layout = new RelativeLayout(this);
//
//		layout.addView(ipreview);
//
//		TrackerGLSurfaceView tGLView = new TrackerGLSurfaceView(this,imagePath);
//		ipreview.setGLView(tGLView);
//		layout.addView(tGLView);

//		tv = new TextView(this);
//		tv.setTextColor(Color.GREEN);
//		tv.setBackgroundColor(Color.BLACK);
//		layout.addView(tv);

//		mHandler.postDelayed(new DoneRunnable(tv, mHandler), 200);
//		mHandler.postDelayed(new GetFaces(mHandler),450);
//    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//
//		layout.setLayoutParams(params);
//		setContentView(layout);

//		ipreview.SetOptimalBitmapSize();
//		this.faceTexture = ipreview.getBitmap();
//		Log.d("TrackerActivity","width and height is "+ipreview.getBitmapWidth()+" , "+ipreview.getBitmapHeight());
//		Thread ImageThread = new Thread(new ImageThread(ipreview.getBitmapWidth(), ipreview.getBitmapHeight()));
////		Thread ImageThread = new Thread(new ImageThread(bitmap.getWidth(), bitmap.getHeight()));
//		ImageThread.start();
	}**/

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

    /*
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case Utils.STORAGE_PERMISSIONS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					for(int i = 0; i < faces.length; i++){
						Utils.saveFaceDataToFile(this,faces[i],""+i+".obj");
					}

				} else {
					Log.d("TrackerActivity","permission denied");
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}*/
    

	/** Interface to native method called for initializing tracker.
	 * 
	 * @param configFilename absolute path to tracker configuration file. 
	 */
	public native void TrackerInit(String configFilename);

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