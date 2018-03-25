package com.roposo.creation.graphics.filters;

import android.util.Log;
import android.support.v4.util.LruCache;

import com.roposo.creation.graphics.gles.GraphicsUtils;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * @author bajaj on 17/12/17.
 */

public class FilterCache {
    String mLock = "FilterCache";
    static String TAG = "FilterCache";

    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    private LruFilterCache mCache;

    int mSize;

    public FilterCache() {
        if (mCache == null)
            mCache = new LruFilterCache(10);
    }

    public BaseFilter get(String description) {
        return mCache.get(description);
    }

    public void put(String description, BaseFilter filter) {
        mCache.put(description, filter);
    }

    public void clear() {
        mCache.evictAll();
    }

    private static class LruFilterCache extends LruCache<String, BaseFilter> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruFilterCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String description, BaseFilter filter, BaseFilter newFilter) {
            if (VERBOSE)
                Log.d(TAG, "Evicting Filter " + filter + " sourced from description: " + description);
            filter.cleanup();
        }
    }
}
