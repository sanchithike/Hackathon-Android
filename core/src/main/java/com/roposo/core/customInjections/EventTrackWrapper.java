package com.roposo.core.customInjections;


import com.roposo.core.util.MyLogger;

import java.util.Map;

/**
 * Created by avinashsaxena on 10/12/16.
 */

public class EventTrackWrapper {


    public static void setEventTrackImpl(EventTrackWrapper.EventTrackImp EventTrackImpl) {
        EventTrackWrapper.EventTrackImpl = EventTrackImpl;
    }

    public static void track(String eventName, Map<String, String> paramMap) {
        EventTrackImpl.track(eventName, paramMap);
    }

    public void log(String msg) {
    }

    public static void track(String eventName) {
        EventTrackImpl.track(eventName);
    }

    public static void pageView(String pageName) {
        EventTrackImpl.pageView(pageName);
    }

    public static void trackWithSource(String eventName, Map<String, String> params) {
        EventTrackImpl.trackWithSource(eventName, params);
    }

    public static EventTrackWrapper.EventTrackImp EventTrackImpl = new EventTrackWrapper.EventTrackImp() {
        @Override
        public void track(String eventName, Map<String, String> paramMap) {
            MyLogger.d("EventTrackWrapper", eventName);
        }

        public void pageView(String pageName) {
            MyLogger.d("EventTrackWrapper", pageName);
        }

        public void track(String eventName) {
            MyLogger.d("EventTrackWrapper", eventName);
        }

        public void trackWithSource(String eventName, Map<String, String> params) {
            MyLogger.d("EventTrackWrapper", eventName);
        }

    };


    public interface EventTrackImp {
        void track(String eventName, Map<String, String> paramMap);

        void pageView(String pageName);

        void track(String eventName);

        void trackWithSource(String eventName, Map<String, String> params);
    }

}
