package com.roposo.core.util;


import android.util.LruCache;

/**
 * Created by rahul on 11/16/14.
 */
public class UniversalLRUCache extends LruCache<String, String> {

    public UniversalLRUCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, String oldValue, String newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
    }
}
