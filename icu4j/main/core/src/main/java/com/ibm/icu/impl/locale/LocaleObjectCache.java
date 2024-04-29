// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LocaleObjectCache<K, V> {
    private ConcurrentHashMap<K, CacheEntry<K, V>> _map;
    private ReferenceQueue<V> _queue = new ReferenceQueue<V>();

    public LocaleObjectCache() {
        this(16, 0.75f, 16);
    }

    public LocaleObjectCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        _map = new ConcurrentHashMap<K, CacheEntry<K, V>>(initialCapacity, loadFactor, concurrencyLevel);
    }

    public V get(K key) {
        V value = null;

        cleanStaleEntries();
        CacheEntry<K, V> entry = _map.get(key);
        if (entry != null) {
            value = entry.get();
        }
        if (value == null) {
            key = normalizeKey(key);
            // subclass must return non-null key object
            if (key == null) {
                return null;
            }

            entry = _map.get(key);
            if (entry != null) {
                value = entry.get();
            }
            // hit cache
            if (value != null) {
                return value;
            }

            // if map not contains key or the referent value of CacheEntry is set to be null
            // both need create a new value
            V newVal = createObject(key);
            if (newVal == null) {
                // subclass must return non-null value object
                return null;
            }

            CacheEntry<K, V> newEntry = new CacheEntry<K, V>(key, newVal, _queue);
            // just replace it
            _map.put(key, newEntry);
            // clean recycled SoftReferences again
            cleanStaleEntries();
            return newVal;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void cleanStaleEntries() {
        CacheEntry<K, V> entry;
        while ((entry = (CacheEntry<K, V>)_queue.poll()) != null) {
            _map.remove(entry.getKey());
        }
    }

    protected abstract V createObject(K key);

    protected K normalizeKey(K key) {
        return key;
    }

    private static class CacheEntry<K, V> extends SoftReference<V> {
        private K _key;

        CacheEntry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            _key = key;
        }

        K getKey() {
            return _key;
        }
    }
}
