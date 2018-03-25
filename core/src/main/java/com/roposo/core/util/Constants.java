package com.roposo.core.util;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author : anilshar on 26/09/16.
 */

public class Constants {

    // 1.  Media Types
    public static final int UNKNOWN = -1;

    public static final int LOCAL_PHOTO = 0;
    public static final int CAPTURED_PHOTO = 1;
    public static final int REMOTE_PHOTO = 2;

    public static final int LOCAL_VIDEO = 3;
    public static final int CAPTURED_VIDEO = 4;
    public static final int REMOTE_VIDEO = 5;

    public static final int YOUTUBE_VIDEO_URL = 6;

    public static final int AUDIO_VIDEO = 7;

    public static final int LOCAL_GIF = 8;
    public static final int REMOTE_GIF = 9;

    // Grouped media types
    public static final int LOCAL_PHOTO_LIST = 10;
    public static final int LOCAL_VIDEO_LIST = 11;
    public static final int LOCAL_MEDIA_LIST = 12;

    public static final int LOCAL_PHOTO_VIDEO = 13;
    public static final int CAPTURED_PHOTO_VIDEO = 14;
    public static final int LOCAL_VIDEO_VIDEO = 15;
    public static final int CAPTURED_VIDEO_VIDEO = 16;
    public static final String OPEN_FROM_KEY = "open_from";
    public static final String OPEN_FROM_MAIN_PAGER = "open_from_main_pager";
    public static final String OPEN_FROM_SWITCH_HELPER = "open_from_switch_helper";
    public static final long FIVE_DAYS = 5 * 24 * 60 * 60 * 1000L;
    public static final int BLUR_RADIUS = 20;
    public static final int EMOJI_OK = 0x1F44C;


    public static float keyBoardHeight = 0;

    public enum TYPE_ENTITY {STORIES, LISTS, DISCOUNT, LIKES, HISTORY}

    @IntDef({
            UNKNOWN,
            LOCAL_PHOTO,
            CAPTURED_PHOTO,
            REMOTE_PHOTO,
            LOCAL_VIDEO,
            CAPTURED_VIDEO,
            REMOTE_VIDEO,
            YOUTUBE_VIDEO_URL,
            AUDIO_VIDEO,
            LOCAL_PHOTO_LIST,
            LOCAL_VIDEO_LIST,
            LOCAL_MEDIA_LIST,
            LOCAL_PHOTO_VIDEO,
            CAPTURED_PHOTO_VIDEO,
            LOCAL_VIDEO_VIDEO,
            CAPTURED_VIDEO_VIDEO

    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaTypes {
    }

    // Animation Types
    public static final int NONE = 0;
    public static final int ENTER_FROM_RIGHT = 1;
    public static final int ENTER_FROM_BOTTOM = 2;
    public static final int ENTER_FROM_UP = 3;
    public static final int ENTER_FROM_LEFT = 4;
    public static final int ENTER_WITH_FADE = 5;
    public static final int ENTER_WITH_BOUNCE = 6;

    @IntDef({
            NONE,
            ENTER_FROM_RIGHT,
            ENTER_FROM_BOTTOM,
            ENTER_FROM_UP,
            ENTER_FROM_LEFT,
            ENTER_WITH_FADE,
            ENTER_WITH_BOUNCE
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationTypes {
    }
}
