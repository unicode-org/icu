/*
 ****************************************************************************
 * Copyright (c) 2007 International Business Machines Corporation and others.
 * All rights reserved.
 ****************************************************************************
 */

package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleCache implements ICUCache {

    public Object get(Object key) {
        SoftReference ref = cacheRef;
        if (ref != null) {
            Map map = (Map)ref.get();
            if (map != null) {
                return map.get(key);
            }
        }
        return null;
    }

    public void put(Object key, Object value) {
        SoftReference ref = cacheRef;
        Map map = null;
        if (ref != null) {
            map = (Map)ref.get();
        }
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap());
            ref = new SoftReference(map);
            cacheRef = ref;
        }
        map.put(key, value);
    }

    public void clear() {
        cacheRef = null;
    }

    private SoftReference cacheRef = null;
}
