/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.demo.translit;

/** VERY Basic Diff program. Compares two sequences of objects fed into it, and
 * lets you know where they are different.
 * @author Mark Davis
 * @version 1.0
 */

final public class Differ {
    public static final String copyright =
      "Copyright (C) 2000, International Business Machines Corporation and others. All Rights Reserved.";

    /**
     * @param stackSize The size of the largest difference you expect.
     * @param matchCount The number of items that have to be the same to count as a match
     */
    public Differ(int stackSize, int matchCount) {
        this.STACKSIZE = stackSize;
        this.EQUALSIZE = matchCount;
        a = new Object[stackSize+matchCount];
        b = new Object[stackSize+matchCount];
    }

    public void add (Object aStr, Object bStr) {
        addA(aStr);
        addB(bStr);
    }

    public void addA (Object aStr) {
        flush();
        a[aCount++] = aStr;
    }

    public void addB (Object bStr) {
        flush();
        b[bCount++] = bStr;
    }

    public int getALine(int offset) {
        return aLine + maxSame + offset;
    }

    public Object getA(int offset) {
        if (offset < 0) return last;
        if (offset > aTop-maxSame) return next;
        return a[offset];
    }

    public int getACount() {
        return aTop-maxSame;
    }

    public int getBCount() {
        return bTop-maxSame;
    }

    public int getBLine(int offset) {
        return bLine + maxSame + offset;
    }

    public Object getB(int offset) {
        if (offset < 0) return last;
        if (offset > bTop-maxSame) return next;
        return b[offset];
    }

    public void checkMatch(boolean finalPass) {
        // find the initial strings that are the same
        int max = aCount;
        if (max > bCount) max = bCount;
        int i;
        for (i = 0; i < max; ++i) {
            if (!a[i].equals(b[i])) break;
        }
        // at this point, all items up to i are equal
        maxSame = i;
        aTop = bTop = maxSame;
        if (maxSame > 0) last = a[maxSame-1];
        next = "";

        if (finalPass) {
            aTop = aCount;
            bTop = bCount;
            next = "";
            return;
        }

        if (aCount - maxSame < EQUALSIZE || bCount - maxSame < EQUALSIZE) return;

        // now see if the last few a's occur anywhere in the b's, or vice versa
        int match = find (a, aCount-EQUALSIZE, aCount, b, maxSame, bCount);
        if (match != -1) {
            aTop = aCount-EQUALSIZE;
            bTop = match;
            next = a[aTop];
            return;
        }
        match = find (b, bCount-EQUALSIZE, bCount, a, maxSame, aCount);
        if (match != -1) {
            bTop = bCount-EQUALSIZE;
            aTop = match;
            next = b[bTop];
            return;
        }
        if (aCount >= STACKSIZE || bCount >= STACKSIZE) {
            // flush some of them
            aCount = (aCount + maxSame) / 2;
            bCount = (bCount + maxSame) / 2;
            next = "";
        }
    }

    /** Convenient utility
     * finds a segment of the first array in the second array.
     * @return -1 if not found, otherwise start position in b
     */

    public int find (Object[] a, int aStart, int aEnd, Object[] b, int bStart, int bEnd) {
        int len = aEnd - aStart;
        int bEndMinus = bEnd - len;
        tryA:
        for (int i = bStart; i <= bEndMinus; ++i) {
            for (int j = 0; j < len; ++j) {
                if (!b[i + j].equals(a[aStart + j])) continue tryA;
            }
            return i; // we have a match!
        }
        return -1;
    }

    // ====================== PRIVATES ======================

    private void flush() {
        if (aTop != 0) {
            int newCount = aCount-aTop;
            System.arraycopy(a, aTop, a, 0, newCount);
            aCount = newCount;
            aLine += aTop;
            aTop = 0;
        }

        if (bTop != 0) {
            int newCount = bCount-bTop;
            System.arraycopy(b, bTop, b, 0, newCount);
            bCount = newCount;
            bLine += bTop;
            bTop = 0;
        }
    }

    private int STACKSIZE;
    private int EQUALSIZE;

    private Object [] a;
    private Object [] b;
    private Object last = "";
    private Object next = "";
    private int aCount = 0;
    private int bCount = 0;
    private int aLine = 1;
    private int bLine = 1;
    private int maxSame = 0, aTop = 0, bTop = 0;

}
