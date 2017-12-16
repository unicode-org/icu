// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class PlusSignMatcher extends SymbolMatcher {

    private static final PlusSignMatcher DEFAULT = new PlusSignMatcher();

    public static PlusSignMatcher getInstance(DecimalFormatSymbols symbols) {
        String symbolString = symbols.getPlusSignString();
        if (DEFAULT.uniSet.contains(symbolString)) {
            return DEFAULT;
        } else {
            return new PlusSignMatcher(symbolString);
        }
    }

    private PlusSignMatcher(String symbolString) {
        super(symbolString, UnicodeSet.EMPTY);
    }

    private PlusSignMatcher() {
        super(UnicodeSetStaticCache.Key.PLUS_SIGN);
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return false;
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        result.setCharsConsumed(segment);
    }

    @Override
    public String toString() {
        return "<PlusSignMatcher>";
    }

}
