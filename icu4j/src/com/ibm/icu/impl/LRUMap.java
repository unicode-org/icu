/*
 * *****************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and others.
 * All Rights Reserved.
 * *****************************************************************************
 */
package com.ibm.icu.impl;

//#ifndef FOUNDATION
import java.util.LinkedHashMap;
//#endif
import java.util.Map;

/*
 * Simple LRU (Least Recent Used) Map implementation
 */
public class LRUMap extends LinkedHashMap {
    private static final long serialVersionUID = -8178106459089682120L;

    private static final int DEFAULT_MAXCAPACITY = 64;
    private static final int DEFAULT_INITIALCAPACITY = 16;
    private static final float DEFAULT_LOADFACTOR = 0.75F;

    private final int maxCapacity;

    /**
     * Construct a new LRU map with the default initial
     * capacity(16) and the maximum capacity(64).
     */
    public LRUMap() {
        super(DEFAULT_INITIALCAPACITY, DEFAULT_LOADFACTOR, true);
        maxCapacity = DEFAULT_MAXCAPACITY;
    }

    /**
     * Construct a new LRU map with the specified initial
     * capacity and the maximum capacity
     * 
     * @param initialCapacity initial capacity of the map
     * @param maxCapacity maximum capacity of the map
     */
    public LRUMap(int initialCapacity, int maxCapacity) {
        super(initialCapacity, DEFAULT_LOADFACTOR, true);
        this.maxCapacity = maxCapacity;
    }

    /*
     * Delete the eldest entry when the size exceeds the limit
     */
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return (size() > maxCapacity);
    }
}
