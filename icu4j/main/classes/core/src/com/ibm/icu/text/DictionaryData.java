/*
 *******************************************************************************
 * Copyright (C) 2012-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

final class DictionaryData {
    // disallow instantiation
    private DictionaryData() { }

    public static final int TRIE_TYPE_BYTES = 0;
    public static final int TRIE_TYPE_UCHARS = 1;
    public static final int TRIE_TYPE_MASK = 7;
    public static final int TRIE_HAS_VALUES = 8;
    public static final int TRANSFORM_NONE = 0;
    public static final int TRANSFORM_TYPE_OFFSET = 0x1000000;
    public static final int TRANSFORM_TYPE_MASK = 0x7f000000;
    public static final int TRANSFORM_OFFSET_MASK = 0x1fffff;

    public static final int IX_STRING_TRIE_OFFSET = 0;
    public static final int IX_RESERVED1_OFFSET = 1;
    public static final int IX_RESERVED2_OFFSET = 2;
    public static final int IX_TOTAL_SIZE = 3;
    public static final int IX_TRIE_TYPE = 4;
    public static final int IX_TRANSFORM = 5;
    public static final int IX_RESERVED6 = 6;
    public static final int IX_RESERVED7 = 7;
    public static final int IX_COUNT = 8;

    private static final int DATA_FORMAT_ID = 0x44696374;

    public static DictionaryMatcher loadDictionaryFor(String dictType) throws IOException {
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BRKITR_BASE_NAME);
        String dictFileName = rb.getStringWithFallback("dictionaries/" + dictType);
        dictFileName = ICUData.ICU_BRKITR_NAME + '/' + dictFileName;
        ByteBuffer bytes = ICUBinary.getRequiredData(dictFileName);
        ICUBinary.readHeader(bytes, DATA_FORMAT_ID, null);
        int[] indexes = new int[IX_COUNT];
        // TODO: read indexes[IX_STRING_TRIE_OFFSET] first, then read a variable-length indexes[]
        for (int i = 0; i < IX_COUNT; i++) {
            indexes[i] = bytes.getInt();
        }
        int offset = indexes[IX_STRING_TRIE_OFFSET];
        Assert.assrt(offset >= (4 * IX_COUNT));
        if (offset > (4 * IX_COUNT)) {
            int diff = offset - (4 * IX_COUNT);
            ICUBinary.skipBytes(bytes, diff);
        }
        int trieType = indexes[IX_TRIE_TYPE] & TRIE_TYPE_MASK;
        int totalSize = indexes[IX_TOTAL_SIZE] - offset;
        DictionaryMatcher m = null;
        if (trieType == TRIE_TYPE_BYTES) {
            int transform = indexes[IX_TRANSFORM];
            byte[] data = new byte[totalSize];
            int i;
            for (i = 0; i < data.length; i++) {
                data[i] = bytes.get();
            }
            Assert.assrt(i == totalSize);
            m = new BytesDictionaryMatcher(data, transform);
        } else if (trieType == TRIE_TYPE_UCHARS) {
            Assert.assrt(totalSize % 2 == 0);
            int num = totalSize / 2;
            char[] data = new char[totalSize / 2];
            for (int i = 0; i < num; i++) {
                data[i] = bytes.getChar();
            }
            m = new CharsDictionaryMatcher(new String(data));
        } else {
            m = null;
        }
        return m;
    }
}
