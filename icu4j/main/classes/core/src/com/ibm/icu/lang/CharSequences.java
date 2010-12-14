/*
 *******************************************************************************
 * Copyright (C) 2010, Google, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.lang;

/**
 * A number of utilities for dealing with CharSequences and related classes.
 * @author markdavis
 * @internal
 */
public class CharSequences {
    
    /**
     * Utility function for comparing codepoint to string without generating new
     * string.
     * 
     * @param codepoint
     * @param other
     * @return true if the codepoint equals the string
     * @internal
     */
    public static final boolean equals(int codepoint, CharSequence other) {
        if (other == null) {
            return false;
        }
        switch (other.length()) {
        case 1: return codepoint == other.charAt(0);
        case 2: return codepoint > 0xFFFF && codepoint == Character.codePointAt(other, 0);
        default: return false;
        }
    }

    /**
     * Utility function for comparing objects that may be null
     * string.
     * @internal
     */
    public static final <T extends Object> boolean equals(T a, T b) {
        return a == null ? b == null
                : b == null ? false
                        : a.equals(b);
    }
    
    /** Are we on a character boundary?
     * @internal
     */
    public static boolean onCharacterBoundary(CharSequence s, int i) {
        return i <= 0 
        || i >= s.length() 
        || !Character.isHighSurrogate(s.charAt(i-1))
        || !Character.isLowSurrogate(s.charAt(i));
    }
    
    /**
     * Find code point in string.
     * @param s
     * @param codePoint
     * @return
     * @internal
     */
    public static int indexOf(CharSequence s, int codePoint) {
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(s, i);
            if (cp == codePoint) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Utility function for simplified, more robust loops, such as:
     * <pre>
     *   for (int codePoint : CharSequences.codePoints(string)) {
     *     doSomethingWith(codePoint);
     *   }
     * </pre>
     * @internal
     */
    public static int[] codePoints(CharSequence s) {
        int[] result = new int[s.length()]; // common case
        int j = 0;
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(s, i);
            result[j++] = cp;
        }
        if (j == result.length) {
            return result;
        }
        int[] shortResult = new int[j];
        System.arraycopy(result, 0, shortResult, 0, j);
        return shortResult;
    }

}
