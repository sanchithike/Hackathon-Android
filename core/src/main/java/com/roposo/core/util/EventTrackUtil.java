package com.roposo.core.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.roposo.core.customInjections.EventTrackWrapper;
import com.roposo.core.customInjections.EventTrackerWrapper;
import com.roposo.core.events.RoposoEventMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anil Sharma on 23/02/17.
 */

public class EventTrackUtil {

    public static final String DEBUG = "AppDebug";

    public static void logDebug(@NonNull String tag, @NonNull String loe, @NonNull String className,
                                @Nullable Map<String, String> params, int severityLevel) {

        if(null == params) {
            params = new HashMap<>();
        }

        params.put("tag", tag);
        params.put("LOE", loe);
        params.put("className", className);
        params.put("sevLevel", String.valueOf(severityLevel));
        EventTrackWrapper.track(DEBUG, params);
        RoposoEventMap map = new RoposoEventMap();
        for (String s : params.keySet()) {
            map.put(s, params.get(s));
        }
        EventTrackerWrapper.trackEvent(DEBUG, map);
    }

}
