/*
*   Copyright (C) 1996-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

/*
    7/29/96
        Modified to search portions of an integer array.  Should be retested.
*/

package com.ibm.richtext.styledtext;

/**
 * This class searches a segment of an array of integers.  The segment
 * must be sorted in ascending order (but this class does not verify this).
 * Also, this class aliases the array;  if the array is modified later the
 * search results are undefined.
 */
final class FastIntBinarySearch
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private int dataArray[];
    private int auxStart;
    private int power;

    private int fFirstIndex;

    private static final int exp2[] = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072 };

    public FastIntBinarySearch(int data[])
    {
        this(data, 0, data.length);
    }

    public FastIntBinarySearch(int data[], int firstValidIndex, int validLength)
    {
        setData(data, firstValidIndex, validLength);
    }
    
    public void setData(int data[]) {
        
        setData(data, 0, data.length);
    }
    
    public void setData(int data[], int firstValidIndex, int validLength) {

        if (data.length < 1) throw new IllegalArgumentException();
        if (data.length >= exp2[exp2.length-1]) throw new IllegalArgumentException();

        dataArray = data;
        fFirstIndex = firstValidIndex;

        for (power = exp2.length-1; power > 0 && validLength < exp2[power]; power--) {}

        // at this point, array.length >= 2^power

        auxStart = validLength - exp2[power];
    }
    
    /**
     * Return the index in the array of the first element which is at least
     * as large as <tt>value</tt>.  If value is larger than the largest
     * element in the array the last valid index in the array is returned.
     */
    public int findIndex(int value)
    {
        int index = exp2[power]-1 + fFirstIndex;
        if (value >= dataArray[auxStart + fFirstIndex]) {
            index += auxStart;
        }

        // at this point, index is the "upper limit" of the search

        switch (power) {
        case 17:
            if (value < dataArray[index-65536]) index -= 65536;
        case 16:
            if (value < dataArray[index-32768]) index -= 32768;
        case 15:
            if (value < dataArray[index-16384]) index -= 16384;
        case 14:
            if (value < dataArray[index-8192]) index -= 8192;
        case 13:
            if (value < dataArray[index-4096]) index -= 4096;
        case 12:
            if (value < dataArray[index-2048]) index -= 2048;
        case 11:
            if (value < dataArray[index-1024]) index -= 1024;
        case 10:
            if (value < dataArray[index-512]) index -= 512;
        case 9:
            if (value < dataArray[index-256]) index -= 256;
        case 8:
            if (value < dataArray[index-128]) index -= 128;
        case 7:
            if (value < dataArray[index-64]) index -= 64;
        case 6:
            if (value < dataArray[index-32]) index -= 32;
        case 5:
            if (value < dataArray[index-16]) index -= 16;
        case 4:
            if (value < dataArray[index-8]) index -= 8;
        case 3:
            if (value < dataArray[index-4]) index -= 4;
        case 2:
            if (value < dataArray[index-2]) index -= 2;
        case 1:
            if (value < dataArray[index-1]) index -= 1;
        case 0:
            if (value < dataArray[index]) index -= 1;
        }
        return index;
    }
}
