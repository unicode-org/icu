/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.Transform;

public class XEquivalenceClass<T,R> implements Iterable<T> {
    private static final String ARROW = "\u2192";

    public SetMaker<T> getSetMaker() {
        return setMaker;
    }

    // quick test
    static public void main(String[] args) {
        XEquivalenceClass<String, Integer> foo1 = new XEquivalenceClass<String, Integer>(1);
        String[][] tests = {{"b","a1"}, {"b", "c"}, {"a1", "c"}, {"d", "e"}, {"e", "f"}, {"c", "d"}};
        for (int i = 0; i < tests.length; ++i) {
            System.out.println("Adding: " + tests[i][0] + ", " + tests[i][1]);
            foo1.add(tests[i][0], tests[i][1], new Integer(i));
            for (String item : foo1.getExplicitItems()) {
                System.out.println("\t" + item  + ";\t" + foo1.getSample(item) + ";\t" + foo1.getEquivalences(item));
                List<Linkage<String, Integer>> reasons = foo1.getReasons(item, foo1.getSample(item));
                if (reasons != null) {
                    System.out.println("\t\t" + XEquivalenceClass.toString(reasons, null));
                }
            }
        }
    }

    private Map<T,Set<T>> toPartitionSet = new HashMap();
    private Map<T,Map<T,Set<R>>> obj_obj_reasons = new HashMap();
    private R defaultReason;
    private SetMaker setMaker;
    
    public interface SetMaker<T> {
        Set<T> make();
    }

    /**
     * empty, as if just created
     */
    public XEquivalenceClass clear(R defaultReasonArg) {
        toPartitionSet.clear();
        obj_obj_reasons.clear();
        this.defaultReason = defaultReasonArg;
        return this;
    }

    /**
     * Create class
     *
     */
    public XEquivalenceClass() {
    }

    /**
     * Create class with comparator, and default reason.
     *
     */
    public XEquivalenceClass(R defaultReason) {
        this.defaultReason = defaultReason;
    }
    
    /**
     * Create class with comparator, and default reason.
     *
     */
    public XEquivalenceClass(R defaultReason, SetMaker<T> setMaker) {
        this.defaultReason = defaultReason;
        this.setMaker = setMaker;
    }

    /**
     * Add two equivalent items, with NO_REASON for the reason.
     */
    public XEquivalenceClass add(T a, T b) {
        return add(a,b,null);
    }

    /**
     * Add two equivalent items, with NO_REASON for the reason.
     */
    public XEquivalenceClass add(T a, T b, R reason) {
        return add(a,b,reason, reason);
    }

    /**
     * Add two equivalent items, plus a reason. The reason is only used for getReasons
     */
    public XEquivalenceClass add(T a, T b, R reasonAB, R reasonBA) {
        if (a.equals(b)) return this;
        if (reasonAB == null) reasonAB = defaultReason;
        if (reasonBA == null) reasonBA = defaultReason;
        addReason(a,b,reasonAB);
        addReason(b,a,reasonBA);
        Set<T>aPartitionSet = toPartitionSet.get(a);
        Set<T>bPartitionSet = toPartitionSet.get(b);
        if (aPartitionSet == null) {
            if (bPartitionSet == null) { // both null, set up bSet
                bPartitionSet = setMaker != null ? setMaker.make() : new HashSet();
                bPartitionSet.add(b);
                toPartitionSet.put(b, bPartitionSet);
            }
            bPartitionSet.add(a);
            toPartitionSet.put(a, bPartitionSet);
        } else if (bPartitionSet == null) { // aSet is not null, bSet null
            aPartitionSet.add(b);
            toPartitionSet.put(b, aPartitionSet);
        } else if (aPartitionSet != bPartitionSet) {  // both non-null, not equal, merge.  Equality check ok here
            aPartitionSet.addAll(bPartitionSet);
            // remap every x that had x => bPartitionSet
            for (T item : bPartitionSet) {
                toPartitionSet.put(item, aPartitionSet);
            }
        }
        return this;
    }

    /**
     * Add all the information from the other class
     *
     */
    public XEquivalenceClass<T,R> addAll(XEquivalenceClass<T,R> other) {
        // For now, does the simple, not optimized version
        for (T a : other.obj_obj_reasons.keySet()) {
            Map<T,Set<R>> obj_reasons = other.obj_obj_reasons.get(a);
            for (T b : obj_reasons.keySet()) {
                Set<R> reasons = obj_reasons.get(b);
                for (R reason: reasons) {
                    add(a, b, reason);
                }
            }
        }
        return this;
    }

    /**
     * 
     */
    private void addReason(T a, T b, R reason) {
        Map<T,Set<R>> obj_reasons = obj_obj_reasons.get(a);
        if (obj_reasons == null) obj_obj_reasons.put(a, obj_reasons = new HashMap());
        Set<R> reasons = obj_reasons.get(b);
        if (reasons == null) obj_reasons.put(b, reasons = new HashSet());
        reasons.add(reason);
    }

    /**
     * Returns a set of all the explicit items in the equivalence set. (Any non-explicit items only
     * have themselves as equivalences.)
     *
     */
    public Set<T> getExplicitItems() {
        return Collections.unmodifiableSet(toPartitionSet.keySet());
    }

    /**
     * Returns an unmodifiable set of all the equivalent objects
     *
     */
    public Set<T>getEquivalences(T a) {
        Set<T> aPartitionSet = toPartitionSet.get(a);
        if (aPartitionSet == null) { // manufacture an equivalence
            aPartitionSet = new HashSet<T>();
            aPartitionSet.add(a); 
        }
        return Collections.unmodifiableSet(aPartitionSet);
    }
    
    public boolean hasEquivalences(T a) {
        return toPartitionSet.get(a) != null;
    }

    public Set<Set<T>> getEquivalenceSets() {
        Set<Set<T>> result = new HashSet<Set<T>>();
        for (T item : toPartitionSet.keySet()) {
            Set<T> partition = toPartitionSet.get(item);
            result.add(Collections.unmodifiableSet(partition));
        }
        return result;
    }
    /**
     * returns true iff a is equivalent to b (or a.equals b)
     *
     */
    public boolean isEquivalent(T a, T b) {
        if (a.equals(b)) return true;
        Set<T>aPartitionSet = toPartitionSet.get(a);
        if (aPartitionSet == null) return false;
        return aPartitionSet.contains(b);
    }

    /**
     * Gets a sample object in the equivalence set for a. 
     *
     */
    public T getSample(T a) {
        Set<T> aPartitionSet = toPartitionSet.get(a);
        if (aPartitionSet == null) return a; // singleton
        return aPartitionSet.iterator().next();
    }

    public interface Filter<T> {
        boolean matches(T o);
    }

    public T getSample(T a, Filter<T> f) {
        Set<T> aPartitionSet = toPartitionSet.get(a);
        if (aPartitionSet == null) return a; // singleton
        for (T obj : aPartitionSet) {
            if (f.matches(obj)) return obj;
        }
        return a;
    }

    /**
     * gets the set of all the samples, one from each equivalence class. 
     *
     */
    public Set<T> getSamples() {
        Set<T> seenAlready = new HashSet();
        Set<T> result = new HashSet();
        for (T item : toPartitionSet.keySet()) {
            if (seenAlready.contains(item)) continue;
            Set<T> partition = toPartitionSet.get(item);
            result.add(partition.iterator().next());
            seenAlready.addAll(partition);
        }
        return result;
    }

    public Iterator<T> iterator() {
        return getSamples().iterator();
    }
    
    public static class Linkage<T,R> {
        /**
         * 
         */
        public Set<R> reasons;
        public T result;
        /**
         * @param reasons
         * @param item
         */
        public Linkage(Set<R> reasons, T result) {
            this.reasons = reasons;
            this.result = result;
        }
        public String toString() {
            return reasons + (result == null ? "" : ARROW + result);
        }
    }

    public static <T,R> String toString(List<Linkage<T,R>> others, Transform<Linkage<T,R>,String> itemTransform) {
        StringBuffer result = new StringBuffer();
        for (Linkage<T,R> item : others) {
            result.append(itemTransform == null ? item.toString() : itemTransform.transform(item));
        }
        return result.toString();
    }

    /**
     * Returns a list of linkages, where each set of reasons to go from one obj to the next. The list does not include a and b themselves.
     * The last linkage has a null result.<br>
     * Returns null if there is no connection.
     */
    public List<Linkage<T,R>> getReasons(T a, T b) {
        // use dumb algorithm for getting shortest path
        // don't bother with optimization
        Set<T> aPartitionSet = toPartitionSet.get(a);
        Set<T> bPartitionSet = toPartitionSet.get(b);

        // see if they connect
        if (aPartitionSet == null || bPartitionSet == null || aPartitionSet != bPartitionSet || a.equals(b)) return null;

        ArrayList<Linkage<T,R>> list = new ArrayList<Linkage<T,R>>();
        list.add(new Linkage(null, a));
        ArrayList<ArrayList<Linkage<T,R>>> lists = new ArrayList<ArrayList<Linkage<T,R>>>();
        lists.add(list);

        // this will contain the results
        Set<T> sawLastTime = new HashSet<T>();
        sawLastTime.add(a);

        // each time, we extend each lists by one (adding multiple other lists)
        while (true) { // foundLists.size() == 0
            ArrayList extendedList = new ArrayList();
            Set<T>sawThisTime = new HashSet();
            for (ArrayList<Linkage<T,R>> lista : lists) {
                Linkage<T,R> last = lista.get(lista.size()-1);
                Map<T,Set<R>> obj_reasons = obj_obj_reasons.get(last.result);
                for (T result : obj_reasons.keySet()) {
                    if (sawLastTime.contains(result)) {
                        continue; // skip since we have shorter
                    }
                    sawThisTime.add(result);
                    Set<R> reasons = obj_reasons.get(result);
                    ArrayList<Linkage<T,R>> lista2 = (ArrayList<Linkage<T,R>>) lista.clone();
                    lista2.add(new Linkage(reasons,result));
                    extendedList.add(lista2);
                    if (result.equals(b)) {
                        // remove first and last
                        ArrayList<Linkage<T,R>> found = (ArrayList<Linkage<T,R>>) lista2.clone();
                        found.remove(0);
                        found.get(found.size()-1).result = null;
                        return found;
                    }
                }
            }
            lists = extendedList;
            sawLastTime.addAll(sawThisTime);
        }
        // return foundLists;
    }
    
    /**
     * For debugging.
     */
    public String toString() {
        return getEquivalenceSets().toString();
    }
}