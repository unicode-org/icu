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
//import java.util.concurrent.ConcurrentHashMap;

public class LocaleObjectPool/*<K,V>*/ {

    //private ConcurrentHashMap<K,ValueRef<V>> _map = new ConcurrentHashMap<K,ValueRef<V>>();
    private Map _map = Collections.synchronizedMap(new HashMap());
    private ReferenceQueue/*<V>*/ _rq = new ReferenceQueue/*<V>*/();

    public LocaleObjectPool() {
    }

    //public V get(Object key) {
    public Object get(Object key) {
        expungeStaleEntries();
        //ValueRef<V> ref = _map.get(key);
        ValueRef ref = (ValueRef)_map.get(key);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    //public V getPermanent(Object key) {
    public Object getPermanent(Object key) {
        expungeStaleEntries();
        //ValueRef<V> ref = _map.get(key);
        ValueRef ref = (ValueRef)_map.get(key);
        if (ref != null && ref instanceof StrongValueRef) {
            return ref.get();
        }
        return null;
    }

    /*
     * Unlike Map#put, this method returns non-null value actually
     * in the pool, even no values for the key was not available
     * before.
     */
    //public V register(K key, V value) {
    public Object register(Object key, Object value) {
        expungeStaleEntries();
        //V valInPool = value;
        Object valInPool = value;
        synchronized(_map) {
            //ValueRef<V> ref = _map.get(key);
            ValueRef ref = (ValueRef)_map.get(key);
            //V tmpVal = (ref == null) ? null : ref.get();
            Object tmpVal = (ref == null) ? null : ref.get();
            if (tmpVal == null) {
                _map.put(key, new WeakValueRef/*<V>*/(key, value, _rq));
            } else {
                valInPool = tmpVal;
            }
        }
        return valInPool;
    }

    /*
     * Unlike Map#put, this method returns non-null value actually
     * in the pool, even no values for the key was not available
     * before.  When the value for the key was available and the value
     * was weakly referenced, this method will replace the reference
     * type to strong, but actual value object won't be changed.
     */
    //synchronized public V registerPermanent(K key, V value) {
    synchronized public Object registerPermanent(Object key, Object value) {
        expungeStaleEntries();
        //V valInPool = value;
        Object valInPool = value;
        synchronized(_map) {
            //ValueRef<V> ref = _map.get(key);
            ValueRef ref = (ValueRef)_map.get(key);
            boolean isStrongRef = false;
            //V tmpVal = null;
            Object tmpVal = null;
            if (ref != null) {
                tmpVal = ref.get();
                isStrongRef = (ref instanceof StrongValueRef);
            }
            if (tmpVal == null) {
                _map.put(key, new StrongValueRef/*<V>*/(value));
            } else if (!isStrongRef) {
                // If the key already exist and its value is currently
                // weakly referenced, we use the value with a strong reference.
                _map.put(key, new StrongValueRef/*<V>*/(tmpVal));
                valInPool = tmpVal;
            }
        }
        return valInPool;
    }

    private void expungeStaleEntries() {
        Reference/*<? extends V>*/ val;
        while ((val = _rq.poll()) != null) {
            Object key = ((WeakValueRef/*<?>*/)val).getKey();
            _map.remove(key);
        }
    }

    private static interface ValueRef/*<V>*/ {
        //V get();
        Object get();
    }

    private static class StrongValueRef/*<V>*/ implements ValueRef/*<V>*/ {
        //private V _value;
        private Object _value;
        //public StrongValueRef(V value) {
        public StrongValueRef(Object value) {
            _value = value;
        }
        //public V get() {
        public Object get() {
            return _value;
        }
    }

    private static class WeakValueRef/*<V>*/ extends WeakReference/*<V>*/ implements ValueRef/*<V>*/{
        private Object _key;

        //public WeakValueRef(Object key, V value, ReferenceQueue<V> rq) {
        public WeakValueRef(Object key, Object value, ReferenceQueue rq) {
            super(value, rq);
            _key = key;
        }

        //public V get() {
        public Object get() {
            return super.get();
        }

        public Object getKey() {
            return _key;
        }
    }
}
