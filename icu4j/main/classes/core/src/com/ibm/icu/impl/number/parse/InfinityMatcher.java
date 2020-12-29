// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StaticUnicodeSets;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * @author sffc
 *
 */
public class InfinityMatcher extends SymbolMatcher {

    private static final InfinityMatcher DEFAULT = new InfinityMatcher();

    public static InfinityMatcher getInstance(DecimalFormatSymbols symbols) {
        String symbolString = symbols.getInfinity();
        if (DEFAULT.uniSet.contains(symbolString)) {
            return DEFAULT;
        } else {
            return new InfinityMatcher(symbolString);
        }
    }

    private InfinityMatcher(String symbolString) {
        super(symbolString, DEFAULT.uniSet);
    }

    private InfinityMatcher() {
        super(StaticUnicodeSets.Key.INFINITY_SIGN);
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return 0 != (result.flags & ParsedNumber.FLAG_INFINITY);
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        result.flags |= ParsedNumber.FLAG_INFINITY;
        result.setCharsConsumed(segment);
    }

    @Override
    public String toString() {
        return "<InfinityMatcher>";
    }
}
