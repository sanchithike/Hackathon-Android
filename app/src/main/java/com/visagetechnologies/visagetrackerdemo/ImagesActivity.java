package com.visagetechnologies.visagetrackerdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


/** Activity started when user interacts with Track from image button in StartupActivity.
 * ImagesActivity images gallery on Android device. User can choose image that will be used for tracking.
 */
public class ImagesActivity extends Activity
{
	private static int RESULT_LOAD_IMAGE = 1;
	final int MY_PERMISSIONS_REQUEST_STORAGE = 2;
	final int MY_PERMISSIONS_REQUEST_CAMERA = 3;
	private static final int CAMERA_REQUEST = 1888;
	final int sourceIndex = 0;
	final int destinationIndex = 1;
	private String sourceImagePath;
	private String destinationImagePath;

	private int currentIndex = -1;
	/** Implementation of onCreate method provided by Activity interface.
	 *  Called when Activity is started for the first time.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onCreate(savedInstanceState);

		copyAssets(getFilesDir().getAbsolutePath());

		setContentView(R.layout.main);
		Button buttonSourceImageClick = (Button)findViewById(R.id.btnSourceImage);
		buttonSourceImageClick.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleGalleryPermissions(sourceIndex);
			}
		});

		Button buttonSourceCamImage = (Button) findViewById(R.id.btnSourceCamImage);
		buttonSourceCamImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleCameraPermissions();
			}
		});

		Button buttonDestinationImageClick = (Button)findViewById(R.id.btnDestinationImage);
		buttonDestinationImageClick.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleGalleryPermissions(destinationIndex);
			}
		});

		Button swap = (Button) findViewById(R.id.btnSwap);
		swap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openTracker();
			}
		});

    }

	public Uri getImageUri(Context inContext, Bitmap inImage) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
		return Uri.parse(path);
	}

	public String getRealPathFromURI(Uri uri) {
		Cursor cursor = getContentResolver().query(uri, null, null, null, null);
		cursor.moveToFirst();
		int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
		return cursor.getString(idx);
	}

    public void openTracker(){
		Intent intent = new Intent(this, TrackerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("sourceImagePath",sourceImagePath);
		bundle.putString("destinationImagePath",destinationImagePath);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	private void openCamera(){
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(cameraIntent, CAMERA_REQUEST);
	}


	public void openGallery(int imageType){
		Intent i = new Intent(
				Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, RESULT_LOAD_IMAGE);
	}

	public void handleGalleryPermissions(int imageType){
		currentIndex = imageType;
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					MY_PERMISSIONS_REQUEST_STORAGE);
		}
		else
			openGallery(imageType);
	}

	public void handleCameraPermissions(){
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					MY_PERMISSIONS_REQUEST_CAMERA);
		}
		else{
			openCamera();
		}
	}

	/** Method invoked when the user responds to permission request.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(currentIndex > -1)
						openGallery(currentIndex);
					else
						Log.d("ImagesActivity","Invalid current index");
				}
				return;
			}
			case MY_PERMISSIONS_REQUEST_CAMERA:{
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					openCamera();
				}
				else{
					Log.d("ImagesActivity", "Permission Denied");
				}
			}
		}
	}

    /** Called when user selects image.
	 * 
	 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			switch(currentIndex){
				case sourceIndex:{
					sourceImagePath = picturePath;
					break;
				}
				case destinationIndex:{
					destinationImagePath = picturePath;
				}
			}
		}
		else if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && null != data){
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			Uri tempUri = getImageUri(getApplicationContext(), photo);
			sourceImagePath = getRealPathFromURI(tempUri);
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




}
