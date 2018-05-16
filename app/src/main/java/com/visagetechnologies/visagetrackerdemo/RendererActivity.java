package com.visagetechnologies.visagetrackerdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.av.SessionConfig;
import com.roposo.creation.fragments.RenderFragment;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.RenderManager;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.scenes.Scene3D;

public class RendererActivity extends AppCompatActivity {

    private FaceData source;

    private FaceData destination;

    private Bitmap sourceBitmap;

    private Bitmap destinationBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        ContextHelper.setContext(this);
        ContextHelper.setApplicationContext(getApplicationContext());
        source = (FaceData) getIntent().getSerializableExtra("sourceFace");
        destination = (FaceData) getIntent().getSerializableExtra("destinationFace");
        sourceBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.source);
//        sourceBitmap = (Bitmap) getIntent().getParcelableExtra("sourceBitmap");
        int destinationResourceId = (Integer) getIntent().getSerializableExtra("destinationBitmapId");
        destinationBitmap = BitmapFactory.decodeResource(getResources(),destinationResourceId);
//        destinationBitmap = (Bitmap) getIntent().getParcelableExtra("BitmapImage");
        render();
    }

    private RectF convertRect(Rect faceRect) {
        RectF result = new RectF();
        result.top = (float) faceRect.top / sourceBitmap.getHeight();
        result.bottom = (float) faceRect.bottom / sourceBitmap.getHeight();
        result.left = (float) faceRect.left / sourceBitmap.getWidth();
        result.right = (float) faceRect.right / sourceBitmap.getWidth();
        return result;
    }

    void render(){
        Scene3D.sourceBitmap = sourceBitmap;
//        Scene3D.verticesBuffer = Utils.getFloatBuffer(destination.getFaceModelVertices());
        Scene3D.verticesBuffer = Utils.getFloatBuffer(source.getFaceModelVertices());
        Scene3D.texCoordBuffer = Utils.getFloatBuffer(source.getFaceModelTextureCoords());
        Scene3D.indicesBuffer = Utils.getShortBuffer(source.getCorrectedTriangles());
//        Scene3D.leftEyeVerticesBuffer = Utils.getFloatBuffer(destination.getLeftEyeVertices());
        Scene3D.leftEyeVerticesBuffer = Utils.getFloatBuffer(source.getLeftEyeVertices());
        Scene3D.leftEyeTexCoordBuffer = Utils.getFloatBuffer(source.getLeftEyeTextureCoordinates());

//        Scene3D.rightEyeVerticesBuffer = Utils.getFloatBuffer(destination.getRightEyeVertices());
        Scene3D.rightEyeVerticesBuffer = Utils.getFloatBuffer(source.getRightEyeVertices());
        Scene3D.rightEyeTexCoordBuffer = Utils.getFloatBuffer(source.getRightEyeTextureCoordinates());

        Scene3D.eyeIndicesBuffer = Utils.getShortBuffer(source.getEyeTriangles());

//        Scene3D.rect = convertRect(source.getFaceRect());
        // Translation
        double[] translation = new double[3];
        float[] translationFace = destination.getFaceTranslation();
        for(int i = 0; i < translationFace.length; i++){
            translation[i] = (double) translationFace[i];
        }
        Scene3D.translation = translation;
        Scene3D.destinationBitmap = destinationBitmap;
        // Rotation
        float[] rotationData = destination.getFaceRotation();
        rotationData[0] = (float) Math.toDegrees(rotationData[0]);
        rotationData[1] = (float) Math.toDegrees(rotationData[1] + Math.PI);
        rotationData[2] = (float) Math.toDegrees(rotationData[2]);
        Scene3D.rotationAngles = rotationData;

        Scene3D.cameraFocus  = destination.getCameraFocus();
        setContentView(R.layout.scenelayout);
        RenderFragment renderFragment = new RenderFragment();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.scenelayout,renderFragment).commit();
        renderFragment.startPlayback("/sdcard/Download/testing-gif.gif", GraphicsConsts.MEDIA_TYPE_IMAGE, GraphicsConsts.RENDER_TARGET_DISPLAY, new RenderManager.AVComponentListener() {
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
}
