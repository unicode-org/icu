// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

public class BreakTransliteratorAccess {
    // Non-public access
    public static Transliterator newInstance() {
        return new BreakTransliterator("Any-Break", UnicodeSet.ALL_CODE_POINTS);
    }
}
