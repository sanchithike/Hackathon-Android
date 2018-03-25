package com.roposo.creation.graphics.gifdecoder;

import android.graphics.Bitmap;

import com.roposo.core.util.EventTrackUtil;
import com.roposo.creation.graphics.gles.LruBitmapCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class GifManager {

    private File gifFile;
    private final GifCallback callBack;
    private GifDecoder gifDecoder;
    private String gifPath;

    public int width() {
        return gifDecoder.getWidth();
    }

    public int height() {
        return gifDecoder.getHeight();
    }

    public interface GifCallback {
        void onGIFDecoderReady();
        void onFrameAvailable(Bitmap nextFrame, int timeMs, int frameCount);
        void onNextFrame(int delay, int frameCount);
        void onError(File file);
    }

    public GifManager(GifCallback callback) {
        this.callBack = callback;
        gifDecoder = new StandardGifDecoder(new StandardBitmapProvider());
    }

    public void setGifFile(File gifFile) {
        this.gifFile = gifFile;
        this.gifPath = gifFile.getPath();
    }

    public void init() {
        try {
            gifDecoder.read(new FileInputStream(gifFile), (int) gifFile.length());
            if (gifDecoder.getDuration() == 0) {
                HashMap<String, String> map = new HashMap<>();
                map.put("path", gifPath);
                map.put("length", String.valueOf(gifFile.length()));
                EventTrackUtil.logDebug("decode", "duration is 0", "GifManager", map, 4);
                gifDecoder.setDuration(1000);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            onError();
        }
    }

    public void start() {
        callBack.onGIFDecoderReady();
    }

    public void onError() {
        callBack.onError(gifFile);
    }

    public void destroy() {
        if (gifDecoder != null) {
            gifDecoder.clear();
            gifDecoder = null;
        }
    }

    public void decodeFrame(int ms) {
        Bitmap bitmap = getFrameAt(ms);
        if (bitmap == null) return;
        callBack.onFrameAvailable(bitmap, ms, gifDecoder.getCurrentFrameIndex());
        callBack.onNextFrame(gifDecoder.getNextDelay(), gifDecoder.getCurrentFrameIndex());
    }

    public Bitmap getFrameAt(int ms) {
        gifDecoder.seekTo(ms);
        return decodeFrame();
    }

    public void decodeNextFrame() {
//        int frame = currentFrame++%totalFrames;
        gifDecoder.advance();
        Bitmap bitmap = decodeFrame();
        if (bitmap == null) return;
        int ms = gifDecoder.getTimestamp(gifDecoder.getCurrentFrameIndex());
        callBack.onFrameAvailable(bitmap, ms, gifDecoder.getCurrentFrameIndex());
        callBack.onNextFrame(gifDecoder.getNextDelay(), gifDecoder.getCurrentFrameIndex());
    }

    public int getDuration() {
        return gifDecoder.getDuration();
    }

    private Bitmap decodeFrame() {
        String key = gifFile.getAbsolutePath() + String.valueOf(gifDecoder.getCurrentFrameIndex());
        Bitmap bitmap = LruBitmapCache.INSTANCE.get(key);
        if (bitmap == null) {
            bitmap = gifDecoder.getNextFrame();
            if (bitmap == null) return null;
            int width = Math.min(1080, bitmap.getWidth());
            int height = (int) (width * ((float) bitmap.getHeight() / bitmap.getWidth()));
            Bitmap outBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            if (outBitmap.equals(bitmap)) {
                outBitmap = bitmap.copy(bitmap.getConfig(), false);
            }
            bitmap = outBitmap;
            LruBitmapCache.INSTANCE.put(key, bitmap);
        }
        return bitmap;
    }
}
