package com.roposo.core.customInjections;

import com.roposo.core.events.RoposoEventMap;

/**
 * Created by amud on 13/09/17.
 */

public abstract class EventTrackerWrapper {

    public static void setEventTrackerImpl(EventTrackerImpl eventTrackerImpl) {
        EventTrackerWrapper.eventTrackerImpl = eventTrackerImpl;
    }

    private static EventTrackerImpl eventTrackerImpl;

    static {
        eventTrackerImpl = new EventTrackerImpl() {
            @Override
            public void trackEvent(String eventName, RoposoEventMap attributes) {

            }
        };
    }

    public static void trackEvent(String eventName, RoposoEventMap attributes) {
        eventTrackerImpl.trackEvent(eventName, attributes);
    }


    public interface EventTrackerImpl {
        public void trackEvent(String eventName,RoposoEventMap attributes);
    }

}
