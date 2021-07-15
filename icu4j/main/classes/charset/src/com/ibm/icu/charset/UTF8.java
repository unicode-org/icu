// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.charset;

/**
 * Partial Java port of ICU4C unicode/utf8.h and ustr_imp.h.
 */
class UTF8 {
    /**
     * Counts the trail bytes for a UTF-8 lead byte.
     * Returns 0 for 0..0xc1 as well as for 0xf5..0xff.
     *
     * @param leadByte The first byte of a UTF-8 sequence. Must be 0..0xff.
     * @return 0..3
     */
    static int countTrailBytes(byte leadByte) {
        if (leadByte < (byte)0xe0) {
            return leadByte < (byte)0xc2 ? 0 : 1;
        } else if (leadByte < (byte)0xf0) {
            return 2;
        } else {
            return leadByte <= (byte)0xf4 ? 3 : 0;
        }
    }

    /**
     * Counts the bytes of any whole valid sequence for a UTF-8 lead byte.
     * Returns 1 for ASCII 0..0x7f.
     * Returns 0 for 0x80..0xc1 as well as for 0xf5..0xff.
     *
     * @param leadByte The first byte of a UTF-8 sequence. Must be 0..0xff.
     * @return 0..4
     */
    static int countBytes(byte leadByte) {
        if (leadByte >= 0) {
            return 1;
        } else if (leadByte < (byte)0xe0) {
            return leadByte < (byte)0xc2 ? 0 : 2;
        } else if (leadByte < (byte)0xf0) {
            return 3;
        } else {
            return leadByte <= (byte)0xf4 ? 4 : 0;
        }
    }

    /**
     * Internal bit vector for 3-byte UTF-8 validity check, for use in {@link #isValidLead3AndT1}.
     * Each bit indicates whether one lead byte + first trail byte pair starts a valid sequence.
     * Lead byte E0..EF bits 3..0 are used as data int index,
     * first trail byte bits 7..5 are used as bit index into that int.
     *
     * @see #isValidLead3AndT1
     */
    private static final int[] U8_LEAD3_T1_BITS = {
        0x20, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x10, 0x30, 0x30
    };

    /**
     * Internal 3-byte UTF-8 validity check.
     *
     * @param lead E0..EF
     * @param t1 00..FF
     * @return true if lead byte E0..EF and first trail byte 00..FF start a valid sequence.
     */
    static boolean isValidLead3AndT1(int lead, byte t1) {
        return (U8_LEAD3_T1_BITS[lead & 0xf] & (1 << ((t1 & 0xff) >> 5))) != 0;
    }

    /**
     * Internal bit vector for 4-byte UTF-8 validity check, for use in {@link #isValidLead4AndT1}.
     * Each bit indicates whether one lead byte + first trail byte pair starts a valid sequence.
     * Lead byte F0..F4 bits 2..0 are used as data int index,
     * first trail byte bits 7..4 are used as bit index into that int.
     *
     * @see #isValidLead4AndT1
     */
    private static final int[] U8_LEAD4_T1_BITS = {
        0x0e00, 0x0f00, 0x0f00, 0x0f00, 0x0100
    };

    /**
     * Internal 4-byte UTF-8 validity check.
     *
     * @param lead F0..F4
     * @param t1 00..FF
     * @return true if lead byte F0..F4 and first trail byte 00..FF start a valid sequence.
     */
    static boolean isValidLead4AndT1(int lead, byte t1) {
        return (U8_LEAD4_T1_BITS[lead & 7] & (1 << ((t1 & 0xff) >> 4))) != 0;
    }

    /**
     * Does this code unit (byte) encode a code point by itself (US-ASCII 0..0x7f)?
     *
     * @param c 8-bit code unit (byte)
     * @return true if c is an ASCII byte
     */
    static boolean isSingle(byte c) {
        return c >= 0;
    }

    /**
     * Is this code unit (byte) a UTF-8 lead byte?
     *
     * @param c 8-bit code unit (byte)
     * @return true if c is a lead byte
     */
    static boolean isLead(byte c) {
        return ((c - 0xc2) & 0xff) <= 0x32;  // 0x32=0xf4-0xc2
    }

    /**
     * Is this code unit (byte) a UTF-8 trail byte? (0x80..0xBF)
     *
     * @param c 8-bit code unit (byte)
     * @return true if c is a trail byte
     */
    static boolean isTrail(byte c) {
        return c < (byte)0xc0;
    }

    /**
     * How many code units (bytes) are used for the UTF-8 encoding
     * of this Unicode code point?
     *
     * @param c 32-bit code point
     * @return 1..4, or 0 if c is a surrogate or not a Unicode code point
     */
    static int length(int c) {
        if (c >= 0) {
            if (c <= 0x7f) {
                return 1;
            } else if (c <= 0x7ff) {
                return 2;
            } else if (c <= 0xd7ff) {
                return 3;
            } else if (c <= 0xffff) {
                return c >= 0xe000 ? 3 : 0;
            } else if (c <= 0x10ffff) {
                return 4;
            }
        }
        return 0;
    }

    /**
     * 4: The maximum number of UTF-8 code units (bytes) per Unicode code point (U+0000..U+10ffff).
     */
    static int MAX_LENGTH = 4;

    /**
     * Is t a valid UTF-8 trail byte?
     *
     * @param prev Must be the preceding lead byte if i==1 and length>=3;
     *             otherwise ignored.
     * @param t The i-th byte following the lead byte.
     * @param i The index (1..3) of byte t in the byte sequence. 0<i<length
     * @param length The length (2..4) of the byte sequence according to the lead byte.
     * @return true if t is a valid trail byte in this context.
     */
    static boolean isValidTrail(int prev, byte t, int i, int length) {
        // The first trail byte after a 3- or 4-byte lead byte
        // needs to be validated together with its lead byte.
        if (length <= 2 || i > 1) {
            return isTrail(t);
        } else if (length == 3) {
            return isValidLead3AndT1(prev, t);
        } else {  // length == 4
            return isValidLead4AndT1(prev, t);
        }
    }
}
