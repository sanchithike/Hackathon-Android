package com.roposo.core.util;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.roposo.core.customInjections.CrashlyticsWrapper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Created by Anil Sharma on 27/04/17.
 */

public class FileUtilities {

    private static final String TAG = FileUtilities.class.getName();

    public static final int FILE_TYPE_UNKNOWN = -1;
    public static final int FILE_TYPE_JPG = 1;
    public static final int FILE_TYPE_PNG = 2;
    public static final int FILE_TYPE_WEBP = 4;
    public static final int FILE_TYPE_TIFF = 5;

    public static final int FILE_TYPE_MP3 = 10;
    public static final int FILE_TYPE_WAVE = 11;
    public static final int FILE_TYPE_OGG = 12;
    public static final int FILE_TYPE_RIFF = 13;

    public static final int FILE_TYPE_MP4 = 20;
    public static final int FILE_TYPE_GIF = 21;
    public static final int FILE_TYPE_FLV = 22;
    public static final int FILE_TYPE_WEBM = 23;
    public static final int FILE_TYPE_3GP = 24;
    public static final int FILE_TYPE_AVI = 25;
    public static final int FILE_TYPE_JSON = 26;
    public static final int FILE_TYPE_BYTE_DATA = 27;
    public static final String STITCH_VIDEO_PREFIX = "RoposoVideo";
    public static final String STITCH_IMAGE_PREFIX = "RoposoImage";

    public static long lastModifiedTime(String vkLogPath) {
        try {
            return new File(vkLogPath).lastModified();
        } catch (Exception e) {
        }
        return -1;
    }

    public static void syncFile(String path) {
        try {
            FileOutputStream os = new FileOutputStream(path, true);
            os.flush();
            os.getFD().sync();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean createNewFile(String path) {
        boolean result = false;
        try {
            result = new File(path).createNewFile();
            syncFile(path);
            Log.d(TAG, "Created new file:: " + path + (new File(path).exists() ? " exists" : " does not exist"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @IntDef({
            FILE_TYPE_UNKNOWN,

            FILE_TYPE_JPG,
            FILE_TYPE_PNG,
            FILE_TYPE_WEBP,
            FILE_TYPE_TIFF,

            FILE_TYPE_MP3,
            FILE_TYPE_WAVE,
            FILE_TYPE_OGG,
            FILE_TYPE_RIFF,

            FILE_TYPE_MP4,
            FILE_TYPE_AVI,
            FILE_TYPE_FLV,
            FILE_TYPE_3GP,
            FILE_TYPE_WEBM,

            FILE_TYPE_GIF,
            FILE_TYPE_JSON,
            FILE_TYPE_BYTE_DATA
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaFileTypes {
    }


    public static final int MEDIA_TYPE_UNKNOWN = -1;
    public static final int MEDIA_TYPE_IMAGE = 0;
    public static final int MEDIA_TYPE_AUDIO = 1;
    public static final int MEDIA_TYPE_GIF = 2;
    public static final int MEDIA_TYPE_VIDEO = 3;
    public static final int MEDIA_TYPE_TEXT = 4;
    public static final int MEDIA_TYPE_BYTE_DATA = 5;

    @IntDef({
            MEDIA_TYPE_UNKNOWN,
            MEDIA_TYPE_IMAGE,
            MEDIA_TYPE_AUDIO,
            MEDIA_TYPE_GIF,
            MEDIA_TYPE_VIDEO,
            MEDIA_TYPE_TEXT,
            MEDIA_TYPE_BYTE_DATA
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaTypes {
    }


    public static final int MEDIA_DIR_CACHE = 0;
    public static final int MEDIA_DIR_AUDIO = 1;
    public static final int MEDIA_DIR_IMAGE = 2;
    public static final int MEDIA_DIR_VIDEO = 3;
    public static final int MEDIA_DIR_CAMERA = 4;
    public static final int MEDIA_DIR_DATA = 5;
    public static final int MEDIA_DIR_STICKERS = 6;
    public static final int MEDIA_DIR_CREATION = 7;



    @IntDef({
            MEDIA_DIR_CACHE,
            MEDIA_DIR_AUDIO,
            MEDIA_DIR_IMAGE,
            MEDIA_DIR_VIDEO,
            MEDIA_DIR_CAMERA,
            MEDIA_DIR_DATA,
            MEDIA_DIR_STICKERS,
            MEDIA_DIR_CREATION
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaDirectoryTypes {
    }

    private static final Random sRandom;
    private static SimpleArrayMap<Integer, File> mediaDirs = null;

    static {
        sRandom = new Random();
    }

    @Nullable
    public static File generateMediaFile(@MediaFileTypes int fileType, @MediaDirectoryTypes int mediaDirectory) {
        return generateMediaFile(null, fileType, mediaDirectory);
    }


    public static File generateMediaFileForThisStory(String storyId, @MediaFileTypes int fileType,
                                                     @MediaDirectoryTypes int mediaDirectory) {
        try {
            String name;
            if (null == mediaDirs) {
                mediaDirs = new SimpleArrayMap<>();
                createMediaPaths(mediaDirs);
            }
            File mediaDir = mediaDirs.get(mediaDirectory);
            if (null == mediaDir) {
                return null;
            }

            if (storyId == null){
                storyId = "COMMON_DATA";
            }

            name = generateFileName(fileType);

            File storyFolder = new File(mediaDir.getAbsolutePath() + File.separator + storyId+"/");
            if (!storyFolder.exists()){
                storyFolder.mkdir();
            }
            return new File(storyFolder,
                    name + getFileExtension(fileType));

        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }
        return null;
    }

    public static File generateMediaFile(String name, @MediaFileTypes int fileType, @MediaDirectoryTypes int mediaDirectory) {
        try {
            if (null == mediaDirs) {
                mediaDirs = new SimpleArrayMap<>();
                createMediaPaths(mediaDirs);
            }
            File mediaDir = mediaDirs.get(mediaDirectory);
            if (null == mediaDir) {
                return null;
            }

            if (null == name) {
                name = generateFileName(fileType);
            }

            return new File(mediaDir, name + getFileExtension(fileType));

        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }
        return null;
    }

    @NonNull
    private static String generateFileName(@MediaFileTypes int fileType) {
        StringBuilder sb = new StringBuilder();
        int mediaType = getMediaType(fileType);
        String contentType;
        switch (mediaType) {
            case MEDIA_TYPE_AUDIO:
                contentType = "AUD_";
                break;
            case MEDIA_TYPE_GIF:
                contentType = "GIF_";
                break;
            case MEDIA_TYPE_IMAGE:
                contentType = "IMG_";
                break;
            case MEDIA_TYPE_VIDEO:
                contentType = "VID_";
                break;

            case MEDIA_TYPE_UNKNOWN:
            case MEDIA_TYPE_TEXT:
            case MEDIA_TYPE_BYTE_DATA:
            default:
                contentType = "FILE_";
                break;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String suffix = String.valueOf(sRandom.nextInt(1000003) + 1003);
        sb.append(contentType).append(timeStamp).append("_").append(suffix);

        return sb.toString();
    }

    @Nullable
    private static String getFileExtension(@MediaFileTypes int fileType) {
        switch (fileType) {
            case FILE_TYPE_JPG:
                return ".jpg";
            case FILE_TYPE_PNG:
                return ".png";
            case FILE_TYPE_WEBP:
                return ".webp";
            case FILE_TYPE_MP4:
                return ".mp4";
            case FILE_TYPE_WAVE:
                return ".wav";
            case FILE_TYPE_GIF:
                return ".gif";
            case FILE_TYPE_MP3:
                return ".mp3";
            case FILE_TYPE_TIFF:
                return ".tiff";
            case FILE_TYPE_UNKNOWN:
                return ".jgp";  // TODO :: unknown me .jpg , re-consider this
            case FILE_TYPE_JSON:
                return ".json";
            case FILE_TYPE_BYTE_DATA:
                return ".rps";
        }

        return null;
    }

    public static @MediaTypes
    int getMediaType(@MediaFileTypes int fileType) {
        switch (fileType) {
            case FILE_TYPE_JPG:
            case FILE_TYPE_PNG:
            case FILE_TYPE_WEBP:
            case FILE_TYPE_TIFF:
                return MEDIA_TYPE_IMAGE;

            case FILE_TYPE_MP3:
            case FILE_TYPE_WAVE:
            case FILE_TYPE_OGG:
            case FILE_TYPE_RIFF:
                return MEDIA_TYPE_AUDIO;

            case FILE_TYPE_MP4:
            case FILE_TYPE_AVI:
            case FILE_TYPE_FLV:
            case FILE_TYPE_3GP:
            case FILE_TYPE_WEBM:
                return MEDIA_TYPE_VIDEO;

            case FILE_TYPE_GIF:
                return MEDIA_TYPE_GIF;

            case FILE_TYPE_JSON:
                return MEDIA_TYPE_TEXT;

            case FILE_TYPE_BYTE_DATA:
                return MEDIA_TYPE_BYTE_DATA;

            case FILE_TYPE_UNKNOWN:
            default:
                return MEDIA_TYPE_UNKNOWN;
        }

    }

    // @author: sahilbajaj
    // Don't use this function, unless you know what it does
    public static @Constants.MediaTypes
    int getMediaType(@Constants.MediaTypes int srcMediaType, int fileMediaType) {
        int finalMediaType = srcMediaType;
        if ((isFileTypeVideo(fileMediaType))) {
            if (srcMediaType == Constants.LOCAL_PHOTO) {
                finalMediaType = Constants.LOCAL_PHOTO_VIDEO;
            } else if (srcMediaType == Constants.CAPTURED_PHOTO) {
                finalMediaType = Constants.CAPTURED_PHOTO_VIDEO;
            } else if (srcMediaType == Constants.LOCAL_VIDEO) {
                finalMediaType = Constants.LOCAL_VIDEO_VIDEO;
            } else if (srcMediaType == Constants.CAPTURED_VIDEO) {
                finalMediaType = Constants.CAPTURED_VIDEO_VIDEO;

            }
        }
        return finalMediaType;
    }

    // TODO sahil duplicate method, should not be required
    public static File generateMediaPathByExt(String fileExt, @MediaDirectoryTypes int mediaDirectory) {
        try {
            if (null == mediaDirs) {
                mediaDirs = new SimpleArrayMap<>();
                createMediaPaths(mediaDirs);
            }
            File mediaDir = mediaDirs.get(mediaDirectory);
            if (null == mediaDir) {
                return null;
            }

            String suffix = String.valueOf(sRandom.nextInt(1000003) + 1003);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(mediaDir, "IMG_" + timeStamp + "_" + suffix + "_" + "." + fileExt);

        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }
        return null;
    }


    public static String copyFile(Context context, Uri uri, @MediaFileTypes int fileType, @MediaDirectoryTypes int directoryType) {
        if (null == uri) {
            return null;
        }

        File file = generateMediaFile(fileType, directoryType);
        if (null == file) {
            return null;
        }

        String filePath = null;
        InputStream inputStream = null;
        BufferedOutputStream outStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);

            filePath = file.getAbsolutePath();
            outStream = new BufferedOutputStream(new FileOutputStream
                    (filePath));

            byte[] buf = new byte[2048];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outStream.write(buf, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
            filePath = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filePath;

    }

    private static boolean shouldCopyFileFromUri(Uri uri) {
        String auth = uri.getAuthority();
        return auth != null && (auth.equals("com.whatsapp.provider.media") ||
                auth.equals("com.google.android.apps.photos.contentprovider"));
    }

    private static File getCacheDir() {
        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.getMessage());
            CrashlyticsWrapper.logException(e);
        }
        if (state == null || state.startsWith(Environment.MEDIA_MOUNTED)) {
            try {
                File file = ContextHelper.applicationContext.getExternalCacheDir();
                if (file != null) {
                    return file;
                }
            } catch (Exception e) {
                MyLogger.e("roposo_msg", e.getMessage());
                CrashlyticsWrapper.logException(e);
            }
        }
        try {
            File file = ContextHelper.applicationContext.getCacheDir();
            if (file != null) {
                return file;
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.getMessage());
            CrashlyticsWrapper.logException(e);
        }
        return new File("");
    }

    @Nullable
    public static File getDirectoryFile(@MediaDirectoryTypes int mediaDirectoryType) {
        if (null == mediaDirs) {
            mediaDirs = new SimpleArrayMap<>();
            createMediaPaths(mediaDirs);
        }

        return mediaDirs.get(mediaDirectoryType);
    }



    private static void createMediaPaths(@NonNull SimpleArrayMap<Integer, File> mediaDirsOut) {

        // 1. Add External cache directory
        File cachePath = getCacheDir();
        if (!cachePath.isDirectory()) {
            try {
                cachePath.mkdirs();
            } catch (Exception e) {
                MyLogger.e("roposo_msg", e.getMessage());
                CrashlyticsWrapper.logException(e);
            }
        }

        // Disable media scanning for cache directory
        try {
            new File(cachePath, ".nomedia").createNewFile();
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.getMessage());
            CrashlyticsWrapper.logException(e);
        }

        mediaDirsOut.put(MEDIA_DIR_CACHE, cachePath);

        // k2. Add Roposo directory

        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File roposoPath = new File(Environment.getExternalStorageDirectory(), "Roposo");
                roposoPath.mkdirs();

                if (roposoPath.isDirectory()) {

                    try {
                        File stickersPath = new File(roposoPath, "Stickers");
                        File stickerNoMedia = new File(stickersPath, ".nomedia");
                        if (stickersPath.exists()) {
                            if (!stickerNoMedia.exists()) {
                                deleteDir(stickersPath);
                            }
                        }
                        stickersPath.mkdir();
                        if (stickersPath.isDirectory()) {
                            // Disable media scanning for cache directory
                            try {
                                stickerNoMedia.createNewFile();
                            } catch (Exception e) {
                                MyLogger.e("roposo_msg", e.getMessage());
                                CrashlyticsWrapper.logException(e);
                            }
                            mediaDirs.put(MEDIA_DIR_STICKERS, stickersPath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }


                    try {
                        File effectsPath = new File(roposoPath, "Effects");
                        File effectsNomedia = new File(effectsPath, ".nomedia");
                        if (effectsPath.exists()) {
                            if (!effectsNomedia.exists()) {
                                deleteDir(effectsPath);
                            }
                        }
                        effectsPath.mkdir();
                        if (effectsPath.isDirectory()) {
                            // Disable media scanning for cache directory
                            try {
                                effectsNomedia.createNewFile();
                            } catch (Exception e) {
                                MyLogger.e("roposo_msg", e.getMessage());
                                CrashlyticsWrapper.logException(e);
                            }
                            mediaDirs.put(MEDIA_DIR_DATA, effectsPath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }

                    try {
                        File imagePath = new File(roposoPath, "Roposo Photos");
                        imagePath.mkdir();
                        if (imagePath.isDirectory()) {
                            mediaDirs.put(MEDIA_DIR_IMAGE, imagePath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }

                    try {
                        File encryptedDataFolder = new File(roposoPath, "Roposo Data");
                        if (!encryptedDataFolder.exists() || !encryptedDataFolder.isDirectory()) {
                            encryptedDataFolder.mkdir();
                        }
                        if (encryptedDataFolder.isDirectory()) {
                            mediaDirsOut.put(MEDIA_DIR_DATA, encryptedDataFolder);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }

                    try {
                        File videoPath = new File(roposoPath, "Roposo Videos");
                        videoPath.mkdir();
                        if (videoPath.isDirectory()) {
                            mediaDirsOut.put(MEDIA_DIR_VIDEO, videoPath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }

                    try {
                        File audioPath = new File(roposoPath, "Roposo Audio");
                        audioPath.mkdir();
                        if (audioPath.isDirectory()) {
                            new File(audioPath, ".nomedia").createNewFile();
                            mediaDirsOut.put(MEDIA_DIR_AUDIO, audioPath);
                            MyLogger.e("roposo_msg", "audio path = " + audioPath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }

                    try {
                        File creationPath = new File(roposoPath, "Roposo Story");
                        creationPath.mkdir();
                        if (creationPath.isDirectory()) {
                            new File(creationPath, ".nomedia").createNewFile();
                            mediaDirsOut.put(MEDIA_DIR_CREATION, creationPath);
                            MyLogger.e("roposo_msg", "audio path = " + creationPath);
                        }
                    } catch (Exception e) {
                        MyLogger.e("roposo_msg", e.getMessage());
                        CrashlyticsWrapper.logException(e);
                    }
                }

                try {
                    String cameraFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

                    File imagePath = new File(cameraFolder, "Roposo");
                    imagePath.mkdir();
                    if (imagePath.isDirectory()) {
                        // TODO anilshar :: ideally we should check if a file can be saved into created directory
                        mediaDirs.put(MEDIA_DIR_CAMERA, imagePath);
                    }
                } catch (Exception e) {
                    MyLogger.e("roposo_msg", e.getMessage());
                    CrashlyticsWrapper.logException(e);
                }
            } else {
                EventTrackUtil.logDebug("Notmounted", "createMediaPath", AndroidUtilities.class.getName(), null, 4);
                // register a broadcast receiver to find when media gets mounted/unmounted etc.
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context arg0, Intent intent) {
                        ContextHelper.applicationContext.unregisterReceiver(this);
                        MyLogger.e("roposo_msg", "file system changed");
                        Runnable r = new Runnable() {
                            public void run() {
                                createMediaPaths(mediaDirs);
                            }
                        };
                        if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                            AndroidUtilities.runOnUIThread(r, 1000);
                        } else {
                            r.run();
                        }
                    }
                };

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
                filter.addAction(Intent.ACTION_MEDIA_CHECKING);
                filter.addAction(Intent.ACTION_MEDIA_EJECT);
                filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
                filter.addAction(Intent.ACTION_MEDIA_NOFS);
                filter.addAction(Intent.ACTION_MEDIA_REMOVED);
                filter.addAction(Intent.ACTION_MEDIA_SHARED);
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
                filter.addDataScheme("file");
                ContextHelper.applicationContext.registerReceiver(receiver, filter);
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.getMessage());
            CrashlyticsWrapper.logException(e);
        }
    }

    public static long getFileSize(String path) {
        if (TextUtils.isEmpty(path)) {
            return AndroidUtilities.ERROR_BAD_FILENAME;
        }
        if (!FileUtilities.fileExists(path)) {
            return AndroidUtilities.ERROR_FILE_NOTEXISTS;
        }
        return new File(path).length();
    }

    private static boolean fileExists(String path) {
        return !TextUtils.isEmpty(path) && new File(path).exists();
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return String.format(Locale.getDefault(), "%d B", size);
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0f);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / 1024.0f / 1024.0f);
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", size / 1024.0f / 1024.0f / 1024.0f);
        }
    }

    public static boolean copyFile(InputStream sourceFile, File destFile) throws IOException {
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = sourceFile.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
        return true;
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        boolean result = (destFile != null) && (destFile.exists() || destFile.createNewFile());
        result = result && ((sourceFile != null) && (sourceFile.exists() || sourceFile.createNewFile()));
        if (!result) {
            HashMap<String, String> map = new HashMap<>(6);
            map.put("targetFile", String.valueOf(destFile));
            if (destFile != null) {
                map.put("isWritable", String.valueOf(destFile.canWrite()));
            }
            map.put("sourceFile", String.valueOf(sourceFile));
            if (sourceFile != null) {
                map.put("isWritable", String.valueOf(sourceFile.canWrite()));
            }
            EventTrackUtil.logDebug("copyFile", "copyFile", AndroidUtilities.class.getName(), map, 4);
            return false;
        }

        FileInputStream source = null;
        FileOutputStream destination = null;
        try {
            source = new FileInputStream(sourceFile);
            destination = new FileOutputStream(destFile);
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
            return false;
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
        return true;
    }

    public static void deleteFileFromMediaStore(final ContentResolver contentResolver, final File file) {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[]{canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

    public static boolean deleteFile(String path) {
        if (path == null || path.isEmpty()) {
            Log.w(TAG, "deleteFile: Invalid path: " + path);
            return false;
        }
        File file = new File(path);
        if (file.isDirectory()) {
            return deleteRecursive(file);
        } else {
            return file.delete();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir == null) return false;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children == null || children.length == 0) return false;
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else {
            return dir.isFile() && dir.delete();
        }
    }

    public static boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                deleteRecursive(childFile);
            }
        }

        return file.delete();
    }

    public static boolean deleteFileAndNotify(String path) {
        if (path == null || path.isEmpty()) {
            Log.w(TAG, "deleteFile: Invalid path: " + path);
            return false;
        }
        boolean result = new File(path).delete();
        deleteFileFromMediaStore(ContextHelper.applicationContext.getContentResolver(), new File(path));
        return result;
    }

    public static boolean isDirectoryEmpty(String path) {
        File dir = new File(path);
        return dir.exists() && dir.listFiles() != null && dir.listFiles().length == 0;
    }

    public static String toHex(String string) {
        char[] hexChars = new char[string.length() * 2];
        for (int j = 0; j < string.length(); j++) {
            int v = string.charAt(j);
            hexChars[j * 2] = AndroidUtilities.hexArray[v >>> 4];
            hexChars[j * 2 + 1] = AndroidUtilities.hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes) {
        //http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = AndroidUtilities.hexArray[v >>> 4];
            hexChars[j * 2 + 1] = AndroidUtilities.hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static boolean isFileNameImage(String path) {
        return isFileTypeImage(getFileNameType(path));
    }

    public static boolean isFileNameVideo(String path) {
        return isFileTypeVideo(getFileNameType(path));
    }

    public static boolean isFileNameAudio(String path) {
        return isFileTypeAudio(getFileNameType(path));
    }

    public static boolean isFileTypeImage(String path) {
        return isFileTypeImage(getFileMediaType(path));
    }

    public static boolean isFileTypeImage(int fileType) {
        return getMediaType(fileType) == MEDIA_TYPE_IMAGE;
    }

    public static boolean isFileTypeVideo(int fileType) {
        return getMediaType(fileType) == MEDIA_TYPE_VIDEO;
    }

    public static boolean isFileTypeAudio(int fileType) {
        return getMediaType(fileType) == MEDIA_TYPE_AUDIO;
    }

    public static boolean isFileTypeVideo(String path) {
        return (getFileMediaType(path) == FILE_TYPE_MP4);
    }

    public static boolean isFileTypeAudio(String path) {
        int fileMediaType = getFileMediaType(path);
        return (fileMediaType == FILE_TYPE_MP3) || (fileMediaType == FILE_TYPE_WAVE);
    }

    public static boolean isFileTypeGif(String path) {
        return (getFileMediaType(path) == FILE_TYPE_GIF);
    }

    /**
     * Determines the Media type of the file
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static int getFileMediaType(URL url) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
        connection.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);
        connection.connect();
        InputStream uInputStream = connection.getInputStream();
        return getFileMediaType(uInputStream);
    }

    /**
     * Determines the Media type of the file
     *
     * @param path
     * @return
     */
    @MediaFileTypes
    public static int getFileMediaType(String path) {
        if (TextUtils.isEmpty(path)) {
            return FILE_TYPE_UNKNOWN;
        }
        FileInputStream fInStream = null;
        try {
            File file = new File(path);
            if (file.exists()) {
                fInStream = new FileInputStream(file);
                return getFileMediaType(fInStream);
            }
        } catch (IOException e) {
            CrashlyticsWrapper.logException(e);
        }
        return FILE_TYPE_UNKNOWN;
    }

    // The first 4 magic bytes
    private static final String VIDEO_MOV_GENERIC_MAGIC_CODE = "0000";   // MP4/3GP
    private static final String VIDEO_MP4_MAGIC_CODE = "00000014";   // MP4/3GP
    private static final String VIDEO_MPEG4_MAGIC_CODE = "00000018";   // MPEG4
    private static final String VIDEO_MPEG42_MAGIC_CODE = "0000001C";   // MP4 - MPEG4
    private static final String VIDEO_M4AV_MAGIC_CODE = "00000020";   // M4A/M4V - MPEG4
    private static final String VIDEO_RIFF_MAGIC_CODE = "52494646";   // RIFF
    private static final String VIDEO_MPEG_MAGIC_CODE = "000001B"; // 7 nibbles (deliberately)

    private static final String MP3_1_MAGIC_CODE = "49443301";
    private static final String MP3_2_MAGIC_CODE = "49443302";
    private static final String MP3_3_MAGIC_CODE = "49443303";
    private static final String MP3_4_MAGIC_CODE = "49443304";
    private static final String OGG_MAGIC_CODE = "4F676753"; // OggS
    private static final String VIDEO_FLV_MAGIC_CODE = "464C5601"; // FLV

    private static final String VIDEO_WEBM_MAGIC_CODE = "1A45DFA3";

    private static final String IMAGE_JPG_MAGIC_NOEXIF_CODE = "FFD8FFE0";
    private static final String IMAGE_JPG_MAGIC_EXIF_CODE = "FFD8FFE1";
    private static final String IMAGE_JPG_MAGIC_CIFF_CODE = "FFD8FFE2";
    private static final String IMAGE_JPG_MAGIC_SPIFF_CODE = "FFD8FFE8";
    private static final String IMAGE_TIFF_MAGIC_CODE = "492049";
    private static final String IMAGE_PNG_MAGIC_CODE = "89504E47";
    private static final String IMAGE_JPG_MAGIC_CODE = "FFD8";
    private static final String IMAGE_JPG_2000_MAGIC_CODE = "0000000C";
    private static final String IMAGE_GIF_MAGIC_CODE = "47494638"; // GIF
    private static final String IMAGE_PSD_MAGIC_CODE = "38425053"; // 8BPS (PSD)
    private static final String IMAGE_CANON_RAW_MAGIC_CODE = "49492A00"; // IIS* //May be TIFF also, little do we care!

    // After the first 4 magic bytes
    private static final String VIDEO__3GP5_CODE = toHex("ftyp3gp5");   // MP4 MPEG-4 video files
    private static final String VIDEO_MP4_QT_CODE = toHex("ftypqt  ");  // MOV QuickTime
    private static final String VIDEO_MP42_CODE = toHex("ftypmp42");   // M4V MPEG-4 video/QuickTime file
    private static final String VIDEO_MSNV_CODE = toHex("ftypMSNV");   // MP4 MPEG-4 video file
    private static final String VIDEO_3GP_CODE = toHex("ftyp3gp");     // 3GG, 3GP, 3G2 (3GGP files)
    private static final String VIDEO_M4V_CODE = toHex("ftypM4V");     // ISO Media, MPEG v4,or iTunes AVC-LC file.
    private static final String VIDEO_M4A_CODE = toHex("ftypM4A");     // M4A Apple Lossless Audio
    private static final String VIDEO_ISO_CODE = toHex("ftypisom");    // ISO Base Media File (MPEG-4)
    private static final String VIDEO_MOV_QT_CODE = toHex("ftypmoov");
    private static final String VID_AVI_CODE = toHex("AVI LIST");
    private static final String AUDIO_CDDA_CODE = toHex("CDDAfmt");
    private static final String AUDIO_QCP_CODE = toHex("QLCMfmt");
    private static final String AUDIO_RMI_CODE = toHex("RMIDdata");
    private static final String AUDIO_WAV_CODE = toHex("WAVEfmt");
    private static final String AUDIO_DAT_CODE = toHex("WMMP");
    private static final String AUDIO_MP3_0_CODE = "FFE" + toHex("ÿ.");
    private static final String AUDIO_MP3_1_CODE = "FFF" + toHex("ÿ.");
    private static final String RIFF_CODE = toHex("RIFF");
    private static final String WEBP_CODE = toHex("WEBP");

    /**
     * Reads the first byte of the file and determines the Media type
     * TODO Handle media/file types identifiable by reading a few more bytes.
     *
     * @param is
     * @return
     * @throws IOException
     */
    @MediaFileTypes
    public static int getFileMediaType(InputStream is) throws IOException {
        int fileType = FILE_TYPE_UNKNOWN;

        byte[] buf = new byte[16];
        is.read(buf);

        String word = bytesToHex(buf);
        String magic = word.substring(0, Math.min(word.length(), 8));
        String code = word.substring(magic.length());

        HashMap<String, String> map = new HashMap<>();
        EventTrackUtil.logDebug("getFileMediaType", "magic", "FileUtilities", map, 2);

        switch (magic) {
            case VIDEO_MPEG_MAGIC_CODE:
                fileType = FILE_TYPE_MP4;
                break;
            case VIDEO_MP4_MAGIC_CODE:
                fileType = FILE_TYPE_MP4;
                break;
            case VIDEO_MPEG4_MAGIC_CODE:
                fileType = FILE_TYPE_MP4;
                break;
            case VIDEO_MPEG42_MAGIC_CODE:
                fileType = FILE_TYPE_MP4;
                break;
            case VIDEO_M4AV_MAGIC_CODE:
                fileType = FILE_TYPE_MP4;
                break;
            case VIDEO_RIFF_MAGIC_CODE:
                fileType = FILE_TYPE_RIFF;
                if (code.contains(AUDIO_CDDA_CODE)) {

                } else if (code.contains(AUDIO_QCP_CODE)) {

                } else if (code.contains(AUDIO_RMI_CODE)) {

                } else if (code.contains(AUDIO_WAV_CODE)) {
                    fileType = FILE_TYPE_WAVE;
                } else if (code.contains(AUDIO_DAT_CODE)) {

                } else if (code.contains(AUDIO_MP3_0_CODE)) {
                    fileType = FILE_TYPE_MP3;
                } else if (code.contains(AUDIO_MP3_1_CODE)) {
                    fileType = FILE_TYPE_MP3;
                } else if (code.contains(WEBP_CODE)) {
                    fileType = FILE_TYPE_WEBP;
                }
                break;
            case MP3_1_MAGIC_CODE:
            case MP3_2_MAGIC_CODE:
            case MP3_3_MAGIC_CODE:
            case MP3_4_MAGIC_CODE:
                fileType = FILE_TYPE_MP3;
                break;
            case OGG_MAGIC_CODE:
                fileType = FILE_TYPE_OGG;
                break;
            case VIDEO_FLV_MAGIC_CODE:
                fileType = FILE_TYPE_FLV;
                break;
            case VIDEO_WEBM_MAGIC_CODE:
                fileType = FILE_TYPE_WEBM;
                break;
            case IMAGE_JPG_MAGIC_NOEXIF_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_JPG_MAGIC_EXIF_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_JPG_MAGIC_CIFF_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_JPG_MAGIC_SPIFF_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_TIFF_MAGIC_CODE:
                break;
            case IMAGE_PNG_MAGIC_CODE:
                fileType = FILE_TYPE_PNG;
                break;
            case IMAGE_JPG_MAGIC_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_JPG_2000_MAGIC_CODE:
                fileType = FILE_TYPE_JPG;
                break;
            case IMAGE_GIF_MAGIC_CODE:
                fileType = FILE_TYPE_GIF;
                break;
            case IMAGE_PSD_MAGIC_CODE:
                break;
            case IMAGE_CANON_RAW_MAGIC_CODE:
                fileType = FILE_TYPE_TIFF;
                break;
            default:
                if (magic.contains("00")) {
                    if (code.contains(VIDEO__3GP5_CODE)) {

                    } else if (code.contains(VIDEO_MP4_QT_CODE)) {
                        fileType = FILE_TYPE_MP4;
                    } else if (code.contains(VIDEO_MP42_CODE)) {
                        fileType = FILE_TYPE_MP4;
                    } else if (code.contains(VIDEO_MSNV_CODE)) {

                    } else if (code.contains(VIDEO_3GP_CODE)) {
                        fileType = FILE_TYPE_3GP;
                    } else if (code.contains(VIDEO_M4V_CODE)) {
                        fileType = FILE_TYPE_MP4;
                    } else if (code.contains(VIDEO_M4A_CODE)) {
                        fileType = FILE_TYPE_MP4;
                    } else if (code.contains(VIDEO_ISO_CODE)) {

                    } else if (code.contains(VIDEO_MOV_QT_CODE)) {

                    } else if (code.contains(VID_AVI_CODE)) {
                        fileType = FILE_TYPE_AVI;
                    }
                } else {
                    fileType = FILE_TYPE_UNKNOWN;
                }
        }
        return fileType;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isLocalFile(String file_path) {
        if (file_path == null)
            return false;
        File file = new File(file_path);
        return file.exists();
    }

    @Deprecated // instead use updateQueryParam for the same
    public static String appendUri(String url, String appendQuery) {
        if (TextUtils.isEmpty(appendQuery)) return url;
        try {
            Uri uri = Uri.parse(url);
            final Set queryParams = uri.getQueryParameterNames();
            if (queryParams.isEmpty()) {
                url += "?" + appendQuery;
            } else {
                if (!url.contains(appendQuery)) {
                    url += "&" + appendQuery;
                }
            }
        } catch (Exception e) {
            CrashlyticsWrapper.logException(e);
        }
        return url;
    }

    /**
     * @param path
     * @return Returns just the file name without the extension
     */
    public static String getFileBaseName(String path) {
        int indexOfDot = path.lastIndexOf('.');
        String fileName = getFileName(path);
        int indexOfSlash = fileName.lastIndexOf('/');
        return fileName.substring(indexOfSlash + 1);
    }

    public static String getDirName(String path) {
        int fileSlash = path.lastIndexOf(File.separatorChar);
        if (fileSlash >= 0) {
            String basePath = path.substring(0, fileSlash);
            int dirSlash = basePath.lastIndexOf(File.separatorChar);
            if (dirSlash >= 0) {
                return path.substring(dirSlash + 1, fileSlash);
            }
        }
        return null;
    }

    /**
     * @param path
     * @return Returns full file path excluding the extension
     */
    public static String getFileName(String path) {
        if (path == null) return null;
        int indexOfDot = path.lastIndexOf('.');
        return path.substring(0, indexOfDot);
    }

    /**
     * @param path
     * @return Returns the file extension (without the dot)
     */
    public static String getFileExt(String path) {
        if (path == null) return null;
        int indexOfDot = path.lastIndexOf('.');
        return path.substring(indexOfDot + 1);
    }

    public static int getFileNameType(String path) {
        int fileTypeImage = FILE_TYPE_UNKNOWN;
        if (path == null) return fileTypeImage;
        String fileExt = getFileExt(path).toLowerCase();
        switch (fileExt) {
            case "mp4":
            case "avi":
            case "mov":
                fileTypeImage = FILE_TYPE_MP4;
                break;
            case "mp3":
            case "wav":
            case "m4a":
                fileTypeImage = FILE_TYPE_MP3;
                break;
            case "png":
                fileTypeImage = FILE_TYPE_PNG;
                break;
            case "jpg":
            case "jpeg":
                fileTypeImage = FILE_TYPE_JPG;
                break;
            case "gif":
                fileTypeImage = FILE_TYPE_GIF;
                break;
        }

        return fileTypeImage;
    }

    public static boolean isFileNameGif(String path) {
        try {
            return getFileExt(path).equals("gif");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isFileNameMp4(String path) {
        try {
            return getFileExt(path).equals("mp4");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getPath(final Uri uri) {
        return getPath(uri, FILE_TYPE_JPG);
    }

    @SuppressLint("NewApi")
    public static String getPath(final Uri uri, int fileType) {
        try {
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat && DocumentsContract.isDocumentUri(ContextHelper.applicationContext, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(ContextHelper.applicationContext, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(ContextHelper.applicationContext, contentUri, selection, selectionArgs);

                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                String data = getDataColumn(ContextHelper.applicationContext, uri, null, null);
                if (null == data || !(new File(data).exists())) {
                    return copyFile(ContextHelper.applicationContext, uri, fileType, MEDIA_DIR_CACHE);
                } else {
                    return data;
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            MyLogger.e("roposo_msg", e.toString());
            CrashlyticsWrapper.logException(e);
        }
        return null;
    }

    // TODO fix this. Not behaving properly
    public static boolean hasAvailableSize(long extraSizeBytes) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            if (Build.VERSION.SDK_INT >= 18) {
                long freeBytes = stat.getAvailableBytes();
                return freeBytes > extraSizeBytes;
            } else {
                long blockSize = stat.getBlockSize();
                long freeBlocks = stat.getAvailableBlocks();
                return (1L * blockSize * freeBlocks) > extraSizeBytes;
            }
        }
        return false;
    }


    public static String updateQueryParam(String url, String key, String newValue) {
        String keyQuery = key + "=";
        String newParams = keyQuery + newValue;
        Uri uri = Uri.parse(url);
        String currentValue = uri.getQueryParameter(key);
        if (currentValue != null) {
            String currentParams = keyQuery + currentValue;
            url = url.replace(currentParams, newParams);
        } else {
            url = appendUri(url, newParams);
        }
        return url;
    }

    public static boolean isFileExists(String path){
        if (TextUtils.isEmpty(path)) return false;
        File file = new File(path);
        if (file.exists()){
            return true;
        }

        return false;
    }


}
