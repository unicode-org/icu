/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

/**
 * Everything that maps to the same value is part of the same equivalence class
 * @author davis
 *
 */
public class XEquivalenceMap<K,V,R> implements Iterable<Set<K>> {
    
    Map<K,Row.R2<V,Set<R>>> source_target_reasons = new HashMap<K,Row.R2<V,Set<R>>>();

    Map<V,Set<K>> target_sourceSet;
    Map<K,Set<K>> source_Set = new HashMap<K,Set<K>>(); // not really needed: could go source-target-sourceset
    
    public XEquivalenceMap() {
        this(new HashMap<V,Set<K>>());
    }
    
    public XEquivalenceMap(Map<V,Set<K>> storage) {
        target_sourceSet = storage;
    }
    
    public XEquivalenceMap clear() {
        source_target_reasons.clear();
        target_sourceSet.clear();
        source_Set.clear();
        return this;
    }
    
    public XEquivalenceMap add(K source, V target) {
        return add(source, target, null);
    }
    
    public XEquivalenceMap add(K source, V target, R reason) {
        R2<V, Set<R>> target_reasons = source_target_reasons.get(source);
        if (target_reasons == null) {
            Set<R> reasons = new HashSet<R>();
            if (reason != null) {
                reasons.add(reason);
            }
            target_reasons = Row.of(target, reasons);
            source_target_reasons.put(source, target_reasons);
        } else {
            V otherTarget = target_reasons.get0();
            Set<R> reasons = target_reasons.get1();
            if (otherTarget.equals(target)) {
                if (reason != null) {
                    reasons.add(reason);
                }
                return this;
            }
            throw new IllegalArgumentException("Same source mapping to different targets: "
                    + source + " => " + otherTarget + " & " + target);
        }

        Set<K> s = target_sourceSet.get(target);
        if (s == null) target_sourceSet.put(target, s = new HashSet<K>());
        s.add(source);
        source_Set.put(source, s);
        return this;
    }
    
    public Set<K> getEquivalences (K source) {
        Set<K> s = source_Set.get(source);
        if (s == null) return null;
        return Collections.unmodifiableSet(s);
    }
    
    public boolean areEquivalent (K source1, K source2) {
        Set<K> s = (Set) source_Set.get(source1);
        if (s == null) return false;
        return s.contains(source2);
    }
    
    public V getTarget(K source) {
        return source_target_reasons.get(source).get0();
    }
    
    public Set<R> getReasons(K source) {
        return Collections.unmodifiableSet(source_target_reasons.get(source).get1());
    }
    
    public Set<K> getSources(V target) {
        Set<K> s = target_sourceSet.get(target);
        return Collections.unmodifiableSet(s);
    }
    
    public Iterator<Set<K>> iterator() {
        return UnmodifiableIterator.from(target_sourceSet.values());
    }
    
    public int size() {
        return target_sourceSet.size();
    }
    
    public boolean isEmpty() {
        return target_sourceSet.isEmpty();
    }

    // Should be moved out on its own
    public static class UnmodifiableIterator<T> implements Iterator<T> {
        private Iterator<T> source;
        
        public static <T> UnmodifiableIterator<T> from(Iterator<T> source) {
            UnmodifiableIterator<T> result = new UnmodifiableIterator<T>();
            result.source = source;
            return result;
        }
        
        public static <T> UnmodifiableIterator<T> from(Iterable<T> source) {
            return from(source.iterator());
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        public boolean hasNext() {
            return source.hasNext();
        }
        
        public T next() {
            return source.next();
        }
    }
}