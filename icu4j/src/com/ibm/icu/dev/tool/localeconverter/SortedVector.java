/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.util.Vector;
import java.util.Enumeration;
/**
    Implements a sorted vector. You can add anything to it; when you access any element, it sorts
    the array internally when necessary.
    <p>A Comparator is used to compare the elements, allowing arbitrary orderings.
    If no Comparator is supplied, then one is constructed based on the type
    of the first element added. Only Numbers and Comparables are handled.
    <p>Duplicates are allowed.
*/
final public class SortedVector {
    /**
        Copies elements of vector, enumeration or array
        Note: the objects in the source are NOT cloned.
        Do not change them or the sorting will be invalid.
    */
    public SortedVector(Object[] newValues, Comparator comparator) {
        this.comparator = comparator;
        addElements(newValues);
            //{{INIT_CONTROLS
//}}
}

    public SortedVector(Vector newValues, Comparator comparator) {
        this.comparator = comparator;
        addElements(newValues);
    }

    public SortedVector(Enumeration newValues, Comparator comparator) {
        this.comparator = comparator;
        addElements(newValues);
    }

    public SortedVector(Object[] newValues) {
        addElements(newValues);
    }

    public SortedVector(Comparator comparator) {
        this.comparator = comparator;
    }

    public SortedVector(Vector newValues) {
        addElements(newValues);
    }

    public SortedVector(Enumeration newValues) {
        addElements(newValues);
    }

    public SortedVector() {
    }

    /**
        Adds one element.
    */
    public void addElement(Object element) {
        if (count >= dataArray.length) setCapacity(count*2 + 17);
        dataArray[count++] = element;
        isValid = false;
    }

    /**
        Adds multiple elements. Faster than adding one at a time.
    */
    public void addElements(Object[] newValues) {
        int newCount = count + newValues.length;
        if (newCount > dataArray.length) setCapacity(newCount);
        for (int i = count; i < newCount; ++i)
            dataArray[i] = newValues[i-count];
        count = newCount;
        isValid = false;
    }

    public void addElements(Vector newValues) {
        int newCount = count + newValues.size();
        if (newCount > dataArray.length) setCapacity(newCount);
        for (int i = count; i < newCount; ++i)
            dataArray[i] = newValues.elementAt(i-count);
        count = newCount;
        isValid = false;
    }

    public void addElements(Enumeration newValues) {
        while (newValues.hasMoreElements()) {
            addElement(newValues.nextElement());
        }
    }

    /**
        Removes elements at indices >= startIndex and < endIndex
    */
    public void removeElements(int startIndex, int endIndex) {
        if (!isValid) validate();
        System.arraycopy(dataArray,endIndex,dataArray,startIndex,count - endIndex);
        for (int i = count - (endIndex - startIndex); i < count;++i)
            dataArray[i] = null;   // free up storage
        count -= (endIndex - startIndex);
    }

    /**
        Sets comparator
    */
    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
        isValid = false;
    }

    public Comparator getComparator() {
        if (comparator == null) validateComparator();
        return this.comparator;
    }

    /**
        Gets size, the actual number of elements.
    */
    public int size() {
        return count;
    }

    /**
        Gets capacity, the number of elements you can have without growing the array.
    */
    public int capacity() {
        return dataArray.length;
    }

    /**
        Sets capacity, the number of elements you can have without growing the array.
    */
    public void setCapacity(int newSize) {
        Object[] temp = new Object[newSize];
        System.arraycopy(dataArray, 0, temp, 0, Math.min(count,newSize));
        dataArray = temp;
    }

    /**
        Trims the array.
    */
    public void trimToSize() {
        setCapacity(count);
    }

    /**
        Gets the element at the index
    */
    public Object elementAt (int index) {
        if (!isValid) validate();
        if (index >= count) return dataArray[dataArray.length];
        return dataArray[index];
    }

    /**
        Sees whether the vector contains the object
    */
    public boolean contains (Object value) {
        int index = indexOf(value);
        return (index >= 0 && comparator.compare(value,dataArray[index]) == 0);
    }

    /**
        Gets an enumeration
    */
    public Enumeration elements() {
        if (!isValid) validate();
        return new ArrayEnumeration(dataArray,0,count);
    }

    public void copyInto(Object[] toFill) {
        if (!isValid) validate();
        System.arraycopy(dataArray,0,toFill,0,toFill.length);
    }

    /**
        Finds first index whose value is greater than or equal to searchValue
        If there are none, returns -1
    */
    public int indexOf(Object searchValue)
    {
        if (!isValid) validate();
        int index = startIndex;
        if (0 <= comparator.compare(searchValue, dataArray[auxStart])) {
            index += auxStart;
        }
        // very fast, completely unrolled binary search
        // each case deliberately falls through to the next
        switch (power) {
        case 31: if (0 > comparator.compare(searchValue, dataArray[index-0x40000000])) index -= 0x40000000;
        case 30: if (0 > comparator.compare(searchValue, dataArray[index-0x20000000])) index -= 0x20000000;
        case 29: if (0 > comparator.compare(searchValue, dataArray[index-0x10000000])) index -= 0x10000000;

        case 28: if (0 > comparator.compare(searchValue, dataArray[index-0x8000000])) index -= 0x8000000;
        case 27: if (0 > comparator.compare(searchValue, dataArray[index-0x4000000])) index -= 0x4000000;
        case 26: if (0 > comparator.compare(searchValue, dataArray[index-0x2000000])) index -= 0x2000000;
        case 25: if (0 > comparator.compare(searchValue, dataArray[index-0x1000000])) index -= 0x1000000;

        case 24: if (0 > comparator.compare(searchValue, dataArray[index-0x800000])) index -= 0x800000;
        case 23: if (0 > comparator.compare(searchValue, dataArray[index-0x400000])) index -= 0x400000;
        case 22: if (0 > comparator.compare(searchValue, dataArray[index-0x200000])) index -= 0x200000;
        case 21: if (0 > comparator.compare(searchValue, dataArray[index-0x100000])) index -= 0x100000;

        case 20: if (0 > comparator.compare(searchValue, dataArray[index-0x80000])) index -= 0x80000;
        case 19: if (0 > comparator.compare(searchValue, dataArray[index-0x40000])) index -= 0x40000;
        case 18: if (0 > comparator.compare(searchValue, dataArray[index-0x20000])) index -= 0x20000;
        case 17: if (0 > comparator.compare(searchValue, dataArray[index-0x10000])) index -= 0x10000;

        case 16: if (0 > comparator.compare(searchValue, dataArray[index-0x8000])) index -= 0x8000;
        case 15: if (0 > comparator.compare(searchValue, dataArray[index-0x4000])) index -= 0x4000;
        case 14: if (0 > comparator.compare(searchValue, dataArray[index-0x2000])) index -= 0x2000;
        case 13: if (0 > comparator.compare(searchValue, dataArray[index-0x1000])) index -= 0x1000;

        case 12: if (0 > comparator.compare(searchValue, dataArray[index-0x800])) index -= 0x800;
        case 11: if (0 > comparator.compare(searchValue, dataArray[index-0x400])) index -= 0x400;
        case 10: if (0 > comparator.compare(searchValue, dataArray[index-0x200])) index -= 0x200;
        case  9: if (0 > comparator.compare(searchValue, dataArray[index-0x100])) index -= 0x100;

        case  8: if (0 > comparator.compare(searchValue, dataArray[index-0x80])) index -= 0x80;
        case  7: if (0 > comparator.compare(searchValue, dataArray[index-0x40])) index -= 0x40;
        case  6: if (0 > comparator.compare(searchValue, dataArray[index-0x20])) index -= 0x20;
        case  5: if (0 > comparator.compare(searchValue, dataArray[index-0x10])) index -= 0x10;

        case  4: if (0 > comparator.compare(searchValue, dataArray[index-0x8])) index -= 8;
        case  3: if (0 > comparator.compare(searchValue, dataArray[index-0x4])) index -= 4;
        case  2: if (0 > comparator.compare(searchValue, dataArray[index-0x2])) index -= 2;
        case  1: if (0 > comparator.compare(searchValue, dataArray[index-0x1])) index -= 1;

        case  0: if (0 > comparator.compare(searchValue, dataArray[index])) index -= 1;
        }
        return index;
    }

    // ================= privates ==================
    /** Only call if comparator is null
    */
    private void validateComparator() {
        try {
            Object trial = dataArray[0];
            if (trial instanceof Float || trial instanceof Double) {
                comparator = new DoubleComparator();
            } else if (trial instanceof Integer) {
                comparator = new IntegerComparator();
            } else if (trial instanceof Number) {
                comparator = new LongComparator();
            } else if (trial instanceof String) {
                comparator = new StringComparator();
            } else {
                comparator = new ComparableComparator();
            }
        } catch (Exception e) {} // leave null
    }

    private void validate() {
        if (isValid) return;
        // if the Comparator is null, then pick a reasonable one
        if (comparator == null) validateComparator();

        // determine search parameters

        // find least power of 2 greater than count
        for (power = exp2.length-1; power > 0 && count < exp2[power]; power--) {}

        // determine the starting point
        if (exp2[power] != count) {
            auxStart = count - exp2[power];
        } else {
            auxStart = 0;
        }
        startIndex = exp2[power]-1;

        // shell sort. Later, make this a QuickSort
        int lo = 0;
        int up = count-1;
        for (int step = up - lo + 1; step > 1;) {
            if (step < 5)
                step = 1;
            else step = (5 * step - 1) / 11;
            for (int i = up - step; i >= lo; --i) {
                Object temp = dataArray[i];
                int j;
                for (j = i + step; j <= up && 0 > comparator.compare(dataArray[j],temp); j += step)
                    dataArray[j-step] = dataArray[j];
                dataArray[j-step] = temp;
            }
        }
        isValid = true;
    }

    private Object[] dataArray = new Object[16];
    private Comparator comparator;
    private int count = 0;
    private boolean isValid = false;
    private int auxStart;
    private int startIndex;
    private int power;
    private static final int exp2[] = {
        0x1, 0x2, 0x4, 0x8,
        0x10, 0x20, 0x40, 0x80,
        0x100, 0x200, 0x400, 0x800,
        0x1000, 0x2000, 0x4000, 0x8000,
        0x10000, 0x20000, 0x40000, 0x80000,
        0x100000, 0x200000, 0x400000, 0x800000,
        0x1000000, 0x2000000, 0x4000000, 0x8000000,
        0x10000000, 0x20000000, 0x40000000};

    // Utility Classes

    public static final class LongComparator implements Comparator {
        public int compare(Object a, Object b) {
            long aa = ((Number)a).longValue();
            long bb = ((Number)b).longValue();
            return (aa < bb ? -1 : aa > bb ? 1 : 0);
        }
    }

    public static final class IntegerComparator implements Comparator {
        public int compare(Object a, Object b) {
            return (((Number)a).intValue() - ((Number)b).intValue());
        }
    }

    public static final class DoubleComparator implements Comparator {
        public int compare(Object a, Object b) {
            double aa = ((Number)a).doubleValue();
            double bb = ((Number)b).doubleValue();
            return (aa < bb ? -1 : aa > bb ? 1 : 0);
        }
    }

    public static final class ComparableComparator implements Comparator {
        public int compare(Object a, Object b) {
            return ((Comparable)a).compareTo(b);
        }
    }

    public static final class StringComparator implements Comparator {
        public int compare(Object a, Object b) {
            return ((String)a).compareTo((String)b);
        };
    }
    //{{DECLARE_CONTROLS
//}}
}

