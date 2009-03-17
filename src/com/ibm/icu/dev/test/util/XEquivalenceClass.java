/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
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

public class XEquivalenceClass {

    public SetMaker getSetMaker() {
        return setMaker;
    }

    // quick test
    static public void main(String[] args) {
        XEquivalenceClass foo1 = new XEquivalenceClass("NONE");
        String[][] tests = {{"b","a1"}, {"b", "c"}, {"a1", "c"}, {"d", "e"}, {"e", "f"}, {"c", "d"}};
        for (int i = 0; i < tests.length; ++i) {
            System.out.println("Adding: " + tests[i][0] + ", " + tests[i][1]);
            foo1.add(tests[i][0], tests[i][1], new Integer(i));
            for (Iterator it = foo1.getExplicitItems().iterator(); it.hasNext();) {
                Object item = it.next();
                System.out.println("\t" + item  + ";\t" + foo1.getSample(item) + ";\t" + foo1.getEquivalences(item));
                System.out.println("\t\t" + foo1.getReasons(item, foo1.getSample(item)));
            }
        }
    }

    private Map toPartitionSet = new HashMap();
    private Map obj_obj_reasons = new HashMap();
    private Object defaultReason;
    private SetMaker setMaker;
    
    public interface SetMaker {
        Set make();
    }

    /**
     * empty, as if just created
     */
    public XEquivalenceClass clear(Object defaultReasonArg) {
        toPartitionSet.clear();
        obj_obj_reasons.clear();
        this.defaultReason = defaultReasonArg;
        return this;
    }

    /**
     * Create class with comparator, and default reason.
     *
     */
    public XEquivalenceClass(Object defaultReason) {
        this.defaultReason = defaultReason;
    }
    
    /**
     * Create class with comparator, and default reason.
     *
     */
    public XEquivalenceClass(Object defaultReason, SetMaker setMaker) {
        this.defaultReason = defaultReason;
        this.setMaker = setMaker;
    }

    /**
     * Add two equivalent items, with NO_REASON for the reason.
     */
    public XEquivalenceClass add(Object a, Object b) {
        return add(a,b,null);
    }

    /**
     * Add two equivalent items, plus a reason. The reason is only used for getReasons
     */
    public XEquivalenceClass add(Object a, Object b, Object reason) {
        if (a.equals(b)) return this;
        if (reason == null) reason = defaultReason;
        addReason(a,b,reason);
        addReason(b,a,reason);
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        Set bPartitionSet = (Set) toPartitionSet.get(b);
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
            for (Iterator it = bPartitionSet.iterator(); it.hasNext();) {
                toPartitionSet.put(it.next(), aPartitionSet);
            }
        }
        return this;
    }

    /**
     * Add all the information from the other class
     *
     */
    public XEquivalenceClass addAll(XEquivalenceClass other) {
        // For now, does the simple, not optimized version
        for (Iterator it = other.obj_obj_reasons.keySet().iterator(); it.hasNext();) {
            Object a = it.next();
            Map obj_reasons = (Map) other.obj_obj_reasons.get(a);
            for (Iterator it2 = obj_reasons.keySet().iterator(); it2.hasNext();) {
                Object b = it2.next();
                Set reasons = (Set) obj_reasons.get(b);
                for (Iterator it3 = reasons.iterator(); it3.hasNext();) {
                    Object reason = it3.next();
                    add(a, b, reason);
                }
            }
        }
        return this;
    }

    /**
     * 
     */
    private void addReason(Object a, Object b, Object reason) {
        Map obj_reasons = (Map) obj_obj_reasons.get(a);
        if (obj_reasons == null) obj_obj_reasons.put(a, obj_reasons = new HashMap());
        Set reasons = (Set) obj_reasons.get(b);
        if (reasons == null) obj_reasons.put(b, reasons = new HashSet());
        reasons.add(reason);
    }

    /**
     * Returns a set of all the explicit items in the equivalence set. (Any non-explicit items only
     * have themselves as equivalences.)
     *
     */
    public Set getExplicitItems() {
        return Collections.unmodifiableSet(toPartitionSet.keySet());
    }

    /**
     * Returns an unmodifiable set of all the equivalent objects
     *
     */
    public Set getEquivalences(Object a) {
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        if (aPartitionSet == null) { // manufacture an equivalence
            aPartitionSet = new HashSet();
            aPartitionSet.add(a); 
        }
        return Collections.unmodifiableSet(aPartitionSet);
    }

    public Set getEquivalenceSets() {
        Set result = new HashSet();
        for (Iterator it = toPartitionSet.keySet().iterator(); it.hasNext();) {
            Object item = it.next();
            Set partition = (Set) toPartitionSet.get(item);
            result.add(Collections.unmodifiableSet(partition));
        }
        return result;
    }
    /**
     * returns true iff a is equivalent to b (or a.equals b)
     *
     */
    public boolean isEquivalent(Object a, Object b) {
        if (a.equals(b)) return true;
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        if (aPartitionSet == null) return false;
        return aPartitionSet.contains(b);
    }

    /**
     * Gets a sample object in the equivalence set for a. 
     *
     */
    public Object getSample(Object a) {
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        if (aPartitionSet == null) return a; // singleton
        return aPartitionSet.iterator().next();
    }

    public interface Filter {
        boolean matches(Object o);
    }

    public Object getSample(Object a, Filter f) {
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        if (aPartitionSet == null) return a; // singleton
        for (Iterator it = aPartitionSet.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (f.matches(obj)) return obj;
        }
        return a;
    }

    /**
     * gets the set of all the samples, one from each equivalence class. 
     *
     */
    public Set getSamples() {
        Set seenAlready = new HashSet();
        Set result = new HashSet();
        for (Iterator it = toPartitionSet.keySet().iterator(); it.hasNext();) {
            Object item = it.next();
            if (seenAlready.contains(item)) continue;
            Set partition = (Set) toPartitionSet.get(item);
            result.add(partition.iterator().next());
            seenAlready.addAll(partition);
        }
        return result;
    }


    /**
     * Returns a list of lists. Each sublist is in the form [reasons, obj, reasons, obj,..., reasons]
     * where each reasons is a set of reasons to go from one obj to the next.<br>
     * Returns null if there is no connection.
     */
    public List getReasons(Object a, Object b) {
        // use dumb algorithm for getting shortest path
        // don't bother with optimization
        Set aPartitionSet = (Set) toPartitionSet.get(a);
        Set bPartitionSet = (Set) toPartitionSet.get(b);

        // see if they connect
        if (aPartitionSet == null || bPartitionSet == null || aPartitionSet != bPartitionSet || a.equals(b)) return null;

        ArrayList list = new ArrayList();
        list.add(a);
        ArrayList lists = new ArrayList();
        lists.add(list);

        // this will contain the results
        List foundLists = new ArrayList();
        Set sawLastTime = new HashSet();
        sawLastTime.add(a);

        // each time, we extend the lists by one (adding multiple other lists)
        while (foundLists.size() == 0) {
            ArrayList extendedList = new ArrayList();
            Set sawThisTime = new HashSet();
            for (Iterator it = lists.iterator(); it.hasNext();) {
                ArrayList lista = (ArrayList) it.next();
                Object last = lista.get(lista.size()-1);
                Map obj_reasons = (Map) obj_obj_reasons.get(last);
                for (Iterator it2 = obj_reasons.keySet().iterator(); it2.hasNext();) {
                    Object item = it2.next();
                    if (sawLastTime.contains(item)) {
                        continue; // skip since we have shorter
                    }
                    sawThisTime.add(item);
                    Set reasons = (Set) obj_reasons.get(item);
                    ArrayList lista2 = (ArrayList)lista.clone();
                    lista2.add(reasons);
                    lista2.add(item);
                    extendedList.add(lista2);
                    if (item.equals(b)) {
                        // remove first and last
                        ArrayList found = (ArrayList)lista2.clone();
                        found.remove(0);
                        found.remove(found.size()-1);
                        foundLists.add(found);
                    }
                }
            }
            lists = extendedList;
            sawLastTime.addAll(sawThisTime);
        }
        return foundLists;
    }
    
    /**
     * For debugging.
     */
    public String toString() {
        return getEquivalenceSets().toString();
    }
}