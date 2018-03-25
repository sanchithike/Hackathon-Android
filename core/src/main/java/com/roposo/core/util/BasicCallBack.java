package com.roposo.core.util;


/**
 * Created by avinash on 28/07/14.
 */
public interface BasicCallBack {
    public static enum CallBackSuccessCode {
        FAIL, SUCCESS, LOGINFAIL, ALREADYPRESENT, HOLD, // HOLD - request has been failed due to network connectivity: user can reload the result
        EDIT_STORY, CANCEL, DELETE_STORY
    }
    void callBack(CallBackSuccessCode code, Object data);
}
