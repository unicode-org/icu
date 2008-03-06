/*
 ***************************************************************************
 * Copyright (c) 2007-2008 International Business Machines Corporation and *
 * others.  All rights reserved.                                           *
 ***************************************************************************
*/

package com.ibm.icu.impl;

public interface ICUCache {
    // Type of reference holding the Map instance
    public static final int SOFT = 0;
    public static final int WEAK = 1;

    // NULL object, which may be used for a cache key
    public static final Object NULL = new Object();

    public void clear();
    public void put(Object key, Object value);
    public Object get(Object key);
}
