/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.util.*;

public final class ArrayEnumeration implements Enumeration {

    public ArrayEnumeration(Object[] array, int start, int limit) {
        this.array = array;
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(byte[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Byte(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(char[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Character(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(short[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Short(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(int[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Integer(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(float[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Float(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public ArrayEnumeration(double[] array, int start, int limit) {
        this.array = new Object[array.length];
        for (int i = 0; i < array.length; ++i) {
            this.array[i] = new Double(array[i]);
        }
        position = start;
        this.limit = limit;
    }

    public boolean hasMoreElements() {
         return position < limit;
    }

    public Object nextElement() {
        if (position < limit)
            return array[position++];
        else
            throw new java.util.NoSuchElementException();
    }
    // privates
    private Object[] array;
    private int position = 0;
    private int limit = 0;
}