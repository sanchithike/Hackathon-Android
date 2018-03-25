package com.roposo.creation.listeners;

/**
 * Created by bajaj on 14/10/16.
 */

public interface StickerEditCallback  {
    public static enum StickerEditCodes {
        ADD, DELETE, MOVE, FINGERDOWN, FINGERUP, CLICK, TEXTUPDATE
    }
    void callBack(StickerEditCodes code, Object data);
}
