/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/Attic/Utility.java,v $
 * $Date: 2001/10/22 05:35:12 $
 * $Revision: 1.9 $
 *
 *****************************************************************************************
 */
package com.ibm.util;
import com.ibm.text.UCharacter;
import com.ibm.text.UTF16;

public final class Utility {

    /**
     * Convenience utility to compare two Object[]s.
     * Ought to be in System
     */
    public final static boolean arrayEquals(Object[] source, Object target) {
        if (source == null) return (target == null);
        if (!(target instanceof Object[])) return false;
        Object[] targ = (Object[]) target;
        return (source.length == targ.length
                && arrayRegionMatches(source, 0, targ, 0, source.length));
    }

    /**
     * Convenience utility to compare two int[]s
     * Ought to be in System
     */
    public final static boolean arrayEquals(int[] source, Object target) {
        if (source == null) return (target == null);
        if (!(target instanceof int[])) return false;
        int[] targ = (int[]) target;
        return (source.length == targ.length
                && arrayRegionMatches(source, 0, targ, 0, source.length));
    }

    /**
     * Convenience utility to compare two double[]s
     * Ought to be in System
     */
    public final static boolean arrayEquals(double[] source, Object target) {
        if (source == null) return (target == null);
        if (!(target instanceof double[])) return false;
        double[] targ = (double[]) target;
        return (source.length == targ.length
                && arrayRegionMatches(source, 0, targ, 0, source.length));
    }

    /**
     * Convenience utility to compare two Object[]s
     * Ought to be in System
     */
    public final static boolean arrayEquals(Object source, Object target) {
        if (source == null) return (target == null);
        // for some reason, the correct arrayEquals is not being called
        // so do it by hand for now.
        if (source instanceof Object[])
            return(arrayEquals((Object[]) source,target));
        if (source instanceof int[])
            return(arrayEquals((int[]) source,target));
        if (source instanceof double[])
            return(arrayEquals((int[]) source,target));
        return source.equals(target);
    }

    /**
     * Convenience utility to compare two Object[]s
     * Ought to be in System.
     * @param len the length to compare.
     * The start indices and start+len must be valid.
     */
    public final static boolean arrayRegionMatches(Object[] source, int sourceStart,
                                            Object[] target, int targetStart,
                                            int len)
    {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (!arrayEquals(source[i],target[i + delta]))
            return false;
        }
        return true;
    }

    /**
     * Convenience utility to compare two int[]s.
     * @param len the length to compare.
     * The start indices and start+len must be valid.
     * Ought to be in System
     */
    public final static boolean arrayRegionMatches(int[] source, int sourceStart,
                                            int[] target, int targetStart,
                                            int len)
    {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta])
            return false;
        }
        return true;
    }

    /**
     * Convenience utility to compare two arrays of doubles.
     * @param len the length to compare.
     * The start indices and start+len must be valid.
     * Ought to be in System
     */
    public final static boolean arrayRegionMatches(double[] source, int sourceStart,
                                            double[] target, int targetStart,
                                            int len)
    {
        int sourceEnd = sourceStart + len;
        int delta = targetStart - sourceStart;
        for (int i = sourceStart; i < sourceEnd; i++) {
            if (source[i] != target[i + delta])
            return false;
        }
        return true;
    }

    /**
     * Convenience utility. Does null checks on objects, then calls equals.
     */
    public final static boolean objectEquals(Object source, Object target) {
    if (source == null)
            return (target == null);
    else
            return source.equals(target);
    }

    /**
     * The ESCAPE character is used during run-length encoding.  It signals
     * a run of identical chars.
     */
    private static final char ESCAPE = '\uA5A5';

    /**
     * The ESCAPE_BYTE character is used during run-length encoding.  It signals
     * a run of identical bytes.
     */
    static final byte ESCAPE_BYTE = (byte)0xA5;

    /**
     * Construct a string representing an int array.  Use run-length encoding.
     * A character represents itself, unless it is the ESCAPE character.  Then
     * the following notations are possible:
     *   ESCAPE ESCAPE   ESCAPE literal
     *   ESCAPE n c      n instances of character c
     * Since an encoded run occupies 3 characters, we only encode runs of 4 or
     * more characters.  Thus we have n > 0 and n != ESCAPE and n <= 0xFFFF.
     * If we encounter a run where n == ESCAPE, we represent this as:
     *   c ESCAPE n-1 c
     * The ESCAPE value is chosen so as not to collide with commonly
     * seen values.
     */
    static public final String arrayToRLEString(int[] a) {
        StringBuffer buffer = new StringBuffer();

        appendInt(buffer, a.length);
        int runValue = a[0];
        int runLength = 1;
        for (int i=1; i<a.length; ++i) {
            int s = a[i];
            if (s == runValue && runLength < 0xFFFF) {
                ++runLength;
            } else {
                encodeRun(buffer, runValue, runLength);
                runValue = s;
                runLength = 1;
            }
        }
        encodeRun(buffer, runValue, runLength);
        return buffer.toString();
    }

    /**
     * Construct a string representing a short array.  Use run-length encoding.
     * A character represents itself, unless it is the ESCAPE character.  Then
     * the following notations are possible:
     *   ESCAPE ESCAPE   ESCAPE literal
     *   ESCAPE n c      n instances of character c
     * Since an encoded run occupies 3 characters, we only encode runs of 4 or
     * more characters.  Thus we have n > 0 and n != ESCAPE and n <= 0xFFFF.
     * If we encounter a run where n == ESCAPE, we represent this as:
     *   c ESCAPE n-1 c
     * The ESCAPE value is chosen so as not to collide with commonly
     * seen values.
     */
    static public final String arrayToRLEString(short[] a) {
        StringBuffer buffer = new StringBuffer();
        // for (int i=0; i<a.length; ++i) buffer.append((char) a[i]);
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        short runValue = a[0];
        int runLength = 1;
        for (int i=1; i<a.length; ++i) {
            short s = a[i];
            if (s == runValue && runLength < 0xFFFF) ++runLength;
            else {
            encodeRun(buffer, runValue, runLength);
            runValue = s;
            runLength = 1;
            }
        }
        encodeRun(buffer, runValue, runLength);
        return buffer.toString();
    }

    /**
     * Construct a string representing a char array.  Use run-length encoding.
     * A character represents itself, unless it is the ESCAPE character.  Then
     * the following notations are possible:
     *   ESCAPE ESCAPE   ESCAPE literal
     *   ESCAPE n c      n instances of character c
     * Since an encoded run occupies 3 characters, we only encode runs of 4 or
     * more characters.  Thus we have n > 0 and n != ESCAPE and n <= 0xFFFF.
     * If we encounter a run where n == ESCAPE, we represent this as:
     *   c ESCAPE n-1 c
     * The ESCAPE value is chosen so as not to collide with commonly
     * seen values.
     */
    static public final String arrayToRLEString(char[] a) {
        StringBuffer buffer = new StringBuffer();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        char runValue = a[0];
        int runLength = 1;
        for (int i=1; i<a.length; ++i) {
            char s = a[i];
            if (s == runValue && runLength < 0xFFFF) ++runLength;
            else {
            encodeRun(buffer, (short)runValue, runLength);
            runValue = s;
            runLength = 1;
            }
        }
        encodeRun(buffer, (short)runValue, runLength);
        return buffer.toString();
    }

    /**
     * Construct a string representing a byte array.  Use run-length encoding.
     * Two bytes are packed into a single char, with a single extra zero byte at
     * the end if needed.  A byte represents itself, unless it is the
     * ESCAPE_BYTE.  Then the following notations are possible:
     *   ESCAPE_BYTE ESCAPE_BYTE   ESCAPE_BYTE literal
     *   ESCAPE_BYTE n b           n instances of byte b
     * Since an encoded run occupies 3 bytes, we only encode runs of 4 or
     * more bytes.  Thus we have n > 0 and n != ESCAPE_BYTE and n <= 0xFF.
     * If we encounter a run where n == ESCAPE_BYTE, we represent this as:
     *   b ESCAPE_BYTE n-1 b
     * The ESCAPE_BYTE value is chosen so as not to collide with commonly
     * seen values.
     */
    static public final String arrayToRLEString(byte[] a) {
        StringBuffer buffer = new StringBuffer();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        byte runValue = a[0];
        int runLength = 1;
        byte[] state = new byte[2];
        for (int i=1; i<a.length; ++i) {
            byte b = a[i];
            if (b == runValue && runLength < 0xFF) ++runLength;
            else {
                encodeRun(buffer, runValue, runLength, state);
                runValue = b;
                runLength = 1;
            }
        }
        encodeRun(buffer, runValue, runLength, state);

        // We must save the final byte, if there is one, by padding
        // an extra zero.
        if (state[0] != 0) appendEncodedByte(buffer, (byte)0, state);

        return buffer.toString();
    }

    /**
     * Encode a run, possibly a degenerate run (of < 4 values).
     * @param length The length of the run; must be > 0 && <= 0xFFFF.
     */
    private static final void encodeRun(StringBuffer buffer, int value, int length) {
        if (length < 4) {
            for (int j=0; j<length; ++j) {
                if (value == ESCAPE) {
                    appendInt(buffer, value);
                }
                appendInt(buffer, value);
            }
        }
        else {
            if (length == (int) ESCAPE) {
                if (value == (int) ESCAPE) {
                    appendInt(buffer, ESCAPE);
                }
                appendInt(buffer, value);
                --length;
            }
            appendInt(buffer, ESCAPE);
            appendInt(buffer, length);
            appendInt(buffer, value); // Don't need to escape this value
        }
    }
    private static final void appendInt(StringBuffer buffer, int value) {
        buffer.append((char)(value >>> 16));
        buffer.append((char)(value & 0xFFFF));
    }


    /**
     * Encode a run, possibly a degenerate run (of < 4 values).
     * @param length The length of the run; must be > 0 && <= 0xFFFF.
     */
    private static final void encodeRun(StringBuffer buffer, short value, int length) {
        if (length < 4) {
            for (int j=0; j<length; ++j) {
                if (value == (int) ESCAPE) buffer.append(ESCAPE);
                buffer.append((char) value);
            }
        }
        else {
            if (length == (int) ESCAPE) {
                if (value == (int) ESCAPE) buffer.append(ESCAPE);
                buffer.append((char) value);
                --length;
            }
            buffer.append(ESCAPE);
            buffer.append((char) length);
            buffer.append((char) value); // Don't need to escape this value
        }
    }

    /**
     * Encode a run, possibly a degenerate run (of < 4 values).
     * @param length The length of the run; must be > 0 && <= 0xFF.
     */
    private static final void encodeRun(StringBuffer buffer, byte value, int length,
                    byte[] state) {
        if (length < 4) {
            for (int j=0; j<length; ++j) {
                if (value == ESCAPE_BYTE) appendEncodedByte(buffer, ESCAPE_BYTE, state);
                appendEncodedByte(buffer, value, state);
            }
        }
        else {
            if (length == ESCAPE_BYTE) {
            if (value == ESCAPE_BYTE) appendEncodedByte(buffer, ESCAPE_BYTE, state);
            appendEncodedByte(buffer, value, state);
            --length;
            }
            appendEncodedByte(buffer, ESCAPE_BYTE, state);
            appendEncodedByte(buffer, (byte)length, state);
            appendEncodedByte(buffer, value, state); // Don't need to escape this value
        }
    }

    /**
     * Append a byte to the given StringBuffer, packing two bytes into each
     * character.  The state parameter maintains intermediary data between
     * calls.
     * @param state A two-element array, with state[0] == 0 if this is the
     * first byte of a pair, or state[0] != 0 if this is the second byte
     * of a pair, in which case state[1] is the first byte.
     */
    private static final void appendEncodedByte(StringBuffer buffer, byte value,
                        byte[] state) {
        if (state[0] != 0) {
            char c = (char) ((state[1] << 8) | (((int) value) & 0xFF));
            buffer.append(c);
            state[0] = 0;
        }
        else {
            state[0] = 1;
            state[1] = value;
        }
    }

    /**
     * Construct an array of ints from a run-length encoded string.
     */
    static public final int[] RLEStringToIntArray(String s) {
        int length = getInt(s, 0);
        int[] array = new int[length];
        int ai = 0, i = 1;

        int maxI = s.length() / 2;
        while (ai < length && i < maxI) {
            int c = getInt(s, i++);

            if (c == ESCAPE) {
                c = getInt(s, i++);
                if (c == ESCAPE) {
                    array[ai++] = c;
                } else {
                    int runLength = c;
                    int runValue = getInt(s, i++);
                    for (int j=0; j<runLength; ++j) {
                        array[ai++] = runValue;
                    }
                }
            }
            else {
                array[ai++] = c;
            }
        }

        if (ai != length || i != maxI) {
            throw new InternalError("Bad run-length encoded int array");
        }

        return array;
    }
    static final int getInt(String s, int i) {
        return (((int) s.charAt(2*i)) << 16) | (int) s.charAt(2*i+1);
    }


    /**
     * Construct an array of shorts from a run-length encoded string.
     */
    static public final short[] RLEStringToShortArray(String s) {
        int length = (((int) s.charAt(0)) << 16) | ((int) s.charAt(1));
        short[] array = new short[length];
        int ai = 0;
        for (int i=2; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ESCAPE) {
                c = s.charAt(++i);
                if (c == ESCAPE) {
                    array[ai++] = (short) c;
                } else {
                    int runLength = (int) c;
                    short runValue = (short) s.charAt(++i);
                    for (int j=0; j<runLength; ++j) array[ai++] = runValue;
                }
            }
            else {
                array[ai++] = (short) c;
            }
        }

        if (ai != length)
            throw new InternalError("Bad run-length encoded short array");

        return array;
    }

    /**
     * Construct an array of shorts from a run-length encoded string.
     */
    static public final char[] RLEStringToCharArray(String s) {
        int length = (((int) s.charAt(0)) << 16) | ((int) s.charAt(1));
        char[] array = new char[length];
        int ai = 0;
        for (int i=2; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ESCAPE) {
                c = s.charAt(++i);
                if (c == ESCAPE) {
                    array[ai++] = c;
                } else {
                    int runLength = (int) c;
                    char runValue = s.charAt(++i);
                    for (int j=0; j<runLength; ++j) array[ai++] = runValue;
                }
            }
            else {
                array[ai++] = c;
            }
        }

        if (ai != length)
            throw new InternalError("Bad run-length encoded short array");

        return array;
    }

    /**
     * Construct an array of bytes from a run-length encoded string.
     */
    static public final byte[] RLEStringToByteArray(String s) {
        int length = (((int) s.charAt(0)) << 16) | ((int) s.charAt(1));
        byte[] array = new byte[length];
        boolean nextChar = true;
        char c = 0;
        int node = 0;
        int runLength = 0;
        int i = 2;
        for (int ai=0; ai<length; ) {
            // This part of the loop places the next byte into the local
            // variable 'b' each time through the loop.  It keeps the
            // current character in 'c' and uses the boolean 'nextChar'
            // to see if we've taken both bytes out of 'c' yet.
            byte b;
            if (nextChar) {
                c = s.charAt(i++);
                b = (byte) (c >> 8);
                nextChar = false;
            }
            else {
                b = (byte) (c & 0xFF);
                nextChar = true;
            }

            // This part of the loop is a tiny state machine which handles
            // the parsing of the run-length encoding.  This would be simpler
            // if we could look ahead, but we can't, so we use 'node' to
            // move between three nodes in the state machine.
            switch (node) {
            case 0:
                // Normal idle node
                if (b == ESCAPE_BYTE) {
                    node = 1;
                }
                else {
                    array[ai++] = b;
                }
                break;
            case 1:
                // We have seen one ESCAPE_BYTE; we expect either a second
                // one, or a run length and value.
                if (b == ESCAPE_BYTE) {
                    array[ai++] = ESCAPE_BYTE;
                    node = 0;
                }
                else {
                    runLength = b;
                    // Interpret signed byte as unsigned
                    if (runLength < 0) runLength += 0x100;
                    node = 2;
                }
                break;
            case 2:
                // We have seen an ESCAPE_BYTE and length byte.  We interpret
                // the next byte as the value to be repeated.
                for (int j=0; j<runLength; ++j) array[ai++] = b;
                node = 0;
                break;
            }
        }

        if (node != 0)
            throw new InternalError("Bad run-length encoded byte array");

        if (i != s.length())
            throw new InternalError("Excess data in RLE byte array string");

        return array;
    }

    static public String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Format a String for representation in a source file.  This includes
     * breaking it into lines and escaping characters using octal notation
     * when necessary (control characters and double quotes).
     */
    static public final String formatForSource(String s) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<s.length();) {
            if (i > 0) buffer.append('+').append(LINE_SEPARATOR);
            buffer.append("        \"");
            int count = 11;
            while (i<s.length() && count<80) {
            char c = s.charAt(i++);
            if (c < '\u0020' || c == '"' || c == '\\') {
                // Represent control characters, backslash and double quote
                // using octal notation; otherwise the string we form
                // won't compile, since Unicode escape sequences are
                // processed before tokenization.
                buffer.append('\\');
                buffer.append(HEX_DIGIT[(c & 0700) >> 6]); // HEX_DIGIT works for octal
                buffer.append(HEX_DIGIT[(c & 0070) >> 3]);
                buffer.append(HEX_DIGIT[(c & 0007)]);
                count += 4;
            }
            else if (c <= '\u007E') {
                buffer.append(c);
                count += 1;
            }
            else {
                buffer.append("\\u");
                buffer.append(HEX_DIGIT[(c & 0xF000) >> 12]);
                buffer.append(HEX_DIGIT[(c & 0x0F00) >> 8]);
                buffer.append(HEX_DIGIT[(c & 0x00F0) >> 4]);
                buffer.append(HEX_DIGIT[(c & 0x000F)]);
                count += 6;
            }
            }
            buffer.append('"');
        }
        return buffer.toString();
    }

    static final char[] HEX_DIGIT = {'0','1','2','3','4','5','6','7',
                     '8','9','A','B','C','D','E','F'};


    /**
     * Convert characters outside the range U+0020 to U+007F to
     * Unicode escapes, and convert backslash to a double backslash.
     */
    public static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                if (c == '\\') {
                    buf.append("\\\\"); // That is, "\\"
                } else {
                    buf.append(c);
                }
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }

    /* This map must be in ASCENDING ORDER OF THE ESCAPE CODE */
    static private final char[] UNESCAPE_MAP = {
        /*"   0x22, 0x22 */
        /*'   0x27, 0x27 */
        /*?   0x3F, 0x3F */
        /*\   0x5C, 0x5C */
        /*a*/ 0x61, 0x07,
        /*b*/ 0x62, 0x08,
        /*f*/ 0x66, 0x0c,
        /*n*/ 0x6E, 0x0a,
        /*r*/ 0x72, 0x0d,
        /*t*/ 0x74, 0x09,
        /*v*/ 0x76, 0x0b
    };

    /**
     * Convert an escape to a 32-bit code point value.  We attempt
     * to parallel the icu4c unesacpeAt() function.
     * @param offset16 an array containing offset to the character
     * <em>after</em> the backslash.  Upon return offset16[0] will
     * be updated to point after the escape sequence.
     * @return character value from 0 to 10FFFF, or -1 on error.
     */
    public static int unescapeAt(String s, int[] offset16) {
        int c;
        int result = 0;
        int n = 0;
        int minDig = 0;
        int maxDig = 0;
        int bitsPerDigit = 4;
        int dig;
        int i;

        /* Check that offset is in range */
        int offset = offset16[0];
        int length = s.length();
        if (offset < 0 || offset >= length) {
            return -1;
        }

        /* Fetch first UChar after '\\' */
        c = UTF16.charAt(s, offset);
        offset += UTF16.getCharCount(c);

        /* Convert hexadecimal and octal escapes */
        switch (c) {
        case 'u':
            minDig = maxDig = 4;
            break;
        case 'U':
            minDig = maxDig = 8;
            break;
        case 'x':
            minDig = 1;
            maxDig = 2;
            break;
        default:
            dig = UCharacter.digit(c, 8);
            if (dig >= 0) {
                minDig = 1;
                maxDig = 3;
                n = 1; /* Already have first octal digit */
                bitsPerDigit = 3;
                result = dig;
            }
            break;
        }
        if (minDig != 0) {
            while (offset < length && n < maxDig) {
                // TEMPORARY
                // TODO: Restore the char32-based code when UCharacter.digit
                // is working (Bug 66).

                //c = UTF16.charAt(s, offset);
                //dig = UCharacter.digit(c, (bitsPerDigit == 3) ? 8 : 16);
                c = s.charAt(offset);
                dig = Character.digit((char)c, (bitsPerDigit == 3) ? 8 : 16);
                if (dig < 0) {
                    break;
                }
                result = (result << bitsPerDigit) | dig;
                //offset += UTF16.getCharCount(c);
                ++offset;
                ++n;
            }
            if (n < minDig) {
                return -1;
            }
            offset16[0] = offset;
            return result;
        }

        /* Convert C-style escapes in table */
        for (i=0; i<UNESCAPE_MAP.length; i+=2) {
            if (c == UNESCAPE_MAP[i]) {
                offset16[0] = offset;
                return UNESCAPE_MAP[i+1];
            } else if (c < UNESCAPE_MAP[i]) {
                break;
            }
        }

        /* If no special forms are recognized, then consider
         * the backslash to generically escape the next character. */
        offset16[0] = offset;
        return c;
    }

    /**
     * Convert all escapes in a given string using unescapeAt().
     * @exception IllegalArgumentException if an invalid escape is
     * seen.
     */
    public static String unescape(String s) {
        StringBuffer buf = new StringBuffer();
        int[] pos = new int[1];
        for (int i=0; i<s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e < 0) {
                    throw new IllegalArgumentException("Invalid escape sequence " +
                                                       s.substring(i-1, Math.min(i+8, s.length())));
                }
                UTF16.append(buf, e);
                i = pos[0];
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Convert all escapes in a given string using unescapeAt().
     * Leave invalid escape sequences unchanged.
     */
    public static String unescapeLeniently(String s) {
        StringBuffer buf = new StringBuffer();
        int[] pos = new int[1];
        for (int i=0; i<s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                pos[0] = i;
                int e = unescapeAt(s, pos);
                if (e < 0) {
                    buf.append(c);
                } else {
                    UTF16.append(buf, e);
                    i = pos[0];
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Convert a char to 4 hex uppercase digits.  E.g., hex('a') =>
     * "0041".
     */
    public static String hex(char ch) {
        StringBuffer temp = new StringBuffer();
        return hex(ch, temp).toString();
    }

    /**
     * Convert a string to comma-separated groups of 4 hex uppercase
     * digits.  E.g., hex('ab') => "0041,0042".
     */
    public static String hex(String s) {
        StringBuffer temp = new StringBuffer();
        return hex(s, temp).toString();
    }

    /**
     * Convert a string to comma-separated groups of 4 hex uppercase
     * digits.  E.g., hex('ab') => "0041,0042".
     */
    public static String hex(StringBuffer s) {
        return hex(s.toString());
    }

    /**
     * Convert a char to 4 hex uppercase digits.  E.g., hex('a') =>
     * "0041".  Append the output to the given StringBuffer.
     */
    public static StringBuffer hex(char ch, StringBuffer output) {
        String foo = Integer.toString(ch,16).toUpperCase();
        for (int i = foo.length(); i < 4; ++i) {
            output.append('0');
        }
        output.append(foo);
        return output;
    }

    /**
     * Convert a integer to size width hex uppercase digits.
     * E.g., hex('a', 4, str) => "0041".
     * Append the output to the given StringBuffer.
     * If width is too small to fit, nothing will be appended to output.
     */
    public static StringBuffer hex(int ch, int width, StringBuffer output) {
        String foo = Integer.toString(ch, 16).toUpperCase();
        for (int i = foo.length(); i < width; ++i) {
            output.append('0');
        }
        output.append(foo);
        return output;
    }

    /**
     * Convert a integer to size width (minimum) hex uppercase digits.
     * E.g., hex('a', 4, str) => "0041".  If the integer requires more
     * than width digits, more will be used.
     */
    public static String hex(int ch, int width) {
        String foo = Integer.toString(ch, 16).toUpperCase();
        return "0000000".substring(foo.length() + 7 - width) + foo;
    }

    /**
     * Convert a string to comma-separated groups of 4 hex uppercase
     * digits.  E.g., hex('ab') => "0041,0042".  Append the output
     * to the given StringBuffer.
     */
    public static StringBuffer hex(String s, StringBuffer result) {
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(',');
            hex(s.charAt(i), result);
        }
        return result;
    }

    /**
     * Split a string into pieces based on the given divider character
     * @param s the string to split
     * @param divider the character on which to split.  Occurrences of
     * this character are not included in the output
     * @param output an array to receive the substrings between
     * instances of divider.  It must be large enough on entry to
     * accomodate all output.  Adjacent instances of the divider
     * character will place empty strings into output.  Before
     * returning, output is padded out with empty strings.
     */
    public static void split(String s, char divider, String[] output) {
        int last = 0;
        int current = 0;
        int i;
        for (i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == divider) {
                output[current++] = s.substring(last,i);
                last = i+1;
            }
        }
        output[current++] = s.substring(last,i);
        while (current < output.length) {
            output[current++] = "";
        }
    }

    /**
     * Look up a given string in a string array.  Returns the index at
     * which the first occurrence of the string was found in the
     * array, or -1 if it was not found.
     * @param source the string to search for
     * @param target the array of zero or more strings in which to
     * look for source
     * @return the index of target at which source first occurs, or -1
     * if not found
     */
    public static int lookup(String source, String[] target) {
        for (int i = 0; i < target.length; ++i) {
            if (source.equals(target[i])) return i;
        }
        return -1;
    }

    /**
     * Skip over a sequence of zero or more white space characters
     * at pos.  Return the index of the first non-white-space character
     * at or after pos, or str.length(), if there is none.
     */
    public static int skipWhitespace(String str, int pos) {
        while (pos < str.length()) {
            int c = UTF16.charAt(str, pos);
            if (!UCharacter.isWhitespace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    /**
     * Parse a pattern string starting at offset pos.  Keywords are
     * matched case-insensitively.  Spaces may be skipped and may be
     * optional or required.  Integer values may be parsed, and if
     * they are, they will be returned in the given array.  If
     * successful, the offset of the next non-space character is
     * returned.  On failure, -1 is returned.
     * @param pattern must only contain lowercase characters, which
     * will match their uppercase equivalents as well.  A space
     * character matches one or more required spaces.  A '~' character
     * matches zero or more optional spaces.  A '#' character matches
     * an integer and stores it in parsedInts, which the caller must
     * ensure has enough capacity.
     * @param parsedInts array to receive parsed integers.  Caller
     * must ensure that parsedInts.length is >= the number of '#'
     * signs in 'pattern'.
     * @return the position after the last character parsed, or -1 if
     * the parse failed
     */
    public static int parsePattern(String rule, int pos, int limit,
                                   String pattern, int[] parsedInts) {
        // TODO Update this to handle surrogates
        int[] p = new int[1];
        int intCount = 0; // number of integers parsed
        for (int i=0; i<pattern.length(); ++i) {
            char cpat = pattern.charAt(i);
            char c;
            switch (cpat) {
            case ' ':
                if (pos >= limit) {
                    return -1;
                }
                c = rule.charAt(pos++);
                if (!UCharacter.isWhitespace(c)) {
                    return -1;
                }
                // FALL THROUGH to skipWhitespace
            case '~':
                pos = skipWhitespace(rule, pos);
                break;
            case '#':
                p[0] = pos;
                parsedInts[intCount++] = parseInteger(rule, p, limit);
                if (p[0] == pos) {
                    // Syntax error; failed to parse integer
                    return -1;
                }
                pos = p[0];
                break;
            default:
                if (pos >= limit) {
                    return -1;
                }
                c = (char) UCharacter.toLowerCase(rule.charAt(pos++));
                if (c != cpat) {
                    return -1;
                }
                break;
            }
        }
        return pos;
    }

    /**
     * Parse an integer at pos, either of the form \d+ or of the form
     * 0x[0-9A-Fa-f]+ or 0[0-7]+, that is, in standard decimal, hex,
     * or octal format.
     * @param pos INPUT-OUTPUT parameter.  On input, the first
     * character to parse.  On output, the character after the last
     * parsed character.
     */
    public static int parseInteger(String rule, int[] pos, int limit) {
        int count = 0;
        int value = 0;
        int p = pos[0];
        int radix = 10;

        if (rule.regionMatches(true, p, "0x", 0, 2)) {
            p += 2;
            radix = 16;
        } else if (p < limit && rule.charAt(p) == '0') {
            p++;
            count = 1;
            radix = 8;
        }

        while (p < limit) {
            int d = UCharacter.digit(rule.charAt(p++), radix);
            if (d < 0) {
                --p;
                break;
            }
            ++count;
            int v = (value * radix) + d;
            if (v <= value) {
                // If there are too many input digits, at some point
                // the value will go negative, e.g., if we have seen
                // "0x8000000" already and there is another '0', when
                // we parse the next 0 the value will go negative.
                return 0;
            }
            value = v;
        }
        if (count > 0) {
            pos[0] = p;
        }
        return value;
    }

    /**
     * Trim whitespace from ends of a StringBuffer.
     */
    public static StringBuffer trim(StringBuffer b) {
        // TODO update to handle surrogates
        int i;
        for (i=0; i<b.length() && Character.isWhitespace(b.charAt(i)); ++i) {}
        b.delete(0, i);
        for (i=b.length()-1; i>=0 && Character.isWhitespace(b.charAt(i)); --i) {}
        return b.delete(i+1, b.length());
    }
}
