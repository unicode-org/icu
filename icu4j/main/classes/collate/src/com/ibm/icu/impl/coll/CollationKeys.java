/*
 *******************************************************************************
 * Copyright (C) 2012-2014, International Business Machines
 * Corporation and others.  All Rights Reserved.
 *******************************************************************************
 * CollationKeys.java, ported from collationkeys.h/.cpp
 *
 * C++ version created on: 2012sep02
 * created by: Markus W. Scherer
 */

package com.ibm.icu.impl.coll;

import com.ibm.icu.text.Collator;

public final class CollationKeys /* all methods are static */ {

    // Java porting note: C++ SortKeyByteSink class extends a common class ByteSink,
    // which is not available in Java. We don't need a super class created for implementing
    // collation features.
    public static abstract class SortKeyByteSink {
        protected byte[] buffer_;
        // protected int capacity_; == buffer_.length
        private int appended_ = 0;
        // not used in Java -- private int ignore_ = 0;

        public SortKeyByteSink(byte[] dest) {
            buffer_ = dest;
        }

        /**
         * Needed in Java for when we write to the buffer directly.
         * In C++, the SortKeyByteSink is a subclass of ByteSink and lower-level code can write to that.
         * TODO: Can we make Java SortKeyByteSink have-a ByteArrayWrapper and write through to it?
         * Or maybe create interface ByteSink, have SortKeyByteSink implement it, and have BOCSU write to that??
         */
        public void setBufferAndAppended(byte[] dest, int app) {
            buffer_ = dest;
            appended_ = app;
        }

        /* not used in Java -- public void IgnoreBytes(int numIgnore) {
            ignore_ = numIgnore;
        } */

        /**
         * @param bytes
         *            the array of byte
         * @param n
         *            the length of bytes to be appended
         */
        public void Append(byte[] bytes, int n) {
            if (n <= 0 || bytes == null) {
                return;
            }

            /* not used in Java -- if (ignore_ > 0) {
                int ignoreRest = ignore_ - n;
                if (ignoreRest >= 0) {
                    ignore_ = ignoreRest;
                    return;
                } else {
                    start = ignore_;
                    n = -ignoreRest;
                    ignore_ = 0;
                }
            } */

            int length = appended_;
            appended_ += n;

            int available = buffer_.length - length;
            if (n <= available) {
                System.arraycopy(bytes, 0, buffer_, length, n);
            } else {
                AppendBeyondCapacity(bytes, 0, n, length);
            }
        }

        public void Append(int b) {
            /* not used in Java -- if (ignore_ > 0) {
                --ignore_;
            } else */ {
                if (appended_ < buffer_.length || Resize(1, appended_)) {
                    buffer_[appended_] = (byte) b;
                }
                ++appended_;
            }
        }

        // Java porting note: This method is not used by collator implementation.
        //
        // virtual char *GetAppendBuffer(int min_capacity,
        // int desired_capacity_hint,
        // char *scratch, int scratch_capacity,
        // int *result_capacity);

        public int NumberOfBytesAppended() {
            return appended_;
        }

        public int GetRemainingCapacity() {
            return /* not used in Java -- ignore_ + */ buffer_.length - appended_;
        }

        public boolean Overflowed() {
            return appended_ > buffer_.length;
        }

        /* not used in Java -- public boolean IsOk() {
            return true;
        } */

        /**
         * @param bytes
         *            the array of byte
         * @param start
         *            the start index within the array to be appended
         * @param n
         *            the length of bytes to be appended
         * @param length
         *            the length of buffer required to store the entire data (i.e. already appended
         *            bytes + bytes to be appended by this method)
         */
        protected abstract void AppendBeyondCapacity(byte[] bytes, int start, int n, int length);

        protected abstract boolean Resize(int appendCapacity, int length);
    }

    public static class LevelCallback {
        /**
         * @param level
         *            The next level about to be written to the ByteSink.
         * @return true if the level is to be written (the base class implementation always returns
         *         true)
         */
        boolean needToWrite(int level) {
            return true;
        }
    }
    public static final LevelCallback SIMPLE_LEVEL_FALLBACK = new LevelCallback();

    private static final class SortKeyLevel {
        private static final int INITIAL_CAPACITY = 40;

        byte[] buffer = new byte[INITIAL_CAPACITY];
        int len = 0;
        // not used in Java -- private static final boolean ok = true;  // In C++ "ok" is reset when memory allocations fail.

        SortKeyLevel() {
        }

        /* not used in Java -- boolean isOk() {
            return ok;
        } */

        boolean isEmpty() {
            return len == 0;
        }

        int length() {
            return len;
        }

        // Java porting note: Java uses this instead of C++ operator [] overload
        // uint8_t operator[](int index)
        byte getAt(int index) {
            return buffer[index];
        }

        byte[] data() {
            return buffer;
        }

        void appendByte(int b) {
            if (len < buffer.length || ensureCapacity(1)) {
                buffer[len++] = (byte) b;
            }
        }

        void appendWeight16(int w) {
            assert ((w & 0xffff) != 0);
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = (b1 == 0) ? 1 : 2;
            if ((len + appendLength) <= buffer.length || ensureCapacity(appendLength)) {
                buffer[len++] = b0;
                if (b1 != 0) {
                    buffer[len++] = b1;
                }
            }
        }

        void appendWeight32(long w) {
            assert (w != 0);
            byte[] bytes = new byte[] { (byte) (w >>> 24), (byte) (w >>> 16), (byte) (w >>> 8),
                    (byte) w };
            int appendLength = (bytes[1] == 0) ? 1 : (bytes[2] == 0) ? 2 : (bytes[3] == 0) ? 3 : 4;
            if ((len + appendLength) <= buffer.length || ensureCapacity(appendLength)) {
                buffer[len++] = bytes[0];
                if (bytes[1] != 0) {
                    buffer[len++] = bytes[1];
                    if (bytes[2] != 0) {
                        buffer[len++] = bytes[2];
                        if (bytes[3] != 0) {
                            buffer[len++] = bytes[3];
                        }
                    }
                }
            }
        }

        void appendReverseWeight16(int w) {
            assert ((w & 0xffff) != 0);
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = (b1 == 0) ? 1 : 2;
            if ((len + appendLength) <= buffer.length || ensureCapacity(appendLength)) {
                if (b1 == 0) {
                    buffer[len++] = b0;
                } else {
                    buffer[len] = b1;
                    buffer[len + 1] = b0;
                    len += 2;
                }
            }
        }

        // Appends all but the last byte to the sink. The last byte should be the 01 terminator.
        void appendTo(SortKeyByteSink sink) {
            assert (len > 0 && buffer[len - 1] == 1);
            sink.Append(buffer, len - 1);
        }

        private boolean ensureCapacity(int appendCapacity) {
            /* not used in Java -- if (!ok) {
                return false;
            } */
            int newCapacity = 2 * buffer.length;
            int altCapacity = len + 2 * appendCapacity;
            if (newCapacity < altCapacity) {
                newCapacity = altCapacity;
            }
            if (newCapacity < 200) {
                newCapacity = 200;
            }
            byte[] newbuf = new byte[newCapacity];
            System.arraycopy(buffer, 0, newbuf, 0, len);
            buffer = newbuf;

            return true;
        }
    }

    private static SortKeyLevel getSortKeyLevel(int levels, int level) {
        return (levels & level) != 0 ? new SortKeyLevel() : null;
    }

    private CollationKeys() {
    } // no instantiation

    // Secondary level: Compress up to 33 common weights as 05..25 or 25..45.
    private static final int SEC_COMMON_LOW = Collation.COMMON_BYTE;
    private static final int SEC_COMMON_MIDDLE = SEC_COMMON_LOW + 0x20;
    static final int SEC_COMMON_HIGH = SEC_COMMON_LOW + 0x40; // read by CollationDataReader
    private static final int SEC_COMMON_MAX_COUNT = 0x21;

    // Case level, lowerFirst: Compress up to 7 common weights as 1..7 or 7..13.
    private static final int CASE_LOWER_FIRST_COMMON_LOW = 1;
    private static final int CASE_LOWER_FIRST_COMMON_MIDDLE = 7;
    private static final int CASE_LOWER_FIRST_COMMON_HIGH = 13;
    private static final int CASE_LOWER_FIRST_COMMON_MAX_COUNT = 7;

    // Case level, upperFirst: Compress up to 13 common weights as 3..15.
    private static final int CASE_UPPER_FIRST_COMMON_LOW = 3;
    @SuppressWarnings("unused")
    private static final int CASE_UPPER_FIRST_COMMON_HIGH = 15;
    private static final int CASE_UPPER_FIRST_COMMON_MAX_COUNT = 13;

    // Tertiary level only (no case): Compress up to 97 common weights as 05..65 or 65..C5.
    private static final int TER_ONLY_COMMON_LOW = Collation.COMMON_BYTE;
    private static final int TER_ONLY_COMMON_MIDDLE = TER_ONLY_COMMON_LOW + 0x60;
    private static final int TER_ONLY_COMMON_HIGH = TER_ONLY_COMMON_LOW + 0xc0;
    private static final int TER_ONLY_COMMON_MAX_COUNT = 0x61;

    // Tertiary with case, lowerFirst: Compress up to 33 common weights as 05..25 or 25..45.
    private static final int TER_LOWER_FIRST_COMMON_LOW = Collation.COMMON_BYTE;
    private static final int TER_LOWER_FIRST_COMMON_MIDDLE = TER_LOWER_FIRST_COMMON_LOW + 0x20;
    private static final int TER_LOWER_FIRST_COMMON_HIGH = TER_LOWER_FIRST_COMMON_LOW + 0x40;
    private static final int TER_LOWER_FIRST_COMMON_MAX_COUNT = 0x21;

    // Tertiary with case, upperFirst: Compress up to 33 common weights as 85..A5 or A5..C5.
    private static final int TER_UPPER_FIRST_COMMON_LOW = Collation.COMMON_BYTE + 0x80;
    private static final int TER_UPPER_FIRST_COMMON_MIDDLE = TER_UPPER_FIRST_COMMON_LOW + 0x20;
    private static final int TER_UPPER_FIRST_COMMON_HIGH = TER_UPPER_FIRST_COMMON_LOW + 0x40;
    private static final int TER_UPPER_FIRST_COMMON_MAX_COUNT = 0x21;

    // Quaternary level: Compress up to 113 common weights as 1C..8C or 8C..FC.
    private static final int QUAT_COMMON_LOW = 0x1c;
    private static final int QUAT_COMMON_MIDDLE = QUAT_COMMON_LOW + 0x70;
    private static final int QUAT_COMMON_HIGH = QUAT_COMMON_LOW + 0xE0;
    private static final int QUAT_COMMON_MAX_COUNT = 0x71;
    // Primary weights shifted to quaternary level must be encoded with
    // a lead byte below the common-weight compression range.
    private static final int QUAT_SHIFTED_LIMIT_BYTE = QUAT_COMMON_LOW - 1; // 0x1b

    /**
     * Map from collation strength (UColAttributeValue) to a mask of Collation.Level bits up to that
     * strength, excluding the CASE_LEVEL which is independent of the strength, and excluding
     * IDENTICAL_LEVEL which this function does not write.
     */
    private static final int levelMasks[] = new int[] {
        2,          // UCOL_PRIMARY -> PRIMARY_LEVEL
        6,          // UCOL_SECONDARY -> up to SECONDARY_LEVEL
        0x16,       // UCOL_TERTIARY -> up to TERTIARY_LEVEL
        0x36,       // UCOL_QUATERNARY -> up to QUATERNARY_LEVEL
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0,
        0x36        // UCOL_IDENTICAL -> up to QUATERNARY_LEVEL
    };

    /**
     * Writes the sort key bytes for minLevel up to the iterator data's strength. Optionally writes
     * the case level. Stops writing levels when callback.needToWrite(level) returns false.
     * Separates levels with the LEVEL_SEPARATOR_BYTE but does not write a TERMINATOR_BYTE.
     */
    public static void writeSortKeyUpToQuaternary(CollationIterator iter, boolean[] compressibleBytes,
            CollationSettings settings, SortKeyByteSink sink, int minLevel, LevelCallback callback,
            boolean preflight) {

        int options = settings.options;
        // Set of levels to process and write.
        int levels = levelMasks[CollationSettings.getStrength(options)];
        if ((options & CollationSettings.CASE_LEVEL) != 0) {
            levels |= Collation.CASE_LEVEL_FLAG;
        }
        // Minus the levels below minLevel.
        levels &= ~((1 << minLevel) - 1);
        if (levels == 0) {
            return;
        }

        long variableTop;
        if ((options & CollationSettings.ALTERNATE_MASK) == 0) {
            variableTop = 0;
        } else {
            // +1 so that we can use "<" and primary ignorables test out early.
            variableTop = settings.variableTop + 1;
        }
        byte[] reorderTable = settings.reorderTable;

        int tertiaryMask = CollationSettings.getTertiaryMask(options);

        byte[] p234 = new byte[3];
        SortKeyLevel cases = getSortKeyLevel(levels, Collation.CASE_LEVEL_FLAG);
        SortKeyLevel secondaries = getSortKeyLevel(levels, Collation.SECONDARY_LEVEL_FLAG);
        SortKeyLevel tertiaries = getSortKeyLevel(levels, Collation.TERTIARY_LEVEL_FLAG);
        SortKeyLevel quaternaries = getSortKeyLevel(levels, Collation.QUATERNARY_LEVEL_FLAG);

        int compressedP1 = 0; // 0==no compression; otherwise reordered compressible lead byte
        int commonCases = 0;
        int commonSecondaries = 0;
        int commonTertiaries = 0;
        int commonQuaternaries = 0;

        int prevSecondary = 0;
        boolean anyMergeSeparators = false;

        for (;;) {
            // No need to keep all CEs in the buffer when we write a sort key.
            iter.clearCEsIfNoneRemaining();
            long ce = iter.nextCE();
            long p = ce >>> 32;
            if (p < variableTop && p > Collation.MERGE_SEPARATOR_PRIMARY) {
                // Variable CE, shift it to quaternary level.
                // Ignore all following primary ignorables, and shift further variable CEs.
                if (commonQuaternaries != 0) {
                    --commonQuaternaries;
                    while (commonQuaternaries >= QUAT_COMMON_MAX_COUNT) {
                        quaternaries.appendByte(QUAT_COMMON_MIDDLE);
                        commonQuaternaries -= QUAT_COMMON_MAX_COUNT;
                    }
                    // Shifted primary weights are lower than the common weight.
                    quaternaries.appendByte(QUAT_COMMON_LOW + commonQuaternaries);
                    commonQuaternaries = 0;
                }
                do {
                    if ((levels & Collation.QUATERNARY_LEVEL_FLAG) != 0) {
                        int p1 = (int) p >>> 24;
                        if (reorderTable != null) {
                            p1 = reorderTable[p1] & 0xff;
                        }
                        if (p1 >= QUAT_SHIFTED_LIMIT_BYTE) {
                            // Prevent shifted primary lead bytes from
                            // overlapping with the common compression range.
                            quaternaries.appendByte(QUAT_SHIFTED_LIMIT_BYTE);
                        }
                        quaternaries.appendWeight32((p1 << 24) | (p & 0xffffff));
                    }
                    do {
                        ce = iter.nextCE();
                        p = ce >>> 32;
                    } while (p == 0);
                } while (p < variableTop && p > Collation.MERGE_SEPARATOR_PRIMARY);
            }
            // ce could be primary ignorable, or NO_CE, or the merge separator,
            // or a regular primary CE, but it is not variable.
            // If ce==NO_CE, then write nothing for the primary level but
            // terminate compression on all levels and then exit the loop.
            if (p > Collation.NO_CE_PRIMARY && (levels & Collation.PRIMARY_LEVEL_FLAG) != 0) {
                int p1 = (int) p >>> 24;
                if (reorderTable != null) {
                    p1 = reorderTable[p1] & 0xff;
                }
                if (p1 != compressedP1) {
                    if (compressedP1 != 0) {
                        if (p1 < compressedP1) {
                            // No primary compression terminator
                            // at the end of the level or merged segment.
                            if (p1 > Collation.MERGE_SEPARATOR_BYTE) {
                                sink.Append(Collation.PRIMARY_COMPRESSION_LOW_BYTE);
                            }
                        } else {
                            sink.Append(Collation.PRIMARY_COMPRESSION_HIGH_BYTE);
                        }
                    }
                    sink.Append(p1);
                    // Test the un-reordered lead byte for compressibility but
                    // remember the reordered lead byte.
                    if (compressibleBytes[(int) p >>> 24]) {
                        compressedP1 = p1;
                    } else {
                        compressedP1 = 0;
                    }
                }
                byte p2 = (byte) (p >>> 16);
                if (p2 != 0) {
                    p234[0] = p2;
                    p234[1] = (byte) (p >>> 8);
                    p234[2] = (byte) p;
                    sink.Append(p234, (p234[1] == 0) ? 1 : (p234[2] == 0) ? 2 : 3);
                }
                // Optimization for internalNextSortKeyPart():
                // When the primary level overflows we can stop because we need not
                // calculate (preflight) the whole sort key length.
                if (!preflight && sink.Overflowed()) {
                    // not used in Java -- if (!sink.IsOk()) {
                    // Java porting note: U_MEMORY_ALLOCATION_ERROR is set here in
                    // C implementation. IsOk() in Java always returns true, so this
                    // is a dead code.
                    return;
                }
            }

            int lower32 = (int) ce;
            if (lower32 == 0) {
                continue;
            } // completely ignorable, no secondary/case/tertiary/quaternary

            if ((levels & Collation.SECONDARY_LEVEL_FLAG) != 0) {
                int s = lower32 >>> 16;  // 16 bits
                if (s == 0) {
                    // secondary ignorable
                } else if (s == Collation.COMMON_WEIGHT16) {
                    ++commonSecondaries;
                } else if ((options & CollationSettings.BACKWARD_SECONDARY) == 0) {
                    if (commonSecondaries != 0) {
                        --commonSecondaries;
                        while (commonSecondaries >= SEC_COMMON_MAX_COUNT) {
                            secondaries.appendByte(SEC_COMMON_MIDDLE);
                            commonSecondaries -= SEC_COMMON_MAX_COUNT;
                        }
                        int b;
                        if (s < Collation.COMMON_WEIGHT16) {
                            b = SEC_COMMON_LOW + commonSecondaries;
                        } else {
                            b = SEC_COMMON_HIGH - commonSecondaries;
                        }
                        secondaries.appendByte(b);
                        commonSecondaries = 0;
                    }
                    secondaries.appendWeight16(s);
                } else {
                    if (commonSecondaries != 0) {
                        --commonSecondaries;
                        // Append reverse weights. The level will be re-reversed later.
                        int remainder = commonSecondaries % SEC_COMMON_MAX_COUNT;
                        int b;
                        if (prevSecondary < Collation.COMMON_WEIGHT16) {
                            b = SEC_COMMON_LOW + remainder;
                        } else {
                            b = SEC_COMMON_HIGH - remainder;
                        }
                        secondaries.appendByte(b);
                        commonSecondaries -= remainder;
                        // commonSecondaries is now a multiple of SEC_COMMON_MAX_COUNT.
                        while (commonSecondaries > 0) { // same as >= SEC_COMMON_MAX_COUNT
                            secondaries.appendByte(SEC_COMMON_MIDDLE);
                            commonSecondaries -= SEC_COMMON_MAX_COUNT;
                        }
                        // commonSecondaries == 0
                    }
                    // Reduce separators so that we can look for byte<=1 later.
                    if (s <= Collation.MERGE_SEPARATOR_WEIGHT16) {
                        if (s == Collation.MERGE_SEPARATOR_WEIGHT16) {
                            anyMergeSeparators = true;
                        }
                        secondaries.appendByte((s >>> 8) - 1);
                    } else {
                        secondaries.appendReverseWeight16(s);
                    }
                    prevSecondary = s;
                }
            }

            if ((levels & Collation.CASE_LEVEL_FLAG) != 0) {
                if ((CollationSettings.getStrength(options) == Collator.PRIMARY) ? p == 0
                        : (lower32 >>> 16) == 0) {
                    // Primary+caseLevel: Ignore case level weights of primary ignorables.
                    // Otherwise: Ignore case level weights of secondary ignorables.
                    // For details see the comments in the CollationCompare class.
                } else {
                    int c = (lower32 >>> 8) & 0xff; // case bits & tertiary lead byte
                    assert ((c & 0xc0) != 0xc0);
                    if ((c & 0xc0) == 0 && c > Collation.MERGE_SEPARATOR_BYTE) {
                        ++commonCases;
                    } else {
                        if ((options & CollationSettings.UPPER_FIRST) == 0) {
                            // lowerFirst: Compress common weights to nibbles 1..7..13, mixed=14,
                            // upper=15.
                            if (commonCases != 0) {
                                --commonCases;
                                while (commonCases >= CASE_LOWER_FIRST_COMMON_MAX_COUNT) {
                                    cases.appendByte(CASE_LOWER_FIRST_COMMON_MIDDLE << 4);
                                    commonCases -= CASE_LOWER_FIRST_COMMON_MAX_COUNT;
                                }
                                int b;
                                if (c <= Collation.MERGE_SEPARATOR_BYTE) {
                                    b = CASE_LOWER_FIRST_COMMON_LOW + commonCases;
                                } else {
                                    b = CASE_LOWER_FIRST_COMMON_HIGH - commonCases;
                                }
                                cases.appendByte(b << 4);
                                commonCases = 0;
                            }
                            if (c > Collation.MERGE_SEPARATOR_BYTE) {
                                c = (CASE_LOWER_FIRST_COMMON_HIGH + (c >>> 6)) << 4; // 14 or 15
                            }
                        } else {
                            // upperFirst: Compress common weights to nibbles 3..15, mixed=2,
                            // upper=1.
                            // The compressed common case weights only go up from the "low" value
                            // because with upperFirst the common weight is the highest one.
                            if (commonCases != 0) {
                                --commonCases;
                                while (commonCases >= CASE_UPPER_FIRST_COMMON_MAX_COUNT) {
                                    cases.appendByte(CASE_UPPER_FIRST_COMMON_LOW << 4);
                                    commonCases -= CASE_UPPER_FIRST_COMMON_MAX_COUNT;
                                }
                                cases.appendByte((CASE_UPPER_FIRST_COMMON_LOW + commonCases) << 4);
                                commonCases = 0;
                            }
                            if (c > Collation.MERGE_SEPARATOR_BYTE) {
                                c = (CASE_UPPER_FIRST_COMMON_LOW - (c >>> 6)) << 4; // 2 or 1
                            }
                        }
                        // c is a separator byte 01 or 02,
                        // or a left-shifted nibble 0x10, 0x20, ... 0xf0.
                        cases.appendByte(c);
                    }
                }
            }

            if ((levels & Collation.TERTIARY_LEVEL_FLAG) != 0) {
                int t = lower32 & tertiaryMask;
                assert ((lower32 & 0xc000) != 0xc000);
                if (t == Collation.COMMON_WEIGHT16) {
                    ++commonTertiaries;
                } else if ((tertiaryMask & 0x8000) == 0) {
                    // Tertiary weights without case bits.
                    // Move lead bytes 06..3F to C6..FF for a large common-weight range.
                    if (commonTertiaries != 0) {
                        --commonTertiaries;
                        while (commonTertiaries >= TER_ONLY_COMMON_MAX_COUNT) {
                            tertiaries.appendByte(TER_ONLY_COMMON_MIDDLE);
                            commonTertiaries -= TER_ONLY_COMMON_MAX_COUNT;
                        }
                        int b;
                        if (t < Collation.COMMON_WEIGHT16) {
                            b = TER_ONLY_COMMON_LOW + commonTertiaries;
                        } else {
                            b = TER_ONLY_COMMON_HIGH - commonTertiaries;
                        }
                        tertiaries.appendByte(b);
                        commonTertiaries = 0;
                    }
                    if (t > Collation.COMMON_WEIGHT16) {
                        t += 0xc000;
                    }
                    tertiaries.appendWeight16(t);
                } else if ((options & CollationSettings.UPPER_FIRST) == 0) {
                    // Tertiary weights with caseFirst=lowerFirst.
                    // Move lead bytes 06..BF to 46..FF for the common-weight range.
                    if (commonTertiaries != 0) {
                        --commonTertiaries;
                        while (commonTertiaries >= TER_LOWER_FIRST_COMMON_MAX_COUNT) {
                            tertiaries.appendByte(TER_LOWER_FIRST_COMMON_MIDDLE);
                            commonTertiaries -= TER_LOWER_FIRST_COMMON_MAX_COUNT;
                        }
                        int b;
                        if (t < Collation.COMMON_WEIGHT16) {
                            b = TER_LOWER_FIRST_COMMON_LOW + commonTertiaries;
                        } else {
                            b = TER_LOWER_FIRST_COMMON_HIGH - commonTertiaries;
                        }
                        tertiaries.appendByte(b);
                        commonTertiaries = 0;
                    }
                    if (t > Collation.COMMON_WEIGHT16) {
                        t += 0x4000;
                    }
                    tertiaries.appendWeight16(t);
                } else {
                    // Tertiary weights with caseFirst=upperFirst.
                    // Do not change the artificial uppercase weight of a tertiary CE (0.0.ut),
                    // to keep tertiary CEs well-formed.
                    // Their case+tertiary weights must be greater than those of
                    // primary and secondary CEs.
                    //
                    // Separators    01..02 -> 01..02  (unchanged)
                    // Lowercase     03..04 -> 83..84  (includes uncased)
                    // Common weight     05 -> 85..C5  (common-weight compression range)
                    // Lowercase     06..3F -> C6..FF
                    // Mixed case    43..7F -> 43..7F
                    // Uppercase     83..BF -> 03..3F
                    // Tertiary CE   86..BF -> C6..FF
                    if (t <= Collation.MERGE_SEPARATOR_WEIGHT16) {
                        // Keep separators unchanged.
                    } else if ((lower32 >>> 16) != 0) {
                        // Invert case bits of primary & secondary CEs.
                        t ^= 0xc000;
                        if (t < (TER_UPPER_FIRST_COMMON_HIGH << 8)) {
                            t -= 0x4000;
                        }
                    } else {
                        // Keep uppercase bits of tertiary CEs.
                        assert (0x8600 <= t && t <= 0xbfff);
                        t += 0x4000;
                    }
                    if (commonTertiaries != 0) {
                        --commonTertiaries;
                        while (commonTertiaries >= TER_UPPER_FIRST_COMMON_MAX_COUNT) {
                            tertiaries.appendByte(TER_UPPER_FIRST_COMMON_MIDDLE);
                            commonTertiaries -= TER_UPPER_FIRST_COMMON_MAX_COUNT;
                        }
                        int b;
                        if (t < (TER_UPPER_FIRST_COMMON_LOW << 8)) {
                            b = TER_UPPER_FIRST_COMMON_LOW + commonTertiaries;
                        } else {
                            b = TER_UPPER_FIRST_COMMON_HIGH - commonTertiaries;
                        }
                        tertiaries.appendByte(b);
                        commonTertiaries = 0;
                    }
                    tertiaries.appendWeight16(t);
                }
            }

            if ((levels & Collation.QUATERNARY_LEVEL_FLAG) != 0) {
                int q = lower32 & 0xffff;
                if ((q & 0xc0) == 0 && q > Collation.MERGE_SEPARATOR_WEIGHT16) {
                    ++commonQuaternaries;
                } else if (q <= Collation.MERGE_SEPARATOR_WEIGHT16
                        && (options & CollationSettings.ALTERNATE_MASK) == 0
                        && (quaternaries.isEmpty() || quaternaries.getAt(quaternaries.length() - 1) == Collation.MERGE_SEPARATOR_BYTE)) {
                    // If alternate=non-ignorable and there are only
                    // common quaternary weights between two separators,
                    // then we need not write anything between these separators.
                    // The only weights greater than the merge separator and less than the common
                    // weight
                    // are shifted primary weights, which are not generated for
                    // alternate=non-ignorable.
                    // There are also exactly as many quaternary weights as tertiary weights,
                    // so level length differences are handled already on tertiary level.
                    // Any above-common quaternary weight will compare greater regardless.
                    quaternaries.appendByte(q >>> 8);
                } else {
                    if (q <= Collation.MERGE_SEPARATOR_WEIGHT16) {
                        q >>>= 8;
                    } else {
                        q = 0xfc + ((q >>> 6) & 3);
                    }
                    if (commonQuaternaries != 0) {
                        --commonQuaternaries;
                        while (commonQuaternaries >= QUAT_COMMON_MAX_COUNT) {
                            quaternaries.appendByte(QUAT_COMMON_MIDDLE);
                            commonQuaternaries -= QUAT_COMMON_MAX_COUNT;
                        }
                        int b;
                        if (q < QUAT_COMMON_LOW) {
                            b = QUAT_COMMON_LOW + commonQuaternaries;
                        } else {
                            b = QUAT_COMMON_HIGH - commonQuaternaries;
                        }
                        quaternaries.appendByte(b);
                        commonQuaternaries = 0;
                    }
                    quaternaries.appendByte(q);
                }
            }

            if ((lower32 >>> 24) == Collation.LEVEL_SEPARATOR_BYTE) {
                break;
            } // ce == NO_CE
        }

        // Append the beyond-primary levels.
        // not used in Java -- boolean ok = true;
        if ((levels & Collation.SECONDARY_LEVEL_FLAG) != 0) {
            if (!callback.needToWrite(Collation.SECONDARY_LEVEL)) {
                return;
            }
            // not used in Java -- ok &= secondaries.isOk();
            sink.Append(Collation.LEVEL_SEPARATOR_BYTE);
            byte[] secs = secondaries.data();
            int length = secondaries.length() - 1; // Ignore the trailing NO_CE.
            if ((options & CollationSettings.BACKWARD_SECONDARY) != 0) {
                // The backwards secondary level compares secondary weights backwards
                // within segments separated by the merge separator (U+FFFE, weight 02).
                // The separator weights 01 & 02 were reduced to 00 & 01 so that
                // we do not accidentally separate at a _second_ weight byte of 02.
                int start = 0;
                for (;;) {
                    // Find the merge separator or the NO_CE terminator.
                    int limit;
                    if (anyMergeSeparators) {
                        limit = start;
                        while (((int)secs[limit] & 0xff) > 1) {
                            ++limit;
                        }
                    } else {
                        limit = length;
                    }
                    // Reverse this segment.
                    if (start < limit) {
                        for (int i = start, j = limit - 1; i < j; i++, j--) {
                            byte tmp = secs[i];
                            secs[i] = secs[j];
                            secs[j] = tmp;
                        }
                    }
                    // Did we reach the end of the string?
                    if (secs[limit] == 0) {
                        break;
                    }
                    // Restore the merge separator.
                    secs[limit] = 2;
                    // Skip the merge separator and continue.
                    start = limit + 1;
                }
            }
            sink.Append(secs, length);
        }

        if ((levels & Collation.CASE_LEVEL_FLAG) != 0) {
            if (!callback.needToWrite(Collation.CASE_LEVEL)) {
                return;
            }
            // not used in Java -- ok &= cases.isOk();
            sink.Append(Collation.LEVEL_SEPARATOR_BYTE);
            // Write pairs of nibbles as bytes, except separator bytes as themselves.
            int length = cases.length() - 1; // Ignore the trailing NO_CE.
            byte b = 0;
            for (int i = 0; i < length; ++i) {
                byte c = cases.getAt(i);
                if (c <= Collation.MERGE_SEPARATOR_BYTE) {
                    assert (c != 0);
                    if (b != 0) {
                        sink.Append(b);
                        b = 0;
                    }
                    sink.Append(c);
                } else {
                    assert ((c & 0xf) == 0);
                    if (b == 0) {
                        b = c;
                    } else {
                        sink.Append(b | (c >>> 4));
                        b = 0;
                    }
                }
            }
            if (b != 0) {
                sink.Append(b);
            }
        }

        if ((levels & Collation.TERTIARY_LEVEL_FLAG) != 0) {
            if (!callback.needToWrite(Collation.TERTIARY_LEVEL)) {
                return;
            }
            // not used in Java -- ok &= tertiaries.isOk();
            sink.Append(Collation.LEVEL_SEPARATOR_BYTE);
            tertiaries.appendTo(sink);
        }

        if ((levels & Collation.QUATERNARY_LEVEL_FLAG) != 0) {
            if (!callback.needToWrite(Collation.QUATERNARY_LEVEL)) {
                return;
            }
            // not used in Java -- ok &= quaternaries.isOk();
            sink.Append(Collation.LEVEL_SEPARATOR_BYTE);
            quaternaries.appendTo(sink);
        }

        // not used in Java -- if (!ok || !sink.IsOk()) {
        // Java porting note: U_MEMORY_ALLOCATION_ERROR is set here in
        // C implementation. IsOk() in Java always returns true, so this
        // is a dead code.
    }
}
