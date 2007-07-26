package com.ibm.icu.impl;

public interface ICUCache {
    public static final Object NULL = new Object();
    public void clear();
    public void put(Object key, Object value);
    public Object get(Object key);
}
