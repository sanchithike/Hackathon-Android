package com.visagetechnologies.visagetrackerdemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

/** Activity started when application is ran.
 */
public class StartupActivity extends Activity
{
	final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
	final int MY_PERMISSIONS_REQUEST_STORAGE = 2;

	boolean camera = false;
	boolean image = false;

	/** Implementation of onCreate method provided by Activity interface.
	 *	Called when Activity is started for the first time.
	 */
	public void onCreate(Bundle savedInstanceState) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onCreate(savedInstanceState);

		copyAssets(getFilesDir().getAbsolutePath());

		setContentView(R.layout.main);
	}

	/** Method called when user interacts with Track from image button.
	 */
	public void StartImages() {
		Intent intent = new Intent(this,ImagesActivity.class);
		startActivity(intent);
	}

	/** Method called when user interacts with Track from cam button.
	 */
	public void StartCam(int type) {
		Intent intent = new Intent(this, TrackerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("type", type);
		intent.putExtras(bundle);
		startActivity(intent);
	}




	/** Method that handles permissions.
	 */
	public void handlePermissions() {
		if (camera == true)
		{
			if (ContextCompat.checkSelfPermission(this,
					Manifest.permission.CAMERA)
					!= PackageManager.PERMISSION_GRANTED) {

				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA},
						MY_PERMISSIONS_REQUEST_CAMERA);

			}
			else
				StartCam(0);
		}
		else
		{
			if (ContextCompat.checkSelfPermission(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {

				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						MY_PERMISSIONS_REQUEST_STORAGE);
			}
			else
				StartImages();
		}

	}
	/** Method invoked when the user responds to permission request.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_CAMERA: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					StartCam(0);
				}
				return;
			}
			case MY_PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					StartImages();
				}
				return;
			}
		}
	}
	
	/** Utility method called to copy required file to trackerdata folder.
	 *
	 * @param rootDir absolute path to directory where files should be copied.
	 * @param filename name of file that will be copied.
	 */
	public void copyFile(String rootDir, String filename) {
		AssetManager assetManager = this.getAssets();

		InputStream in = null;
		OutputStream out = null;
 		try {
			String newFileName = rootDir + File.separator + filename;
			File file = new File(newFileName);

			if(!file.exists()) {
				in = assetManager.open("trackerdata/" + filename);
				out = new FileOutputStream(newFileName);

				byte[] buffer = new byte[4*1024];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				out.flush();
				out.close();
				out = null;
				
				in.close();
				in = null;
			}
		} catch (Exception e) {
			Log.e("VisageTrackerDemo", e.getMessage());
		}
	}

	/** Utility method called to create required directories and initiate copy of all assets required for tracking.
	 *
	 * @param rootDir absolute path to root directory used for storing assets required for tracking.
	 */
	public void copyAssets(String rootDir) {
		
		AssetManager assetManager = this.getAssets();
		
		String assets[] = null;
		try {
			assets = assetManager.list("trackerdata");
			
			for (String asset : assets) {
				Log.i("VisageTrackerDemo", rootDir + File.separator + asset);
				try {
					if (!asset.contains("bdtsdata")) copyFile(rootDir, asset);
				} catch (Exception e) {
					Log.e("VisageTrackerDemo", e.getMessage());
				}
			}
		} catch (Exception e) {
			Log.e("VisageTrackerDemo", e.getMessage());
		}

		// create dirs
		final String[] dirs = {
				"bdtsdata",
				"bdtsdata/FF",
				"bdtsdata/LBF",
				"bdtsdata/NN",
				"bdtsdata/LBF/pe",
				"bdtsdata/LBF/vfadata",
				"bdtsdata/LBF/ye",
				"bdtsdata/LBF/vfadata/ad",
				"bdtsdata/LBF/vfadata/ed",
				"bdtsdata/LBF/vfadata/gd"
		};
		
		for (String dirname : dirs) {
			try {
				File dir = new File(rootDir + File.separator + dirname);
				if (!dir.exists()) dir.mkdir();
			} catch (Exception e) {
				Log.e("VisageTrackerDemo", e.getMessage());
			}
		}
		
		// copy files
		final String[] files = {
				"bdtsdata/FF/ff.dat",
				"bdtsdata/LBF/lv",
				"bdtsdata/LBF/pr.lbf",
				"bdtsdata/LBF/pe/landmarks.txt",
				"bdtsdata/LBF/pe/lp11.bdf",
				"bdtsdata/LBF/pe/W",
				"bdtsdata/LBF/vfadata/ad/ad0.lbf",
				"bdtsdata/LBF/vfadata/ad/ad1.lbf",
				"bdtsdata/LBF/vfadata/ad/ad2.lbf",
				"bdtsdata/LBF/vfadata/ad/ad3.lbf",
				"bdtsdata/LBF/vfadata/ad/ad4.lbf",
				"bdtsdata/LBF/vfadata/ad/regressor.lbf",
				"bdtsdata/LBF/vfadata/ed/ed0.lbf",
				"bdtsdata/LBF/vfadata/ed/ed1.lbf",
				"bdtsdata/LBF/vfadata/ed/ed2.lbf",
				"bdtsdata/LBF/vfadata/ed/ed3.lbf",
				"bdtsdata/LBF/vfadata/ed/ed4.lbf",
				"bdtsdata/LBF/vfadata/ed/ed5.lbf",
				"bdtsdata/LBF/vfadata/ed/ed6.lbf",
				"bdtsdata/LBF/vfadata/gd/gd.lbf",
				"bdtsdata/LBF/ye/landmarks.txt",
				"bdtsdata/LBF/ye/lp11.bdf",
				"bdtsdata/LBF/ye/W",
				"bdtsdata/NN/fa.lbf",
				"bdtsdata/NN/fc.lbf",
				"bdtsdata/NN/fr.bin",
				"bdtsdata/NN/pr.bin"
		};
		
		for (String filename : files) {
			try {
				Log.i("VisageTrackerDemo", rootDir + File.separator + filename);
				copyFile(rootDir, filename);
			} catch(Exception e) {
				Log.e("VisageTrackerDemo", e.getMessage());
			}
		}
	}
}
