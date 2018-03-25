package com.roposo.core.util;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by anilshar on 10/29/15.
 */
public class SupportedViewTypes {
    public static final String COMMENT_TYPE = "comb";

    //Please NOTE the Convention :: Name of data model type will end with underscore if this type is not
    // received from any api call. Its only for the purpose of internal usage
    public static final String RIGHT_COMMENT_TYPE = "comr_";
    public static final String REPLY_COMMENT_TYPE = "rep_";
    public static final String COLLAPSED_TYPE = "colb";
    public static final String LIKE_STORY_TYPE = "intro1";
    public static final String FOLLOW_USER_TICK_TYPE = "iusb";
    public static final String FOLLOW_USER_TYPE = "usb";
    public static final String COMBINED_LOADER_TYPE = "rlb"; // roposo loader block
    public static final String FOLLOW_USER_GROUP_TYPE = "Feed";
    public static final String BLOCKED_USER_TYPE = "bl_ub";
    public static final String INVITE_FRIEND_TYPE = "cntct";
    public static final String FOLLOW_USER_TICK_TYPE_NEW = "iusb2";
    public static final String LIKE_STORY_TYPE_NEW = "intro2";
    public static final String INTEREST_POST_STORY = "ddc";
    public static final String CHAT_ACCEPT_REJECT_TYPE = "arb";
    public static final String CHAT_PRESENCE_TYPE = "prs";
    public static final String CHAT_LOAD_HISTORY_BLOCK_TYPE = "clh";
    public static final String CHAT_REQUEST_SENT_TYPE = "crs";
    public static final String CHAT_SUGGESTION_TYPE = "ch_ub";
    public static final String CHAT_INVITE_TYPE = "ch_in";
    public static final String CHAT_SUGGESTED_TYPE = "ch_su";
    public static final String STICKERS_TYPE = "stick";
    public static final String INVENTORY_ITEM_TYPE = "invb";
    public static final String ADD_ITEM_TO_INVENTORY = "atoi_";
    public static final String CHAT_TEXT_TYPE = "ct";
    public static final String SHORTLISTED_PRODUCT_TYPE = "shlp";
    public static final String SHORTLISTED_DETAIL_CARD = "fbc";
    public static final String CHAT_STORY_TYPE = "cs_rp";
    public static final String CHAT_RELATED_PRODUCT_SELLER_AUTO_REPLY_TYPE = "ascr";
    public static final String CHAT_PRODUCT_OOS_TYPE = "cs_oos";
    public static final String CHAT_PAYMENT_METHOD_TYPE = "aspo";
    public static final String CHAT_ADDRESS_CARD_TYPE = "b_addr";
    public static final String CHAT_PAYMENT_CONFIRMED_TYPE = "cr_pm";
    public static final String CHAT_STORY_SA_TYPE = "cs_sa";
    public static final String CHAT_STORY_SHORTLISTED_PRODUCT_TYPE = "cs_sp";
    public static final String CHAT_STORY_REMOVED_PRODUCT_TYPE = "cs_rmp";
    public static final String CHAT_CHECKOUT_PRODUCT_TYPE = "cr_cp";
    public static final String CHAT_SHARED_IMAGE_TYPE = "img_ch";
    public static final String CHAT_REQUEST_ADDRESS_TYPE = "assa";
    public static final String TYPE_SUGGESTED_HASHTAG = "sg_hb";
    public static final String TYPE_SUGGESTED_USER = "sg_ub";
    public static final String CHAT_STORY_INTERESTED = "cs_int";
    private static final Map<String, Integer> sMap = new Hashtable<>();

    public static final String EXPLORE_CATEGORIES_TYPE = "imc";
    private static final String SEARCH_CAT_CARD = "scc";
    private static final String GRID_SEARCH_VIEW = "sb";
    private static final String NAV_STORY_VIEW = "nsb";
    public static final String DUMMY_LOADER_ITEM = "dli";
    public static final String EMPTY_HOME_ITEM_TYPE = "ehi";
    public static final String SPLASH_VIEW = "spsc";
    public static final String SPLASH_VIEW_V2 = "spch";
    public static final String GENERIC_VIEW_V2 = "shgen";
    public static final String DISCOVER_ITEM_TYPE = "dhcb";
    public static final String TUTORIAL_VIDEO_UNIT_TYPE = "vpre";

    //// TODO: 30/01/17 only for testing


    public static final String STORY_VIEW_CARD_TYPE = "sac";
    public static final String LIKE_ACTION_CARD_TYPE = "like_crd";
    public static final String COMMENT_ACTION_CARD_TYPE = "com_crd";
    public static final String GENERIC_ACTION_CARD_TYPE = "gen_crd";
    public static final String CUSTOM_SHARE_SHEET_TYPE = "cus_ssh";
    public static final String SIMILAR_ITEMS_CARD_TYPE = "sim_item";
    public static final String SCROLL_LIST_CARD_TYPE = "slc";
    public static final String SCROLL_LIST_CARD_NAVIGATION_TYPE = "slcv";
    private static final String SIMILAR_PRODUCTS_TYPE = "sicp";
    public static final String STORY_CREATION_PROGRESS = "_ncs";

    private static final String PROFILE_FOLLOW_CARD_TYPE = "pfsuc";
    public static final String FOLLOW_BLOGGER_CARD_TYPE = "pfbs";
    private static final String FOLLOW_BLOGGER_CARD_ITEM_TYPE = "pfbsi";
    private static final String HOME_TIMER_ITEM_TYPE = "cscb";

    public static final String CHANNEL_CARD_TYPE = "hxpl";
    public static final String TODAYS_CHANNEL_ITEM_TYPE = "hxplb";
    private static final String CHANNEL_CARD_V2_ITEM_TYPE = "hxplo";
    public static final String CIRCULAR_FEED_CARD_TYPE = "hxplc";
    private static final String DISCOVER_CHANNEL_CARD_TYPE = "dmcb";
    private static final String HOME_FOLLOW_HORIZONTAL_TYPE = "hxpls";
    public static final String SHOP_CATEGORY_GRID_CARD = "hxplsb";
    public static final String NEW_DRAWER_TYPE = "ndt_";
    public static final String PROFILE_DRAWER_TYPE = "pdt_";
    public static final String DRAWER_ACTION_TYPE = "dat_";
    public static final String NEW_FOR_YOU_DRAWER_TYPE = "nfut_";
    public static final String ABOUT_US_TYPE = "aut_";
    public static final String ACCOUNT_SETTING_TYPE = "ast_";
    public static final String ACCOUNT_SETTING_HEADER = "ash_";
    private static final String FOLLOW_PEOPLE_TPYE = "gensc";
    public static final String USER_DETAILS_TYPE = "ubh";
    private static final String PROFILE_CUSTOM_TYPE = "cprv";
    public static final String PROFILE_CUSTOM_IMAGE_ITEM = "pci";
    public static final String PROFILE_SHARABLE_ITEM = "tyf";
    public static final String PROFILE_ACTION_TYPE = "pa";
    public static final String PROFILE_ALL_POST_TYPE = "gview";
    private static final String PROFILE_GRID_ITEM = "psbp";
    private static final String USER_BLOCK_TYPE = "ub";
    public static final String PROFILE_CREATION_ITEM = "crpn";
    public static final String LIST_CHANNEL_CARD = "gvpl";
    public static final String SCROLLABLE_PRODUCT_LIST = "spl";
    public static final String PROFILE_CONTEST_ITEM = "awd";
    public static final String CUSTOM_BANNER_TYPE = "cbnr";
    private static final String MANAGE_SHOP_TYPE = "mys";
    public static final String INSIGHTS_CARD_ITEM_TYPE = "mys_in";
    public static final String PROFILE_ACTION_BUTTON_TYPE = "pab";
    private static final String TYPE_NEW_CREATE_STORY = "ncs";
    public static final String NO_INTERNET_ITEM_TYPE = "niv";
    public static final String UPDATE_APP_TYPE = "huac";
    private static final String GENERIC_CARD_ITEM_TYPE = "hgen";

    //New Cards for Reaction Sheet
    public static final String STORY_INFO_CARD_TYPE = "sincrd";
    public static final String STORY_VIEW_CARD_V2 = "sac2";
    public static final String STORY_SHARE_CARD_TYPE = "sshcrd";
    public static final String ACTIONS_CARD_TYPE = "actioncrd";
    public static final String FESTIVE_CARD_ITEM = "fstv";
    public static final String CHANNEL_AD_CARD = "cac";
    public static final String LANGUAGE_CHOOSER_CARD = "lngc";
    private static final String DISCOVER_MINI_ITEM = "chcrd";
    public static final String CONTEST_INVITE_ITEM = "ucni";
    public static final String ONE_TO_ONE_INVITE_ITEM = "ticb";
    private static final String REDIRECT_BAR = "rbar";
    public static final String LOCAL_DATA_LOADER_TYPE = "dldr";
    public static final String ONBOARDING_CHANNEL_ITEM = "oncb";
    public static final String GENERIC_INVITE_ITEM = "gib";
    private static final String LIKE_STORY_V2_TYPE = "intro3";
    public static final String AD_MOVI_NAV_TYPE = "wadvtemp";

    public static final String AUDIO_FOLDER = "ad_f";
    public static final String AUDIO_ITEM = "ad_ent";

    private static final String GIFT_ITEM_TYPE = "vgift";
    private static final String TOP_GIFTERS_ITEM_TYPE = "topvg";
    private static final String VIEW_GIFTER_ITEM_TYPE = "glub";

    static {
        sMap.put(COLLAPSED_TYPE, DataModels.COLLAPSED);
        sMap.put(COMMENT_TYPE, DataModels.COMMENT);
        sMap.put(REPLY_COMMENT_TYPE, DataModels.REPLY_COMMENT);
        sMap.put(RIGHT_COMMENT_TYPE, DataModels.RIGHT_COMMENT);
        sMap.put(LIKE_STORY_TYPE, DataModels.LIKE_STORY);
        sMap.put(FOLLOW_USER_TICK_TYPE, DataModels.FOLLOW_USER_TICK);
        sMap.put(FOLLOW_USER_TYPE, DataModels.FOLLOW_USER);
        sMap.put(COMBINED_LOADER_TYPE, DataModels.COMBINED_LOADER);
        sMap.put(FOLLOW_USER_GROUP_TYPE, DataModels.FOLLOW_USER_GROUP);
        sMap.put(BLOCKED_USER_TYPE, DataModels.BLOCKED_USER);
        sMap.put(INVITE_FRIEND_TYPE, DataModels.INVITE_FRIENDS_VIEW);
        sMap.put(FOLLOW_USER_TICK_TYPE_NEW, DataModels.FOLLOW_USER_TICK_NEW);
        sMap.put(LIKE_STORY_TYPE_NEW, DataModels.LIKE_STORY_NEW);
        sMap.put(STICKERS_TYPE, DataModels.STICKER);
        sMap.put(SHORTLISTED_PRODUCT_TYPE, DataModels.SHORTLISTED_PRODUCT);
        sMap.put(SHORTLISTED_DETAIL_CARD, DataModels.SHORTLISTED_DETAIL_CARD);
        sMap.put(CHAT_TEXT_TYPE, DataModels.CHAT_TEXT);
        sMap.put(CHAT_ACCEPT_REJECT_TYPE, DataModels.CHAT_ACCEPT_REJECT);
        sMap.put(CHAT_PRESENCE_TYPE, DataModels.RECENT_CHAT);
        sMap.put(CHAT_LOAD_HISTORY_BLOCK_TYPE, DataModels.CHAT_LOAD_HISTORY_BLOCK);
        sMap.put(CHAT_REQUEST_SENT_TYPE, DataModels.CHAT_REQUEST_SENT);
        sMap.put(CHAT_SUGGESTION_TYPE, DataModels.CHAT_SUGGESTION);
        sMap.put(CHAT_INVITE_TYPE, DataModels.CHAT_INVITE_UNIT);
        sMap.put(CHAT_SUGGESTED_TYPE, DataModels.CHAT_SUGGESTED_UNIT);
        sMap.put(CHAT_STORY_TYPE, DataModels.CHAT_STORY_RP);
        sMap.put(CHAT_RELATED_PRODUCT_SELLER_AUTO_REPLY_TYPE, DataModels.CHAT_RELATED_PRODUCT_SELLER_AUTO_REPLY);
        sMap.put(CHAT_PRODUCT_OOS_TYPE, DataModels.CHAT_PRODUCT_OOS);
        sMap.put(CHAT_PAYMENT_METHOD_TYPE, DataModels.CHAT_PAYMENT_METHOD);
        sMap.put(CHAT_ADDRESS_CARD_TYPE, DataModels.CHAT_ADDRESS_CARD);
        sMap.put(CHAT_PAYMENT_CONFIRMED_TYPE, DataModels.CHAT_PAYMENT_CONFIRMED);
        sMap.put(INVENTORY_ITEM_TYPE, DataModels.INVENTORY_ITEM_TYPE);
        sMap.put(ADD_ITEM_TO_INVENTORY, DataModels.ADD_ITEM_TO_INVENTORY);
        sMap.put(CHAT_STORY_SA_TYPE, DataModels.CHAT_STORY_SA);
        sMap.put(CHAT_STORY_SHORTLISTED_PRODUCT_TYPE, DataModels.CHAT_STORY_SA);
        sMap.put(CHAT_STORY_REMOVED_PRODUCT_TYPE, DataModels.CHAT_STORY_SA);
        sMap.put(CHAT_CHECKOUT_PRODUCT_TYPE, DataModels.CHAT_CHECKOUT_PRODUCT);
        sMap.put(CHAT_REQUEST_ADDRESS_TYPE, DataModels.CHAT_REQUEST_ADDRESS);
        sMap.put(EXPLORE_CATEGORIES_TYPE, DataModels.EXPLORE_CATEGORIES);
        sMap.put(TYPE_SUGGESTED_USER, DataModels.TYPE_SUGGESTED_USER);
        sMap.put(CHAT_SHARED_IMAGE_TYPE, DataModels.CHAT_SHARED_IMAGE);
        sMap.put(TYPE_SUGGESTED_HASHTAG, DataModels.TYPE_SUGGESTED_HASHTAG);
        sMap.put(SEARCH_CAT_CARD, DataModels.SEARCH_CATEGORIES_CARD);
        sMap.put(GRID_SEARCH_VIEW, DataModels.GRID_SEARCH_VIEW);
        sMap.put(NAV_STORY_VIEW, DataModels.NAV_STORY_VIEW);
        sMap.put(CHANNEL_CARD_TYPE, DataModels.CHANNEL_CARD);
        sMap.put(STORY_VIEW_CARD_TYPE, DataModels.SINGLE_TEXT_CARD);
        sMap.put(LIKE_ACTION_CARD_TYPE, DataModels.LIKE_ACTION_CARD);
        sMap.put(COMMENT_ACTION_CARD_TYPE, DataModels.COMMENT_ACTION_CARD);
        sMap.put(GENERIC_ACTION_CARD_TYPE, DataModels.GENERIC_ACTION_CARD);
        sMap.put(CUSTOM_SHARE_SHEET_TYPE, DataModels.CUSTOM_SHARE_SHEET);
        sMap.put(DUMMY_LOADER_ITEM, DataModels.DUMMY_LOADER_ITEM);
        sMap.put(SIMILAR_ITEMS_CARD_TYPE, DataModels.SIMILAR_ITEMS_CARD);
        sMap.put(SCROLL_LIST_CARD_TYPE, DataModels.SCROLL_LIST_CARD);
        sMap.put(SCROLL_LIST_CARD_NAVIGATION_TYPE, DataModels.SCROLL_LIST_CARD_NAVIGATION);
        sMap.put(SIMILAR_PRODUCTS_TYPE, DataModels.SIMILAR_PRODUCT_ITEM_TYPE);
        sMap.put(STORY_CREATION_PROGRESS, DataModels.CREATE_STORY_PROGRESS);
        sMap.put(SIMILAR_PRODUCTS_TYPE, DataModels.SIMILAR_PRODUCT_ITEM);
        sMap.put(PROFILE_FOLLOW_CARD_TYPE, DataModels.PROFILE_FOLLOW_CARD);
        sMap.put(FOLLOW_BLOGGER_CARD_TYPE, DataModels.FOLLOW_BLOGGER_CARD);
        sMap.put(FOLLOW_BLOGGER_CARD_ITEM_TYPE, DataModels.FOLLOW_BLOGGER_CARD_ITEM);
        sMap.put(EMPTY_HOME_ITEM_TYPE, DataModels.EMPTY_HOME_ITEM);
        sMap.put(HOME_TIMER_ITEM_TYPE, DataModels.HOME_TIMER_CARD);
        sMap.put(SPLASH_VIEW, DataModels.SPLASH_VIEW);
        sMap.put(SPLASH_VIEW_V2, DataModels.SPLASH_VIEW_V2);
        sMap.put(TODAYS_CHANNEL_ITEM_TYPE, DataModels.TODAYS_CHANNEL);
        sMap.put(CHANNEL_CARD_V2_ITEM_TYPE, DataModels.CHANNEL_CARD_V2);
        sMap.put(HOME_FOLLOW_HORIZONTAL_TYPE, DataModels.HOME_FOLLOW_HORIZONTAL);
        sMap.put(CIRCULAR_FEED_CARD_TYPE, DataModels.CIRCULAR_FEED_CARD);
        sMap.put(SHOP_CATEGORY_GRID_CARD, DataModels.SHOP_CATEGORY_GRID_CARD);
        sMap.put(NEW_DRAWER_TYPE, DataModels.DRAWER_LAYOUT_ITEM);
        sMap.put(PROFILE_DRAWER_TYPE, DataModels.PROFILE_DRAWER_ITEM);
        sMap.put(DRAWER_ACTION_TYPE, DataModels.DRAWER_ACTION_ITEM);
        sMap.put(NEW_FOR_YOU_DRAWER_TYPE, DataModels.NEW_FOR_YOU_DRAWER_ITEM);
        sMap.put(ABOUT_US_TYPE, DataModels.ABOUT_US_DRAWER_ITEM);
        sMap.put(ACCOUNT_SETTING_TYPE, DataModels.ACCOUNT_SETTING_TYPE);
        sMap.put(ACCOUNT_SETTING_HEADER, DataModels.ACCOUNT_SETTING_HEADER);
        sMap.put(DISCOVER_CHANNEL_CARD_TYPE, DataModels.DISCOVER_CHANNEL_CARD);
        sMap.put(FOLLOW_PEOPLE_TPYE, DataModels.FOLLOW_PEOPLE_CARD_TYPE);
        sMap.put(USER_DETAILS_TYPE, DataModels.USER_DETAILS_CARD_TYPE);
        sMap.put(PROFILE_CUSTOM_TYPE, DataModels.TYPE_PROFILE_CUSTOM_BLOCK);
        sMap.put(PROFILE_CUSTOM_IMAGE_ITEM, DataModels.TYPE_PROFILE_CUSTOM_IMAGE_ITEM);
        sMap.put(PROFILE_SHARABLE_ITEM, DataModels.TYPE_PROFILE_SHARABLE_ITEM);
        sMap.put(PROFILE_ACTION_TYPE, DataModels.PROFILE_ACTION_CARD_TYPE);
        sMap.put(PROFILE_ALL_POST_TYPE, DataModels.PROFILE_ALL_POST_CARD_TYPE);
        sMap.put(PROFILE_GRID_ITEM, DataModels.PROFILE_GRID_ITEM);
        sMap.put(USER_BLOCK_TYPE, DataModels.USER_BLOCK_TYPE);
        sMap.put(PROFILE_CREATION_ITEM, DataModels.TYPE_PROFILE_SHARABLE_ITEM);
        sMap.put(LIST_CHANNEL_CARD, DataModels.LIST_ITEM_CARD);
        sMap.put(SCROLLABLE_PRODUCT_LIST, DataModels.SCROLLABLE_PRODUCT_LIST_CARD);
        sMap.put(MANAGE_SHOP_TYPE, DataModels.MANAGE_SHOP);
        sMap.put(INSIGHTS_CARD_ITEM_TYPE, DataModels.INSIGHTS_CARD_ITEM);
        sMap.put(CHAT_STORY_INTERESTED, DataModels.CHAT_INTERESTED_VIEW);
        sMap.put(PROFILE_ACTION_BUTTON_TYPE, DataModels.PROFILE_ACTION_BUTTON);
        sMap.put(TYPE_NEW_CREATE_STORY, DataModels.TYPE_NEW_CREATE_STORY);
        sMap.put(DISCOVER_ITEM_TYPE, DataModels.DISCOVER_ITEM_TYPE);
        sMap.put(DISCOVER_MINI_ITEM, DataModels.DISCOVER_MINI_ITEM_TYPE);
        sMap.put(NO_INTERNET_ITEM_TYPE, DataModels.NO_INTERNET_ITEM);
        sMap.put(UPDATE_APP_TYPE, DataModels.UPDATE_APP_VIEW);
        sMap.put(GENERIC_CARD_ITEM_TYPE, DataModels.GENERIC_CARD);
        sMap.put(STORY_INFO_CARD_TYPE, DataModels.STORY_INFO_CARD);
        sMap.put(STORY_VIEW_CARD_V2, DataModels.STORY_VIEW_CARD_V2);
        sMap.put(STORY_SHARE_CARD_TYPE, DataModels.STORY_SHARE_CARD);
        sMap.put(ACTIONS_CARD_TYPE, DataModels.ACTIONS_CARD);
        sMap.put(FESTIVE_CARD_ITEM, DataModels.FESTIVE_CARD_ITEM);
        sMap.put(TUTORIAL_VIDEO_UNIT_TYPE, DataModels.TUTORIAL_VIDEO_ITEM);
        sMap.put(GENERIC_VIEW_V2, DataModels.GENERIC_CARD_V2);
        sMap.put(CHANNEL_AD_CARD, DataModels.CHANNEL_AD_CARD);
        sMap.put(PROFILE_CONTEST_ITEM, DataModels.PROFILE_CONTEST_ITEM_TYPES);
        sMap.put(CUSTOM_BANNER_TYPE, DataModels.PROFILE_CONTEST_ITEM_TYPES);
        sMap.put(CONTEST_INVITE_ITEM, DataModels.CONTEST_INVITE_ITEM_TYPE);
        sMap.put(REDIRECT_BAR, DataModels.REDIRECT_BAR);
        sMap.put(LOCAL_DATA_LOADER_TYPE, DataModels.LOCAL_DATA_LOADER_ITEM);
        sMap.put(ONBOARDING_CHANNEL_ITEM, DataModels.ONBOARDING_CHANNEL_CARD);
        sMap.put(ONE_TO_ONE_INVITE_ITEM, DataModels.ONE_TO_ONE_INVITE_CARD);
        sMap.put(LANGUAGE_CHOOSER_CARD, DataModels.LANGUAGE_CHOOSER_CARD);
        sMap.put(GENERIC_INVITE_ITEM, DataModels.GENERIC_INIVTE_CARD);
        sMap.put(LIKE_STORY_V2_TYPE, DataModels.LIKE_STORY_V2);
        sMap.put(AUDIO_FOLDER, DataModels.AUDIO_ALBUM);
        sMap.put(AUDIO_ITEM, DataModels.AUDIO_ITEM);
        sMap.put(AD_MOVI_NAV_TYPE, DataModels.PLAY_AD_MOBI_NAV_CARD);
        sMap.put(GIFT_ITEM_TYPE, DataModels.GIFT_ITEM_VIEW);
        sMap.put(TOP_GIFTERS_ITEM_TYPE, DataModels.TOP_GIFTERS);
        sMap.put(VIEW_GIFTER_ITEM_TYPE, DataModels.VIEW_GIFTER_ITEM);
    }

    private SupportedViewTypes() {
        throw new UnsupportedOperationException("Non-instantiable class");
    }

    public static int getTypeValue(String type) {
        if (type != null && sMap.containsKey(type)) {
            return sMap.get(type);
        }
        return DataModels.INVALID;
    }
}
