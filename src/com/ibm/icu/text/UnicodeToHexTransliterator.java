/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/UnicodeToHexTransliterator.java,v $ 
 * $Date: 2003/06/03 18:49:35 $ 
 * $Revision: 1.15 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;


/**
 * A transliterator that converts from Unicode characters to 
 * hexadecimal Unicode escape sequences.  It outputs a
 * prefix specified in the constructor and optionally converts the hex
 * digits to uppercase.
 *
 * <p>The format of the output is set by a pattern.  This pattern
 * follows the same syntax as <code>HexToUnicodeTransliterator</code>,
 * except it does not allow multiple specifications.  The pattern sets
 * the prefix string, suffix string, and minimum and maximum digit
 * count.  There are no setters or getters for these attributes; they
 * are set only through the pattern.
 *
 * <p>The setUppercase() and isUppercase() methods control whether 'a'
 * through 'f' or 'A' through 'F' are output as hex digits.  This is
 * not controlled through the pattern; only through the methods.  The
 * default is uppercase.
 *
 * @author Alan Liu
 * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
 */
public class UnicodeToHexTransliterator extends Transliterator {

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static final String _ID = "Any-Hex";

    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
    };

    // Character constants for special pattern chars
    private static final char ZERO      = '0';
    private static final char POUND     = '#';
    private static final char BACKSLASH = '\\';

    /**
     * The pattern set by applyPattern() and returned by toPattern().
     */
    private String pattern;

    /**
     * The string preceding the hex digits, parsed from the pattern.
     */
    private String prefix;

    /**
     * The string following the hex digits, parsed from the pattern.
     */
    private String suffix;

    /**
     * The minimum number of hex digits to output, between 1 and 4,
     * inclusive.  Parsed from the pattern.
     */
    private int minDigits;

    /**
     * If true, output uppercase hex digits; otherwise output
     * lowercase.  Set by setUppercase() and returned by isUppercase().
     */
    private boolean uppercase;

    /**
     * Constructs a transliterator.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @param uppercase if true, the four hex digits will be
     * converted to uppercase; otherwise they will be lowercase.
     * Ignored if direction is HEX_UNICODE.
     * @param filter the filter for this transliterator, or
     * null if none.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public UnicodeToHexTransliterator(String pattern, boolean uppercase,
                                      UnicodeFilter filter) {
        super(_ID, filter);
        this.uppercase = uppercase;
        applyPattern(pattern);
    }

    /**
     * Constructs an uppercase transliterator with no filter.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public UnicodeToHexTransliterator(String pattern) {
        this(pattern, true, null);
    }

    /**
     * Constructs a transliterator with the default prefix "&#092;u"
     * that outputs four uppercase hex digits.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public UnicodeToHexTransliterator() {
        super(_ID, null);
        pattern = "\\\\u0000";
        prefix = "\\u";
        suffix = "";
        minDigits = 4;
        uppercase = true;
    }

    /**
     * Set the pattern recognized by this transliterator.  The pattern
     * must contain zero or more prefix characters, one or more digit
     * characters, and zero or more suffix characters.  The digit
     * characters indicates optional digits ('#') followed by required
     * digits ('0').  The total number of digits cannot exceed 4, and
     * must be at least 1 required digit.  Use a backslash ('\\') to
     * escape any of the special characters.  An empty pattern is not
     * allowed.
     *
     * <p>Example: "U+0000" specifies a prefix of "U+", exactly four
     * digits, and no suffix.  "<###0>" has a prefix of "<", between
     * one and four digits, and a suffix of ">".
     *
     * <p><pre>
     * pattern := prefix-char* digit-spec suffix-char*
     * digit-spec := '#'* '0'+
     * prefix-char := [^special-char] | '\\' special-char
     * suffix-char := [^special-char] | '\\' special-char
     * special-char := ';' | '0' | '#' | '\\'
     * </pre>
     *
     * <p>Limitations: There is no way to set the uppercase attribute
     * in the pattern.  (applyPattern() does not alter the uppercase
     * attribute.)
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public void applyPattern(String thePattern) {
        StringBuffer prefixBuf = null;
        StringBuffer suffixBuf = null;
        int minDigits = 0;
        int maxDigits = 0;

        /* The mode specifies where we are in each spec.
         * mode 0 = in prefix
         * mode 1 = in optional digits (#)
         * mode 2 = in required digits (0)
         * mode 3 = in suffix
         */
        int mode = 0;

        for (int i=0; i<thePattern.length(); ++i) {
            char c = thePattern.charAt(i);
            boolean isLiteral = false;
            if (c == BACKSLASH) {
                if ((i+1)<thePattern.length()) {
                    isLiteral = true;
                    c = thePattern.charAt(++i);
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
                default:
                    isLiteral = true;
                    break;
                }
            }

            if (isLiteral) {
                if (mode == 0) {
                    if (prefixBuf == null) {
                        prefixBuf = new StringBuffer();
                    }
                    prefixBuf.append(c);
                } else {
                    // Any literal outside the prefix moves us into mode 3
                    // (suffix)
                    mode = 3;
                    if (suffixBuf == null) {
                        suffixBuf = new StringBuffer();
                    }
                    suffixBuf.append(c);
                }
            }
        }

        if (minDigits < 1 || maxDigits > 4) {
            // Invalid min/max digit count
            throw new IllegalArgumentException("Invalid min/max digit count");
        }

        pattern = thePattern;
        prefix = (prefixBuf == null) ? "" : prefixBuf.toString();
        suffix = (suffixBuf == null) ? "" : suffixBuf.toString();
        this.minDigits = minDigits;
    }

    /**
     * Return this transliterator's pattern.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Returns the string that precedes the four hex digits.
     * @return prefix string
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the string that precedes the four hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The prefix should not be changed by one
     * thread while another thread may be transliterating.
     * @param prefix prefix string
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns true if this transliterator outputs uppercase hex digits.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public boolean isUppercase() {
        return uppercase;
    }

    /**
     * Sets if this transliterator outputs uppercase hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The uppercase mode should not be changed by
     * one thread while another thread may be transliterating.
     * @param outputUppercase if true, then this transliterator
     * outputs uppercase hex digits.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    public void setUppercase(boolean outputUppercase) {
        uppercase = outputUppercase;
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @deprecated ICU 2.4 This class to be removed after 2003-12-01. Use the Transliterator factory methods.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        /**
         * Performs transliteration changing all characters to
         * Unicode hexadecimal escapes.  For example, '@' -> "U+0040",
         * assuming the prefix is "U+". 
         */
        int cursor = offsets.start;
        int limit = offsets.limit;

        StringBuffer hex = new StringBuffer(prefix);
        int prefixLen = prefix.length();

        while (cursor < limit) {
            char c = text.charAt(cursor);

            hex.setLength(prefixLen);
            boolean showRest = false;
            for (int i=3; i>=0; --i) {
                int d = (c >> (i*4)) & 0xF;
                if (showRest || (d != 0) || minDigits > i) {
                    hex.append(HEX_DIGITS[uppercase ? (d|16) : d]);
                    showRest = true;
                }
            }
            hex.append(suffix);

            text.replace(cursor, cursor+1, hex.toString());
            int len = hex.length();
            cursor += len; // Advance cursor by 1 and adjust for new text
            --len;
            limit += len;
        }

        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        offsets.start = cursor;
    }
}
