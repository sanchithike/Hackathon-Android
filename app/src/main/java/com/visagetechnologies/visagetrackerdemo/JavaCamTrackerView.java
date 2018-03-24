package com.visagetechnologies.visagetrackerdemo;



import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

/** Derives from TrackerView. 
 * Class JavaCamTrackerView is used for initiating Android camera preview and sending raw image date to the VisageTracker object.
 * Implements Runnable interface provided by Android. Runnable interface is used for implementation of simple multi-threading.    
 */
public class JavaCamTrackerView extends TrackerView implements Runnable
{
	public final String TAG = "JavaCamTrackerView";
	
	Camera cam;
	Context _context;
	SurfaceTexture tex;
	public float cameraFps;
	public int previewWidth, previewHeight;
	Thread videoReader;
	Bitmap bit;
	int cameraId;
	/** Constructor. 
	 * 
	 * @param context global information about Android application environment.
	 */
	JavaCamTrackerView(Context context, TrackerActivity activity, int cameraId)
	{
		super(context);
		_context = context;
		_activity = activity;
		this.cameraId = cameraId;
		GrabFromCamera();
		getHolder().addCallback(this);
	}
	
	/** Implementation of surfaceCreated method provided by SurfaceHolder.Callback class.
	 *  Creates and starts rendering thread. 
	 */
	public void surfaceCreated(SurfaceHolder holder)
	{	
    	isRunning = true;
    	setKeepScreenOn(true);
	}
	/**
	 * Sets preview size so that width is closest to param width
	 * @param parameters
	 * @param width
	 */
	private void setPreviewSize(Camera.Parameters parameters, int width){
		int idx = 0,  dist = 100000;
		List<Size> sizes = parameters.getSupportedPreviewSizes();
		for (int i = 0;i<sizes.size();i++){
			if (Math.abs(sizes.get(i).width-width)<dist){
				idx = i;
				dist = Math.abs(sizes.get(i).width-width);
			}
		}
		parameters.setPreviewSize(sizes.get(idx).width, sizes.get(idx).height);
	}
	
	/**
	 * Start grabbing frames from camera
	 */
	public void GrabFromCamera(){
		try{
			cam = Camera.open(this.cameraId);
		}catch(Exception e){
			Log.e(TAG, "Unable to open camera");
			return;
		}
		Camera.Parameters parameters = cam.getParameters();
		setPreviewSize(parameters,300);
		parameters.setPreviewFormat(ImageFormat.NV21);
		cam.setParameters(parameters);
		
		final Size previewSize=cam.getParameters().getPreviewSize();
		Log.i (TAG,  "Current preview size is " + previewSize.width + ", " + previewSize.height);

		int dataBufferSize=(int)(previewSize.height*previewSize.width*
                (ImageFormat.getBitsPerPixel(cam.getParameters().getPreviewFormat())/8.0));
        for (int i=0;i<10;i++){
        	cam.addCallbackBuffer(new byte[dataBufferSize]);
        }
        tex = new SurfaceTexture(0);
        try {
			cam.setPreviewTexture(tex);
		} catch (IOException e) {
			e.printStackTrace();
		}
        this.previewHeight = previewSize.height;
        this.previewWidth = previewSize.width;
        cam.setPreviewCallbackWithBuffer(new PreviewCallback() {
        	private long timestamp = 0;
			public void onPreviewFrame(byte[] data, Camera camera) {
				//Log.v("CameraTest","FPS = "+1000.0/(System.currentTimeMillis()-timestamp));
				cameraFps = 1000.0f/(System.currentTimeMillis()-timestamp);
				timestamp=System.currentTimeMillis();
				camera.addCallbackBuffer(data);
				WriteFrameCamera(data);				
			}
		});
        cam.startPreview();
	}
	
	/** Implementation of surfaceChanged method provided by SurfaceHolder.Callback class.
	 * Sets new width and height values of view.
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		_width = width;
		_height = height;
	}
	
	/** Implementation of surfaceDestroyed method provided by SurfaceHolder.Callback class.
	 * Ends rendering loop.
	 * Calls TrackerStop method to stop tracking.
	 * Releases Android camera resource.
	 */
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		isRunning = false;
	}
	
	public void closeCamera()
	{
		if (cam !=null){
			cam.stopPreview();				
			cam.release();
			cam = null;
		}		
	}

	/** Implementation of run method of Runnable interface. 
	 * Android requires that all rendering to a surface is done from separate thread than the one which created the View object. Rendering thread is started from surfaceCreated method.
	 * Method calls rendering loop of application.
	 */
	public void run()
	{
		SurfaceHolder surfaceHolder = getHolder();
		Canvas c = null;
		boolean isAutoStopped = false;
		while(isRunning)
		{
			try
			{
				//int st = GetStatus();
				//int count = GetNotifyCount();
				c = surfaceHolder.lockCanvas(null);		
				synchronized(this)
				{	
					_tview.requestRender();		
				}			
			}
			finally
			{
				if(c != null)
				{
					surfaceHolder.unlockCanvasAndPost(c);
				}
			}
			
			isAutoStopped = IsAutoStopped();
			
			if(isAutoStopped)
			{
				isRunning = false;
			}
		}	
		
		if(isAutoStopped)
		{
			_activity.ShowDialog();
		}
	}
	
	/** Interface to native method used for passing raw pixel data to tracker.
	 * This method is called to write camera frames into VisageSDK::VisageTracker object through VisageSDK::AndroidCameraCapture
	 * 
	 * @param frame raw pixel data of image used for tracking.
	 */
	public static native void WriteFrameCamera(byte[] frame);
}
