// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class MinusSignMatcher extends SymbolMatcher {

    private static final MinusSignMatcher DEFAULT = new MinusSignMatcher();

    public static MinusSignMatcher getInstance(DecimalFormatSymbols symbols) {
        String symbolString = symbols.getMinusSignString();
        if (DEFAULT.uniSet.contains(symbolString)) {
            return DEFAULT;
        } else {
            return new MinusSignMatcher(symbolString);
        }
    }

    private MinusSignMatcher(String symbolString) {
        super(symbolString, UnicodeSet.EMPTY);
    }

    private MinusSignMatcher() {
        super(UnicodeSetStaticCache.Key.MINUS_SIGN);
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return 0 != (result.flags & ParsedNumber.FLAG_NEGATIVE);
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        result.flags |= ParsedNumber.FLAG_NEGATIVE;
        result.setCharsConsumed(segment);
    }

    @Override
    public String toString() {
        return "<MinusSignMatcher>";
    }
}
