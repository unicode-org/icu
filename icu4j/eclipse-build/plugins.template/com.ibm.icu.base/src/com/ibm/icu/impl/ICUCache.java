// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ***************************************************************************
 * Copyright (c) 2007-2011 International Business Machines Corporation and *
 * others.  All rights reserved.                                           *
 ***************************************************************************
*/

package com.ibm.icu.impl;

public interface ICUCache<K, V> {
    // Type of reference holding the Map instance
    public static final int SOFT = 0;
    public static final int WEAK = 1;

    // NULL object, which may be used for a cache key
    public static final Object NULL = new Object();

    public void clear();
    public void put(K key, V value);
    public V get(Object key);
}
