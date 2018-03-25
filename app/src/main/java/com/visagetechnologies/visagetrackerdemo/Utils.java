package com.visagetechnologies.visagetrackerdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by sanchitsharma on 18/03/18.
 */

public class Utils {


    public static String SPACE = " ";

    public static final int STORAGE_PERMISSIONS = 1;
    public static Bitmap LoadBitmapFromFile(String path)
    {
        File image = new File(path);

        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(image) ;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(fis);
    }

    public static Bitmap CreateOptimalBitmapSize(Bitmap bitmap, Context context)
    {
        int maxW = context.getResources().getDisplayMetrics().widthPixels;
        int maxH =  context.getResources().getDisplayMetrics().heightPixels;

        int w = maxW;
        int h = maxH;

        if(bitmap.getWidth() < maxW && bitmap.getHeight() < maxH && bitmap.getWidth() % 4 == 0 && bitmap.getHeight() % 4 == 0)
        {
            return bitmap;
        }

        int bitmapW = bitmap.getWidth();
        int bitmapH = bitmap.getHeight();

        w = (bitmapW / 4) * 4;
        h = (bitmapH / 4) * 4;

        Bitmap mBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        WeakReference<Bitmap> mBitmapReference = new WeakReference<Bitmap>(mBitmap);
        bitmap = mBitmapReference.get();
        return bitmap;
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public static FloatBuffer getFloatBuffer(float[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(array);
        floatBuffer.position(0);
        return floatBuffer;
    }

    public static IntBuffer getIntBuffer(int[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(array);
        intBuffer.position(0);
        return intBuffer;
    }

    public static ShortBuffer getShortBuffer(int[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer intBuffer = byteBuffer.asShortBuffer();
        for(int i = 0; i < array.length; i++) {
            intBuffer.put((short) array[i]);
        }
        intBuffer.position(0);
        return intBuffer;
    }

    public static void saveFaceDataToFile(Activity activity, FaceData faceData, String fileName){
        boolean externalStorageStatus = isExternalStorageAvailable();
        Log.d("Utils","externalStorageAvailable = "+externalStorageStatus);
        boolean isReadOnly = isExternalStorageReadOnly();
        Log.d("Utils","externalStorageReadOnly = " + externalStorageStatus);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/HACKATHON/");
        dir.mkdirs();
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/HACKATHON/" + fileName;
        if (ContextCompat.checkSelfPermission(activity, // request permission when it is not granted.
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
        else{
            try{
                File file = new File(filePath);
//                file.mkdirs();
                if(!file.exists())
                {
                    file.createNewFile();
                    // write code for saving data to the file
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                // Vertices
                int numVertices = faceData.getFaceModelVertices().length / 3;
                for (int i = 0; i < numVertices; i++) {
                    String line = "v" + SPACE + faceData.getFaceVerticesProjected()[2 * i] + SPACE + faceData.getFaceVerticesProjected()[2 * i + 1] + SPACE + 0;
                    bw.write(line);
                    bw.newLine();
                }
                bw.newLine();
                bw.newLine();

                // Texture Coordinates
                for(int i = 0; i < numVertices; i++){
                    String line = "vt" + SPACE + faceData.getFaceModelTextureCoords()[2 * i] + SPACE + faceData.getFaceModelTextureCoords()[2 * i + 1];
                    bw.write(line);
                    bw.newLine();
                }

                bw.newLine();
                bw.newLine();

                // Faces
                int numTriangles = faceData.getFaceModelTriangles().length / 3;
                for(int i = 0; i < numTriangles; i++){
                    String line = "f" + SPACE + faceData.getFaceModelTriangles()[3 * i] + SPACE + faceData.getFaceModelTriangles()[3 * i + 1] + SPACE + faceData.getFaceModelTriangles()[3 * i + 2];
                    bw.write(line);
                    bw.newLine();
                }
                bw.close();
                fos.close();
                if(file.exists()){
                    Log.d("Utils","file saved");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public static byte[] ConvertToByte(Bitmap bitmap)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] rgbStream = new int[ w * h];
        bitmap.getPixels(rgbStream, 0, w, 0, 0, w, h);

        byte[] buffer = new byte [w * h * 4];
        int offset = 0;
        for(int i = 0; i < w * h; i++)
        {
            int pixel = rgbStream[i];
            //int A = (pixel >> 24) & 0xFF;
            int R = (pixel >> 16) & 0xFF;
            int G = (pixel >> 8) & 0xFF;
            int B = pixel & 0xFF;

            buffer[offset + 0] = (byte)R;
            buffer[offset + 1] = (byte)G;
            buffer[offset + 2] = (byte)B;

            offset = offset + 3;
        }

        return buffer;
    }

    public static int[] getMedian(Bitmap bitmap, Rect rect){
        Bitmap resizedbitmap =Bitmap.createBitmap(bitmap, rect.left,rect.top,(rect.right - rect.left) , (rect.bottom - rect.top));
        int width = resizedbitmap.getWidth();
        int height = resizedbitmap.getHeight();
        int totalPixels = width * height;
        int[] blue = new int[totalPixels];
        int[] red = new int[totalPixels];
        int[] green = new int[totalPixels];
        int[] pixelData = new int[totalPixels];
        resizedbitmap.getPixels(pixelData,0,width,0,0,width,height);
        for(int i = 0; i < totalPixels; i++){
            int pixelColor = pixelData[i];
            int R = (pixelColor >> 16) & 0xFF;
            int G = (pixelColor >> 8) & 0xFF;
            int B = pixelColor & 0xFF;
            blue[i] = B;
            red[i] = R;
            green[i] = G;
        }
        Arrays.sort(blue);
        Arrays.sort(red);
        Arrays.sort(green);
        int medianRed = getMedian(red);
        int medianBlue = getMedian(blue);
        int medianGreen = getMedian(green);
        int[] medianColor = new int[3];
        medianColor[0] = medianRed;
        medianColor[1] = medianGreen;
        medianColor[2] = medianBlue;
        return medianColor;
    }

    public static float[] getUnWrapBounds(Bitmap bitmap){
        int x_size = NearestPow2(bitmap.getWidth());
        int y_size = NearestPow2(bitmap.getHeight());
        float tex_x_coord = (float) bitmap.getWidth() / (float) x_size;
        float tex_y_coord = (float) bitmap.getHeight() / (float) y_size;
        float[] texCoords = new float[2];
        texCoords[0] = tex_x_coord;
        texCoords[1] = tex_y_coord;
        return texCoords;
    }

    public static int NearestPow2(int n)
    {
        int v; // compute the next highest power of 2 of 32-bit v
        v = n;
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    private static int getMedian(int[] array){
        int median;
        if (array.length % 2 == 0)
            median = (int)((double)array[array.length/2] + (double)array[array.length/2 - 1])/2;
        else
            median = array[array.length/2];
        return median;
    }
}
