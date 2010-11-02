/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, thread-safe cache implementation, storing SoftReferences to cached instances.
 * To use, instantiate a subclass which implements the createInstance() method,
 * and call get() with the key and the data. The get() call will use the data
 * only if it needs to call createInstance(), otherwise the data is ignored.
 *
 * By using SoftReferences to instances, the Java runtime can release instances
 * once they are not used any more at all. If such an instance is then requested again,
 * the get() method will call createInstance() again and also create a new SoftReference.
 * The cache holds on to its map of keys to SoftReferenced instances forever.
 *
 * @param <K> Cache lookup key type
 * @param <V> Cache instance value type
 * @param <D> Data type for creating a new instance value
 *
 * @author Markus Scherer, Mark Davis
 */
public abstract class SoftCache<K, V, D> extends CacheBase<K, V, D> {
    @Override
    public final V getInstance(K key, D data) {
        // We synchronize twice, once on the map and once on valueRef,
        // because we prefer the fine-granularity locking of the ConcurrentHashMap
        // over coarser locking on the whole cache instance.
        // We use a SettableSoftReference (a second level of indirection) because
        // ConcurrentHashMap.putIfAbsent() never replaces the key's value, and if it were
        // a simple SoftReference we would not be able to reset its value after it has been cleared.
        // (And ConcurrentHashMap.put() always replaces the value, which we don't want either.)
        SettableSoftReference<V> valueRef = map.get(key);
        V value;
        if(valueRef != null) {
            synchronized(valueRef) {
                value = valueRef.ref.get();
                if(value != null) {
                    return value;
                } else {
                    // The instance has been evicted, its SoftReference cleared.
                    // Create and set a new instance.
                    valueRef.ref = new SoftReference<V>(value = createInstance(key, data));
                    return value;
                }
            }
        } else /* valueRef == null */ {
            // We had never cached an instance for this key.
            value = createInstance(key, data);
            valueRef = map.putIfAbsent(key, new SettableSoftReference<V>(value));
            if(valueRef == null) {
                // Normal "put": Our new value is now cached.
                return value;
            } else {
                // Race condition: Another thread beat us to putting a SettableSoftReference
                // into the map. Return its value, but just in case the garbage collector
                // was aggressive, we also offer our new instance for caching.
                return valueRef.setIfAbsent(value);
            }
        }
    }
    /**
     * Value type for cache items: Has a SoftReference which can be set
     * to a new value when the SoftReference has been cleared.
     * The SoftCache class sometimes accesses the ref field directly.
     *
     * @param <V> Cache instance value type
     */
    private static final class SettableSoftReference<V> {
        private SettableSoftReference(V value) {
            ref = new SoftReference<V>(value);
        }
        /**
         * If the SoftReference has been cleared, then this replaces it with a new SoftReference
         * for the new value and returns the new value; otherwise returns the current
         * SoftReference's value.
         * @param value Replacement value, for when the current reference has been cleared
         * @return The value that is held by the SoftReference, old or new
         */
        private synchronized V setIfAbsent(V value) {
            V oldValue = ref.get();
            if(oldValue == null) {
                ref = new SoftReference<V>(value);
                return value;
            } else {
                return oldValue;
            }
        }
        private SoftReference<V> ref;  // never null
    }
    private ConcurrentHashMap<K, SettableSoftReference<V>> map =
        new ConcurrentHashMap<K, SettableSoftReference<V>>();
}
