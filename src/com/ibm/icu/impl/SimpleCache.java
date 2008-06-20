/*
 ****************************************************************************
 * Copyright (c) 2007-2008 International Business Machines Corporation and  *
 * others.  All rights reserved.                                            *
 ****************************************************************************
 */

package com.ibm.icu.impl;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleCache implements ICUCache {
    private static final int DEFAULT_CAPACITY = 16;

    private Reference cacheRef = null;
    private int type = ICUCache.SOFT;
    private int capacity = DEFAULT_CAPACITY;

    public SimpleCache() {
    }

    public SimpleCache(int cacheType) {
        this(cacheType, DEFAULT_CAPACITY);
    }

    public SimpleCache(int cacheType, int initialCapacity) {
        if (cacheType == ICUCache.WEAK) {
            type = cacheType;
        }
        if (initialCapacity > 0) {
            capacity = initialCapacity;
        }
    }

    public Object get(Object key) {
        Reference ref = cacheRef;
        if (ref != null) {
            Map map = (Map)ref.get();
            if (map != null) {
                return map.get(key);
            }
        }
        return null;
    }

    public void put(Object key, Object value) {
        Reference ref = cacheRef;
        Map map = null;
        if (ref != null) {
            map = (Map)ref.get();
        }
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap(capacity));
            if (type == ICUCache.WEAK) {
                ref = new WeakReference(map);
            } else {
                ref = new SoftReference(map);
            }
            cacheRef = ref;
        }
        map.put(key, value);
    }

    public void clear() {
        cacheRef = null;
    }

}
