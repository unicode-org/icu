/*
 ******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

/* 
 * This is a port of the C++ class UConverterSelector. 
 *
 * Methods related to serialization are not ported in this version. In addition,
 * the selectForUTF8 method is not going to be ported, as UTF8 is seldom used
 * in Java.
 * 
 * @author Shaopeng Jia
 */

package com.ibm.icu.charset;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Vector;

import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.impl.PropsVectors;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Charset Selector
 * 
 * A charset selector is built with a list of charset names and given an input
 * CharSequence returns the list of names the corresponding charsets which can
 * convert the CharSequence.
 * 
 * @draft ICU 4.2
 */
public final class CharsetSelector {
    private IntTrie trie;
    private int[] pv; // table of bits
    private String[] encodings; // encodings users ask to use

    private void generateSelectorData(PropsVectors pvec,
            UnicodeSet excludedCodePoints, int mappingTypes) {
        int columns = (encodings.length + 31) / 32;

        // set errorValue to all-ones
        for (int col = 0; col < columns; ++col) {
            pvec.setValue(PropsVectors.ERROR_VALUE_CP,
                    PropsVectors.ERROR_VALUE_CP, col, ~0, ~0);
        }

        for (int i = 0; i < encodings.length; ++i) {
            Charset testCharset = CharsetICU.forNameICU(encodings[i]);
            UnicodeSet unicodePointSet = new UnicodeSet(); // empty set
            ((CharsetICU) testCharset).getUnicodeSet(unicodePointSet,
                    mappingTypes);
            int column = i / 32;
            int mask = 1 << (i % 32);
            // now iterate over intervals on set i
            int itemCount = unicodePointSet.getRangeCount();
            for (int j = 0; j < itemCount; ++j) {
                int startChar = unicodePointSet.getRangeStart(j);
                int endChar = unicodePointSet.getRangeEnd(j);
                pvec.setValue(startChar, endChar, column, ~0, mask);
            }
        }

        // handle excluded encodings
        // Simply set their values to all 1's in the pvec
        if (!excludedCodePoints.isEmpty()) {
            int itemCount = excludedCodePoints.getRangeCount();
            for (int j = 0; j < itemCount; ++j) {
                int startChar = excludedCodePoints.getRangeStart(j);
                int endChar = excludedCodePoints.getRangeEnd(j);
                for (int col = 0; col < columns; col++) {
                    pvec.setValue(startChar, endChar, col, ~0, ~0);
                }
            }
        }

        trie = pvec.compactToTrieWithRowIndexes();
        pv = pvec.getCompactedArray();
    }

    // internal function to intersect two sets of masks
    // returns whether the mask has reduced to all zeros. The
    // second set of mask consists of len elements in pv starting from 
    // pvIndex
    private boolean intersectMasks(int[] dest, int pvIndex, int len) {
        int oredDest = 0;
        for (int i = 0; i < len; ++i) {
            oredDest |= (dest[i] &= pv[pvIndex + i]);
        }
        return oredDest == 0;
    }

    // internal function
    private List selectForMask(int[] mask) {
        // this is the context we will use. Store a table of indices to which
        // encodings are legit

        Vector result = new Vector();
        int columns = (encodings.length + 31) / 32;
        int numOnes = countOnes(mask, columns);

        // now we know the exact space we need to index
        if (numOnes > 0) {
            int k = 0;
            for (int j = 0; j < columns; j++) {
                int v = mask[j];
                for (int i = 0; i < 32 && k < encodings.length; i++, k++) {
                    if ((v & 1) != 0) {
                        result.addElement(encodings[k]);
                    }
                    v >>= 1;
                }
            }
        }

        // otherwise, index will remain NULL
        return result;
    }

    // internal function to count how many 1's are there in a mask
    // algorithm taken from http://graphics.stanford.edu/~seander/bithacks.html
    private int countOnes(int[] mask, int len) {
        int totalOnes = 0;
        for (int i = 0; i < len; ++i) {
            int ent = mask[i];
            for (; ent != 0; totalOnes++) {
                ent &= ent - 1; // clear the least significant bit set
            }
        }
        return totalOnes;
    }

    /**
     * Construct a CharsetSelector from a list of charset names.
     * 
     * @param charsetList
     *            a list of charset names in the form of strings. If charsetList
     *            is empty, a selector for all available charset is constructed.
     * @param excludedCodePoints
     *            a set of code points to be excluded from consideration.
     *            Excluded code points appearing in the input CharSequence do
     *            not change the selection result. It could be empty when no
     *            code point should be excluded.
     * @param mappingTypes
     *            an int which determines whether to consider only roundtrip
     *            mappings or also fallbacks, e.g. CharsetICU.ROUNDTRIP_SET. See
     *            CharsetICU.java for the constants that are currently
     *            supported.
     * @throws IllegalArgumentException
     *             if the parameters is invalid.
     * @throws IllegalCharsetNameException
     *             If the given charset name is illegal.
     * @throws UnsupportedCharsetException
     *             If no support for the named charset is available in this
     *             instance of the Java virtual machine.
     * @draft ICU 4.2
     */
    public CharsetSelector(List charsetList, UnicodeSet excludedCodePoints,
            int mappingTypes) {
        if (mappingTypes != CharsetICU.ROUNDTRIP_AND_FALLBACK_SET
                && mappingTypes != CharsetICU.ROUNDTRIP_SET) {
            throw new IllegalArgumentException("Unsupported mappingTypes");
        }

        int encodingCount = charsetList.size();
        if (encodingCount > 0) {
            encodings = new String[encodingCount];
            for (int i = 0; i < encodingCount; i++) {
                encodings[i] = (String) charsetList.get(i);
            }
        } else {
            Object[] availableNames = CharsetProviderICU.getAvailableNames();
            encodingCount = availableNames.length;
            encodings = new String[encodingCount];
            for (int i = 0; i < encodingCount; i++) {
                encodings[i] = (String) availableNames[i];
            }
        }

        PropsVectors pvec = new PropsVectors((encodingCount + 31) / 32);
        generateSelectorData(pvec, excludedCodePoints, mappingTypes);
    }

    /**
     * Select charsets that can map all characters in a CharSequence, ignoring
     * the excluded code points.
     * 
     * @param unicodeText
     *            a CharSequence. It could be empty.
     * @return a list that contains charset names in the form of strings. The
     *         returned encoding names and their order will be the same as
     *         supplied when building the selector.
     * 
     * @draft ICU 4.2
     */
    public List selectForString(CharSequence unicodeText) {
        int columns = (encodings.length + 31) / 32;
        int[] mask = new int[columns];
        for (int i = 0; i < columns; i++) {
            mask[i] = - 1; // set each bit to 1
                           // Note: All integers are signed in Java, assigning
                           // 2 ^ 32 -1 to mask is wrong!
        }
        int index = 0;
        while (index < unicodeText.length()) {
            int c = UTF16.charAt(unicodeText, index);
            int pvIndex = trie.getCodePointValue(c);
            index += UTF16.getCharCount(c);
            if (intersectMasks(mask, pvIndex, columns)) {
                break;
            }
        }
        return selectForMask(mask);
    }
}
