/*
 * *****************************************************************************
 * Copyright (C) 2006-2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * *****************************************************************************
 */
package com.ibm.icu.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * JDK1.4 LinkedHashMap equivalent implementation for
 * Java foundation profile support.  This class is used
 * by <code>com.ibm.icu.impl.LRUMap</code> on eclipse
 * distributions, which require JDK1.3/Java Foundation
 * profile support.
 */
public class LinkedHashMap extends HashMap {
    private static final long serialVersionUID = -2497823480436618075L;

    private boolean accessOrder = false;
    private LinkedList keyList = new LinkedList();

    public LinkedHashMap() {
        super();
    }

    /*public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }
    
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }*/

    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

    public LinkedHashMap(Map m) {
        super();
        putAll(m);
    }

    public void clear() {
        super.clear();
        keyList.clear();
    }

    public Object remove(Object key) {
        Object value = super.remove(key);
        if (value == null) {
            // null might be returned for a map entry
            // for null value.  So we need to check if
            // the key is actually available or not.
            // If the key list contains the key, then
            // remove the key from the list.
            int index = getKeyIndex(key);
            if (index >= 0) {
                keyList.remove(index);
            }
        }
        return value;
    }
    
    public Object get(Object key) {
        Object value = super.get(key);
        if (accessOrder) {
            // When accessOrder is true, move the key
            // to the end of the list
            int index = getKeyIndex(key);
            if (index >= 0) {
                if (index != keyList.size() - 1) {
                    keyList.remove(index);
                    keyList.addLast(key);
                }
            }
        }
        return value;
    }

    public void putAll(Map m) {
        Set keySet = m.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            Object value = m.get(key);
            put(key, value);
        }
    }

    public Object put(Object key, Object value) {
        Object oldValue = super.put(key, value);

        // Move the key to the end of key list
        // if it exists.  If not, append the key
        // to the end of key list
        int index = getKeyIndex(key);
        if (index >= 0) {
            if (index != keyList.size() - 1) {
                keyList.remove(index);
                keyList.addLast(key);
            }
        }
        else {
            keyList.addLast(key);
        }

        // Check if we need to remove the eldest
        // entry.
        Object eldestKey = keyList.getFirst();
        Object eldestValue = super.get(eldestKey);
        MapEntry entry = new MapEntry(eldestKey, eldestValue);
        if (removeEldestEntry(entry)) {
            keyList.removeFirst();
            super.remove(eldestKey);
        }

        return oldValue;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        return false;
    }

    private int getKeyIndex(Object key) {
        int index = -1;
        for (int i = 0; i < keyList.size(); i++) {
            Object o = keyList.get(i);
            if (o.equals(key)) {
                index = i;
                break;
            }
        }
        return index;
    }

    protected static class MapEntry implements Map.Entry {
        private Object key;
        private Object value;

        private MapEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public boolean equals(Object o) {
            Object otherKey = ((Map.Entry)o).getKey();
            Object otherValue = ((Map.Entry)o).getValue();
            if (key.equals(otherKey) && value.equals(otherValue)) {
                return true;
            }
            return false;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }
}
