// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2016, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.demo.translit;

/**
 * VERY Basic Diff program. Compares two sequences of ints fed into it, and
 * lets you know where they are different.
 *
 * <p>This version compares ints while the CLDR class Differ compares Objects.
 *
 * @author Mark Davis
 * @version 1.0
 */
final public class IntDiffer {
    /**
     * @param stackSize The size of the largest difference you expect.
     * @param matchCount The number of items that have to be the same to count as a match
     */
    public IntDiffer(int stackSize, int matchCount) {
        this.STACKSIZE = stackSize;
        this.EQUALSIZE = matchCount;
        a = new int[stackSize+matchCount];
        b = new int[stackSize+matchCount];
    }

    public void addA(int aStr) {
        flush();
        a[aCount++] = aStr;
    }

    public void addB(int bStr) {
        flush();
        b[bCount++] = bStr;
    }

    public int getA(int offset) {
        return a[maxSame + offset];
    }

    public int getACount() {
        return aTop-maxSame;
    }

    public int getBCount() {
        return bTop-maxSame;
    }

    public int getB(int offset) {
        return b[maxSame + offset];
    }

    /**
     * Checks for initial & final match.
     * To be called after addA() and addB().
     * Middle segments that are different are returned via get*Count() and get*().
     *
     * @param finalPass true if no more input
     */
    public void checkMatch(boolean finalPass) {
        // find the initial strings that are the same
        int max = aCount;
        if (max > bCount) max = bCount;
        int i;
        for (i = 0; i < max; ++i) {
            if (a[i] != b[i]) break;
        }
        // at this point, all items up to i are equal
        maxSame = i;
        aTop = bTop = maxSame;

        if (finalPass) {
            aTop = aCount;
            bTop = bCount;
            return;
        }

        if (aCount - maxSame < EQUALSIZE || bCount - maxSame < EQUALSIZE) return;

        // now see if the last few a's occur anywhere in the b's, or vice versa
        int match = find(a, aCount-EQUALSIZE, aCount, b, maxSame, bCount);
        if (match != -1) {
            aTop = aCount-EQUALSIZE;
            bTop = match;
            return;
        }
        match = find(b, bCount-EQUALSIZE, bCount, a, maxSame, aCount);
        if (match != -1) {
            bTop = bCount-EQUALSIZE;
            aTop = match;
            return;
        }
        if (aCount >= STACKSIZE || bCount >= STACKSIZE) {
            // flush some of them
            aCount = (aCount + maxSame) / 2;
            bCount = (bCount + maxSame) / 2;
        }
    }

    /**
     * Finds a segment of the first array in the second array.
     * @return -1 if not found, otherwise start position in bArr
     */
    private int find(int[] aArr, int aStart, int aEnd, int[] bArr, int bStart, int bEnd) {
        int len = aEnd - aStart;
        int bEndMinus = bEnd - len;
        tryA:
        for (int i = bStart; i <= bEndMinus; ++i) {
            for (int j = 0; j < len; ++j) {
                if (bArr[i + j] != aArr[aStart + j]) continue tryA;
            }
            return i; // we have a match!
        }
        return -1;
    }

    // ====================== PRIVATES ======================

    /** Removes equal prefixes of both arrays. */
    private void flush() {
        if (aTop != 0) {
            int newCount = aCount-aTop;
            System.arraycopy(a, aTop, a, 0, newCount);
            aCount = newCount;
            aTop = 0;
        }

        if (bTop != 0) {
            int newCount = bCount-bTop;
            System.arraycopy(b, bTop, b, 0, newCount);
            bCount = newCount;
            bTop = 0;
        }
    }

    private int STACKSIZE;
    private int EQUALSIZE;

    // a[] and b[] are equal at 0 to before maxSame.
    // maxSame to before *Top are different.
    // *Top to *Count are equal again.
    private int [] a;
    private int [] b;
    private int aCount = 0;
    private int bCount = 0;
    private int maxSame = 0, aTop = 0, bTop = 0;
}
