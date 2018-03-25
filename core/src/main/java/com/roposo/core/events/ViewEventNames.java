package com.roposo.core.events;


/**
 * Created by anilshar on 11/2/15.
 */
public class ViewEventNames {

    public static final String ON_DETACHED_EVENT = "onde";
    public static final String EMPTY_LIST_CHANGE_EVENT = "em";
    public static final String RELOAD_CALLBACK = "recall";
    public static final String ON_ITEM_REMOVED = "rm";
    public static final String ON_ITEM_CLICKED = "clk";
    public static final String ON_ITEM_DOUBLE_CLICKED = "dbl_clk";
    public static final String ON_ITEM_LONG_CLICKED = "lng_clk";
    public static final String ON_ITEMS_SWAPPED = "swap";
    public static final String SCROLL_VIEW_EVENT = "sc";
    public static final String ADD_ITEM_EVENT = "add";
    public static final String CHANGE_ITEM_EVENT = "ch";

    public static final String COMMENT_ITEM_CLICKED = "com_click";
    public static final String COMMENT_REPLY_ITEM_CLICKED = "comr_click";
    public static final String COLLAPSED_DATA_ITEM_CLICKED = "col_click";
    public static final String DATA_RECEIVED = "drd";
    public static final String DATA_QUERY_DONE = "dnndnd";
    public static final String ON_SCREENSHOT_TAKEN = "screenshot";
    public static final String ON_CHANNEL_SELECT = "on_channel_select";
    public static final String ON_LANGUAGE_CHANGE = "olch";
    public static final String ON_PARTICLE_EFFECT_SELECTED = "onparticle_select";



    private ViewEventNames() {
        throw new UnsupportedOperationException("Non instantiable class");
    }
}
