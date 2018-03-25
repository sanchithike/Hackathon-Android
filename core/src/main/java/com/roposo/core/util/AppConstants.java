package com.roposo.core.util;

import android.support.annotation.IntDef;

import com.roposo.core.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by anilshar on 2/5/16.
 */
public class AppConstants {


    public static final int VIEW_TYPE_LOCATION = 101;
    public static final int VIEW_TYPE_TOPIC = 102;
    public static final int VIEW_TYPE_PEOPLE = 103;
    public static final int VIEW_TYPE_UNKOWN = -1;
    public static final int TYPE_NONE = 0;
    public static final int TYPE_REPOST = 1;
    public static final int TYPE_TRENDING = 2;
    public static final int TYPE_LIKE = 3;
    public static final int TYPE_COMMENT = 4;


    public static final String STORY_ID = "story_id";
    public static final String DEEP_LINK_STRING = "deep_link";
    public static final CharSequence ROPOSO_VIDEO_REGEX = ".ropose.com";
    public static final String VIDEO_THUMBNAIL = "video_thumbnail";
    public static final String CURRENT_ITEM_POSITION = "currentItem";
    public static final int NO_SHAPE = -1;
    public static final int CIRCULAR = 0;
    public static final int RECTANGULAR = 1;
    public static final int ARC = 2;
    public static final int MAX_DURATION = 15;

    public static final int WHITE_LOADER = 0;
    public static final int DARK_LOADER = 1;


    public static final int INVALID_VIDEO_TYPE = -1;
    public static final int YOUTUBE = 1;
    public static final int LOCAL_VIDEO = 2;
    public static final int CAPTURED = 3;
    public static final int AUDIO_IMAGE = 4;
    public static final int LOCAL_IMAGE = 5;


    public static final String MEDIA_PATH = "mp";
    public static final String MEDIA_ENTRY = "me";
    public static final String AUDIO_ENTRY = "ae";
    public static final String VOICE_ENTRY = "ve";
    public static final String MAX_ALLOWED_CAPTURE_DURATION = "max_vid_dur";
    public static final String REMAINING_CAPTURE_DURATION = "remaining_media_dur";
    public static final String MIN_MEDIA_ITEM_DURATION = "img_media_dur";

    public static final String VIDEO_LOOP_SELF = "video_loop_self";
    public static final String ADD_SUB_STORY = "add_sub_story";
    public static final int MALE = 0;
    public static final int FEMALE = 1;
    public static final int GENDER_HIDDEN = -1;
    public static final String VIDEO_ID = "videoId";


    public static final String YOUTUBE_VIEW = "youtube_view";
    // Login screen type
    public static final int LOGIN_V2 = 1;
    public static final int LOGIN_V1 = 0;
    public static final int LOGIN_V2_PAGE2 = 2;
    public static final int LOGIN_V3 = 3;
    public static final int LOGIN_V4 = 4;

    public static final String HASH_TAG_REGEX = "([#@][a-zA-Z0-9\\u0900-\\u097F_-]+)"; // for english, hindi and numeric match (hash tag or )
    //gender types
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    public static final String FACEBOOK_PACKAGE = "com.facebook.katana";
    public static final String WHATSAPP_PACKAGE = "com.whatsapp";
    public static final String FACEBOOK_MESSENGER_PACKAGE = "com.facebook.orca";
    public static final String CHROME_PACKAGE_NAME = "com.android.chrome";
    public static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    public static final String VIDEO_PATH = "video_path";
    public static final String CLEAR_STORIES = "clear_stories_draft";
    public static final String COMMON_KEY = "c_key";
    public static final boolean TOLOG = false;
    public static final String EXTERNAL_SHARE_PARAM = "s_ext=true";
    public static final String BASE_APP_URI = "android-app://com.roposo.android/http/www.roposo.com/";
    public static final String GOOGLE_MAP_PACKAGE = "com.google.android.apps.maps";

    public final static String[] TITLES = {"Feed", "Shop", "People", "You", "Explore"};
    public final static int[] ICONS = {R.string.icon_feed_new, R.string.icon_shop, R.string.icon_people, R.string.icon_you, R.string.icon_explore};
    public static final int FEED_POSITION = 0;
    public static final int SHOP_POSITION = 1;
    public static final int PEOPLE_POSITION = 2;
    public static final int YOU_POSITION = 3;
    public static final int EXPLORE_POSITION = 4;
    public static final String INBOX_ITEM_SELECTED = "inboxItemSelected";
    public static final String ON_PROGRESS = "onProgress";
    public static final String ON_IMAGE_LOAD = "onImageLoad";
    public static final String ON_ITEM_CLICK = "onItemClick";
    public static final String DELETE_ITEM_AT_POSITION = "deleteItemAtPosition";
    public static final String LIKE_STORY_CALLBACK = "likeStoryCallback";
    public static final String AD_LOADING_CALLBACK = "adLoadingCallback";
    public static final String RELOAD_CALL = "reload_call";
    public static final String FORCE_MOVE_TO_NEXT = "force_move_to_next";
    public static final String SET_TEXT_EXPANDED = "set_text_expanded";
    public static final String ON_LOAD_SUCCESS = "on_load_success";
    public static final String ON_LOAD_DATA = "onLoadData";
    public static final String ON_REFRESH_DATA = "on_refresh_data";
    public static final String FIRST_SPLASH_TIMER = "first_splash_timer";
    public static final String SPLASH_TIMER = "splash_timer";
    public static final String MODE = "mode";
    public static final long DEFAULT_SPLASH_TIMER = 5000;
    public static final int DEFAULT_STORY_TIME = 5000;
    public static final String DEFAULT_MODE = "original";
    public static final String LOADING = "loading";
    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";
    public static final String ON_DRAWER_CLOSE = "on_drawer_close";
    public static final String TRUE_CALLER_PACKAGE = "com.truecaller";
    public static final String ON_CLOSE_FESTIVE_CARD = "on_close_festive_card";
    public static final String ON_BOARDING = "onBoarding";
    public static final String NAV_VIDEO_ID = "nav_video_id";
    public static final String BLOCKS = "initialBlocks";
    public static final String POST_SHARE_FRAGMENT = "post_share_framgnet";
    public static final String PLATFORM_ZOOM_API_KEY = "5ffd7634cb32c1e759a3d0af1d3a0f2a54e20f43";
    public static final String PLATFORM_ZOOM_APP_ID = "3ecc7e8c4a7455c35965bd972448641c";
    public static final String WEBVIEW_PACKAGE = "com.google.android.webview";
    public static final String XENDER_PACKAGE = "cn.xender";
    public static final String FACEBOOK_LITE_PACKAGE = "com.facebook.lite";
    public static final String SHAREIT_PACKAGE = "com.lenovo.anyshare.gps";
    public static final long SECONDS = 1000L;
    public static final long MINUTES = 60 * SECONDS;
    public static final long MIN_LATENCY_MILLIS = 20 * MINUTES;
    public static final String ADMOB_APP_ID = "ca-app-pub-5150109663795473~6827459007";

    @IntDef({
            MALE,
            FEMALE,
            GENDER_HIDDEN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Gender {
    }

    @IntDef({
            NO_SHAPE,
            CIRCULAR,
            RECTANGULAR,
            ARC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Shapes {
    }

    @IntDef({
            LEFT,
            RIGHT,
            TOP,
            BOTTOM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    @IntDef({
            YOUTUBE,
            LOCAL_VIDEO,
            CAPTURED,
            AUDIO_IMAGE,
            LOCAL_IMAGE,
            INVALID_VIDEO_TYPE

    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VideoTypes {
    }
}
