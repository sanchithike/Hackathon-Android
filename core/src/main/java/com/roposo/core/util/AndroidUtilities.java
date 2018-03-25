package com.roposo.core.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.roposo.core.R;
import com.roposo.core.customInjections.CrashlyticsWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by anilshar on 1/6/16.
 */
public class AndroidUtilities {
    private static final String TAG = AndroidUtilities.class.getName();
    public static float density = 1;
    public static Point displaySize = new Point();
    public static boolean usingHardwareInput;
    public static DisplayMetrics displayMetrics = new DisplayMetrics();
    private static int adjustOwnerClassGuid = 0;
    private static Handler mBackgroundHandler;

    static {
        HandlerThread thread = new HandlerThread("BackgroundThread");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
    }

    public static int screenDiagonal;

    public static final int ERROR_NONE = 0;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_BAD_FILENAME = -2;
    public static final int ERROR_FILE_NOTEXISTS = -3;
    public static final int ERROR_BAD_FILETYPE = -4;
    public static final int ERROR_ACCESS = -5;
    public static final int ERROR_READING = -6;
    public static final int ERROR_DECODING = -7;
    public static final int ERROR_PARSING = -8;
    public static final int ERROR_MISSING_INFO = -9;
    public static final int ERROR_ILLEGAL_ARGUMENT = -10;
    public static final int ERROR_INVALID_DIMEN = -11;

    public static boolean hasAudio(String path) {
        boolean ret = false;
        if (!FileUtilities.isFileNameVideo(path) && !FileUtilities.isFileNameAudio(path)) {
            return false;
        }
        MediaExtractor videoMediaExtractor = new MediaExtractor();
        try {
            videoMediaExtractor.setDataSource(path);
            try {
                int trackindex = selectTrack(videoMediaExtractor, "audio/");
                ret = trackindex >= 0;
            } catch (Exception e) {
                e.printStackTrace();
                CrashlyticsWrapper.logException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashlyticsWrapper.logException(e);
        } finally {
            videoMediaExtractor.release();
        }
        return ret;
    }

    public static boolean isResponseEmpty(String string) {
        if (TextUtils.isEmpty(string)) return true;

        try {
            JSONObject responseObj = new JSONObject(string);

            JSONObject dataObj = responseObj.optJSONObject("data");
            if (dataObj == null
                    || dataObj.optJSONArray("blocks") == null
                    || dataObj.optJSONObject("det") == null
                    || dataObj.optJSONObject("det").length() == 0
                    || dataObj.optJSONArray("blocks").length() == 0)
                return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @IntDef({
            ERROR_NONE,
            ERROR_UNKNOWN,
            ERROR_BAD_FILENAME,
            ERROR_FILE_NOTEXISTS,
            ERROR_BAD_FILETYPE,
            ERROR_ACCESS,
            ERROR_DECODING,
            ERROR_ILLEGAL_ARGUMENT,
            ERROR_INVALID_DIMEN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    // If
    public static boolean recompressImage(String inputPath, String outputPath, int quality, Bitmap.CompressFormat compressFormat) {
        return recompressImage(inputPath, outputPath, quality, compressFormat, true);
    }

    public static boolean recompressImage(String inputPath, String outputPath, int quality, Bitmap.CompressFormat compressFormat, boolean retry) {
        boolean ret = false;
        if (!FileUtilities.isFileTypeImage(inputPath)) return false;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(inputPath);
            // If converting from jpg to png, extract the rotation info from meta to Rotate the pixels
            if ((FileUtilities.getFileNameType(inputPath) == FileUtilities.FILE_TYPE_JPG) && (FileUtilities.getFileNameType(outputPath) == FileUtilities.FILE_TYPE_PNG) && (AndroidUtilities.getImageRotation(inputPath) % 360 != 0)) {
                Matrix matrix = new Matrix();
                matrix.setRotate(AndroidUtilities.getImageRotation(inputPath));
                Bitmap tempBitmap = bitmap;
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                tempBitmap.recycle();
            }
            FileOutputStream os = new FileOutputStream(outputPath);
            bitmap.compress(compressFormat, quality, os);
            os.flush();
            os.getFD().sync();
            os.close();
            bitmap.recycle();
            ret = true;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (retry) {
                CrashlyticsWrapper.log(4, "recompressImage", "OutofMemory: Trying again");
                CrashlyticsWrapper.logException(e);
                HashMap<String, String> map = new HashMap<>(2);
                map.put("failure", "OOM");
                EventTrackUtil.logDebug("recompressImage", "recompress", AndroidUtilities.class.getName(), map, 4);
                System.gc();
                ret = recompressImage(inputPath, outputPath, quality, compressFormat, false);
            } else {
                CrashlyticsWrapper.log(4, "recompressImage", "OutofMemory again");
                CrashlyticsWrapper.logException(e);
            }
        } catch (Exception e) {
            AndroidUtilities.showToast("Can't access file");
            HashMap<String, String> map = new HashMap<>(2);
            map.put("failure", e.getMessage());
            map.put("srcBitmap", String.valueOf(bitmap));
            map.put("srcFile", inputPath);
            EventTrackUtil.logDebug("recompressImage", "recompress", AndroidUtilities.class.getName(), map, 4);
        }
        return ret;
    }

    public static long getMediaDuration(final String path) {
        long videoDuration = Long.MIN_VALUE;
        if (FileUtilities.isFileTypeVideo(path) || FileUtilities.isFileTypeAudio(path)) {
            videoDuration = getAVDuration(path);
        } else if (FileUtilities.isFileTypeGif(path)) {
            videoDuration = getGifDuration(path);
        }
        return videoDuration;
    }


    static {
        density = ContextHelper.applicationContext.getResources().getDisplayMetrics().density;
        checkDisplaySize();
    }

    private AndroidUtilities() {
        throw new UnsupportedOperationException("Non-instantiable class");
    }

    @NonNull
    public static GradientDrawable getGradientDrawable(int bgColor, int cornerRadius,
                                                       int strokeWidth, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(bgColor);
        background.setCornerRadius(cornerRadius);
        if (strokeColor == 0 && strokeWidth == 0) return background;
        background.setStroke(strokeWidth, strokeColor);
        return background;
    }

    public static void requestAdjustResize(Activity activity, int classGuid) {
        if (activity == null) {
            return;
        }
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        adjustOwnerClassGuid = classGuid;
    }

    public static void removeAdjustResize(Activity activity, int classGuid) {
        if (activity == null) {
            return;
        }
        if (adjustOwnerClassGuid == classGuid) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    public static void checkDisplaySize() {
        try {
            Configuration configuration = ContextHelper.applicationContext.getResources().getConfiguration();
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) ContextHelper.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    display.getSize(displaySize);
                    screenDiagonal = (int) Math.hypot(displayMetrics.widthPixels, displayMetrics.heightPixels);
                }
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }

    }

    @SuppressLint("MissingPermission")
    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ContextHelper.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void showLongToast(String msg) {
        showToast(msg, Toast.LENGTH_LONG);
    }

    public static void showShortToast(String msg) {
        showToast(msg, Toast.LENGTH_SHORT);
    }

    public static void showShortToast(@StringRes int stringRes) {
        Toast.makeText(ContextHelper.applicationContext, ContextHelper.getContext().getString(stringRes), Toast.LENGTH_SHORT).show();
    }

    public static boolean isKeyboardShowed(View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        return inputManager.isActive(view);
    }

    public static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!imm.isActive()) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void addMediaToGallery(String fromPath) {
        if (fromPath == null) {
            return;
        }
        File f = new File(fromPath);
        Uri contentUri = Uri.fromFile(f);
        addMediaToGallery(contentUri);
    }

    public static void addMediaToGallery(Uri uri) {
        if (uri == null) {
            return;
        }
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        ContextHelper.applicationContext.sendBroadcast(mediaScanIntent);
    }

    public static int pixelsToDp(int pixels) {
        if (pixels == 0 || density <= 0) {
            return pixels;
        }

        return (int) (pixels / density);
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static void runOnBackgroundThread(Runnable runnable) {
        runOnBackgroundThread(runnable, 0);
    }

    public static void runOnBackgroundThread(Runnable runnable, long delay) {
        mBackgroundHandler.postDelayed(runnable, delay);
    }

    public static void cancelRunOnBackgroundThread(Runnable runnable) {
        mBackgroundHandler.removeCallbacks(runnable);
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            ContextHelper.applicationHandler.post(runnable);
        } else {
            ContextHelper.applicationHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        ContextHelper.applicationHandler.removeCallbacks(runnable);
    }

    // TODO change name
    public static void cancelAllThreads() {
        ContextHelper.applicationHandler.removeCallbacksAndMessages(null);
    }

    public static int sp(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                displayMetrics);
    }

    public static int pixelsToSp(float px) {
        float scaledDensity = ContextHelper.getContext().getResources()
                .getDisplayMetrics().scaledDensity;
        return (int) (px / scaledDensity);
    }


    /*
     * @param: videoPath of local video in disk
     * returns : the file path of the generated video thumbnail
     */
    public static String generateVideoThumbnail(String videoPath) {
        String thumbFilePath = null;
        File exportDir = FileUtilities.getDirectoryFile(FileUtilities.MEDIA_DIR_IMAGE);
        if (null != exportDir) {
            thumbFilePath = new File(exportDir.getAbsoluteFile(), "THUMB_" + System.currentTimeMillis() + ".jpeg").getAbsolutePath();
            try {
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath,
                        MediaStore.Images.Thumbnails.MINI_KIND);
                final File thumb = new File(thumbFilePath);
                if (!thumb.exists()) {
                    thumb.createNewFile();
                }

                FileOutputStream fos = new FileOutputStream(thumb);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
        }

        return thumbFilePath;
    }


    public static String getScreenShotPath() {
        String PATH = null;
        File screenShotDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Screenshots/");
        if (screenShotDirectory.isDirectory()) {
            PATH = screenShotDirectory.getPath() + "/";
        } else {
            screenShotDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + "/Screenshots/");
            if (screenShotDirectory.isDirectory()) {
                PATH = screenShotDirectory.getPath() + "/";
            }
        }
        return PATH;
    }

    @NonNull
    public static String formatMediaDuration(long duration) {
        if (duration > 1000 * 3600) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.MILLISECONDS.toMinutes(duration) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) % 60);
        }
    }


    public static boolean isValidAbsolutePath(String path) {
        return !(path == null || path.isEmpty() || !path.startsWith("/"));
    }

    public static boolean isOnLowMemory() {
        ActivityManager activityManager = (ActivityManager) ContextHelper.applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (null != activityManager) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        }

        return false;
    }

    // Can be used to filter out actual screenshot taken on the device
    public static Point getRealScreenSize() {
        Point size = new Point();
        try {
            WindowManager windowManager = (WindowManager) ContextHelper.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                windowManager.getDefaultDisplay().getRealSize(size);
            } else {
                try {
                    Method mGetRawW = Display.class.getMethod("getRawWidth");
                    Method mGetRawH = Display.class.getMethod("getRawHeight");
                    size.set((Integer) mGetRawW.invoke(windowManager.getDefaultDisplay()), (Integer) mGetRawH.invoke(windowManager.getDefaultDisplay()));
                } catch (Exception e) {
                    size.set(windowManager.getDefaultDisplay().getWidth(), windowManager.getDefaultDisplay().getHeight());
                    CrashlyticsWrapper.logException(e);
                    MyLogger.e("roposo_msg", e.toString());
                }
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }
        return size;
    }

    /**
     * @param data raw data to be written to the file.
     * @param path
     * @throws IOException
     */
    public static void writeRawData(byte[] data, String path) throws IOException {
        final File thumb = new File(path);
        FileOutputStream fos = new FileOutputStream(thumb);
        fos.write(data, 0, data.length);
        fos.flush();
        fos.close();
    }

    /**
     * @param bitmap
     * @param path
     * @param format  One of JPEG, PNG or WEBP.
     * @param quality 0 to 100 representing quality of the image saved.
     * @throws IOException
     */
    public static void writeImage(Bitmap bitmap, String path, Bitmap.CompressFormat format, int quality) throws IOException {
        writeImage(bitmap, path, format, quality, ExifInterface.ORIENTATION_NORMAL);
    }

    /**
     * @param bitmap
     * @param path
     * @param format          One of JPEG, PNG or WEBP.
     * @param quality         0 to 100 representing quality of the image saved.
     * @param exifOrientation exif orientation to be embedded within the file
     * @throws IOException
     */
    public static void writeImage(Bitmap bitmap, String path, Bitmap.CompressFormat format, int quality, int exifOrientation) throws IOException {
        if (bitmap == null || bitmap.isRecycled()) {
            CrashlyticsWrapper.logException(new RuntimeException("writeImage: invalid bitmap: " + bitmap));
            EventTrackUtil.logDebug("Invalid bitmap: " + bitmap, "writeImage", AndroidUtilities.class.getName(), null, 4);
            return;
        }
        final File thumb = new File(path);
        FileOutputStream fos = new FileOutputStream(thumb);
        bitmap.compress(format, quality, fos);
        fos.flush();
        fos.getFD().sync();
        fos.close();

        if (format == Bitmap.CompressFormat.JPEG) {
            writeImageExif(path, exifOrientation);
        }
    }

    public static void writeImageExif(String path, int exifOrientation) {
        try {
            if (FileUtilities.isFileNameImage(path) && (FileUtilities.getFileMediaType(path) == FileUtilities.FILE_TYPE_JPG)) {
                ExifInterface exif = new ExifInterface(path);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exifOrientation));
                exif.saveAttributes();
            }
        } catch (IOException | UnsupportedOperationException e) {
            e.printStackTrace();
        }

//        syncFile(path);
    }

    public static float MAX_COMPRESSION_FACTOR = 0.181f;

    public static boolean imageNeedsCompression(String path) {
        boolean result = false;
        if (!FileUtilities.isFileNameImage(path) && !FileUtilities.isFileTypeImage(path)) {
            Log.w(TAG, "imageNeedsCompression:: Not a valid image file");
            return false;
        }
        long size = FileUtilities.getFileSize(path);
        int[] res = new int[2];
        int status = getImageSize(path, res);
        if (res == null || res.length < 2) {
            Log.w(TAG, "imageNeedsCompression:: Corrupt image");
            return false;
        }
        long compSize = res[0] * res[1] * 3;
        float compressionFactor = (float) size / compSize;
        if ((Math.min(res[0], res[1]) > 1600) && compressionFactor > MAX_COMPRESSION_FACTOR) {
            result = true;
        }
        return result;
    }

    public static boolean isPackageExisted(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void showToast(@StringRes final int resourceId) {
        showToast(ContextHelper.applicationContext.getString(resourceId));
    }

    public static void showToast(final String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    private static void showToast(@StringRes final int resourceId, final int length) {
        showToast(ContextHelper.applicationContext.getString(resourceId), length);
    }

    private static void showToast(final String message, final int length) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContextHelper.applicationContext, message, length).show();
            }
        });
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        return calculateInSampleSize(width, height, reqWidth, reqHeight);
    }

    public static int calculateInSampleSize(
            int inWidth, int inHeight, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int width = inWidth;
        final int height = inHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int queryImageSize(int resId, int[] size) {
        int status = ERROR_NONE;
        if (size == null) {
            return ERROR_ILLEGAL_ARGUMENT;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(ContextHelper.getContext().getResources(), resId, options);
        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return status;
    }

    public static int queryImageSize(@NonNull String path, int[] size) {
        int status = ERROR_NONE;
        if (size == null) {
            return ERROR_ILLEGAL_ARGUMENT;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth == -1 || options.outHeight == -1) {
            return ERROR_DECODING;
        }

        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return status;
    }

    // This snippet hides the system bars.
    public static void hideSystemUI(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public static void showSystemUI(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Change the status bar Color and navigation bar color for fragment
     * - Muddassir Iqbal
     *
     * @param activity to which fragment attached
     * @param color    that is to be set
     */
    public static void changeTheme(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);
            //window.setNavigationBarColor(activity.getResources().getColor(color));
        }
    }

    @Nullable
    public static Rect getActivityUsableArea(Activity activity) {
        if (activity == null) {
            return null;
        }
        View decorView = activity.getWindow().getDecorView();
        Rect outRect = new Rect();
        decorView.getWindowVisibleDisplayFrame(outRect);
//        outRect.bottom -= getNavigationBarHeight(activity);
        return outRect;
    }

    public static int getNavigationBarHeight(Context context) {
        if (null != context) {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return resources.getDimensionPixelSize(resourceId);
            }
        }
        return 0;
    }

    /**
     * @param srcRect  original Rect to extract original width and height values
     * @param destRect target Rect (the location specifier).
     * @param crop     true for centerCrop and false for centerInside
     * @return
     */
    public static void scaleCenter(Rect srcRect, Rect destRect, Rect computedRect, boolean crop) {
        scaleCenter(srcRect, destRect, computedRect, crop ? SCALE_CENTER_CROP : SCALE_CENTER_INSIDE);
    }

    /**
     * @param srcRect   original Rect to extract original width and height values
     * @param destRect  target Rect (the location specifier).
     * @param scaleType
     * @return
     */
    public static void scaleCenter(Rect srcRect, Rect destRect, Rect computedRect, int scaleType) {
        if (srcRect == null || destRect == null) return;

        int destWidth = destRect.width();
        int destHeight = destRect.height();

        int sourceWidth = srcRect.width();
        int sourceHeight = srcRect.height();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) destWidth / sourceWidth;
        float yScale = (float) destHeight / sourceHeight;

        float scale;
        if (scaleType == SCALE_CENTER_CROP) {
            scale = Math.max(xScale, yScale);
        } else if (scaleType == SCALE_CENTER_INSIDE) {
            scale = Math.min(xScale, yScale);
        } else if (scaleType == SCALE_FIT_WIDTH) {
            scale = xScale;
        } else if (scaleType == SCALE_FIT_HEIGHT) {
            scale = yScale;
        } else {
            scale = xScale;
        }

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (destWidth - scaledWidth) / 2;
        float top = (destHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRectF = new RectF(left, top, left + scaledWidth, top + scaledHeight);
        targetRectF.offset(destRect.left, destRect.top);

        Rect targetRect = new Rect();
        targetRectF.roundOut(targetRect);

        computedRect.set(targetRect);
    }

    public static boolean isPointInView(Float x, Float y, View view) {

        RectF rect = new RectF(view.getX(), view.getY(), view.getX() + view.getWidth(), view.getY() + view.getHeight());
        if (rect.isEmpty()) return false;
        return rect.contains(x, y);

    }

    public static int getMediaTranspose(String path) {
        int transpose;
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov")) {
            transpose = getVideoTranspose(path);
        } else if (lowerPath.endsWith("png") || lowerPath.endsWith("jpg") || lowerPath.endsWith("jpeg")) {
            transpose = getImageTranspose(path);
        } else { //Give video the benefit of doubt for now
            transpose = getVideoTranspose(path);
        }
        return transpose;
    }

    public static int getMediaSize(String path, int[] size) {
        int status = ERROR_NONE;
        if (size == null) {
            return ERROR_ILLEGAL_ARGUMENT;
        }
        if (TextUtils.isEmpty(path)) {
            return ERROR_BAD_FILENAME;
        }
        if (!new File(path).exists()) {
            return ERROR_FILE_NOTEXISTS;
        }
        if (!new File(path).canRead()) {
            return ERROR_ACCESS;
        }
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov")) {
            status = getVideoSize(path, size);
        } else if (lowerPath.endsWith("png") || lowerPath.endsWith("jpg") || lowerPath.endsWith("jpeg")) {
            status = getImageSize(path, size);
        } else if (lowerPath.endsWith(".gif")) {
            status = getGifSize(path, size);
        } else { //Give video the benefit of doubt for now
            status = getVideoSize(path, size);
        }
        return status;
    }

    public static Bitmap getFrameAtTime(String path, long millisec) {
        Bitmap bitmap = null;
        try {
            if (FileUtilities.isFileTypeVideo(path)) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(path);
                    bitmap = retriever.getFrameAtTime(millisec * 1000, MediaMetadataRetriever
                            .OPTION_CLOSEST_SYNC);

                } catch (Exception e) {
                    e.printStackTrace();
                    CrashlyticsWrapper.logException(e);
                } finally {
                    retriever.release();
                }
            } else if (FileUtilities.isFileTypeImage(path)) {
                bitmap = BitmapFactoryUtils.decodeFile(path, 360);
            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put("path", path);
                map.put("type", String.valueOf(FileUtilities.getFileMediaType(path)));
                EventTrackUtil.logDebug("getFrameAtTime", "getFileMediaType", "AndroidUtilities", map, 4);
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashlyticsWrapper.logException(e);
        }
        return bitmap;
    }

    private static int getVideoSize(String path, int[] size) {
        int status = ERROR_NONE;
        if (size == null) {
            return ERROR_ILLEGAL_ARGUMENT;
        }
        if (TextUtils.isEmpty(path)) {
            return ERROR_BAD_FILENAME;
        }
        if (!new File(path).exists()) {
            return ERROR_FILE_NOTEXISTS;
        }
        if (!new File(path).canRead()) {
            return ERROR_ACCESS;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            try {
                String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (TextUtils.isEmpty(widthStr) || TextUtils.isEmpty(heightStr)) {
                    return ERROR_MISSING_INFO;
                }
                size[0] = Integer.valueOf(widthStr);
                size[1] = Integer.valueOf(heightStr);

                int transpose = getVideoTranspose(path);
                if (transpose == TRANSPOSE_CCLOCK || transpose == TRANSPOSE_CLOCK || transpose == TRANSPOSE_CCLOCK_FLIP || transpose == TRANSPOSE_CLOCK_FLIP) {
                    int temp = size[0];
                    size[0] = size[1];
                    size[1] = temp;
                }
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
                status = ERROR_PARSING;
            }
        } catch (Exception e) {
            status = ERROR_READING;
        } finally {
            retriever.release();
        }

        if (size[0] == 0 || size[1] == 0) {
            status = ERROR_INVALID_DIMEN;
        }
        return status;
    }


    public static final int selectTrack(final MediaExtractor extractor, final String mimeType) {
        final int numTracks = extractor.getTrackCount();
        MediaFormat format;
        String mime;
        for (int i = 0; i < numTracks; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                return i;
            }
        }
        return -1;
    }

    private static int getBitrate(String path, String trackName) {
        if (TextUtils.isEmpty(path)) {
            return ERROR_BAD_FILENAME;
        }
        if (!new File(path).exists()) {
            return ERROR_FILE_NOTEXISTS;
        }
        if (!new File(path).canRead()) {
            return ERROR_ACCESS;
        }
        int bitrate = -1;
        MediaExtractor videoMediaExtractor = new MediaExtractor();
        try {
            videoMediaExtractor.setDataSource(ContextHelper.getContext(), Uri.parse(path), null);
            try {
                int trackindex = selectTrack(videoMediaExtractor, trackName);
                if (trackindex >= 0) {
                    videoMediaExtractor.selectTrack(trackindex);
                    final MediaFormat format = videoMediaExtractor.getTrackFormat(trackindex);
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                    }
                    if (bitrate <= 0) {
                        bitrate = extractVideoBitrate(path);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                CrashlyticsWrapper.logException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashlyticsWrapper.logException(e);
            extractVideoBitrate(path);
        } finally {
            videoMediaExtractor.release();
        }
        return bitrate;
    }

    public static int getAudioBitrate(String path) {
        return getBitrate(path, "audio/");
    }

    public static int getVideoBitrate(String path) {
        return getBitrate(path, "video/");
    }

    // Extracts video bit rate using MetaMetadataRetriever
    private static int extractVideoBitrate(String path) {
        int videoBitrate = ERROR_UNKNOWN;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (TextUtils.isEmpty(bitrateStr)) {
                return ERROR_DECODING;
            }
            try {
                videoBitrate = Integer.valueOf(bitrateStr);
            } catch (Exception e) {
                e.printStackTrace();
                return ERROR_DECODING;
            }
            return videoBitrate;
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            retriever.release();
        }
        return videoBitrate;
    }

    public static int getAudioChannelCount(String path) {
        int trackCount = 0;
        MediaExtractor videoMediaExtractor = new MediaExtractor();
        try {
            videoMediaExtractor.setDataSource(path);
            try {
                int trackindex = selectTrack(videoMediaExtractor, "audio/");
                if (trackindex >= 0) {
                    videoMediaExtractor.selectTrack(trackindex);
                    final MediaFormat format = videoMediaExtractor.getTrackFormat(trackindex);
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        trackCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                }
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        } finally {
            videoMediaExtractor.release();
        }
        return trackCount;
    }

    public static int getAudioSampleRate(String path) {
        int audioSampleRate = -1;
        MediaExtractor videoMediaExtractor = new MediaExtractor();
        try {
            videoMediaExtractor.setDataSource(path);
            try {
                int trackindex = selectTrack(videoMediaExtractor, "audio/");
                if (trackindex >= 0) {
                    videoMediaExtractor.selectTrack(trackindex);
                    final MediaFormat format = videoMediaExtractor.getTrackFormat(trackindex);
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    }
                }
            } catch (Exception e) {
                CrashlyticsWrapper.logException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            videoMediaExtractor.release();
        }
        return audioSampleRate;
    }

    public static int getImageSize(String imagePath, int[] size) {
        int status = ERROR_NONE;
        if (TextUtils.isEmpty(imagePath)) {
            return ERROR_BAD_FILENAME;
        }
        if (!FileUtilities.isFileTypeImage(imagePath)) {
            return ERROR_BAD_FILETYPE;
        }
        status = AndroidUtilities.queryImageSize(imagePath, size);
        int orientation = getImageOrientation(imagePath);
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            int temp = size[0];
            size[0] = size[1];
            size[1] = temp;
        }
        return status;
    }

    public static int getGifSize(String path, int[] size) {
        try {
            Movie decodedMovie = Movie.decodeFile(path);
            if (decodedMovie == null) {
                return ERROR_DECODING;
            }
            size[0] = decodedMovie.width();
            size[1] = decodedMovie.height();
            return ERROR_NONE;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ERROR_UNKNOWN;
    }

    public static long getAVDuration(String path) {
        long videoDuration = Long.MIN_VALUE;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(path);
            String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            videoDuration = Long.parseLong(duration);
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
            MyLogger.e(MyLogger.makeLogTag(AndroidUtilities.class), e.toString());
            HashMap<String, String> map = new HashMap<>(3);
            map.put("failure", e.getMessage());
            map.put("videoPath", path);
            if (path != null) {
                map.put("exists", String.valueOf(new File(path).exists()));
                map.put("fileLength", String.valueOf(new File(path).length()));
            }
            EventTrackUtil.logDebug("MediaMetaDataRetriever", "getMediaDuration", AndroidUtilities.class.getName(), map, 4);
        } finally {
            mediaMetadataRetriever.release();
        }
        return videoDuration;
    }

    public static long getGifDuration(String path) {
        long duration = ERROR_UNKNOWN;
        try {
            Movie decodedMovie = Movie.decodeFile(path);
            if (decodedMovie == null) {
                return ERROR_DECODING;
            }
            duration = decodedMovie.duration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duration;
    }

    public static final int SCALE_NONE = 0;    // Drawable dimensions determine how it's displayed.
    public static final int SCALE_CENTER_INSIDE = 1;  // Best Fit, Preserves Aspect ratio;
    public static final int SCALE_CENTER_CROP = 2;    // FULL Screen, Preserves Aspect ratio, CROPS;
    public static final int SCALE_FIT = 3;     // FULL Screen, Does NOT preserve Aspect ratio;
    public static final int SCALE_SQUARE = 4;  // LARGEST SQUARE, Preserves Aspect ratio, CROPS;
    public static final int SCALE_FIT_WIDTH = 5;   // FIT WIDTH, Preserves Aspect ratio;
    public static final int SCALE_FIT_HEIGHT = 6;  // FIT HEIGHT, Preserves Aspect ratio;
    public static final int SCALE_FIT_AUTO = 7;  // FIT WIDTH, or center crop based on image and screen resolution, Preserves Aspect ratio;


    @IntDef({
            SCALE_NONE,
            SCALE_CENTER_INSIDE,
            SCALE_CENTER_CROP,
            SCALE_FIT,
            SCALE_SQUARE,
            SCALE_FIT_WIDTH,
            SCALE_FIT_HEIGHT,
            SCALE_FIT_AUTO
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {
    }

    // Originally intended to be used for FFMpeg, so the values are directly compatible with FFMpeg
    // but can be used elsewhere as well.
    public static final int TRANSPOSE_NONE = -1;
    public static final int TRANSPOSE_CCLOCK_FLIP = 0;
    public static final int TRANSPOSE_CLOCK = 1;
    public static final int TRANSPOSE_CCLOCK = 2;
    public static final int TRANSPOSE_CLOCK_FLIP = 3;
    public static final int TRANSPOSE_180 = 4;

    public static final int CH_ORIENTATION_TRANSPOSE = 9;
    public static final int CH_ORIENTATION_TRANSVERSE = 10;

    private static int getVideoTranspose(String path) {
        if (!FileUtilities.isFileTypeVideo(path)) return ERROR_BAD_FILETYPE;
        int transpose = TRANSPOSE_NONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(path);
                String rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (rotationStr != null) {
                    int rotation = 0;
                    try {
                        rotation = Integer.valueOf(rotationStr);
                        if (rotation == 90) {
                            transpose = TRANSPOSE_CLOCK;
                        } else if ((rotation == -90) || (rotation == 270)) {
                            transpose = TRANSPOSE_CCLOCK;
                        } else if (rotation == 180) {
                            transpose = TRANSPOSE_180;
                        }
                    } catch (NumberFormatException e) {
                        transpose = TRANSPOSE_NONE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                CrashlyticsWrapper.logException(e);
                transpose = TRANSPOSE_NONE;
            } finally {
                retriever.release();
            }
        }
        return transpose;
    }

    public static boolean isImageFlipHorizontal(String imagePath) {
        int orientation = getImageOrientation(imagePath);
        return isFlipHorizontal(orientation);
    }

    public static boolean isFlipHorizontal(int orientation) {
        boolean flipHorizontal = false;
        if ((orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) || (orientation == ExifInterface.ORIENTATION_TRANSVERSE) || (orientation == AndroidUtilities.CH_ORIENTATION_TRANSVERSE)) {
            flipHorizontal = true;
        }
        return flipHorizontal;
    }

    public static boolean isImageFlipVertical(String imagePath) {
        int orientation = getImageOrientation(imagePath);
        return isFlipVertical(orientation);
    }

    public static boolean isFlipVertical(int orientation) {
        boolean flipVertical = false;
        if ((orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) || (orientation == ExifInterface.ORIENTATION_TRANSPOSE) || (orientation == AndroidUtilities.CH_ORIENTATION_TRANSPOSE)) {
            flipVertical = true;
        }
        return flipVertical;
    }

    public static int getImageRotation(String imagePath) {
        int orientation = getImageOrientation(imagePath);
        return getRotationFromOrientation(orientation);
    }

    /**
     * @param orientation the exif orientation
     * @return angle to be rotated (in degrees)
     */
    public static int getRotationFromOrientation(int orientation) {
        int rotation = 0;
        // Rotation from Exif Tag (Embedded inside the image)
        switch (orientation) {
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case AndroidUtilities.CH_ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotation = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case AndroidUtilities.CH_ORIENTATION_TRANSVERSE:
                rotation = 90;
                break;
            default:
                rotation = 0;
        }
        return rotation;
    }

    /**
     * @param rotation angle in degrees
     * @return the exif orientation
     */
    public static int getOrientationFromRotation(int rotation) {
        int orientation = 0;
        // Rotation from Exif Tag (Embedded inside the image)
        switch (rotation) {
            case 0:
            case 360:
            default:
                orientation = ExifInterface.ORIENTATION_NORMAL;
                break;
            case 90:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case 180:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case 270:
            case -90:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
        }
        return orientation;
    }

    public static int getImageTranspose(String imagePath) {
        int orientation = getImageOrientation(imagePath);
        int transpose = 0;
        // Rotation from Exif Tag (Embedded inside the image)
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                transpose = TRANSPOSE_CCLOCK;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                transpose = TRANSPOSE_180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                transpose = TRANSPOSE_CLOCK;
                break;
            default:
                transpose = TRANSPOSE_NONE;
        }
        return transpose;
    }

    public static int getImageOrientation(String path) {
        int orientation = ERROR_NONE;
        if (TextUtils.isEmpty(path)) {
            return ERROR_BAD_FILENAME;
        }
        if (!new File(path).exists()) {
            return ERROR_FILE_NOTEXISTS;
        }
        if (!new File(path).canRead()) {
            return ERROR_ACCESS;
        }
        ExifInterface ei = null;
        if (FileUtilities.getFileMediaType(path) != FileUtilities.FILE_TYPE_JPG) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
        try {
            ei = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            CrashlyticsWrapper.logException(e);
        }
        if (ei == null) {
            Map<String, String> map = new HashMap<>();
            map.put("path", path);
            EventTrackUtil.logDebug("getImageOrientation", "errorReadingExif", "AndUtils", map, 4);
            return ERROR_READING;
        }
        orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return orientation;
    }

    public static Matrix getImageMatrixFromExif(int exifOrientation) {
        Matrix matrix = new Matrix();
        matrix.reset();
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setScale(1, -1);
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setScale(-1, 1);
                matrix.postRotate(90);
                break;
            case AndroidUtilities.CH_ORIENTATION_TRANSPOSE:
                matrix.setScale(1, -1);
                matrix.postRotate(90);
                break;
            case AndroidUtilities.CH_ORIENTATION_TRANSVERSE:
                matrix.setScale(-1, 1);
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        return matrix;
    }

    /**
     * Calculates the tile length of a grid element considering square grid element type.
     *
     * @param numberOfColumns    Number of columns in the grid containing the tiles
     * @param sideMarginInPixels The margin of the grid from sides(left,right) in pixels
     * @param gridMarginInPixels The margin in between the grid in pixels
     * @return The tile length of the square in pixels
     */

    public static int getSquareTileLength(int numberOfColumns, int sideMarginInPixels, int gridMarginInPixels) {
        int dm = AndroidUtilities.displayMetrics.widthPixels;
        int size = 0;
        if (numberOfColumns > 0) {
            size = (int) ((double) (dm - 2 * sideMarginInPixels - 2 * numberOfColumns * gridMarginInPixels) / (double) 3);
        }
        return size;
    }

    /**
     * Calculates the tile dimensions of a grid element.
     * <p>
     * Note: For calculation of defaultSquareTileLength and totalFixedCardHeight a default values of columns,rows
     * are taken so that all the cards get drawn from the perspective of the base card.
     *
     * @param baseRows    Number of rows for baseCase. 0 for no base case.
     * @param baseColumns Number of columns for baseCase. 0 for no base case.
     * @param rows        Number of rows in the grid containing the tiles
     * @param columns     Number of columns in the grid containing the tiles
     * @param sideMargin  The margin of the grid from sides(left,right) in pixels
     * @param gridMargin  The margin in between the grid in pixels
     * @return The tile dimensions of the rectangular grid element in pixels
     */
    public static Pair<Integer, Integer> getGridElementDimensions(int rows, int columns, int sideMargin, int gridMargin, int baseRows, int baseColumns) {
        int defaultSqaureTileLength;
        int totalFixedCardHeight;

        if (baseColumns > 0 && baseRows > 0) {               //Consider Base Case
            defaultSqaureTileLength = getSquareTileLength(baseColumns, sideMargin, gridMargin);
            totalFixedCardHeight = baseRows * defaultSqaureTileLength + 2 * baseRows * gridMargin;
        } else {
            defaultSqaureTileLength = getSquareTileLength(columns, sideMargin, gridMargin);
            totalFixedCardHeight = rows * defaultSqaureTileLength + 2 * rows * gridMargin;
        }
        int tileLength = (totalFixedCardHeight - 2 * rows * gridMargin) / rows;
        int tileWidth = (AndroidUtilities.displayMetrics.widthPixels - 2 * columns * gridMargin - 2 * sideMargin) / columns;
        if (tileLength > 0 && tileWidth > 0) {
            return new Pair<>(tileLength, tileWidth);
        }
        return null;
    }

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();

    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String formatCountToString(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return formatCountToString(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + formatCountToString(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        float truncated = value / (float) divideBy;
        return String.format(Locale.getDefault(), "%.1f%s", truncated, suffix);
    }


    public static float getPercentageOfLong(long currentTime, long maxTime) {
        if (currentTime > 0 && maxTime > 0)
            return ((0.5f + ((float) currentTime / (float) maxTime) * 100));
        else
            return 0;
    }

    public static Intent getInstalledAppDetailsIntent() {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + ContextHelper.applicationContext.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return i;
    }

    public static int widthInPixel() {
        return displayMetrics.widthPixels;
    }

    public static int heightInPixel() {
        return displayMetrics.heightPixels;
    }

    public static int getColor(int color) {
        return ContextCompat.getColor(ContextHelper.getContext(), color);
    }

    public static String getEmojiByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }

        return size;
    }

    public static void setNewMarginsForView(View v, int left, int top, int right, int bottom) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            v.requestLayout();
        }
    }

    /**
     * to validate index in the collection
     *
     * @param position   int
     * @param collection can be list, map, set etc.
     * @return true if valid
     */
    public static boolean isValidIndex(int position, Collection collection) {
        return collection != null && position >= 0 && position < collection.size();
    }

    /**
     * to validate index in the array
     *
     * @param position int
     * @param array    can be list, map, set etc.
     * @return true if valid
     */
    public static <T> boolean isValidIndex(int position, T[] array) {
        return array != null && position >= 0 && position < array.length;
    }

    /**
     * show dialog from a view
     *
     * @param dialog to be shown
     * @param view   to which dialog is to be attached
     */
    public static void showDialog(Dialog dialog, View view) {
        if (dialog == null || view == null) return;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && view.isAttachedToWindow()) {
            dialog.show();
        } else {
            try {
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * show dialog from a fragment
     *
     * @param dialog   to be shown
     * @param fragment to which dialog is to be attached
     */
    public static void showDialog(Dialog dialog, Fragment fragment) {
        if (dialog == null || fragment == null) return;
        if (fragment.isAdded() && fragment.getActivity() != null) {
            dialog.show();
        }
    }

    public static GradientDrawable getInstagramGradientDrawable(int cornerRadius) {
        GradientDrawable instagramDrawable = AndroidUtilities.getGradientDrawable(
                0, cornerRadius, 0, 0);
        instagramDrawable.setColors(new int[]{
                ContextCompat.getColor(ContextHelper.getContext(), R.color.instagram_9)
                , ContextCompat.getColor(ContextHelper.getContext(), R.color.instagram_6)
                , ContextCompat.getColor(ContextHelper.getContext(), R.color.instagram_4)});
        instagramDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        instagramDrawable.setOrientation(GradientDrawable.Orientation.BL_TR);
        return instagramDrawable;
    }

    public static float getTextWidth(Paint textPaint, String text) {
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.width();
    }

    public static float getTextHeight(Paint textPaint, String text) {
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.height();
    }

    public static void cleanMemory() {
        try {
            CrashlyticsWrapper.log("Main Activity:: on Low memory");
            ContextHelper.killFragmentAtBottom();
            Runtime.getRuntime().gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getString(@StringRes int id) {
        return ContextHelper.getContext().getString(id);
    }

    public static void addExtraClickArea(final View delegate, final View parent, final int leftExtra,
                                         final int rightExtra, final int bottomExtra,
                                         final int topExtra) {
        if (delegate == null || parent == null)
            return;
        parent.post(new Runnable() {
            // Post in the parent's message queue to make sure the parent
            // lays out its children before we call getHitRect()
            public void run() {
                final Rect r = new Rect();
                delegate.getHitRect(r);
                r.top -= topExtra;
                r.bottom += bottomExtra;
                r.left -= leftExtra;
                r.right += rightExtra;
                parent.setTouchDelegate(new TouchDelegate(r, delegate));
            }
        });
    }

    @NonNull
    public static GradientDrawable getDefaultShaderDrawable() {
        GradientDrawable nextDrawable = AndroidUtilities.getGradientDrawable(
                0, AndroidUtilities.dp(30), 0, 0);
        nextDrawable.setColors(new int[]{
                ContextCompat.getColor(ContextHelper.getContext(), com.roposo.core.R.color.color_gr_1)
                , ContextCompat.getColor(ContextHelper.getContext(), com.roposo.core.R.color.color_gr_4)});
        nextDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        nextDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        return nextDrawable;
    }


}
