package com.visagetechnologies.visagetrackerdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sanchitsharma on 17/03/18.
 */

public class FaceActivity extends Activity {

    FaceRenderer renderer;

    private ImageButton[] candidates = new ImageButton[5];

    private Spinner possibleFaceIndices;

    private int[] numfacesFound = new int[5];

    private FaceData[][] destinationFaces = new FaceData[5][];

    private Bitmap source;

    private FaceData[] sourceFaces;

    private Button apply;

    private int faceIndex = -1;

    private int buttonIndex = -1;

    private int[] resourceIds = new int[]{R.drawable.karan_arjun,R.drawable.aamir_salman,R.drawable.krunal_bigb_hardik,R.drawable.salman_sanjay,R.drawable.vijender_ramdev};



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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scroll_view);
        copyAssets(getFilesDir().getAbsolutePath());
        TrackerInit(getFilesDir().getAbsolutePath() + "/Facial Features Tracker - High.cfg");
        candidates[0] = findViewById(R.id.dest1);
        candidates[1] = findViewById(R.id.dest2);
        candidates[2] = findViewById(R.id.dest3);
        candidates[3] = findViewById(R.id.dest4);
        candidates[4] = findViewById(R.id.dest5);
        apply = (Button) findViewById(R.id.applyButton);
        disableApply();
        possibleFaceIndices = (Spinner) findViewById(R.id.faceSpinner);
        BitmapDrawable drawable = (BitmapDrawable)((ImageView) findViewById(R.id.source)).getDrawable();
        source = drawable.getBitmap();
        trackSource();
        attachDestClickListener();
        applyOnChangeEvent();
        attachApplyButtonListener();
    }

    void attachDestClickListener(){
        for(int i = 0; i < candidates.length; i++){
                candidates[i].setOnClickListener(new ImageButtonClickListener(candidates[i],i,this));
        }
    }

    void applyOnChangeEvent(){
        possibleFaceIndices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                faceIndex = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    void attachApplyButtonListener(){
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("FaceActivity",faceIndex+" - "+ buttonIndex);
                startRendering();

            }
        });
    }

    void startRendering(){
        Intent intent = new Intent(this, RendererActivity.class);
        intent.putExtra("sourceFace",sourceFaces[0]);
        intent.putExtra("destinationFace",destinationFaces[buttonIndex][faceIndex]);
        intent.putExtra("destinationBitmapId",resourceIds[buttonIndex]);
        startActivity(intent);
    }

    private class ImageButtonClickListener implements View.OnClickListener{

        private ImageButton imageButton;
        private int index;
        private FaceActivity faceActivity;

        public ImageButtonClickListener(ImageButton imageButton,int index, FaceActivity faceActivity){
            this.imageButton = imageButton;
            this.index = index;
            this.faceActivity = faceActivity;
        }
        @Override
        public void onClick(View view) {
            buttonIndex = index;
            if(this.faceActivity.destinationFaces[this.index] == null){
                disableApply();
                final Toast toast = Toast.makeText(faceActivity,"Extracting Face Information from image",Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 100ms
                        DestinationFaceTracker destinationFaceTracker = new DestinationFaceTracker(imageButton,faceActivity,index);
                        destinationFaceTracker.run();
                        toast.setText("Extraction Completed");
//                        Toast toastCompletion = Toast.makeText(faceActivity,"Extraction Completed",Toast.LENGTH_SHORT);
//                        toastCompletion.show();
                    }
                }, 30);
            }
            else {
                updateSpinner(index);
            }
        }
    }

    List<Integer> getAllowedFaceIndices(int index){
        List<Integer> result = new ArrayList<>();
        for(int i = 0; i < this.numfacesFound[index]; i++){
            result.add(i+1);
        }
        return result;
    }

    void trackSource(){
        SourceFaceTracker sourceFaceTracker = new SourceFaceTracker(source,this);
        sourceFaceTracker.run();
    }

    public void setDestinationFaces(FaceData[] faceData, int index){
        destinationFaces[index] = faceData;
        numfacesFound[index] = faceData.length;
        updateSpinner(index);
        enableApply();
    }

    void disableApply(){
        apply.setEnabled(false);
    }

    void enableApply(){
        apply.setEnabled(true);
    }

    public void updateSpinner(int index){
        List<Integer> data = getAllowedFaceIndices(index);
        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,data);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        possibleFaceIndices.setAdapter(spinnerAdapter);
        enableApply();
    }

    public void setSourceFaces(FaceData[] faces){
        sourceFaces = faces;
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

    static {
        System.loadLibrary("VisageVision");
        System.loadLibrary("VisageWrapper");
    }

    /** Interface to native method called for initializing tracker.
     *
     * @param configFilename absolute path to tracker configuration file.
     */
    public  native void TrackerInit(String configFilename);

}
