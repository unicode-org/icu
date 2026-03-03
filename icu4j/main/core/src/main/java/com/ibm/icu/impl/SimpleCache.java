// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ****************************************************************************
 * Copyright (c) 2007-2015 International Business Machines Corporation and  *
 * others.  All rights reserved.                                            *
 ****************************************************************************
 */

package com.ibm.icu.impl;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleCache<K, V> implements ICUCache<K, V> {
    private static final int DEFAULT_CAPACITY = 16;

    private volatile Reference<Map<K, V>> cacheRef = null;
    private final int type;
    private final int capacity;
    private final Object lock = new Object();

    public SimpleCache() {
        this(ICUCache.SOFT, DEFAULT_CAPACITY);
    }

    public SimpleCache(int cacheType) {
        this(cacheType, DEFAULT_CAPACITY);
    }

    public SimpleCache(int cacheType, int initialCapacity) {
        if (cacheType == ICUCache.WEAK) {
            type = cacheType;
        } else {
            type = ICUCache.SOFT;
        }
        if (initialCapacity > 0) {
            capacity = initialCapacity;
        } else {
            capacity = DEFAULT_CAPACITY;
        }
    }

    @Override
    public V get(Object key) {
        Reference<Map<K, V>> ref = cacheRef;
        if (ref != null) {
            Map<K, V> map = ref.get();
            if (map != null) {
                return map.get(key);
            }
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        Reference<Map<K, V>> ref = cacheRef;
        Map<K, V> map = null;
        if (ref != null) {
            map = ref.get();
        }
        if (map == null) {
            synchronized (lock) {
                ref = cacheRef;
                if (ref != null) {
                    map = ref.get();
                }
                if (map == null) {
                    map = new ConcurrentHashMap<K, V>(capacity);
                    if (type == ICUCache.WEAK) {
                        ref = new WeakReference<Map<K, V>>(map);
                    } else {
                        ref = new SoftReference<Map<K, V>>(map);
                    }
                    cacheRef = ref;
                }
            }
        }
        // NOTE: There is a benign race here where clear() could be called
        // by another thread after we've fetched the map but before we call put().
        // In such a case, we insert into the "dead" map, which is acceptable
        // for a soft/weak cache as the entry will eventually be GC'd.
        map.put(key, value);
    }

    @Override
    public void clear() {
        cacheRef = null;
    }
}
