/*
**********************************************************************
* Copyright (c) 2002-2006, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package com.ibm.icu.dev.test.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Relation is a set of mappings from keys to values.
 * Unlike Map, there is not guaranteed to be a single value per key.
 * The Map-like APIs return collections for values.
 * @author medavis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Relation {
    private Map m;
    private CollectionFactory subcollection;

    public Relation(Map mainMap, CollectionFactory subcollection) {
        m = mainMap;
        if (subcollection == null) subcollection = new CollectionMaker(null);
        this.subcollection = subcollection;
    }

    public void clear() {
        m.clear();
    }
    public boolean containsKey(Object key) {
        return m.containsKey(key);
    }
    public boolean containsValue(Object value) {
        return m.containsValue(value);
    }
    public Set entrySet() {
        return m.entrySet();
    }
    public boolean equals(Object obj) {
        return m.equals(obj);
    }
    public int hashCode() {
        return m.hashCode();
    }
    public boolean isEmpty() {
        return m.isEmpty();
    }
    public Object remove(Object key) {
        return m.remove(key);
    }
    public int size() {
        return m.size();
    }
    public String toString() {
        return m.toString();
    }
    public Set keySet() {
        return m.keySet();
    }
    /*
    public void addAll(Relation t) {
        for (Iterator it = t.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            add(key, t.get(key));
        }
    }
    */
    public Collection values() {
        return m.values();
    }
    public Collection get(Object key, Collection output) {
        output.addAll((Collection)m.get(key));
        return output;
    }
    public void add(Object key, Object value) {
        Collection o = (Collection) m.get(key);
        if (o == null) m.put(key, o = subcollection.make());
        o.add(value);
    }
    public Iterator iterator() {
        return m.keySet().iterator();
    }
    public interface CollectionFactory {
        Collection make();
    }

    /**
     * This is just temporary, and may change!!
     * @author medavis
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    public static class CollectionMaker implements CollectionFactory {
        public static final int HASH = 0, TREE = 1;
        private Comparator comparator = null;
        private int type = HASH;

        public CollectionMaker(int type) {
            this.type = type;
        }
        public CollectionMaker(Comparator comparator) {
            this.comparator = comparator;
        }
        public Collection make() {
            if (comparator != null) return new TreeSet(comparator);
            else if (type == HASH) return new HashSet();
            else return new TreeSet();
        }
    }
}