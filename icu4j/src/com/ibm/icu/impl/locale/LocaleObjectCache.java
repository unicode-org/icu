/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//public class LocaleObjectCache<K, V> {
public class LocaleObjectCache {

//    private ConcurrentHashMap<K, WeakValueRef<V>> _map = new ConcurrentHashMap<K, WeakValueRef<V>>();
    private Map _map = Collections.synchronizedMap(new HashMap());
//    private ReferenceQueue<V> _rq = new ReferenceQueue<V>();
    private ReferenceQueue _rq = new ReferenceQueue();

    public LocaleObjectCache() {
    }

//    public V get(Object key) {
    public Object get(Object key) {
        expungeStaleEntries();
//        WeakValueRef<V> ref = _map.get(key);
        WeakValueRef ref = (WeakValueRef)_map.get(key);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    /*
     * Unlike Map#put, this method returns non-null value actually
     * in the cache, even no values for the key was not available
     * before.
     */
//    public V put(K key, V value) {
    public Object put(Object key, Object value) {
        expungeStaleEntries();
//        WeakValueRef<V> ref = _map.get(key);
        WeakValueRef ref = (WeakValueRef)_map.get(key);
        if (ref != null) {
            // Make sure if another thread put the new value
//            V valInCache = ref.get();
            Object valInCache = (String)ref.get();
            if (valInCache != null) {
                return valInCache;
            }
        }
        // We do not synchronize the internal map here.
        // In the worst case, another thread may put the new
        // value with the same contents, but it should not cause
        // any serious problem.
//        _map.put(key, new WeakValueRef<V>(key, value, _rq));
        _map.put(key, new WeakValueRef(key, value, _rq));
        return value;
    }

    private void expungeStaleEntries() {
//        Reference<? extends V> val;
        Reference val;
        while ((val = _rq.poll()) != null) {
//            Object key = ((WeakValueRef<?>)val).getKey();
            Object key = ((WeakValueRef)val).getKey();
            _map.remove(key);
        }
    }

//    private static class WeakValueRef<V> extends WeakReference<V> {
    private static class WeakValueRef extends WeakReference {
        private Object _key;

//        public WeakValueRef(Object key, V value, ReferenceQueue<V> rq) {
        public WeakValueRef(Object key, Object value, ReferenceQueue rq) {
            super(value, rq);
            _key = key;
        }

//        public V get() {
        public Object get() {
            return super.get();
        }

        public Object getKey() {
            return _key;
        }
    }
}
