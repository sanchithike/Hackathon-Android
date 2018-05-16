package com.visagetechnologies.visagetrackerdemo;

/**
 * Created by sanchitsharma on 17/03/18.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL10Ext;
import javax.microedition.khronos.opengles.GL11;

public class FaceRenderer implements GLSurfaceView.Renderer {

    private int _width;
    private int _height;
    private Context _context;
    private int _sourceIndex;
    private int _destinationIndex;
    private int[] _medianColor;
    private TrackerActivity _trackerActivity;
    private int source_frame_tex_id;
    private int destination_frame_tex_id;
    private int[] textures = new int[2];
    private boolean sourceTextureLoaded;
    private boolean destinationTextureLoaded;
    private Bitmap _sourceBitmap;
    private Bitmap _destinationBitmap;
    private int[] pixelData;
    private ShortBuffer triangleBuffer;
    private int triangleArrayLength;

    public FaceRenderer(Context context, int sourceIndex, int destinationIndex, int[] medianColor){
        this._context = context;
        _sourceIndex = sourceIndex;
        _destinationIndex = destinationIndex;
        _medianColor = medianColor;
    }

    public FaceRenderer(Context context, TrackerActivity trackerActivity,int sourceIndex, int destinationIndex, int[] medianColor){
        this._context = context;
        _sourceIndex = sourceIndex;
        _destinationIndex = destinationIndex;
        _medianColor = medianColor;
        _trackerActivity = trackerActivity;
        _sourceBitmap = _trackerActivity.getSourceFaces()[_sourceIndex].getBitmap();
        _destinationBitmap = _trackerActivity.getDestinationFaces()[_destinationIndex].getBitmap();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        WindowManager wm = (WindowManager) _context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        loadTriangleBuffer(_trackerActivity.getSourceFaces()[_sourceIndex]);
        loadSourceTexture(gl);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,0,_sourceBitmap,0);
        loadDestinationTexture(gl);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, _destinationBitmap, 0);
        //loadSourceTexture(gl);
        //GLUtils.texImage2D(GL10.GL_TEXTURE_2D,0,_sourceBitmap,0);
/*        pixelData = new int[_destinationBitmap.getHeight()*_destinationBitmap.getWidth()];
        ByteBuffer bb = ByteBuffer.allocateDirect(_destinationBitmap.getHeight()*_destinationBitmap.getWidth()*4);
        bb.order(ByteOrder.nativeOrder());
        _destinationBitmap.getPixels(pixelData,0,_destinationBitmap.getWidth(),0,0,_destinationBitmap.getWidth(),_destinationBitmap.getHeight());
        for(int i = 0; i < _destinationBitmap.getWidth() * _destinationBitmap.getHeight(); i++){
            int colorTemp = pixelData[i];
            int a = colorTemp & 0xFF;
            int r = (colorTemp >> 8) & 0xFF;
            int g = (colorTemp >> 16) & 0xFF;
            int b = (colorTemp >> 24) & 0xFF;
            pixelData[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        bb.asIntBuffer().put(pixelData);*/
//        gl.glTexSubImage2D(GL10.GL_TEXTURE_2D,0,0,0,_destinationBitmap.getWidth(),_destinationBitmap.getHeight(),GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,bb);
    }

    int[] calculateViewPort(Bitmap bitmap,int width, int height){
        int[] viewportDimensions = new int[2];
        int glWidth = width;
        int glHeight = height;
        float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        int tmp;
        if(bitmap.getWidth() < bitmap.getHeight())
        {
            tmp = glHeight;
            glHeight = (int) (glWidth / aspectRatio);
            if (glHeight > tmp)
            {
                glWidth  = (glWidth*tmp/glHeight);
                glHeight = tmp;
            }
        }
        else
        {
            tmp = glWidth;
            glWidth = (int)(glHeight * aspectRatio);
            if (glWidth > tmp)
            {
                glHeight  = glHeight*tmp/glWidth;
                glWidth = tmp;
            }
        }
        viewportDimensions[0] = glWidth;
        viewportDimensions[1] = glHeight;
        return viewportDimensions;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        _width = width;
        _height = height;
    }

    private void SetupCamera(GL10 gl, int width, int height, float f)
    {
        float x_offset = 1;
        float y_offset = 1;
        if (width > height)
            x_offset = ((float)width)/((float)height);
        else if (width < height)
            y_offset = ((float)height)/((float)width);

        //Note:
        // FOV in radians is: fov*0.5 = arctan ((top-bottom)*0.5 / near)
        // In this case: FOV = 2 * arctan(frustum_y / frustum_near)
        //set frustum specs
        float frustum_near = 0.001f;
        float frustum_far = 30; //hard to estimate face too far away
        float frustum_x = x_offset*frustum_near/f;
        float frustum_y = y_offset*frustum_near/f;
        //set frustum
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-frustum_x,frustum_x,-frustum_y,frustum_y,frustum_near,frustum_far);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        //clear matrix
        gl.glLoadIdentity();
        //camera in (0,0,0) looking at (0,0,1) up vector (0,1,0)
        GLU.gluLookAt(gl,0,0,0,0,0,1,0,1,0);
        //gl.gluLookAt(0,0,0,0,0,1,0,1,0);

    }

    private void loadTriangleBuffer(FaceData faceData){
//        int numTriangles = faceData.getFaceModelTriangles().length/3;
//        int[] triangles = new int[numTriangles * 3  * 2];
//        for (int i = 0; i < numTriangles; i++) {
//            int[] triangle = {
//                    (faceData.getFaceModelTriangles()[3*i+0]),
//                    (faceData.getFaceModelTriangles()[3*i+1]),
//                    (faceData.getFaceModelTriangles()[3*i+2]),
//            };
//            if (triangle[0] > triangle[1])
//                swap(triangle,0, 1);
//            if (triangle[0] > triangle[2])
//                swap(triangle,0, 2);
//            if (triangle[1] > triangle[2])
//                swap(triangle,1, 2);
//
//            triangles[6*i + 0] = triangle[0];
//            triangles[6*i + 1] = triangle[1];
//            triangles[6*i + 2] = triangle[2];
//            triangles[6*i + 3] = triangle[2];
//            triangles[6*i + 4] = triangle[1];
//            triangles[6*i + 5] = triangle[2];
//
////            indexList.insert(std::make_pair(triangle[0], triangle[1]));
////            indexList.insert(std::make_pair(triangle[1], triangle[2]));
////            indexList.insert(std::make_pair(triangle[0], triangle[2]));
//        }

        triangleBuffer = getShortBuffer(faceData.getCorrectedTriangles());
        triangleArrayLength = faceData.getCorrectedTriangles().length;
    }


    private void displayWireFrame(GL10 gl, FaceData sourceFaceData, FaceData destinationFaceData, int width, int height, int textureId){

        //set image specs
//        int[] viewportDimensions = calculateViewPort(_destinationBitmap,width,height);
//        Log.d("FaceRenderer" , "calculated viewports "+viewportDimensions[0] + ","+ viewportDimensions[1]);
//        Log.d("FaceRenderer" , "before calculation"+viewportDimensions[0] + ","+ viewportDimensions[1]);
        //gl.glViewport(0, 0, viewportDimensions[0], viewportDimensions[1]);
        gl.glViewport(0,0,width,height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_CULL_FACE);
        SetupCamera(gl,width, height, destinationFaceData.getCameraFocus());

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
        //set the color for the wireframe
//        gl.glColor4f(0.0f,1.0f,0.0f,1.0f);
        //vertex list
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,getFloatBuffer(destinationFaceData.getFaceModelVertices()));
        gl.glTexCoordPointer(2,GL10.GL_FLOAT,0,getFloatBuffer(sourceFaceData.getFaceModelTextureCoords()));
        //gl.glLineWidth(1);

        gl.glTranslatef(destinationFaceData.getFaceTranslation()[0], destinationFaceData.getFaceTranslation()[1], destinationFaceData.getFaceTranslation()[2]);
        float[] r = destinationFaceData.getFaceRotation();
        gl.glRotatef((float)Math.toDegrees(r[1] + Math.PI), 0.0f, 1.0f, 0.0f);
        gl.glRotatef((float)Math.toDegrees(r[0]), 1.0f, 0.0f, 0.0f);
        gl.glRotatef((float) Math.toDegrees(r[2]), 0.0f, 0.0f, 1.0f);

        //draw the wireframe
        //gl.glDrawElements(GL10.GL_LINES, triangleArrayLength, GL10.GL_UNSIGNED_SHORT, triangleBuffer);

        // draw with triangles
//        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 357);
        gl.glDrawElements(GL10.GL_TRIANGLES, destinationFaceData.getCorrectedTriangles().length, GL10.GL_UNSIGNED_SHORT, getShortBuffer(destinationFaceData.getCorrectedTriangles()));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
    }

    private void swap(int[] array, int from, int to){
        int temp = array[from];
        array[from] = array[to];
        array[to] = temp;
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        setCamera(gl);
        //gl.glClearColor(0,0,0,0);
//        SetupCamera(gl,_width, _height, _trackerActivity.getDestinationFaces()[_destinationIndex].getCameraFocus());
        drawDestinationFrame(gl);
        //drawSourceFace(gl);
        //displayResults(_width,_height,_sourceIndex,_destinationIndex,_medianColor);
        displayWireFrame(gl,_trackerActivity.getSourceFaces()[_sourceIndex],_trackerActivity.getDestinationFaces()[_destinationIndex], _width,_height,textures[0]);
        //gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        //gl.glEnable(GL10.GL_BLEND);
    }

    void setCamera(GL10 gl){
        gl.glClearColor(0,0,0,0);
        gl.glDisable(GL10.GL_ALPHA_TEST);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_BLEND );
        gl.glDisable(GL10.GL_DITHER);
        gl.glDisable(GL10.GL_FOG);
        gl.glDisable(GL10.GL_SCISSOR_TEST);
        gl.glDisable(GL10.GL_STENCIL_TEST);
        gl.glDisable(GL10.GL_LIGHTING);
        gl.glDisable(GL10.GL_LIGHT0);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    void drawDestinationFrame(GL10 gl){
        gl.glBindTexture(GL10.GL_TEXTURE_2D,textures[1]);
        float[] vertices = {
                0.0f,0.0f,-5.0f,
                1.0f,0.0f,-5.0f,
                1.0f,1.0f,-5.0f,
                0.0f,1.0f,-5.0f,
        };
//        float[] uvBounds = Utils.getUnWrapBounds(_destinationBitmap);
        // tex coords are flipped upside down instead of an image
//        float[] texcoords = {
//                0.0f,			uvBounds[1],
//                uvBounds[0],	uvBounds[1],
//                0.0f,			0.0f,
//                uvBounds[0],	0.0f,
//        };

        float[] texcoords = {
                0.0f,	1.0f,
                1.0f,	1.0f,
                1.0f,	0.0f,
                0.0f,	0.0f,
        };
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        // Vertex Buffer
        ByteBuffer vertexByteBuffer=ByteBuffer.allocateDirect(vertices.length*4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertexbuffer=vertexByteBuffer.asFloatBuffer();
        vertexbuffer.put(vertices);
        vertexbuffer.position(0);

        // Texture Coordinates
        ByteBuffer textureByteBuffer = ByteBuffer.allocateDirect(texcoords.length * 4);
        textureByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer textureBuffer = textureByteBuffer.asFloatBuffer();
        textureBuffer.put(texcoords);
        textureBuffer.position(0);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexbuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
        int[] viewportDimensions = calculateViewPort(_destinationBitmap,_width,_height);
        gl.glViewport(0, (_height - viewportDimensions[1]) / 2, viewportDimensions[0],viewportDimensions[1]);
//        gl.glViewport(0, 0, _width,_height);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
    }

    FloatBuffer getFloatBuffer(float[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(array);
        floatBuffer.position(0);
        return floatBuffer;
    }

    IntBuffer getIntBuffer(int[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(array);
        intBuffer.position(0);
        return intBuffer;
    }

    ShortBuffer getShortBuffer(int[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer intBuffer = byteBuffer.asShortBuffer();
        for(int i = 0; i < array.length; i++) {
            intBuffer.put((short) array[i]);
        }
        intBuffer.position(0);
        return intBuffer;
    }

//    void drawSourceFace(GL10 gl){
//        gl.glEnable(GL10.GL_TEXTURE_2D);
//        gl.glBindTexture(GL10.GL_TEXTURE_2D,textures[0]);
//        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//
//
//        float[] vertices = _trackerActivity.getDestinationFaces()[_destinationIndex].getFaceContourVertices();
//        ByteBuffer vertexByteBuffer=ByteBuffer.allocateDirect(vertices.length*4);
//        vertexByteBuffer.order(ByteOrder.nativeOrder());
//        FloatBuffer vertexbuffer=vertexByteBuffer.asFloatBuffer();
//        vertexbuffer.put(vertices);
//        vertexbuffer.position(0);
//
//        // Texture Coordinates
//        float[] texcoords = _trackerActivity.getSourceFaces()[_sourceIndex].getFaceContourTextureCoordinates();
//        ByteBuffer textureByteBuffer = ByteBuffer.allocateDirect(texcoords.length * 4);
//        textureByteBuffer.order(ByteOrder.nativeOrder());
//        FloatBuffer textureBuffer = textureByteBuffer.asFloatBuffer();
//        textureBuffer.put(texcoords);
//        textureBuffer.position(0);
//
//
//        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexbuffer);
//        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
//        int[] viewportDimensions = calculateViewPort(_sourceBitmap,_width,_height);
//        gl.glViewport(0, 0, viewportDimensions[0], viewportDimensions[1]);
//        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, vertices.length/3);
//        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//        gl.glDisable(GL10.GL_TEXTURE_2D);
//        gl.glPopMatrix();
//        gl.glMatrixMode(GL10.GL_MODELVIEW);
//        gl.glPopMatrix();
//        gl.glBindTexture(GL10.GL_TEXTURE_2D,0);
//    }



    void loadSourceTexture(GL10 gl){
        gl.glGenTextures(1, textures,0);
        int textureId = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    }

    void loadDestinationTexture(GL10 gl){
        gl.glGenTextures(1, textures,1);
        int textureId = textures[1];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    }

    /** Interface to native method used to draw current frame analyzed by tracker and face model generated from tracking results
     *
     * @param width width of drawing view.
     * @param height height of drawing view.
     */
    public native void displayFace(int width, int height);
    public native void displayResults(int width, int height,int sourceIndex, int destinationIndex, int[] medianColor);

    static
    {
        System.loadLibrary("VisageVision");
        System.loadLibrary("VisageWrapper");
    }

    public int[] get_medianColor() {
        return _medianColor;
    }

    public void set_medianColor(int[] _medianColor) {
        this._medianColor = _medianColor;
    }

    public TrackerActivity get_trackerActivity() {
        return _trackerActivity;
    }

    public void set_trackerActivity(TrackerActivity _trackerActivity) {
        this._trackerActivity = _trackerActivity;
    }
}
