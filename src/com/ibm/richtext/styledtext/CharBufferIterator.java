/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.styledtext;

import java.text.CharacterIterator;

final class CharBufferIterator implements CharacterIterator,
                                          Cloneable
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private int fRangeStart;
    private int fRangeLimit;
    private int fCurrentIndex;
    private char fStorage[];
    private int fGap;
    private int fGapLength;
    private Validation fValidation;

    CharBufferIterator(int start,
                       int limit,
                       char[] storage,
                       int length,
                       int gap,
                       Validation validation) {

        if (start > limit) {
            throw new IllegalArgumentException("start > limit");
        }
        fRangeStart = start;
        fRangeLimit = limit;
        fCurrentIndex = fRangeStart;
        fStorage = storage;
        fGap = gap;
        fGapLength = (storage==null? 0 : storage.length) - length;
        fValidation = validation;
    }

    private void checkValidation() {

        if (!fValidation.isValid()) {
            throw new Error("Iterator is no longer valid");
        }
    }

    public char first()
    {
        return setIndex(fRangeStart);
    }

    public char last()
    {
        return setIndex(fRangeLimit - 1);
    }

    public char current()
    {
        checkValidation();
        if (fCurrentIndex < fRangeStart || fCurrentIndex >= fRangeLimit)
            return DONE;
        int i = (fCurrentIndex < fGap) ? fCurrentIndex : (fCurrentIndex + fGapLength);
        return fStorage[i];
    }

    public char next()
    {
        checkValidation();
        fCurrentIndex++;
        if (fCurrentIndex >= fRangeLimit)
        {
            fCurrentIndex = fRangeLimit;
            return DONE;
        }
        int i = (fCurrentIndex < fGap) ? fCurrentIndex : (fCurrentIndex + fGapLength);
        return fStorage[i];
    }

    public char previous()
    {
        fCurrentIndex--;
        if (fCurrentIndex >= fRangeStart)
            return current();
        fCurrentIndex = fRangeStart;
        return DONE;
    }

    public char setIndex(int i)
    {
        if (i < fRangeStart || i > fRangeLimit)
            throw new IllegalArgumentException("Invalid position");
        fCurrentIndex = i;
        return current();
    }

    public int getBeginIndex()
    {
        return fRangeStart;
    }

    public int getEndIndex()
    {
        return fRangeLimit;
    }

    public int getIndex()
    {
        return fCurrentIndex;
    }

    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
