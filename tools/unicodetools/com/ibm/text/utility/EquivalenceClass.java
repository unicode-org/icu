/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/EquivalenceClass.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;


import java.io.*;
import java.util.*;

public class EquivalenceClass {
    static final boolean DEBUG = false;
    /**
     * Takes a many:many relation between source and value.
     * Produces equivalence class.
     * Two sources are in the same equivalence class any time they share the same value.
     */
    // associated with each value, we keep a set of sources.
    // whenever we add a <source, value> pair, we see if any sets collide.
    // associated with each set of sources, we keep a representative Whenever we add to the set, if we
    //
    Map sourceToEquiv = new HashMap();
    Map valueToRepresentativeSource = new HashMap();
    Map forcedMerge = new HashMap();
    /**
     * @return true if made a difference
     */

    String itemSeparator;
    int places;
    boolean hex;

    public EquivalenceClass() {
        this(",", 4, true);
    }

    public EquivalenceClass(String itemSeparator, int places, boolean hex) {
        this.itemSeparator = itemSeparator;
        this.places = places;
        this.hex = hex;
    }

    public boolean add(Object source, Object value) {
        boolean result = false;
        Object repSource = valueToRepresentativeSource.get(value);
        Set equivSet = (Set)sourceToEquiv.get(source);
        Set fm = (Set)forcedMerge.get(source);
        if (fm == null) {
            fm = new TreeSet();
            forcedMerge.put(source, fm);
        }

        if (DEBUG) System.out.println("+Source " + source
            + ", value: " + value);
        if (repSource == null && equivSet == null) {
            equivSet = new HashSet();
            equivSet.add(source);
            sourceToEquiv.put(source, equivSet);
            valueToRepresentativeSource.put(value, source);
            repSource = source; // for debugging
        } else if (equivSet == null) {
            equivSet = (Set) sourceToEquiv.get(repSource);
            equivSet.add(source);
            sourceToEquiv.put(source, equivSet);
            result = true;
        } else if (repSource == null) {
            valueToRepresentativeSource.put(value, source);
            repSource = source; // for debugging;
        } else { // both non-null
            Set repEquiv = (Set) sourceToEquiv.get(repSource);
            if (!repEquiv.equals(equivSet)) {

                result = true;
                if (DEBUG) System.out.println("Merging (" + repSource + ") " + toString(repEquiv)
                    + " + (" + source + ") " + toString(equivSet));
                // merge!!
                // put all items from equivSet into repEquiv
                repEquiv.addAll(equivSet);

                // now add the values to the forced sets
                Iterator it = repEquiv.iterator();
                while (it.hasNext()) {
                    Object n = it.next();
                    fm = (Set)forcedMerge.get(n);
                    fm.add(value);
                }

                // then replace all instances for equivSet by repEquiv
                // we have to do this in two steps, since iterators are invalidated by changes
                Set toReplace = new HashSet();
                it = sourceToEquiv.keySet().iterator();
                while (it.hasNext()) {
                    Object otherSource = it.next();
                    Set otherSet = (Set) sourceToEquiv.get(otherSource);
                    if (otherSet == equivSet) {
                        toReplace.add(otherSource);
                    }
                }
                it = toReplace.iterator();
                while (it.hasNext()) {
                    Object otherSource = it.next();
                    sourceToEquiv.put(otherSource,repEquiv);
                }
                equivSet = repEquiv; // for debugging
            }
        }
        if (DEBUG) System.out.println("--- repSource: " + repSource
            + ", equivSet: " + equivSet);
        return result;
    }

    public String toString () {
        StringBuffer result = new StringBuffer();
        // make a set to skip duplicates
        Iterator it = new HashSet(sourceToEquiv.values()).iterator();
        while (it.hasNext()) {
            toString((Set)it.next(), result, forcedMerge);
        }
        return result.toString();
    }

    private String toString(Object s) {
        if (s == null) return "null";
        if (s instanceof Collection) {
            StringBuffer sb = new StringBuffer();
            toString((Collection)s, sb, null);
            return sb.toString();
        }
        if (hex && s instanceof Number) {
            return Utility.hex(s, places);
        }
        return s.toString();
    }

    private void toString(Collection s, StringBuffer sb, Map valueToRep) {
        if (sb.length() != 0) sb.append(itemSeparator);
        if (s == null) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        Iterator it = s.iterator();
        boolean notFirst = false;
        while (it.hasNext()) {
            if (notFirst) sb.append(", ");
            notFirst = true;
            Object n = it.next();
            sb.append(toString(n));
            /*if (valueToRep != null) {
                sb.append("(" + toString(valueToRep.get(n)) + ")");
            }*/
        }
        sb.append('}');
    }

}