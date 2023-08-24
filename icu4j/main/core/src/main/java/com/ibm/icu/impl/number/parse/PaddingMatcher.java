// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class PaddingMatcher extends SymbolMatcher implements NumberParseMatcher.Flexible {

    public static PaddingMatcher getInstance(String padString) {
        return new PaddingMatcher(padString);
    }

    private PaddingMatcher(String symbolString) {
        super(symbolString, UnicodeSet.EMPTY);
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return false;
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<PaddingMatcher>";
    }
}
