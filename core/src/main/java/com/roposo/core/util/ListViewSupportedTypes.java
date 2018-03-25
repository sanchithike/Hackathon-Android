package com.roposo.core.util;

import android.support.v4.util.ArrayMap;

/**
 * Created by amud on 19/06/17.
 */

public class ListViewSupportedTypes {

    private static final int TYPE_BLOCK_0 = 0;
    private static final int TYPE_BLOCK_1 = 1;
    private static final int TYPE_LIST = 2;
    private static final int TYPE_STORY = 3;
    private static final int TYPE_SORT = 5;
    private static final int TYPE_COLOUR = 6;
    private static final int TYPE_TREND = 7;
    private static final int TYPE_USER = 8;
    private static final int TYPE_COMMENT = 9;
    private static final int TYPE_DISCOUNT = 10;
    private static final int TYPE_NOTIFICATION = 11;
    private static final int TYPE_MAINSTORY = 12;
    private static final int TYPE_BANNER = 13;
    public static final int TYPE_NEW_CREATE_STORY = 14; // local block for story creation with progress
    private static final int TYPE_SELECT_COUPON = 15;
    private static final int TYPE_REDEEMED_COUPON = 16;
    private static final int TYPE_MAINLIST = 17;
    private static final int TYPE_DRAWER_ITEM = 18;
    private static final int TYPE_DRAWER_TOP_ITEM = 19;
    private static final int TYPE_LINE_HORIZONTAL = 20;
    private static final int TYPE_VENDOR_CONTACT = 21;
    private static final int TYPE_SEARCH_TEXT_TITLE = 22;
    private static final int TYPE_SEARCH_RESULT_TEXT = 23;
    private static final int TYPE_STORY_SEARCH_RESULT = 24;
    private static final int TYPE_PRODUCT_SEARCH_RESULT = 25;
    private static final int TYPE_PRODUCT_LIST_SEARCH_RESULT = 26;
    private static final int TYPE_SHOW_MORE = 27;
    private static final int TYPE_HEADER_CARD = 28;
    private static final int TYPE_TIP_CARD = 29;
    private static final int TYPE_USER_FOLLOW = 30;
    private static final int TYPE_INVITE_FRIEND = 31;
    private static final int TYPE_PEOPLE_TEXT_TITLE = 32;
    private static final int TYPE_SUGGESTED_HASHTAG = 33;
    private static final int TYPE_SUGGESTED_USER = 34;
    private static final int TYPE_TRENDING_NOW = 35;
    private static final int TYPE_NO_MORE_STORY = 36;
    private static final int TYPE_STORY_ALTERNATE = 37;
    private static final int TYPE_LIST_DIVIDER = 38;
    private static final int TYPE_STORY_PRODUCTS_BLOCK = 39;
    private static final int LIKE_STORY_BLOCK = 40;
    private static final int FEED_SUGGESTIONS = 41;
    private static final int LOAD_HISTORY = 42;
    private static final int BOOKMARK_LIST = 43;
    private static final int TYPE_USER_FOLLOW_WITH_OPTIONS = 44;
    private static final int FOLLOW_USER_GROUP = 45;
    private static final int TYPE_TRENDING_STORIES_RELATING_HASHTAG = 46;
    private static final int TYPE_NO_STORY_CARD = 47;
    private static final int FEED_HASHTAG_BULK_SUGGESTION = 48;
    private static final int TYPE_TRENDING_STORY = 49;
    private static final int TYPE_FEEDBACK = 50;
    public static final int TYPE_GO_SOCIAL = 51;
    private static final int TYPE_SIMILAR_PRODUCT = 52;
    private static final int TYPE_UPDATE_APP = 53;
    public static final int TYPE_FIND_FRIENDS = 54;
    public static final int TYPE_GENERIC_CARD = 55;
    private static final int TYPE_BANNER_PAGER = 56;
    private static final int TYPE_ADD_INTEREST = 57;
    private static final int TYPE_SHOP_CATEGORIES_CARD = 58;
    private static final int TYPE_ASK_CREATE_STORY = 59;
    private static final int TYPE_STORY_CREATE_FEED_CARD = 60;
    private static final int TYPE_STORY_VIEWS_COUNT = 61;
    private static final int TYPE_CAROUSEL = 62;
    private static final int TYPE_CONNECT_FB_CARD = 63;
    private static final int FEED_SUGGESTIONS_USER_SPEC = 64;
    private static final int TERMS_AND_CONDITIONS_CARD = 65;
    private static final int TYPE_BANNER_ADV = 66;
    private static final int TYPE_CONNECT_FACEBOOK_FRIENDS = 67;
    private static final int TYPE_EXPLORE_FEED_CARD = 68;
    private static final int EXPLO_CAT_PAR = 69;
    private static final int TYPE_FESTIVE_CARD_VIEW = 70;
    private static final int TYPE_SEARCH_SUGGESTED_USERS = 71;
    private static final int TYPE_SEARCH_SUGGESTED_POSTS = 72;
    private static final int TYPE_SEARCH_SUGGESTED_PRODUCTS = 73;
    private static final int TYPE_TREND_NEW = 74;
    private static final int TYPE_CUSTOM_CARD_VIEW = 75;

    private static final ArrayMap<String, Integer> typeArray = new ArrayMap<>();

    /**
     * Type array initialization
     * If you want to add any card, add the type here and Type value above
     */
    static {
        typeArray.put("pb", TYPE_BLOCK_0);
        typeArray.put("st", TYPE_STORY);
        typeArray.put("sb", TYPE_STORY);
        typeArray.put("uasb", TYPE_STORY);
        typeArray.put("rpsb", TYPE_STORY);
        typeArray.put("lb", TYPE_LIST);
        typeArray.put("ualb", TYPE_LIST);
        typeArray.put("clr", TYPE_COLOUR);
        typeArray.put("sor", TYPE_SORT);
        typeArray.put("tr", TYPE_TREND);
        typeArray.put("ub", TYPE_USER);
        typeArray.put("uaub", TYPE_USER);
        typeArray.put("comb", TYPE_COMMENT);
        typeArray.put("dpb", TYPE_DISCOUNT);
        typeArray.put("uapb", TYPE_DISCOUNT);
        typeArray.put("not", TYPE_NOTIFICATION);
        typeArray.put("mainsb", TYPE_MAINSTORY);
        typeArray.put("banner", TYPE_BANNER);
        typeArray.put("ncs", TYPE_NEW_CREATE_STORY);
        typeArray.put("cs", TYPE_SELECT_COUPON);
        typeArray.put("cr", TYPE_REDEEMED_COUPON);
        typeArray.put("mainlist", TYPE_MAINLIST);
        typeArray.put("drawerItem", TYPE_DRAWER_ITEM);
        typeArray.put("drawerTopItem", TYPE_DRAWER_TOP_ITEM);
        typeArray.put("horizontalLine", TYPE_LINE_HORIZONTAL);
        typeArray.put("vb", TYPE_VENDOR_CONTACT);
        typeArray.put("vc", TYPE_VENDOR_CONTACT);
        typeArray.put("stb", TYPE_SEARCH_TEXT_TITLE);
        typeArray.put("srb", TYPE_SEARCH_RESULT_TEXT);
        typeArray.put("stm", TYPE_STORY_SEARCH_RESULT);
        typeArray.put("pbm", TYPE_PRODUCT_SEARCH_RESULT);
        typeArray.put("plm", TYPE_PRODUCT_LIST_SEARCH_RESULT);
        typeArray.put("smb", TYPE_SHOW_MORE);
        typeArray.put("header_card", TYPE_HEADER_CARD);
        typeArray.put("tc", TYPE_TIP_CARD);
        typeArray.put("usb", TYPE_USER_FOLLOW);
        typeArray.put("fbinv", TYPE_INVITE_FRIEND);
        typeArray.put("ptt", TYPE_PEOPLE_TEXT_TITLE);
        typeArray.put("sab", TYPE_STORY_ALTERNATE);
        typeArray.put("divb", TYPE_LIST_DIVIDER);
        typeArray.put("spb", TYPE_STORY_PRODUCTS_BLOCK);
        typeArray.put("sg_hb", TYPE_SUGGESTED_HASHTAG);
        typeArray.put("sg_ub", TYPE_SUGGESTED_USER);
        typeArray.put("trn", TYPE_TRENDING_NOW);
        typeArray.put("nms", TYPE_NO_MORE_STORY);
        typeArray.put("intro1", LIKE_STORY_BLOCK);
        typeArray.put("fsug", FEED_SUGGESTIONS);
        typeArray.put("lhb", LOAD_HISTORY);
        typeArray.put("bmb", BOOKMARK_LIST);
        typeArray.put("uabmb", BOOKMARK_LIST);
        typeArray.put("rpbmb", BOOKMARK_LIST);
        typeArray.put("usbo", TYPE_USER_FOLLOW_WITH_OPTIONS);
        typeArray.put("fbs", FOLLOW_USER_GROUP);
        typeArray.put("fhss", TYPE_TRENDING_STORIES_RELATING_HASHTAG);
        typeArray.put("nmsu", TYPE_NO_STORY_CARD);
        typeArray.put("fhbs", FEED_HASHTAG_BULK_SUGGESTION);
        typeArray.put("shtrb", TYPE_TRENDING_STORY);
        typeArray.put("fdb", TYPE_FEEDBACK);
        typeArray.put("goscl", TYPE_GO_SOCIAL);
        typeArray.put("smp", TYPE_SIMILAR_PRODUCT);
        typeArray.put("uac", TYPE_UPDATE_APP);
        typeArray.put("iia", TYPE_FIND_FRIENDS);
        typeArray.put("syncfb", TYPE_CONNECT_FB_CARD);
        typeArray.put("gencrd", TYPE_GENERIC_CARD);
        typeArray.put("introT", TYPE_BANNER_PAGER);
        typeArray.put("addInt", TYPE_ADD_INTEREST);
        typeArray.put("spct", TYPE_SHOP_CATEGORIES_CARD);
        typeArray.put("fpcp", TYPE_ASK_CREATE_STORY);
        typeArray.put("fpcf", TYPE_STORY_CREATE_FEED_CARD);
        typeArray.put("svcb", TYPE_STORY_VIEWS_COUNT);
        typeArray.put("ftsv2", TYPE_CAROUSEL);
        typeArray.put("gen2b", TERMS_AND_CONDITIONS_CARD);
        typeArray.put("fsus", FEED_SUGGESTIONS_USER_SPEC);
        typeArray.put("st_atc", TYPE_STORY);
        typeArray.put("badv", TYPE_BANNER_ADV);
        typeArray.put("cwff", TYPE_CONNECT_FACEBOOK_FRIENDS);
        typeArray.put("efc", TYPE_EXPLORE_FEED_CARD);
        typeArray.put("xpl", EXPLO_CAT_PAR);
        typeArray.put("fcv", TYPE_FESTIVE_CARD_VIEW);
        typeArray.put("ssgu", TYPE_SEARCH_SUGGESTED_USERS);
        typeArray.put("ssgs", TYPE_SEARCH_SUGGESTED_POSTS);
        typeArray.put("ssgp", TYPE_SEARCH_SUGGESTED_PRODUCTS);
        typeArray.put("tsf", TYPE_TREND_NEW);
        typeArray.put("tsf", TYPE_TREND_NEW);
        typeArray.put("awdgencrd", TYPE_CUSTOM_CARD_VIEW);  // the name of type could have a been a lot more generic
    }


    public static int getTypeValue(String type) {
        if (typeArray.containsKey(type))
            return typeArray.get(type);
        return DataModels.INVALID;
    }

}
