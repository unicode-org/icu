/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.util.Stack;

abstract class DictionaryBreakEngine implements LanguageBreakEngine {
    protected UnicodeSet fSet = new UnicodeSet();
    private final int fTypes;

    /**
     * @param breakTypes A mask of the break iterators that can use this engine.
     *  For example, (1 << KIND_WORD) | (1 << KIND_LINE) could be used by 
     *  word iterators and line iterators, but not any other kind.
     */
    public DictionaryBreakEngine(int breakTypes) {
        // TODO: consider using a java.util.BitSet with nbits <= 32
        fTypes = breakTypes;
    }

    public boolean handles(int c, int breakType) {
        return (breakType >= 0 && breakType < 32) && // breakType is in range
                ((1 << breakType) & fTypes) != 0 && // this type can use us
                fSet.contains(c); // we recognize the character
    }

    public int findBreaks(CharacterIterator text_, int startPos, int endPos, 
            boolean reverse, int breakType, Stack<Integer> foundBreaks) {
        if (breakType < 0 || breakType >= 32 ||
                ((1 << breakType) & fTypes) == 0) {
            return 0;
        }

        int result = 0;
        UCharacterIterator text = UCharacterIterator.getInstance(text_);
        int start = text.getIndex();
        int current, rangeStart, rangeEnd;
        int c = text.current();
        if (reverse) {
            boolean isDict = fSet.contains(c);
            while ((current = text.getIndex()) > startPos && isDict) {
                c = text.previous();
                isDict = fSet.contains(c);
            }
            rangeStart = (current < startPos) ? startPos :
                current + (isDict ? 0 : 1);
            rangeEnd = start + 1;
        } else {
            while ((current = text.getIndex()) < endPos && fSet.contains(c)) {
                c = text.next();
            }
            rangeStart = start;
            rangeEnd = current;
        }

        result = divideUpDictionaryRange(text, rangeStart, rangeEnd, foundBreaks);
        text.setIndex(current);

        return result;
    }

    protected abstract int divideUpDictionaryRange(UCharacterIterator text, 
            int rangeStart, int rangeEnd, Stack<Integer> foundBreaks);
}
