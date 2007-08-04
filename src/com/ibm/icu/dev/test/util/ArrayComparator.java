/*
 *******************************************************************************
 * Copyright (C) 2002-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;
import java.util.Comparator;

public class ArrayComparator implements Comparator {
    public static final Comparator COMPARABLE = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((Comparable)o1).compareTo(o2);
        } 
    };
    private Comparator[] comparators;
    private int[] reordering;
    
    public ArrayComparator (Comparator[] comparators, int[] reordering) {
        this.comparators = comparators;
        this.reordering = reordering;
        if (this.reordering == null) {
        	this.reordering = new int[comparators.length];
        	for (int i = 0; i < this.reordering.length; ++i) {
        		this.reordering[i] = i;
        	}
        } else {
        	if (this.reordering.length != this.comparators.length) {
        		throw new IllegalArgumentException("comparator and reordering lengths must match");
        	}
        }
    }
    
    public ArrayComparator (Comparator[] comparators) {
        this(comparators,null);
    }
    
    /* Lexigraphic compare. Returns the first difference
     * @return zero if equal. Otherwise +/- (i+1) 
     * where i is the index of the first comparator finding a difference
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object a0, Object a1) {
    	Object[] arg0 = (Object[]) a0;
    	Object[] arg1 = (Object[]) a1;
        for (int j = 0; j < comparators.length; ++j) {
        	int i = reordering[j];
        	Comparator comp = comparators[i];
        	if (comp == null) continue;
            int result = comp.compare(arg0[i], arg1[i]);
            if (result == 0) continue;
            if (result > 0) return i+1;
            return -(i+1);
        }
        return 0;
    }

    static class CatchExceptionComparator implements Comparator {
        private Comparator other;
        
        public CatchExceptionComparator(Comparator other) {
            this.other = other;
        }

        public int compare(Object arg0, Object arg1) throws RuntimeException {
            try {
                return other.compare(arg0, arg1);
            } catch (RuntimeException e) {
                System.out.println("Arg0: " + arg0);
                System.out.println("Arg1: " + arg1);
                throw e;
            }
        }
    }
    
}