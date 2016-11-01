/*
 ************************************************************************************
 * Copyright (C) 2007-2010, Google Inc, International Business Machines Corporation *
 * and others. All Rights Reserved.                                                 *
 ************************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Comparator;
import java.util.Iterator;

public class IterableComparator<T> implements Comparator<Iterable<T>> {
    private final Comparator<T> comparator;
    private final int shorterFirst; // = 1 for shorter first, -1 otherwise

    public IterableComparator() {
        this(null, true);
    }

    public IterableComparator(Comparator<T> comparator) {
        this(comparator, true);
    }

    public IterableComparator(Comparator<T> comparator, boolean shorterFirst) {
        this.comparator = comparator;
        this.shorterFirst = shorterFirst ? 1 : -1;
    }

    public int compare(Iterable<T> a, Iterable<T> b) {
        if (a == null) {
            return b == null ? 0 : -shorterFirst;
        } else if (b == null) {
            return shorterFirst;
        }
        Iterator<T> ai = a.iterator();
        Iterator<T> bi = b.iterator();
        while (true) {
            if (!ai.hasNext()) {
                return bi.hasNext() ? -shorterFirst : 0;
            }
            if (!bi.hasNext()) {
                return shorterFirst;
            }
            T aItem = ai.next();
            T bItem = bi.next();
            @SuppressWarnings("unchecked")
            int result = comparator != null ? comparator.compare(aItem, bItem) : ((Comparable<T>)aItem).compareTo(bItem);
            if (result != 0) {
                return result;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> int compareIterables(Iterable<T> a, Iterable<T> b) {
        return NOCOMPARATOR.compare(a, b);
    }

    @SuppressWarnings("rawtypes")
    private static final IterableComparator NOCOMPARATOR = new IterableComparator();
}