/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/HexToUnicodeTransliterator.java,v $ 
 * $Date: 2003/06/03 18:49:34 $ 
 * $Revision: 1.16 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.Utility;

/**
 * A transliterator that converts from hexadecimal Unicode escape
 * sequences to the characters they represent.  For example, "U+0040"
 * and '\u0040'.  A default HexToUnicodeTransliterator recognizes the
 * prefixes "U+", "u+", "&#92;U", and "&#92;u".  Hex values may be
 * upper- or lowercase.  By calling the applyPattern() method, one
 * or more custom prefix/suffix pairs may be specified.  See
 * applyPattern() for details.
 *
 * @author Alan Liu
 * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
 */
public class HexToUnicodeTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static final String _ID = "Hex-Any";

    /**
     * This pattern encodes the following specs for the default constructor:
     *   \\u0000
     *   \\U0000
     *   u+0000
     *   U+0000
     * The multiple backslashes resolve to a single backslash
     * in the effective prefix.
     */
    private static final String DEFAULT_PATTERN = "\\\\u0000;\\\\U0000;u+0000;U+0000";

    // Character constants for special pattern characters
    private static final char SEMICOLON = ';';
    private static final char ZERO      = '0';
    private static final char POUND     = '#';
    private static final char BACKSLASH = '\\';

    /**
     * The pattern for this transliterator
     */
    private String pattern;

    /**
     * The processed pattern specification.  See applyPattern() for
     * details.
     */
    private char[] affixes;

    /**
     * The number of different affix sets in affixes.
     */
    private int affixCount;

    /**
     * Constructs a transliterator.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public HexToUnicodeTransliterator() {
        super(_ID, null);
        applyPattern(DEFAULT_PATTERN);
    }

    /**
     * Constructs a transliterator.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public HexToUnicodeTransliterator(String thePattern) {
        this(thePattern, null);
    }
    
    /**
     * Constructs a transliterator.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public HexToUnicodeTransliterator(String thePattern,
                                      UnicodeFilter theFilter) {
        super(_ID, theFilter);
        applyPattern(thePattern);
    }

    /**
     * Set the patterns recognized by this transliterator.  One or
     * more patterns may be specified, separated by semicolons (';').
     * Each pattern contains zero or more prefix characters, one or
     * more digit characters, and zero or more suffix characters.  The
     * digit characters indicates optional digits ('#') followed by
     * required digits ('0').  The total number of digits cannot
     * exceed 4, and must be at least 1 required digit.  Use a
     * backslash ('\\') to escape any of the special characters.  An
     * empty pattern is allowed; it specifies a transliterator that
     * does nothing.
     *
     * <p>Example: "U+0000;<###0>" specifies two patterns.  The first
     * has a prefix of "U+", exactly four digits, and no suffix.  The
     * second has a prefix of "<", between one and four digits, and a
     * suffix of ">".
     *
     * <p><pre>
     * pattern := spec | ( pattern ';' spec )
     * spec := prefix-char* digit-spec suffix-char*
     * digit-spec := '#'* '0'+
     * prefix-char := [^special-char] | '\\' special-char
     * suffix-char := [^special-char] | '\\' special-char
     * special-char := ';' | '0' | '#' | '\\'
     * </pre>
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public void applyPattern(String pattern) {

        /* The pattern is processed and stored in affixes.  The pattern
         * consists of zero or more affixes.  Each affix is parsed to
         * determine the prefix, suffix, minimum digit count, and maximum
         * digit count.  These values are then stored as a four character
         * header.  That is, their numeric values are cast to UChars and
         * stored in the string.  Following these four characters, the prefix
         * characters, then suffix characters are stored.  Each spec takes
         * n+4 characters, where n is the total length of the prefix and
         * suffix.
         */

        StringBuffer affixes = new StringBuffer();
        affixCount = 0;

        /* The mode specifies where we are in each spec.
         * mode 0 = in prefix
         * mode 1 = in optional digits (#)
         * mode 2 = in required digits (0)
         * mode 3 = in suffix
         */
        int mode = 0;

        int prefixLen = 0, suffixLen = 0, minDigits = 0, maxDigits = 0;
        int start = 0;

        /* To make parsing easier, we append a virtual ';' at the end of
         * the pattern string, if there isn't one already.  When we get to
         * the index pattern.length() (that is, one past the end), we
         * create a virtual ';' if necessary.
         */
        char c = 0;                // These are outside the loop so we can
        boolean isLiteral = false; // see the previous character...
        for (int i=0; i<=pattern.length(); ++i) {
            // Create the virtual trailing ';' if necessary
            if (i == pattern.length()) {
                // If the last character was not a non-literal ';'...
                if (i > 0 && !(c == SEMICOLON && !isLiteral)) {
                    c = SEMICOLON;
                    isLiteral = false;
                } else {
                    break;
                }
            } else {
                c = pattern.charAt(i);
                isLiteral = false;
            }

            if (c == BACKSLASH) {
                if ((i+1)<pattern.length()) {
                    isLiteral = true;
                    c = pattern.charAt(++i);
                } else {
                    // Trailing '\\'
                    throw new IllegalArgumentException("Trailing '\\'");
                }
            }

            if (!isLiteral) {
                switch (c) {
                case POUND:
                    // Seeing a '#' moves us from mode 0 (prefix) to mode 1
                    // (optional digits).
                    if (mode == 0) {
                        ++mode;
                    } else if (mode != 1) {
                        // Unquoted '#'
                        throw new IllegalArgumentException("Unquoted '#'");
                    }
                    ++maxDigits;
                    break;
                case ZERO:
                    // Seeing a '0' moves us to mode 2 (required digits)
                    if (mode < 2) {
                        mode = 2;
                    } else if (mode != 2) {
                        // Unquoted '0'
                        throw new IllegalArgumentException("Unquoted '0'");
                    }
                    ++minDigits;
                    ++maxDigits;
                    break;
                case SEMICOLON:
                    if (minDigits < 1 || maxDigits > 4
                        // Invalid min/max digit count
                        || prefixLen > 0xFFFF || suffixLen > 0xFFFF) {
                        // Suffix or prefix too long
                        throw new IllegalArgumentException("Suffix or prefix too long");
                    }
                    // If there was no prefix and no suffix, then the
                    // header will not have been allocated yet.  We need
                    // allocate the header now.
                    if (start == affixes.length()) {
                        affixes.append("AAAA");
                    }
                    // Fill in 4-character header
                    affixes.setCharAt(start++, (char) prefixLen);
                    affixes.setCharAt(start++, (char) suffixLen);
                    affixes.setCharAt(start++, (char) minDigits);
                    affixes.setCharAt(start,   (char) maxDigits);
                    start = affixes.length();
                    ++affixCount;
                    prefixLen = suffixLen = minDigits = maxDigits = mode = 0;
                    break;
                default:
                    isLiteral = true;
                    break;
                }
            }

            if (isLiteral) {
                if (start == affixes.length()) {
                    // Make space for the header.  Append any four
                    // characters as place holders for the header values.
                    // We fill these in when we parse the ';'.
                    affixes.append("AAAA");
                }
                affixes.append(c);
                if (mode == 0) {
                    ++prefixLen;
                } else {
                    // Any literal outside the prefix moves us into mode 3
                    // (suffix)
                    mode = 3;
                    ++suffixLen;
                }
            }
        }

        // We only modify the pattern and affixes member variables if
        // we get to this point, that is, if the parse succeeds.
        this.pattern = pattern;
        int len = affixes.length();
        this.affixes = new char[len];
        Utility.getChars(affixes, 0, len, this.affixes, 0);
    }

    /**
     * Return this transliterator's pattern.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int cursor = offsets.start;
        int limit = offsets.limit;
        int i, j, ipat;

      loop:
        while (cursor < limit) {
            // Loop over the specs in affixes.  If affixCount is zero (an
            // empty pattern), then we do nothing.  We exit this loop when
            // we match one of the specs.  We exit this function (by
            // jumping to exit: below) if a partial match is detected and
            // isIncremental is true.
            for (j=0, ipat=0; j<affixCount; ++j) {

                // Read the header
                int prefixLen = affixes[ipat++];
                int suffixLen = affixes[ipat++];
                int minDigits = affixes[ipat++];
                int maxDigits = affixes[ipat++];

                // curs is a copy of cursor that is advanced over the
                // characters as we parse them.
                int curs = cursor;
                boolean match = true;

                for (i=0; i<prefixLen; ++i) {
                    if (curs >= limit) {
                        if (i > 0) {
                            // We've already matched a character.  This is
                            // a partial match, so we return if in
                            // incremental mode.  In non-incremental mode,
                            // go to the next spec.
                            if (isIncremental) {
                                break loop;
                            }
                            match = false;
                            break;
                        }
                    }
                    char c = text.charAt(curs++);
                    if (c != affixes[ipat + i]) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    char u = 0;
                    int digitCount = 0;
                    for (;;) {
                        if (curs >= limit) {
                            // Check for partial match in incremental mode.
                            if (curs > cursor && isIncremental) {
                                break loop;
                            }
                            break;
                        }
                        int digit = Character.digit(text.charAt(curs), 16);
                        if (digit < 0) {
                            break;
                        }
                        ++curs;
                        u <<= 4;
                        u |= (char) digit;
                        if (++digitCount == maxDigits) {
                            break;
                        }
                    }

                    match = (digitCount >= minDigits);

                    if (match) {
                        for (i=0; i<suffixLen; ++i) {
                            if (curs >= limit) {
                                // Check for partial match in incremental mode.
                                if (curs > cursor && isIncremental) {
                                    break loop;
                                }
                                match = false;
                                break;
                            }
                            char c = text.charAt(curs++);
                            if (c != affixes[ipat + prefixLen + i]) {
                                match = false;
                                break;
                            }
                        }

                        if (match) {
                            // At this point, we have a match
                            text.replace(cursor, curs, String.valueOf(u));
                            limit -= curs - cursor - 1;
                            // The following break statement leaves the
                            // loop that is traversing the specs in
                            // affixes.  We then parse the next input
                            // character.
                            break;
                        }
                    }
                }

                ipat += prefixLen + suffixLen;
            }

            ++cursor;
        }

        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        offsets.start = cursor;
    }
}
