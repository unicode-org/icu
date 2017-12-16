// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

/**
 * A collection of utility functions used by the number parsing package.
 */
public class ParsingUtils {

    /**
     * Adds all chars and lead surrogates from input into output.
     */
    public static void putLeadSurrogates(UnicodeSet input, UnicodeSet output) {
        if (input.isEmpty()) {
            return;
        }
        for (EntryRange range : input.ranges()) {
            if (range.codepointEnd <= 0xFFFF) {
                // All BMP chars
                output.add(range.codepoint, range.codepointEnd);
            } else {
                // Need to get the lead surrogates
                // TODO: Make this more efficient?
                if (range.codepoint <= 0xFFFF) {
                    output.add(range.codepoint, 0xFFFF);
                }
                for (int cp = Math.max(0x10000, range.codepoint); cp <= range.codepointEnd; cp++) {
                    output.add(UTF16.getLeadSurrogate(cp));
                }
            }
        }
    }

    /**
     * Adds the first char of the given string to leadChars, performing case-folding if necessary.
     */
    public static void putLeadingChar(String str, UnicodeSet leadChars, boolean ignoreCase) {
        if (str.isEmpty()) {
            return;
        }
        if (ignoreCase) {
            leadChars.add(getCaseFoldedLeadingChar(str));
        } else {
            leadChars.add(str.charAt(0));
        }
    }

    public static char getCaseFoldedLeadingChar(CharSequence str) {
        int cp = UCharacter.foldCase(Character.codePointAt(str, 0), true);
        if (cp <= 0xFFFF) {
            return (char) cp;
        } else {
            return UTF16.getLeadSurrogate(cp);
        }
    }

}
