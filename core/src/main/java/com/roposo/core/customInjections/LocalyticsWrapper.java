package com.roposo.core.customInjections;

import android.content.Context;
import android.content.Intent;

import com.roposo.core.util.MyLogger;

import java.util.Map;

/**
 * @author : anilshar on 11/10/16.
 */

public abstract class LocalyticsWrapper {
    private static LocalyticsImpl localyticsImpl;

    static {
        localyticsImpl = new LocalyticsImpl() {
            @Override
            public void tagEvent(String eventName, Map<String, String> attributes) {
                if(null != eventName && null != attributes) {

                }
            }

            @Override
            public void tagEvent(String eventName) {
                MyLogger.d("LocalyticsWrapper", eventName);
            }

            @Override
            public void integrate(Context context) {

            }

            @Override
            public void onActivityResume(Context context) {

            }

            @Override
            public void onActivityPause(Context context) {

            }

            @Override
            public void onNewIntent(Context context, Intent intent) {

            }
        };
    }

    public static void tagEvent(String eventName, Map<String, String> attributes) {
        localyticsImpl.tagEvent(eventName, attributes);
    }

    public static void tagEvent(String eventName) {
        localyticsImpl.tagEvent(eventName);
    }

    public static void integrate(Context context) {
        localyticsImpl.integrate(context);
    }

    public static void onActivityResume(Context context) {
        localyticsImpl.onActivityResume(context);
    }

    public static void onActivityPause(Context context) {
        localyticsImpl.onActivityPause(context);
    }

    public static void onNewIntent(Context context, Intent intent) {
        localyticsImpl.onNewIntent(context,intent);
    }

    public static void setLocalyticsImpl(LocalyticsImpl impl) {
        localyticsImpl = impl;
    }


    public interface LocalyticsImpl {
        void tagEvent(String eventName, Map<String, String> attributes);
        void tagEvent(String eventName);
        void integrate(Context context);
        void onActivityResume(Context context);
        void onActivityPause(Context context);
        void onNewIntent(Context context,Intent intent);
    }
}
