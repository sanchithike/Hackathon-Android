package com.roposo.creation.graphics;

/**
 * Created by tajveer on 12/22/17.
 */

import android.util.Log;
import android.util.LruCache;

import com.roposo.creation.graphics.gles.FBObject;
import com.roposo.creation.graphics.gles.GraphicsUtils;

public class FBCache {
    String mLock;
    static String TAG;

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    private LruFBCache mCache;

    int mSize;

    public FBCache() {
        if (mCache == null)
            mCache = new LruFBCache(16);
    }

    public FBObject get(int handle) {
        return mCache.get(handle);
    }

    public void put(int handle, FBObject fbObject) {
        mCache.put(handle, fbObject);
    }

    public void clear() {
        mCache.evictAll();
    }

    private static class LruFBCache extends LruCache<Integer, FBObject> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruFBCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Integer handle, FBObject fbObject, FBObject newFBObject) {
            if (VERBOSE)
                Log.d(TAG, "Evicting FBObject " + fbObject + "  handle: " + handle);
            fbObject.cleanup();
        }
    }
}
