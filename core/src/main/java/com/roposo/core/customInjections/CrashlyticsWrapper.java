package com.roposo.core.customInjections;

import com.roposo.core.util.MyLogger;

/**
 * @author : anilshar on 11/10/16.
 */

public class CrashlyticsWrapper {
    public static void setCrashlyticsImpl(CrashlyticsImp crashlyticsImpl) {
        CrashlyticsWrapper.crashlyticsImpl = crashlyticsImpl;
    }

    public static void logException(Throwable throwable) {
        crashlyticsImpl.logException(throwable);
    }

    public static void log(String msg) {
        crashlyticsImpl.log(msg);
    }

    public static void log(int priority, String tag, String msg) {
        crashlyticsImpl.log(priority, tag, msg);
    }




    public static CrashlyticsImp crashlyticsImpl = new CrashlyticsImp() {
        @Override
        public void logException(Throwable throwable) {
            MyLogger.e("CrashlyticsWrapper",throwable.toString());
        }

        @Override
        public void log(String msg) {
            MyLogger.e("CrashlyticsWrapper",msg);
        }

        @Override
        public void log(int priority, String tag, String msg) {
            MyLogger.e("CrashlyticsLog",msg);
        }

    };


    public interface CrashlyticsImp {
        void logException(Throwable throwable);
        void log(String msg);
        void log(int priority, String tag, String msg);
    }
}
