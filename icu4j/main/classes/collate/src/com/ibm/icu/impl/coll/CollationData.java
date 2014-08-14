/*
*******************************************************************************
* Copyright (C) 2010-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* CollationData.java, ported from collationdata.h/.cpp
*
* C++ version created on: 2010oct27
* created by: Markus W. Scherer
*/

package com.ibm.icu.impl.coll;

import com.ibm.icu.impl.Normalizer2Impl;
import com.ibm.icu.impl.Trie2_32;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Collation data container.
 * Immutable data created by a CollationDataBuilder, or loaded from a file,
 * or deserialized from API-provided binary data.
 *
 * Includes data for the collation base (root/default), aliased if this is not the base.
 */
public final class CollationData {
    CollationData(Normalizer2Impl nfc) {
        nfcImpl = nfc;
    }

    public int getCE32(int c) {
        return trie.get(c);
    }

    int getCE32FromSupplementary(int c) {
        return trie.get(c);  // TODO: port UTRIE2_GET32_FROM_SUPP(trie, c) to Java?
    }

    boolean isDigit(int c) {
        return c < 0x660 ? c <= 0x39 && 0x30 <= c :
                Collation.hasCE32Tag(getCE32(c), Collation.DIGIT_TAG);
    }

    public boolean isUnsafeBackward(int c, boolean numeric) {
        return unsafeBackwardSet.contains(c) || (numeric && isDigit(c));
    }

    public boolean isCompressibleLeadByte(int b) {
        return compressibleBytes[b];
    }

    public boolean isCompressiblePrimary(long p) {
        return isCompressibleLeadByte((int)p >>> 24);
    }

    /**
     * Returns the CE32 from two contexts words.
     * Access to the defaultCE32 for contraction and prefix matching.
     */
    int getCE32FromContexts(int index) {
        return ((int)contexts.charAt(index) << 16) | contexts.charAt(index + 1);
    }

    /**
     * Returns the CE32 for an indirect special CE32 (e.g., with DIGIT_TAG).
     * Requires that ce32 is special.
     */
    int getIndirectCE32(int ce32) {
        assert(Collation.isSpecialCE32(ce32));
        int tag = Collation.tagFromCE32(ce32);
        if(tag == Collation.DIGIT_TAG) {
            // Fetch the non-numeric-collation CE32.
            ce32 = ce32s[Collation.indexFromCE32(ce32)];
        } else if(tag == Collation.LEAD_SURROGATE_TAG) {
            ce32 = Collation.UNASSIGNED_CE32;
        } else if(tag == Collation.U0000_TAG) {
            // Fetch the normal ce32 for U+0000.
            ce32 = ce32s[0];
        }
        return ce32;
    }

    /**
     * Returns the CE32 for an indirect special CE32 (e.g., with DIGIT_TAG),
     * if ce32 is special.
     */
    int getFinalCE32(int ce32) {
        if(Collation.isSpecialCE32(ce32)) {
            ce32 = getIndirectCE32(ce32);
        }
        return ce32;
    }

    /**
     * Computes a CE from c's ce32 which has the OFFSET_TAG.
     */
    long getCEFromOffsetCE32(int c, int ce32) {
        long dataCE = ces[Collation.indexFromCE32(ce32)];
        return Collation.makeCE(Collation.getThreeBytePrimaryForOffsetData(c, dataCE));
    }

    /**
     * Returns the single CE that c maps to.
     * Throws UnsupportedOperationException if c does not map to a single CE.
     */
    long getSingleCE(int c) {
        CollationData d;
        int ce32 = getCE32(c);
        if(ce32 == Collation.FALLBACK_CE32) {
            d = base;
            ce32 = base.getCE32(c);
        } else {
            d = this;
        }
        while(Collation.isSpecialCE32(ce32)) {
            switch(Collation.tagFromCE32(ce32)) {
            case Collation.LATIN_EXPANSION_TAG:
            case Collation.BUILDER_DATA_TAG:
            case Collation.PREFIX_TAG:
            case Collation.CONTRACTION_TAG:
            case Collation.HANGUL_TAG:
            case Collation.LEAD_SURROGATE_TAG:
                throw new UnsupportedOperationException(String.format(
                        "there is not exactly one collation element for U+%04X (CE32 0x%08x)",
                        c, ce32));
            case Collation.FALLBACK_TAG:
            case Collation.RESERVED_TAG_3:
                throw new AssertionError(String.format(
                        "unexpected CE32 tag for U+%04X (CE32 0x%08x)", c, ce32));
            case Collation.LONG_PRIMARY_TAG:
                return Collation.ceFromLongPrimaryCE32(ce32);
            case Collation.LONG_SECONDARY_TAG:
                return Collation.ceFromLongSecondaryCE32(ce32);
            case Collation.EXPANSION32_TAG:
                if(Collation.lengthFromCE32(ce32) == 1) {
                    ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                    break;
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "there is not exactly one collation element for U+%04X (CE32 0x%08x)",
                            c, ce32));
                }
            case Collation.EXPANSION_TAG: {
                if(Collation.lengthFromCE32(ce32) == 1) {
                    return d.ces[Collation.indexFromCE32(ce32)];
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "there is not exactly one collation element for U+%04X (CE32 0x%08x)",
                            c, ce32));
                }
            }
            case Collation.DIGIT_TAG:
                // Fetch the non-numeric-collation CE32 and continue.
                ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                break;
            case Collation.U0000_TAG:
                assert(c == 0);
                // Fetch the normal ce32 for U+0000 and continue.
                ce32 = d.ce32s[0];
                break;
            case Collation.OFFSET_TAG:
                return d.getCEFromOffsetCE32(c, ce32);
            case Collation.IMPLICIT_TAG:
                return Collation.unassignedCEFromCodePoint(c);
            }
        }
        return Collation.ceFromSimpleCE32(ce32);
    }

    /**
     * Returns the FCD16 value for code point c. c must be >= 0.
     */
    int getFCD16(int c) {
        return nfcImpl.getFCD16(c);
    }

    /**
     * Returns the first primary for the script's reordering group.
     * @return the primary with only the first primary lead byte of the group
     *         (not necessarily an actual root collator primary weight),
     *         or 0 if the script is unknown
     */
    long getFirstPrimaryForGroup(int script) {
        int index = findScript(script);
        if(index < 0) {
            return 0;
        }
        long head = scripts[index];
        return (head & 0xff00) << 16;
    }

    /**
     * Returns the last primary for the script's reordering group.
     * @return the last primary of the group
     *         (not an actual root collator primary weight),
     *         or 0 if the script is unknown
     */
    public long getLastPrimaryForGroup(int script) {
        int index = findScript(script);
        if(index < 0) {
            return 0;
        }
        int head = scripts[index];
        long lastByte = head & 0xff;
        return ((lastByte + 1) << 24) - 1;
    }

    /**
     * Finds the reordering group which contains the primary weight.
     * @return the first script of the group, or -1 if the weight is beyond the last group
     */
    public int getGroupForPrimary(long p) {
        p >>= 24;  // Reordering groups are distinguished by primary lead bytes.
        for(int i = 0; i < scripts.length; i = i + 2 + scripts[i + 1]) {
            int lastByte = scripts[i] & 0xff;
            if(p <= lastByte) {
                return scripts[i + 2];
            }
        }
        return -1;
    }

    private int findScript(int script) {
        if(script < 0 || 0xffff < script) { return -1; }
        for(int i = 0; i < scripts.length;) {
            int limit = i + 2 + scripts[i + 1];
            for(int j = i + 2; j < limit; ++j) {
                if(script == scripts[j]) { return i; }
            }
            i = limit;
        }
        return -1;
    }

    public int[] getEquivalentScripts(int script) {
        int i = findScript(script);
        if(i < 0) { return EMPTY_INT_ARRAY; }
        int length = scripts[i + 1];
        assert(length != 0);
        int dest[] = new int[length];
        i += 2;
        dest[0] = scripts[i++];
        for(int j = 1; j < length; ++j) {
            script = scripts[i++];
            // Sorted insertion.
            for(int k = j;; --k) {
                // Invariant: dest[k] is free to receive either script or dest[k - 1].
                if(k > 0 && script < dest[k - 1]) {
                    dest[k] = dest[k - 1];
                } else {
                    dest[k] = script;
                    break;
                }
            }
        }
        return dest;
    }

    /**
     * Writes the permutation table for the given reordering of scripts and groups,
     * mapping from default-order primary-weight lead bytes to reordered lead bytes.
     * The caller checks for illegal arguments and
     * takes care of [DEFAULT] and memory allocation.
     */
    public void makeReorderTable(int[] reorder, byte[] table) {
        int length = reorder.length;
        // Initialize the table.
        // Never reorder special low and high primary lead bytes.
        int lowByte;
        for(lowByte = 0; lowByte <= Collation.MERGE_SEPARATOR_BYTE; ++lowByte) {
            table[lowByte] = (byte)lowByte;
        }
        // lowByte == 03

        int highByte;
        for(highByte = 0xff; highByte >= Collation.TRAIL_WEIGHT_BYTE; --highByte) {
            table[highByte] = (byte)highByte;
        }
        // highByte == FE

        // Set intermediate bytes to 0 to indicate that they have not been set yet.
        for(int i = lowByte; i <= highByte; ++i) {
            table[i] = 0;
        }

        // Get the set of special reorder codes in the input list.
        // This supports up to 32 special reorder codes;
        // it works for data with codes beyond Collator.ReorderCodes.LIMIT.
        int specials = 0;
        for(int i = 0; i < length; ++i) {
            int reorderCode = reorder[i] - Collator.ReorderCodes.FIRST;
            if(0 <= reorderCode && reorderCode <= 31) {
                specials |= 1 << reorderCode;
            }
        }

        // Start the reordering with the special low reorder codes that do not occur in the input.
        for(int i = 0;; i += 3) {
            if(scripts[i + 1] != 1) { break; }  // Went beyond special single-code reorder codes.
            int reorderCode = scripts[i + 2] - Collator.ReorderCodes.FIRST;
            if(reorderCode < 0) { break; }  // Went beyond special reorder codes.
            if((specials & (1 << reorderCode)) == 0) {
                int head = scripts[i];
                int firstByte = head >> 8;
                int lastByte = head & 0xff;
                do { table[firstByte++] = (byte)lowByte++; } while(firstByte <= lastByte);
            }
        }

        // Reorder according to the input scripts, continuing from the bottom of the bytes range.
        for(int i = 0; i < length;) {
            int script = reorder[i++];
            if(script == UScript.UNKNOWN) {
                // Put the remaining scripts at the top.
                while(i < length) {
                    script = reorder[--length];
                    if(script == UScript.UNKNOWN) {  // Must occur at most once.
                        throw new IllegalArgumentException(
                                "setReorderCodes(): duplicate UScript.UNKNOWN");
                    }
                    if(script == Collator.ReorderCodes.DEFAULT) {
                        throw new IllegalArgumentException(
                                "setReorderCodes(): UScript.DEFAULT together with other scripts");
                    }
                    int index = findScript(script);
                    if(index < 0) { continue; }
                    int head = scripts[index];
                    int firstByte = head >> 8;
                    int lastByte = head & 0xff;
                    if(table[firstByte] != 0) {  // Duplicate or equivalent script.
                        throw new IllegalArgumentException(
                                "setReorderCodes(): duplicate or equivalent script " +
                                scriptCodeString(script));
                    }
                    do { table[lastByte--] = (byte)highByte--; } while(firstByte <= lastByte);
                }
                break;
            }
            if(script == Collator.ReorderCodes.DEFAULT) {
                // The default code must be the only one in the list, and that is handled by the caller.
                // Otherwise it must not be used.
                throw new IllegalArgumentException(
                        "setReorderCodes(): UScript.DEFAULT together with other scripts");
            }
            int index = findScript(script);
            if(index < 0) { continue; }
            int head = scripts[index];
            int firstByte = head >> 8;
            int lastByte = head & 0xff;
            if(table[firstByte] != 0) {  // Duplicate or equivalent script.
                throw new IllegalArgumentException(
                        "setReorderCodes(): duplicate or equivalent script " +
                        scriptCodeString(script));
            }
            do { table[firstByte++] = (byte)lowByte++; } while(firstByte <= lastByte);
        }

        // Put all remaining scripts into the middle.
        // Avoid table[0] which must remain 0.
        for(int i = 1; i <= 0xff; ++i) {
            if(table[i] == 0) { table[i] = (byte)lowByte++; }
        }
        assert(lowByte == highByte + 1);
    }

    private static String scriptCodeString(int script) {
        // Do not use the script name here: We do not want to depend on that data.
        return (script < Collator.ReorderCodes.FIRST) ?
                Integer.toString(script) : "0x" + Integer.toHexString(script);
    }

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /** @see jamoCE32s */
    static final int JAMO_CE32S_LENGTH = 19 + 21 + 27;

    /** Main lookup trie. */
    Trie2_32 trie;
    /**
     * Array of CE32 values.
     * At index 0 there must be CE32(U+0000)
     * to support U+0000's special-tag for NUL-termination handling.
     */
    int[] ce32s;
    /** Array of CE values for expansions and OFFSET_TAG. */
    long[] ces;
    /** Array of prefix and contraction-suffix matching data. */
    String contexts;
    /** Base collation data, or null if this data itself is a base. */
    public CollationData base;
    /**
     * Simple array of JAMO_CE32S_LENGTH=19+21+27 CE32s, one per canonical Jamo L/V/T.
     * They are normally simple CE32s, rarely expansions.
     * For fast handling of HANGUL_TAG.
     */
    int[] jamoCE32s = new int[JAMO_CE32S_LENGTH];
    public Normalizer2Impl nfcImpl;
    /** The single-byte primary weight (xx000000) for numeric collation. */
    long numericPrimary = 0x12000000;

    /** 256 flags for which primary-weight lead bytes are compressible. */
    public boolean[] compressibleBytes;
    /**
     * Set of code points that are unsafe for starting string comparison after an identical prefix,
     * or in backwards CE iteration.
     */
    UnicodeSet unsafeBackwardSet;

    /**
     * Fast Latin table for common-Latin-text string comparisons.
     * Data structure see class CollationFastLatin.
     */
    public char[] fastLatinTable;
    /**
     * Header portion of the fastLatinTable.
     * In C++, these are one array, and the header is skipped for mapping characters.
     * In Java, two arrays work better.
     */
    char[] fastLatinTableHeader;

    /**
     * Data for scripts and reordering groups.
     * Uses include building a reordering permutation table and
     * providing script boundaries to AlphabeticIndex.
     *
     * This data is a sorted list of primary-weight lead byte ranges (reordering groups),
     * each with a list of pairs sorted in base collation order;
     * each pair contains a script/reorder code and the lowest primary weight for that script.
     *
     * Data structure:
     * - Each reordering group is encoded in n+2 16-bit integers.
     *   - First integer:
     *     Bits 15..8: First byte of the reordering group's range.
     *     Bits  7..0: Last byte of the reordering group's range.
     *   - Second integer:
     *     Length n of the list of script/reordering codes.
     *   - Each further integer is a script or reordering code.
     */
    char[] scripts;

    /**
     * Collation elements in the root collator.
     * Used by the CollationRootElements class. The data structure is described there.
     * null in a tailoring.
     */
    public long[] rootElements;
}
