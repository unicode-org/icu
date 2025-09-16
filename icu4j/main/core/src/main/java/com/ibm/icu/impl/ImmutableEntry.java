// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2009-2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Map;

/**
 * @author markdavis
 *
 */
public class ImmutableEntry<K,V> implements Map.Entry<K,V> {
    final K k;
    final V v;

    ImmutableEntry(K key, V value) {
        k = key;
        v = value;
    }

    @Override
    public K getKey()   {return k;}

    @Override
    public V getValue() {return v;}

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        try {
            Map.Entry e = (Map.Entry)o;
            return UnicodeMap.areEqual(e.getKey(), k) && UnicodeMap.areEqual(e.getValue(), v);
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ((k==null ? 0 : k.hashCode()) ^ (v==null ? 0 : v.hashCode()));
    }

    @Override
    public String toString() {
        return k+"="+v;
    }
}
