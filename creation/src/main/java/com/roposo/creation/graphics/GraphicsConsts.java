package com.roposo.creation.graphics;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bajaj on 13/07/16.
 */
public class GraphicsConsts {
    public static final int BITMAP_CACHE_SIZE = 45 * 1024 * 1024;

    public static final int RENDER_TARGET_DISPLAY = 0x01;
    public static final int RENDER_TARGET_VIDEO = 0x02;
    public static final int RENDER_TARGET_IMAGE = 0x04;

    @IntDef({
            RENDER_TARGET_DISPLAY,
            RENDER_TARGET_IMAGE,
            RENDER_TARGET_VIDEO
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RENDER_TARGET_TYPES {
    }

    public static final int MEDIA_TYPE_CAMERA = 0x01; // Cannot behave as a render target
    public static final int MEDIA_TYPE_IMAGE = 0x02; // Can behave both as a source and target
    public static final int MEDIA_TYPE_VIDEO = 0x04; // Can behave both as a source and target
    public static final int MEDIA_TYPE_AUDIO = 0x08; // Can behave only as a source :'D
    public static final int MEDIA_TYPE_MIC = 0x10; // Can behave only as a source :'D
    public static final int MEDIA_TYPE_BLANK_AUDIO = 0x20; // Can behave only as a source :'D


    @IntDef({
            MEDIA_TYPE_CAMERA,
            MEDIA_TYPE_IMAGE,
            MEDIA_TYPE_VIDEO,
            MEDIA_TYPE_AUDIO,
            MEDIA_TYPE_MIC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SOURCE_MEDIA_TYPES {
    }

    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;
}
