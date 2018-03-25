package com.roposo.core.constants;

import java.util.HashMap;

/**
 * @author muddassir on 10/20/16.
 *         Product features to be used in posting story as product
 */

public enum ProductFeatures {
    CHAT_N_BUY("CHAT_N_BUY"),
    ASK_PRICE("ASK_PRICE"),

    SHOP_NOW("SHOP_NOW"),
    BOOK_NOW("BOOK_NOW"),
    KNOW_MORE_SELL("KNOW_MORE_SELL"),


    VISIT_BLOG("BOOK_DIRECT"),
    KNOW_MORE_BLOG("KNOW_MORE_BLOG"),
    CHAT_TO_BOOK("CHAT_TO_BOOK");


    String displayName;

    ProductFeatures(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }


    private static HashMap<ProductFeatures, String> getButtonList() {
        HashMap<ProductFeatures, String> buttonTitleMap = new HashMap<>();
        buttonTitleMap.put(CHAT_N_BUY, "Chat to buy");
        buttonTitleMap.put(ASK_PRICE, "Ask price");
        buttonTitleMap.put(SHOP_NOW, "Shop now");
        buttonTitleMap.put(BOOK_NOW, "Book now");
        buttonTitleMap.put(KNOW_MORE_SELL, "Know more");
        buttonTitleMap.put(VISIT_BLOG, "Visit blog");
        buttonTitleMap.put(KNOW_MORE_BLOG, "Know more");
        buttonTitleMap.put(CHAT_TO_BOOK, "Chat to book");
        return buttonTitleMap;
    }

    public static String getButtonTitles(ProductFeatures productFeatures) {
        return getButtonList().get(productFeatures);
    }


}
