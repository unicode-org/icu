/*
 * *****************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and others.
 * All Rights Reserved.
 * *****************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * LRU hash map using softly-referenced values
 */
public class SoftCache {
    private final LRUMap map;
    private final ReferenceQueue queue = new ReferenceQueue();

    /**
     * Construct a SoftCache with default cache size
     */
    public SoftCache() {
        map = new LRUMap();
    }
    
    /**
     * Construct a SoftCache with sepcified initial/max size
     * 
     * @param initialSize the initial cache size
     * @param maxSize the maximum cache size
     */
    public SoftCache(int initialSize, int maxSize) {
        map = new LRUMap(initialSize, maxSize);
    }

    /**
     * Put an object to the cache
     * @param key key object
     * @param value value object
     * @return the value previously put, null when not matching key is found.
     */
    public synchronized Object put(Object key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        ProcessQueue();
        Object obj = map.put(key, new SoftMapEntry(key, value, queue));
        return obj;
    }

    /**
     * Get an object from the cache
     * @param key key object
     * @return the cached value, null when the value is not found.
     */
    public synchronized Object get(Object key) {
        ProcessQueue();
        Object obj = null;
        SoftMapEntry entry = (SoftMapEntry)map.get(key);
        if (entry != null) {
            obj = entry.get();
            if (obj == null) {
                // It is unlikely to enter into this block, because
                // ProcessQueue() should already remove a map entrie
                // whose value was deleted by the garbage collactor.
                map.remove(key);
            }
        }
        return obj;
    }

    /**
     * Remove a cache entry from the cache
     * @param key key object
     * @return the value of cache entry which is removed from this cache,
     * or null when no entry for the key was no found.
     */
    public synchronized Object remove(Object key) {
        return map.remove(key);
    }

    /**
     * Clear the cache contents
     */
    public synchronized void clear() {
        ProcessQueue();
        map.clear();
    }

    /**
     * Remove map entries which no longer have value
     */
    private void ProcessQueue() {
        while (true) {
            SoftMapEntry entry = (SoftMapEntry)queue.poll();
            if (entry == null) {
                break;
            }
            map.remove(entry.getKey());
        }
    }

    /**
     * A class for map entry with soft-referenced value
     */
    private static class SoftMapEntry extends SoftReference {
        private final Object key;

        private SoftMapEntry(Object key, Object value, ReferenceQueue queue) {
            super(value, queue);
            this.key = key;
        }

        private Object getKey() {
            return key;
        }
    }
}