/*
 **********************************************************************
 * Copyright (c) 2002-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.icu.util.Freezable;

/**
 * A Relation is a set of mappings from keys to values.
 * Unlike Map, there is not guaranteed to be a single value per key.
 * The Map-like APIs return collections for values.
 * @author medavis

 */
public class Relation<K, V> implements Freezable {
    private Map<K, Set<V>> data;

    Constructor<Set<V>> setCreator;
    Object[] setComparatorParam;

    public Relation(Map<K, Set<V>> map, Class<Set<V>> setCreator) {
        this(map, setCreator, null);
    }

    public Relation(Map<K, Set<V>> map, Class<Set<V>> setCreator, Comparator<V> setComparator) {
        try {
            setComparatorParam = setComparator == null ? null : new Object[]{setComparator};
            if (setComparator == null) {
                this.setCreator = setCreator.getConstructor();
                this.setCreator.newInstance(setComparatorParam); // check to make sure compiles
            } else {
                this.setCreator = setCreator.getConstructor(Comparator.class);
                this.setCreator.newInstance(setComparatorParam); // check to make sure compiles        
            }
            data = map == null ? new HashMap() : map;     
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e);
        }

    }

    public void clear() {
        data.clear();
    }

    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Set<V> values : data.values()) {
            if (values.contains(value))
                return true;
        }
        return false;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = new LinkedHashSet();
        for (K key : data.keySet()) {
            for (V value : data.get(key)) {
                result.add(new SimpleEntry(key, value));
            }
        }
        return result;
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o.getClass() != this.getClass())
            return false;
        return data.equals(((Relation) o).data);
    }

    //  public V get(Object key) {
    //      Set<V> set = data.get(key);
    //      if (set == null || set.size() == 0)
    //        return null;
    //      return set.iterator().next();
    //  }

    public Set<V> getAll(Object key) {
        return data.get(key);
    }

    public int hashCode() {
        return data.hashCode();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Set<K> keySet() {
        return data.keySet();
    }

    public V put(K key, V value) {
        Set<V> set = data.get(key);
        if (set == null) {
            data.put(key, set = newSet());
        }
        set.add(value);
        return value;
    }

    public V putAll(K key, Collection<V> value) {
        Set<V> set = data.get(key);
        if (set == null) {
            data.put(key, set = newSet());
        }
        set.addAll(value);
        return value.size() == 0 ? null : value.iterator().next();
    }

    public V putAll(Collection<K> keys, V value) {
        V result = null;
        for (K key : keys) {
            result = put(key, value);
        }
        return result;
    }

    private Set<V> newSet() {
        try {
            return (Set<V>) setCreator.newInstance(setComparatorParam);
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e);
        }
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        for (K key : t.keySet()) {
            put(key, t.get(key));
        }
    }

    public void putAll(Relation<? extends K, ? extends V> t) {
        for (K key : t.keySet()) {
            for (V value : t.getAll(key)) {
                put(key, value);
            }
        }
    }

    public Set<V> removeAll(K key) {
        return data.remove(key);
    }

    public boolean remove(K key, V value) {
        Set<V> set = data.get(key);
        if (set == null) return false;
        boolean result = set.remove(value);
        if (set.size() == 0) {
            data.remove(key);
        }
        return result;
    }

    public int size() {
        return data.size();
    }

    public Collection<V> values() {
        Set<V> result = newSet();
        for (K key : data.keySet()) {
            result.addAll(data.get(key));
        }
        return result;
    }

    public String toString() {
        return data.toString();
    }

    static class SimpleEntry<K, V> implements Entry<K, V> {
        K key;

        V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Entry<K, V> e) {
            this.key = e.getKey();
            this.value = e.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    public Relation<K,V> addAllInverted(Relation<V,K> source) {
        for (V value : source.data.keySet()) {
            for (K key : source.data.get(value)) {
                put(key, value);
            }
        }
        return this;
    }

    public Relation<K,V> addAllInverted(Map<V,K> source) {
        for (V value : source.keySet()) {
            put(source.get(value), value);
        }
        return this;
    }

    boolean frozen = false;

    public boolean isFrozen() {
        return frozen;
    }

    public Object freeze() {
        if (!frozen) {
            frozen = true;
            // does not handle one level down, so we do that on a case-by-case basis
            for (K key : data.keySet()) {
                data.put(key, Collections.unmodifiableSet(data.get(key)));
            }
            // now do top level
            data = Collections.unmodifiableMap(data);
        }
        return this;
    }

    public Object cloneAsThawed() {
        // TODO do later
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Relation<K, V> toBeRemoved) {
        boolean result = false;
        for (K key : toBeRemoved.keySet()) {
            Set<V> values = toBeRemoved.getAll(key);
            if (values != null) {
                result |= removeAll(key, values);
            }
        }
        return result;
    }

    public boolean removeAll(K key, Iterable<V> all) {
        boolean result = false;
        for (V value : all) {
            result |= remove(key, value);
        }
        return result;
    }

    public Set<V> removeAll(Collection<K> toBeRemoved) {
        Set<V> result = new LinkedHashSet();
        for (K key : toBeRemoved) {
            final Set<V> removals = data.remove(key);
            if (removals != null) {
                result.addAll(removals);
            }
        }
        return result;
    }
}