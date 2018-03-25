package com.roposo.creation.graphics.scenes.screenSplitScenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.roposo.core.util.ContextHelper;
import com.roposo.creation.R;
import com.roposo.creation.av.AVUtils;
import com.roposo.creation.graphics.GraphicsConsts;
import com.roposo.creation.graphics.ImageSource;
import com.roposo.creation.graphics.SceneManager;
import com.roposo.creation.graphics.filters.BaseFilter;
import com.roposo.creation.graphics.filters.PointGatheringFilter;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Drawable2d;
import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.OpenGLRenderer;
import com.roposo.creation.graphics.gles.RenderTarget;
import com.roposo.creation.graphics.scenes.Scene;

import java.util.ArrayList;



/**
 * Created by Tanvi on 08/01/18.
 */

public class ShineScene extends Scene {

    private static final String TAG = "ShineScene";
    private Drawable2d mRootDrawableResult;
    private Drawable2d[] mHistoPyramidDrawables;
    private Drawable2d mRootDrawableShineDetect;
    private Drawable2d mRootDrawablePointGathering;
    //private Drawable2d mRootDrawableTemp;
    private NestedBuffer[] buffers;
    int fb1 = 0, fb2 = 0, fb3 = 0, fb4 = 0, fb5 = 0, fb1k = 0, fbTemp = 0;
    //TODO - Get this from the input source
    int resolutionX = 512;
    int resolutionY =512;
    //padded resolution od the buffer[0] in the fbo
    int paddedResolution;
    float paddedResolutionX;
    float paddedResolutionY;
    private int numberOfPoints;
    Bitmap bmp;

    enum SparkleType {
        SPARKLE(R.drawable.bigsparkle)
        ;

        int id;
        SparkleType(int id) {
            this.id = id;
        }
        public int getInt() {
            return id;
        }

    }

    public ShineScene(ArrayList<ImageSource> imageSources, SceneManager.SceneDescription sceneDescription) {
        super(4, sceneDescription);

        bmp = BitmapFactory.decodeResource(ContextHelper.getContext().getResources(), SparkleType.SPARKLE.getInt());

        for (int i = 0; i < mRootDrawables.size(); i++) {
            mRootDrawables.get(i).setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            {
                Drawable2d drawable = Drawable2d.create(imageSources);
                drawable.setFilterMode(i % 2 == 0 ? FilterManager.BLUR_H_FILTER : FilterManager.BLUR_V_FILTER);
                drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
                mRootDrawables.get(i).addChild(drawable);
            }
        }

        mRootDrawableShineDetect = new Drawable2d();
        mRootDrawableShineDetect.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        {
            Drawable2d drawable = Drawable2d.create(imageSources);
            drawable.setFilterMode(FilterManager.SHINE_DETECT_FILTER);
            drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
            mRootDrawableShineDetect.addChild(drawable);
        }

        /*
        mRootDrawableTemp = new Drawable2d();
        mRootDrawableTemp.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
        {
            Drawable2d drawable = Drawable2d.create(imageSources);
            drawable.setFilterMode(FilterManager.CREATE_IMAGE_FILTER);
            mRootDrawableTemp.addChild(drawable);
        }
        */

        // drawables that make the histopyramid
        buffers = initHistoPyramidBuffers();

        mHistoPyramidDrawables = new Drawable2d[buffers.length];
        {
            Drawable2d drawable = Drawable2d.create(imageSources);
            drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
            drawable.setFilterMode(FilterManager.HISTOPYRAMID_CALCULATOR_FILTER);
//            drawable.setFilterMode(FilterManager.HISTOPYRAMIDSUM_CALCULATOR_FILTER);
            mHistoPyramidDrawables[0] = new Drawable2d();
            mHistoPyramidDrawables[0].addChild(drawable);
        }

        for (int i = 1; i < mHistoPyramidDrawables.length; i++) {
            Drawable2d drawable = Drawable2d.create(imageSources);
/*            if (i == 0) {*/
                drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
/*            }*/
//            drawable.setFilterMode(FilterManager.HISTOPYRAMID_CALCULATOR_FILTER);
            drawable.setFilterMode(FilterManager.HISTOPYRAMIDSUM_CALCULATOR_FILTER);
            mHistoPyramidDrawables[i] = new Drawable2d();
            mHistoPyramidDrawables[i].addChild(drawable);

        }

        {
            mRootDrawablePointGathering = new Drawable2d();
            mRootDrawablePointGathering.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            Drawable2d drawableC = Drawable2d.create(imageSources);
            drawableC.setScaleType(Drawable2d.SCALE_TYPE_FIT_WIDTH);
            drawableC.setFilterMode(FilterManager.POINT_GATHERING_FILTER);
            drawableC.setDefaultGeometryScale(1.0f, 1.0f);
            mRootDrawablePointGathering.addChild(drawableC);
        }

        {
            mRootDrawableResult = new Drawable2d();
            mRootDrawableResult.setDefaultGeometryScale(GraphicsConsts.MATCH_PARENT, GraphicsConsts.MATCH_PARENT);
            Drawable2d drawableC = Drawable2d.create(imageSources);
            drawableC.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
//            drawableC.setDefaultTranslate(0.0f, -0.25f);
//            drawableC.setDefaultGeometryScale(1.0f, 0.5f);
            mRootDrawableResult.addChild(drawableC);
        }


    }

    private Drawable2d createDrawable(String scenePath, boolean isCamera) {
        Drawable2d drawable;
        if (scenePath == null) {
            drawable = Drawable2d.createExternalSourceDrawable(isCamera,
                    isCamera ? Drawable2d.SCALE_TYPE_CROP : Drawable2d.SCALE_TYPE_CROP);
        } else {
            drawable = Drawable2d.createBitmapDrawable(scenePath, Drawable2d.SCALE_TYPE_FIT_WIDTH);
        }
        drawable.setDefaultTranslate(0.0f, 0.0f);
        drawable.setDefaultGeometryScale(1.0f, 1.0f);
        return drawable;
    }

    @Override
    public void draw(OpenGLRenderer.Fuzzy renderTargetType, long timeStampMs) {
        OpenGLRenderer renderer = OpenGLRenderer.getRenderer(renderTargetType);

        long startTime= System.currentTimeMillis();
        // 1. 2 Passes of Blur
        FBObject blurResultFBO1 = renderer.getFrameBufferObject(fb1);
        FBObject blurResultFBO2 = renderer.getFrameBufferObject(fb2);
        if (blurResultFBO1 == null || blurResultFBO2 == null) {
            int width = 512;
            int height = 512;

            //((Drawable2d)mRootDrawables.get(0).getChildAt(0)).addImageSource(new ImageSource(Caches.blurKernel));

            blurResultFBO1 = renderer.createFrameBufferObject(width, height, false);
            fb1 = blurResultFBO1.getFrameBufferId();

            ((Drawable2d)mRootDrawables.get(1).getChildAt(0)).setImageSource(new ImageSource(blurResultFBO1));
            //((Drawable2d)mRootDrawables.get(1).getChildAt(0)).addImageSource(new ImageSource(Caches.blurKernel));

            blurResultFBO2 = renderer.createFrameBufferObject(width, height, false);
            fb2 = blurResultFBO2.getFrameBufferId();

            ((Drawable2d) mRootDrawableShineDetect.getChildAt(0)).addImageSource(new ImageSource(blurResultFBO2));

        }
        renderer.drawFrame(mRootDrawables.get(0), blurResultFBO1);
        renderer.drawFrame(mRootDrawables.get(1), blurResultFBO2);

        long blurTime= System.currentTimeMillis();
        if (AVUtils.VERBOSE) Log.d(TAG, "blurTime=" + (blurTime-startTime));
        //  2. WRITE TO THE SECOND BUFFER THE RESULT OF EDGE DETECTION - SAME SIZE
        FBObject shineDetectFBO = renderer.getFrameBufferObject(fb3);
        if (shineDetectFBO == null) {
            shineDetectFBO = renderer.createFrameBufferObject(512, 512, false);
            fb3 = shineDetectFBO.getFrameBufferId();
            ((Drawable2d)mHistoPyramidDrawables[0].getChildAt(0)).setImageSource(new ImageSource(shineDetectFBO));
        }
        renderer.drawFrame(mRootDrawableShineDetect, shineDetectFBO);

        long shineDetectTime= System.currentTimeMillis();
        if (AVUtils.VERBOSE) Log.d(TAG, "shineDetectTime=" + (shineDetectTime - blurTime));
        //For showing a self created image.
/*
        FBObject newimageEmpty = renderer.getFrameBufferObject(fb1);
        if (newimageEmpty == null) {
            newimageEmpty = renderer.createFrameBufferObject(8, 8, false);
            fb1 = newimageEmpty.getFrameBufferId();
            ((Drawable2d) mRootDrawableTemp.getChildAt(0)).setImageSource(new ImageSource(newimageEmpty));
        }

        FBObject newimage = renderer.getFrameBufferObject(fb2);
        if (newimage == null) {
            newimage = renderer.createFrameBufferObject(8, 8, false);
            fb2 = newimage.getFrameBufferId();
            ((Drawable2d)mHistoPyramidDrawables[0].getChildAt(0)).setImageSource(new ImageSource(newimage));
            ((Drawable2d) mRootDrawableResult.getChildAt(0)).setImageSource(new ImageSource(newimage));
//            ((Drawable2d)mRootDrawable1k.getChildAt(0)).setImageSource(new ImageSource(newimage));
        }

        renderer.drawFrame(mRootDrawableTemp, newimage);
*/


        // 2. CREATE ANOTHER FBO TO STORE HISTO PYRAMID.
        FBObject histoPyramidFBO = renderer.getFrameBufferObject(fb4);
        if (histoPyramidFBO == null) {
            histoPyramidFBO = renderer.createFrameBufferObject(buffers[0].size * 2, buffers[0].size, false);
            //Log.w(TAG, "HP Buffer Size: " + buffers[0].size * 2 + "*" + buffers[0].size);
            fb4 = histoPyramidFBO.getFrameBufferId();
            ImageSource iSource = new ImageSource(histoPyramidFBO);
            //nth BufferDrawable will write to nth Buffer
            //set image source of buffer 1 onwards to the i - 1th buffer
            for (int i = 1; i < buffers.length; i++) {
                ImageSource imageSource = new ImageSource(histoPyramidFBO);
                imageSource.setRegionOfInterest(buffers[i - 1].getROI(), ((Drawable2d) mHistoPyramidDrawables[i].getChildAt(0)));
                //Log.w(TAG, "HP i : " + i + " ROI: " + buffers[i - 1].getROI());
                ((Drawable2d) mHistoPyramidDrawables[i].getChildAt(0)).setImageSource(imageSource);
            }
        }

        // call histopyramid filter on all the drawables
        for (int i = 0; i < mHistoPyramidDrawables.length; i++) {
            //Log.w(TAG, "HP i: " + i + " Extent: " + buffers[i].getExtent());
            histoPyramidFBO.setExtent(buffers[i].getExtent());
            renderer.drawFrame(mHistoPyramidDrawables[i], histoPyramidFBO);
        }

        long histoPyramidTime= System.currentTimeMillis();
        if (AVUtils.VERBOSE) Log.d(TAG, "histoPyramidTime=" + (histoPyramidTime - shineDetectTime));

        // 3. NOW THE FBO HAS THE HISTOPYRAMID.
        // GET THE TOTAL POINTS. MAKE A NEW FBO WHICH HAS THE RESULTING POINTS
        int xCoordinateOf1x1Point = buffers[buffers.length - 1].startX;

        int[] totalPositivePoints = renderer.readPixelsFromGLSurface(histoPyramidFBO, xCoordinateOf1x1Point, 0, 1, 1);

        int positivePointCount = 0;
        if (totalPositivePoints != null && totalPositivePoints.length > 0) {
            positivePointCount = totalPositivePoints[0];
//            positivePointCount = (int) ((positivePointCount & 0xff)/256.0 * Math.pow(4, 6));    //4^highest level - for the averaging out histopyramid
            positivePointCount = (int) ((positivePointCount & 0xff)/256.0 * 100);   //for the sum histopyramid
        }
        
        Drawable2d sceneDrawable = (Drawable2d) mRootDrawableResult.getChild(0);
        sceneDrawable.removeChildren();
        long readPixelTime = 0;
        if (positivePointCount > 0) {
            numberOfPoints = positivePointCount;
            FBObject positivePointsCoordinatesFBO = renderer.getFrameBufferObject(fb5);
            if (positivePointsCoordinatesFBO == null) {
                positivePointsCoordinatesFBO = renderer.createFrameBufferObject(512, 1, false);
                fb5 = positivePointsCoordinatesFBO.getFrameBufferId();

                // 4. RUN POINT GATHERING SHADER ON HISTOPYRAMID TO O/P COORDINATES TO LAST FBO
                ImageSource imageSource = new ImageSource(histoPyramidFBO);
                ((Drawable2d) mRootDrawablePointGathering.getChildAt(0)).setImageSource(imageSource);
            }

            renderer.drawFrame(mRootDrawablePointGathering, positivePointsCoordinatesFBO);
            long gatherPointTime= System.currentTimeMillis();
            if (AVUtils.VERBOSE) Log.d(TAG, "gatherPointTime=" + (gatherPointTime - histoPyramidTime));

            int[] data = renderer.readPixelsFromGLSurface(positivePointsCoordinatesFBO, 0, 0, positivePointCount, 1);
            readPixelTime= System.currentTimeMillis();
            if (AVUtils.VERBOSE) Log.d(TAG, "readPixelTime=" + (readPixelTime - gatherPointTime) + " for pixels:" + data.length);
            //Draw the bitmap on the points
            for(int i = 0; i < data.length; i++) {
                Drawable2d child = Drawable2d.createBitmapDrawable(bmp, Drawable2d.SCALE_TYPE_INSIDE);
                child.setDefaultGeometryScale(Math.random()/10.0, Math.random()/10.0);
                child.setDefaultTranslate(((data[i] & 0xff)/256.0) * 2.0 - 0.5f, ((data[i] >> 8 & 0xff)/256.0 - 0.5f) * 0.56);
                child.setRotate(0, 0, Math.random() * 360);
                sceneDrawable.addChild(child);
            }
        }
        renderer.drawFrame(mRootDrawableResult, false);
        long drawShineTime= System.currentTimeMillis();
        if (AVUtils.VERBOSE) Log.d(TAG, "drawShineTime=" + (drawShineTime - readPixelTime));

        sceneDrawable.removeChildren();
    }

    /**
     * @return Smallest power of 2 greater than the number
     */
    private int smallestPowerOf2GreaterThan(int x) {
        return (int) Math.pow(2, Math.ceil(Math.log(x) / Math.log(2)));
    }

    private int floorLog2(int x) {
        int y, v;
        // No log of 0 or negative
        if (x <= 0) {
            throw new IllegalArgumentException("" + x + " <= 0");
        }
        v = x;
        y = -1;
        while (v > 0) {
            v >>= 1;
            y++;
        }
        return y;
    }

    private class NestedBuffer {
        int level;  //level in the chain of pyramids
        int size;
        float sizeF;
        int startX;
        int startY;
        float sizeFX;
        float sizeFY;
        float startFX;
        float startFY;

        Rect mExtent;
        RectF mROI;

        NestedBuffer(int level, int size, int startX, int startY, float sizeFX, float sizeFY, float startFX, float startFY) {
            this.level = level;
            this.size = size;
            this.sizeFX = sizeFX;
            this.sizeFY = sizeFY;
            this.startX = startX;
            this.startY = startY;
            this.startFX = startFX;
            this.startFY = startFY;
        }

        Rect getExtent() {
            return mExtent;
        }

        RectF getROI() {
            return mROI;
        }
    }

    private NestedBuffer[] initHistoPyramidBuffers() {
        int maxResolution = resolutionX > resolutionY ? resolutionX : resolutionY;
        paddedResolution = smallestPowerOf2GreaterThan(maxResolution);
        paddedResolutionX = 0.5f;
        paddedResolutionY = 1.0f;
        int l0size = paddedResolution;
        int numOfBuffers = floorLog2(l0size) + 1;
        NestedBuffer[] buffers = new NestedBuffer[numOfBuffers];
        int size = l0size;
        float sizeFX = paddedResolutionX;
        float sizeFY = paddedResolutionY;
        int startX = 0;
        int startY = 0;
        float startFX = 0;
        float startFY = 0;
        for (int i = 0; i < numOfBuffers; i++) {
            buffers[i] = new NestedBuffer(i, size, startX, startY, sizeFX, sizeFY, startFX, startFY);
            buffers[i].mROI = new RectF(startFX, startFY, sizeFX + startFX, sizeFY + startFY);
            buffers[i].mExtent = new Rect(startX, startY, size + startX, size + startY);
            startX = startX + size;
            size = size / 2;
            startFX = startFX + sizeFX;
            sizeFX = sizeFX / 2;
            sizeFY = sizeFY / 2;
        }
        return buffers;
    }

    @Override
    public void onReady() {
        super.onReady();

        mRootDrawablePointGathering.setLifecycleListener(new Drawable.LifecycleListener() {
            @Override
            public void onReady() {

            }

            @Override
            public void onMeasure(RenderTarget renderTarget, Drawable drawable, double width, double height) {

            }

            @Override
            public void onFilterSetup(RenderTarget renderTarget, Drawable drawable, BaseFilter filter) {

            }

            @Override
            public void onFilterPredraw(OpenGLRenderer.Fuzzy renderTargetType, RenderTarget renderTarget, Drawable drawable, BaseFilter filter) {
                ((PointGatheringFilter) filter).mNumberOfPoints = numberOfPoints;

            }
        });
    }

    @Override
    public Scene clone() {
        ShineScene shineScene = (ShineScene) super.clone();

        shineScene.mHistoPyramidDrawables = mHistoPyramidDrawables.clone();
        for (int i = 0;  i < mHistoPyramidDrawables.length; i++) {
            shineScene.mHistoPyramidDrawables[i] = mHistoPyramidDrawables[i].clone();
        }
        shineScene.mRootDrawableShineDetect = mRootDrawableShineDetect.clone();
        shineScene.mRootDrawablePointGathering = mRootDrawablePointGathering.clone();
        shineScene.buffers = buffers.clone();

        shineScene.mRootDrawableResult = mRootDrawableResult.clone();

        return shineScene;
    }
}
