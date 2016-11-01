/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Comparator;

public class MultiComparator<T> implements Comparator<T> {
    private Comparator<T>[] comparators;

    public MultiComparator (Comparator<T>... comparators) {
        this.comparators = comparators;
    }

    /* Lexigraphic compare. Returns the first difference
     * @return zero if equal. Otherwise +/- (i+1) 
     * where i is the index of the first comparator finding a difference
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(T arg0, T arg1) {
        for (int i = 0; i < comparators.length; ++i) {
            int result = comparators[i].compare(arg0, arg1);
            if (result == 0) {
                continue;
            }
            if (result > 0) {
                return i + 1;
            }
            return -(i + 1);
        }
        return 0;
    }
}