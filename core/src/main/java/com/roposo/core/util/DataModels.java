package com.roposo.core.util;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author anilshar on 10/16/15.
 */
public class DataModels {

    public static final int INVALID = -1;
    public static final int COMMENT = 0;
    public static final int RIGHT_COMMENT = 1;
    public static final int REPLY_COMMENT = 2;
    public static final int COLLAPSED = 3;
    public static final int CREATE_STORY_TITLE_HEADER = 5;
    public static final int LIKE_STORY = 6;
    public static final int FOLLOW_USER_TICK = 7;
    public static final int FOLLOW_USER = 8;
    public static final int COMBINED_LOADER = 11;
    public static final int EMPTY_STORY_ITEMS_LIST = 13;
    public static final int FOLLOW_USER_GROUP = 14;
    public static final int BLOCKED_USER = 15;
    public static final int INVITE_FRIENDS_VIEW = 16;
    public static final int FOLLOW_USER_TICK_NEW = 17;
    public static final int LIKE_STORY_NEW = 18;
    public static final int INTEREST_POST_STORY = 19;
    public static final int CHAT_TEXT = 20;
    public static final int CHAT_ACCEPT_REJECT = 21;
    public static final int RECENT_CHAT = 22;
    public static final int CHAT_LOAD_HISTORY_BLOCK = 23;
    public static final int CHAT_REQUEST_SENT = 24;
    public static final int AUDIO_CHOOSE = 25;
    public static final int AUDIO_TYPE = 26;
    public static final int MEDIA_ALBUM_STRIP = 28;
    public static final int ADD_MEDIA_ALBUM_STRIP = 29;
    public static final int USER_SINGLE_SUGGESTION_TYPE = 30;
    public static final int USER_MULTIPLE_SUGGESTION_TYPE = 31;
    public static final int CHAT_REQUEST_COUNT = 32;
    public static final int CHAT_SUGGESTION = 33;
    public static final int CHAT_INVITE_UNIT = 34;
    public static final int CHAT_TOP_USER = 35;
    public static final int CHAT_TOP_USER_UNIT = 36;
    public static final int CHAT_MESSAGE = 37;
    public static final int CHAT_UNKNOWN = 38;
    public static final int CHAT_STORY_RP = 39;
    public static final int CHAT_RELATED_PRODUCT_SELLER_AUTO_REPLY = 40;
    public static final int CHAT_PRODUCT_OOS = 41;
    public static final int CHAT_PAYMENT_METHOD = 42;
    public static final int CHAT_ADDRESS_CARD = 43;
    public static final int CHAT_PAYMENT_CONFIRMED = 44;
    public static final int TEXT_CHIP_TYPE = 45;
    public static final int SUGGESTED_LOCATION_TYPE = 46;
    public static final int SEARCH_LOCATION_TYPE = 47;
    public static final int SEARCH_PRODUCT_TYPE = 48;
    public static final int INVENTORY_ITEM_TYPE = 49;
    public static final int CHAT_STORY_SA = 51;
    public static final int ADDRESS_ENTRY = 52;
    public static final int ADDRESS_HEADER = 53;
    public static final int STATE_CHOOSE = 54;
    public static final int SHORTLISTED_PRODUCT = 55;
    public static final int SHORTLISTED_DETAIL_CARD = 56;
    public static final int CHAT_STORY_SHORTLISTED_PRODUCT = 57;
    public static final int CHAT_STORY_REMOVED_PRODUCT = 58;
    public static final int CHAT_CHECKOUT_PRODUCT = 59;
    public static final int ADD_ITEM_TO_INVENTORY = 60;
    public static final int CHAT_SHARED_IMAGE = 61;
    public static final int CHAT_REQUEST_ADDRESS = 62;
    public static final int CHAT_SUGGESTED_UNIT = 63;
    public static final int ADD_CUSTOM_STORY_TOPIC = 64;
    public static final int EXPLORE_CATEGORIES = 65;
    public static final int TYPE_SUGGESTED_USER = 66;
    public static final int TYPE_SUGGESTED_HASHTAG = 67;
    public static final int SEARCH_CATEGORIES_CARD = 68;
    public static final int GRID_SEARCH_VIEW = 69;
    public static final int SEARCH_FILTER = 70;
    public static final int NAV_STORY_VIEW = 71;
    public static final int CHANNEL_CARD = 72;
    //// TODO: 30/01/17 only for testing

    public static final int SINGLE_TEXT_CARD = 73;
    public static final int LIKE_ACTION_CARD = 74;
    public static final int COMMENT_ACTION_CARD = 75;
    public static final int GENERIC_ACTION_CARD = 76;
    public static final int CUSTOM_SHARE_SHEET = 77;
    public static final int SIMILAR_ITEMS_CARD = 78;
    public static final int DUMMY_LOADER_ITEM = 79;
    public static final int SCROLL_LIST_CARD = 80;
    public static final int SIMILAR_PRODUCT_ITEM_TYPE = 81;
    public static final int CREATE_STORY_PROGRESS = 82;
    public static final int SIMILAR_PRODUCT_ITEM = 83;
    public static final int PROFILE_FOLLOW_CARD = 84;
    public static final int FOLLOW_BLOGGER_CARD = 85;
    public static final int FOLLOW_BLOGGER_CARD_ITEM = 86;
    public static final int EMPTY_HOME_ITEM = 87;
    public static final int HOME_TIMER_CARD = 88;
    public static final int SPLASH_VIEW = 89;
    public static final int FEED_SMALL_CARD = 90;
    public static final int TODAYS_CHANNEL = 91;
    public static final int CHANNEL_CARD_V2 = 92;
    public static final int HOME_FOLLOW_HORIZONTAL = 93;
    public static final int CIRCULAR_FEED_CARD = 94;
    public static final int SHOP_CATEGORY_GRID_CARD = 95;
    public static final int DRAWER_LAYOUT_ITEM = 96;
    public static final int DISCOVER_CHANNEL_CARD = 97;
    public static final int PROFILE_DRAWER_ITEM = 98;
    public static final int DRAWER_ACTION_ITEM = 99;
    public static final int NEW_FOR_YOU_DRAWER_ITEM = 100;
    public static final int ABOUT_US_DRAWER_ITEM = 101;
    public static final int ACCOUNT_SETTING_TYPE = 102;
    public static final int ACCOUNT_SETTING_HEADER = 103;
    public static final int STICKER = 104;
    public static final int FOLLOW_PEOPLE_CARD_TYPE = 105;
    public static final int USER_DETAILS_CARD_TYPE = 106;
    public static final int TYPE_PROFILE_CUSTOM_BLOCK = 107;
    public static final int TYPE_PROFILE_CUSTOM_IMAGE_ITEM = 108;
    public static final int PROFILE_ACTION_CARD_TYPE = 109;
    public static final int TYPE_PROFILE_SHARABLE_ITEM = 110;
    public static final int PROFILE_ALL_POST_CARD_TYPE = 111;
    public static final int USER_BLOCK_TYPE = 112;
    public static final int SCROLL_LIST_CARD_NAVIGATION = 113;
    public static final int PRODUCT_NAV_CARD = 114;
    public static final int SCROLLABLE_PRODUCT_LIST_CARD = 115;
    public static final int MANAGE_SHOP = 116;
    public static final int INSIGHTS_CARD_ITEM = 117;
    public static final int PROFILE_GRID_ITEM = 118;
    public static final int LIST_ITEM_CARD = 119;
    public static final int AWARD_BLOGGER = 120;
    public static final int CHAT_INTERESTED_VIEW = 121;
    public static final int PROFILE_ACTION_BUTTON = 122;
    public static final int DATA_LOADING = 123;
    public static final int AUDIO_ITEM = 124;
    public static final int TEXT_ITEM = 125;
    public static final int TYPE_NEW_CREATE_STORY = 126;
    public static final int NO_INTERNET_ITEM = 127;
    public static final int DISCOVER_ITEM_TYPE = 128;
    public static final int UPDATE_APP_VIEW = 129;
    public static final int GENERIC_CARD = 130;
    public static final int STORY_INFO_CARD = 131;
    public static final int STORY_VIEW_CARD_V2 = 132;
    public static final int STORY_SHARE_CARD = 133;
    public static final int ACTIONS_CARD = 134;
    public static final int FESTIVE_CARD_ITEM = 135;
    public static final int SPLASH_VIEW_V2 = 136;
    public static final int TUTORIAL_VIDEO_ITEM = 137;
    public static final int GENERIC_CARD_V2 = 138;
    public static final int CHANNEL_AD_CARD = 139;
    public static final int PROFILE_CONTEST_ITEM_TYPES = 140;
    public static final int DISCOVER_MINI_ITEM_TYPE = 141;
    public static final int CONTEST_INVITE_ITEM_TYPE = 142;
    public static final int REDIRECT_BAR = 143;
    public static final int LOCAL_DATA_LOADER_ITEM = 144;
    public static final int ONBOARDING_CHANNEL_CARD = 145;
    public static final int ONE_TO_ONE_INVITE_CARD = 146;
    public static final int LANGUAGE_CHOOSER_CARD = 147;
    public static final int GENERIC_INIVTE_CARD = 148;
    public static final int LIKE_STORY_V2 = 149;
    public static final int AUDIO_ALBUM = 150;
    public static final int PARTICLE_EFFECT_ITEM = 151;
    public static final int FILTER_ITEM = 152;
    public static final int PLAY_AD_MOBI_NAV_CARD = 154;
    public static final int TIMELINE_MEDIA_STRIP = 155;
    public static final int GIFT_ITEM_VIEW = 156;
    public static final int TOP_GIFTERS = 157;
    public static final int VIEW_GIFTER_ITEM = 158;


    private DataModels() {
        throw new UnsupportedOperationException();
    }

//    public static final int FOLLOW_STORY_USER_GROUP = 16;

    @IntDef({
            COLLAPSED,
            COMMENT,
            INVALID,
            REPLY_COMMENT,
            RIGHT_COMMENT,
            CREATE_STORY_TITLE_HEADER,
            COMBINED_LOADER,
            EMPTY_STORY_ITEMS_LIST,
            FOLLOW_USER_GROUP,
            BLOCKED_USER,
            INVITE_FRIENDS_VIEW,
            FOLLOW_USER_TICK_NEW,
            LIKE_STORY,
            LIKE_STORY_NEW,
            FOLLOW_USER_TICK,
            INTEREST_POST_STORY,
            CHAT_TEXT,
            RECENT_CHAT,
            CHAT_ACCEPT_REJECT,
            CHAT_LOAD_HISTORY_BLOCK,
            CHAT_REQUEST_SENT,
            AUDIO_CHOOSE,
            AUDIO_TYPE,
            MEDIA_ALBUM_STRIP,
            ADD_MEDIA_ALBUM_STRIP,
            USER_SINGLE_SUGGESTION_TYPE,
            USER_MULTIPLE_SUGGESTION_TYPE,
            CHAT_REQUEST_COUNT,
            CHAT_SUGGESTION,
            CHAT_INVITE_UNIT,
            CHAT_TOP_USER,
            CHAT_TOP_USER_UNIT,
            CHAT_SUGGESTED_UNIT,
            CHAT_MESSAGE,
            CHAT_UNKNOWN,
            TEXT_CHIP_TYPE,
            SUGGESTED_LOCATION_TYPE,
            SEARCH_LOCATION_TYPE,
            SEARCH_PRODUCT_TYPE,
            INVENTORY_ITEM_TYPE,
            CHAT_PRODUCT_OOS,
            ADDRESS_ENTRY,
            ADDRESS_HEADER,
            STATE_CHOOSE,
            SHORTLISTED_PRODUCT,
            SHORTLISTED_DETAIL_CARD,
            CHAT_STORY_SHORTLISTED_PRODUCT,
            CHAT_STORY_REMOVED_PRODUCT,
            FOLLOW_USER,
            ADD_ITEM_TO_INVENTORY,
            CHAT_SHARED_IMAGE,
            CHAT_REQUEST_ADDRESS,
            EXPLORE_CATEGORIES,
            ADD_CUSTOM_STORY_TOPIC,
            TYPE_SUGGESTED_USER,
            TYPE_SUGGESTED_HASHTAG,
            SEARCH_FILTER,
            STICKER,
            GRID_SEARCH_VIEW,
            DUMMY_LOADER_ITEM,
            NAV_STORY_VIEW,
            CHANNEL_CARD,
            SINGLE_TEXT_CARD,
            LIKE_ACTION_CARD,
            COMMENT_ACTION_CARD,
            GENERIC_ACTION_CARD,
            CUSTOM_SHARE_SHEET,
            SIMILAR_ITEMS_CARD,
            SCROLL_LIST_CARD,
            SIMILAR_PRODUCT_ITEM_TYPE,
            CREATE_STORY_PROGRESS,
            SIMILAR_PRODUCT_ITEM,
            PROFILE_FOLLOW_CARD,
            FOLLOW_BLOGGER_CARD,
            FOLLOW_BLOGGER_CARD_ITEM,
            EMPTY_HOME_ITEM,
            HOME_TIMER_CARD,
            SPLASH_VIEW,
            HOME_FOLLOW_HORIZONTAL,
            CHANNEL_CARD_V2,
            TODAYS_CHANNEL,
            CIRCULAR_FEED_CARD,
            DRAWER_LAYOUT_ITEM,
            SHOP_CATEGORY_GRID_CARD,
            PROFILE_DRAWER_ITEM,
            DRAWER_ACTION_ITEM,
            NEW_FOR_YOU_DRAWER_ITEM,
            ABOUT_US_DRAWER_ITEM,
            ACCOUNT_SETTING_TYPE,
            ACCOUNT_SETTING_HEADER,
            FOLLOW_PEOPLE_CARD_TYPE,
            DISCOVER_CHANNEL_CARD,
            SCROLL_LIST_CARD_NAVIGATION,
            PRODUCT_NAV_CARD,
            USER_DETAILS_CARD_TYPE,
            TYPE_PROFILE_CUSTOM_BLOCK,
            TYPE_PROFILE_CUSTOM_IMAGE_ITEM,
            TYPE_PROFILE_SHARABLE_ITEM,
            PROFILE_ACTION_CARD_TYPE,
            PROFILE_ALL_POST_CARD_TYPE,
            USER_BLOCK_TYPE,
            SCROLLABLE_PRODUCT_LIST_CARD,
            MANAGE_SHOP,
            INSIGHTS_CARD_ITEM,
            PROFILE_GRID_ITEM,
            LIST_ITEM_CARD,
            CHAT_INTERESTED_VIEW,
            PROFILE_ACTION_BUTTON,
            DATA_LOADING,
            AUDIO_ITEM,
            TEXT_ITEM,
            TYPE_NEW_CREATE_STORY,
            NO_INTERNET_ITEM,
            UPDATE_APP_VIEW,
            GENERIC_CARD,
            DISCOVER_ITEM_TYPE,
            DISCOVER_MINI_ITEM_TYPE,
            STORY_INFO_CARD,
            STORY_SHARE_CARD,
            STORY_VIEW_CARD_V2,
            ACTIONS_CARD,
            FESTIVE_CARD_ITEM,
            SPLASH_VIEW_V2,
            GENERIC_CARD_V2,
            CHANNEL_AD_CARD,
            ONBOARDING_CHANNEL_CARD,
            PROFILE_CONTEST_ITEM_TYPES,
            CONTEST_INVITE_ITEM_TYPE,
            REDIRECT_BAR,
            LOCAL_DATA_LOADER_ITEM,
            LANGUAGE_CHOOSER_CARD,
            GENERIC_INIVTE_CARD,
            LIKE_STORY_V2,
            ONE_TO_ONE_INVITE_CARD,
            TIMELINE_MEDIA_STRIP,
            PARTICLE_EFFECT_ITEM,
            AUDIO_ALBUM,
            FILTER_ITEM,
            PLAY_AD_MOBI_NAV_CARD,
            GIFT_ITEM_VIEW,
            TOP_GIFTERS,
            VIEW_GIFTER_ITEM
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedType {
    }
}
