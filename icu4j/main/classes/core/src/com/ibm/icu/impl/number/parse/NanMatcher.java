// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class NanMatcher extends SymbolMatcher {

    private static final NanMatcher DEFAULT = new NanMatcher("NaN");

    public static NanMatcher getInstance(DecimalFormatSymbols symbols, int parseFlags) {
        String symbolString = symbols.getNaN();
        if (DEFAULT.string.equals(symbolString)) {
            return DEFAULT;
        } else {
            return new NanMatcher(symbolString);
        }
    }

    private NanMatcher(String symbolString) {
        super(symbolString, UnicodeSet.EMPTY);
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        // Overriding this here to allow use of statically allocated sets
        int leadCp = string.codePointAt(0);
        UnicodeSet s = UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.NAN_LEAD);
        if (s.contains(leadCp)) {
            return s;
        } else {
            return super.getLeadCodePoints();
        }
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return result.seenNumber();
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        result.flags |= ParsedNumber.FLAG_NAN;
        result.setCharsConsumed(segment);
    }

    @Override
    public String toString() {
        return "<NanMatcher>";
    }

}
