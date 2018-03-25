package com.roposo.creation.graphics.scenes;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLU;

import com.roposo.creation.graphics.Camera;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.OpenGLRenderer;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by sanchitsharma on 25/03/18.
 */

public class Scene3D extends DemoScene {
    private Drawable2d face;
    private Drawable2d imagePlane;
    private Drawable2d root;
    public static Bitmap sourceBitmap;
    public static Bitmap destinationBitmap;


    public static FloatBuffer verticesBuffer;
    public static FloatBuffer texCoordBuffer;
    public static ShortBuffer indicesBuffer;

    public static float[] rotationAngles;
    public static double[] translation;
    public static double[] scale;
    public static float cameraFocus;
    public static Bitmap desinationBitmap;
    public Scene3D(ArrayList<ImageSource> imageSources) {
        super(imageSources);
        root = (Drawable2d) mRootDrawables.get(0);
        root.cleanup();
        Drawable2d drawable2d = new Drawable2d();
        drawable2d.setDefaultGeometryScale(1,1);
        drawable2d.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
        drawable2d.setImageSource(new ImageSource(destinationBitmap));
        root.addChild(drawable2d);


        Drawable2d drawableFace = new Drawable2d();
        drawableFace.setRotate(rotationAngles[0],rotationAngles[1],rotationAngles[2]);
//        drawableFace.setScale(scale);
        drawableFace.setVertexBuffer(verticesBuffer);
        drawableFace.setTextureBuffer(texCoordBuffer);
        drawableFace.setIndicesBuffer(indicesBuffer);
        drawableFace.setScaleType(Drawable2d.SCALE_TYPE_FIT);
        drawableFace.setImageSource(new ImageSource(sourceBitmap));

        root.addChild(drawableFace);
    }

    @Override
    public void onDraw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);
        renderer.drawFrame(root);
    }

    public Drawable getFace() {
        return face;
    }

    public void setFace(Drawable2d face) {
        this.face = face;
    }

    public Drawable getImagePlane() {
        return imagePlane;
    }

    public void setImagePlane(Drawable2d imagePlane) {
        this.imagePlane = imagePlane;
    }
}
