package com.roposo.core.listeners;

import org.json.JSONObject;

/**
 * Created by Amud on 18/11/16.
 */
public interface RequestLifeCycle {
    void onStart(String url);

    void onFailed(String url);

    void onSuccess(String url);

    void onReload(String mURL);
}
