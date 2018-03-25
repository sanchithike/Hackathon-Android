package com.roposo.core.events;

import android.support.annotation.UiThread;
import android.util.Log;
import android.util.SparseArray;

import com.roposo.core.util.AndroidUtilities;
import com.roposo.core.util.BuildVars;
import com.roposo.core.util.ContextHelper;
import com.roposo.core.util.EventTrackUtil;

import java.util.ArrayList;

/**
 * Created by anilshar on 1/13/16.
 */


/**
 * Event manager will be responsible for register/unregister and post events
 * <p>
 * To register the event add an eventId, call addObserver(eventId, observer) and implement EventManagerDelegate
 * <p>
 * 1. You can only call addObserver(), removeObserver and postEventName from main thread
 */

public class EventManager {
    private static volatile EventManager Instance = null;
    private SparseArray<ArrayList<EventManagerDelegate>> observers = new SparseArray<>();
    private SparseArray<ArrayList<EventManagerDelegate>> removeAfterBroadcast = new SparseArray<>();
    private SparseArray<ArrayList<EventManagerDelegate>> addAfterBroadcast = new SparseArray<>();
    private ArrayList<DelayedPost> delayedPosts = new ArrayList<>(10);
    private int broadcasting = 0;
    private boolean animationInProgress;

    public static EventManager getInstance() {
        EventManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (EventManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new EventManager();
                }
            }
        }
        return localInstance;
    }

    /*
     * Post the delayed events when the animation has finished
     */
    public void setAnimationInProgress(boolean value) {
        animationInProgress = value;
        if (!animationInProgress && !delayedPosts.isEmpty()) {
            for (DelayedPost delayedPost : delayedPosts) {
                postEventNameInternal(delayedPost.id, true, delayedPost.args);
            }
            delayedPosts.clear();
        }
    }

    public void postEventName(final int id, final Object... args) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                boolean allowDuringAnimation = eventDispatchAllowedDuringAnimation(id);
                postEventNameInternal(id, allowDuringAnimation, args);
            }
        });
    }

    private boolean eventDispatchAllowedDuringAnimation(int id) {

        return true;
    }

    public void postEventNameInternal(int id, boolean allowDuringAnimation, Object... args) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ContextHelper.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("postEventName allowed only from MAIN thread");
            }
        }
        // When event is not allowed during animation store it as a delayed post
        if (!allowDuringAnimation && animationInProgress) {
            DelayedPost delayedPost = new DelayedPost(id, args);
            delayedPosts.add(delayedPost);
            if (BuildVars.DEBUG_VERSION) {
                Log.e("roposo_msg", "delay post Event " + id + " with args count = " + args.length);
            }
            return;
        }
        broadcasting++;
        ArrayList<EventManagerDelegate> objects = observers.get(id);
        if (objects != null && !objects.isEmpty()) {
            for (int a = 0; a < objects.size(); a++) {
                Object obj = objects.get(a);
                if (obj != null) {
                    boolean shouldTerminate = ((EventManagerDelegate) obj).didReceivedEvent(id, args);
                    if (shouldTerminate) {
                        break;
                    }
                }
            }
        }
        broadcasting--;
        if (broadcasting == 0) {
            if (removeAfterBroadcast.size() != 0) {
                for (int a = 0; a < removeAfterBroadcast.size(); a++) {
                    int key = removeAfterBroadcast.keyAt(a);
                    ArrayList<EventManagerDelegate> arrayList = removeAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        removeObserver(arrayList.get(b), key);
                    }
                }
                removeAfterBroadcast.clear();
            }
            if (addAfterBroadcast.size() != 0) {
                for (int a = 0; a < addAfterBroadcast.size(); a++) {
                    int key = addAfterBroadcast.keyAt(a);
                    ArrayList<EventManagerDelegate> arrayList = addAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        addObserver(arrayList.get(b), key);
                    }
                }
                addAfterBroadcast.clear();
            }
        }
    }

    /*
     * Add an eventId which you don't want to be dispatched immediately when the view observing the
     * event in under animation.
     */
    @UiThread
    public void addObserver(EventManagerDelegate observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ContextHelper.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("addObserver allowed only from MAIN thread");
            }
        }
        if (observer == null) {
            EventTrackUtil.logDebug("EventManager", "object_null", EventManager.class.getName(), null, 4);
            return;
        }
        if (broadcasting != 0) {
            ArrayList<EventManagerDelegate> arrayList = addAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                addAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<EventManagerDelegate> objects = observers.get(id);
        if (objects == null) {
            observers.put(id, (objects = new ArrayList<>()));
        }
        if (objects.contains(observer)) {
            return;
        }
        objects.add(observer);
    }

    public void removeObserver(EventManagerDelegate observer, int id) {
        if (BuildVars.DEBUG_VERSION) {
            if (Thread.currentThread() != ContextHelper.applicationHandler.getLooper().getThread()) {
                throw new RuntimeException("removeObserver allowed only from MAIN thread");
            }
        }
        if (broadcasting != 0) {
            ArrayList<EventManagerDelegate> arrayList = removeAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                removeAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<EventManagerDelegate> objects = observers.get(id);
        if (objects != null) {
            objects.remove(observer);
        }
    }

    public boolean isObserversAddedForThisEvent(int eventId) {
        if (observers != null && observers.get(eventId) != null
                && observers.get(eventId).size() > 0) {
            return true;
        }
        return false;
    }

    /*
     *  Always remember to removeObserver when you have added corresponding observer.
     *  Example usage :: addObserver() in createView/constructor and removeObserver in removeView/destructor
     */

    public interface EventManagerDelegate {
        // return true when you don't want to further dispatch the event to other observer after it is handled by some observer.
        // Most of the time you would like to return false. :)
        boolean didReceivedEvent(int id, Object... args);
    }

    private class DelayedPost {

        private int id;
        private Object[] args;

        private DelayedPost(int id, Object[] args) {
            this.id = id;
            this.args = args;
        }
    }

}
