package com.visagetechnologies.visagetrackerdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;

/** Implementation of GLSurfaceView class provided by Android.
 * 
 *  Creates and sets OpenGL renderer that contains the actual rendering code to render on this view.
 */
public class TrackerGLSurfaceView extends GLSurfaceView
{
	private TrackerRenderer _trenderer;
	private FaceData[] faces;
	/** Constructor.
	 * 
	 * @param context global information about android application environment.
	 */
	TrackerGLSurfaceView(Context context,String imagePath)
	{
		super(context);
		//setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		_trenderer = new TrackerRenderer(context,this);
		setRenderer(_trenderer);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
		setKeepScreenOn(true);
	}

	public FaceData[] getFaces() {
		return faces;
	}

	public void setFaces(FaceData[] faceData) {
		this.faces = faceData;
	}
}
