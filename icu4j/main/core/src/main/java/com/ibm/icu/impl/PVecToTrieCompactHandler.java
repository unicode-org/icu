// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

/*
 * @author Shaopeng Jia
 */

package com.ibm.icu.impl;

import com.ibm.icu.impl.PropsVectors.CompactHandler;

public class PVecToTrieCompactHandler implements CompactHandler {
    public IntTrieBuilder builder;
    public int initialValue;

    @Override
    public void setRowIndexForErrorValue(int rowIndex) {
    }

    @Override
    public void setRowIndexForInitialValue(int rowIndex) {
        initialValue = rowIndex;
    }

    @Override
    public void setRowIndexForRange(int start, int end, int rowIndex) {
        builder.setRange(start, end + 1, rowIndex, true);
    }

    @Override
    public void startRealValues(int rowIndex) {
        if (rowIndex > 0xffff) {
            // too many rows for a 16-bit trie
            throw new IndexOutOfBoundsException();
        } else {
            builder = new IntTrieBuilder(null, 100000, initialValue,
                    initialValue, false);
        }
    }
}
