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
public class ApproximatelySignMatcher extends SymbolMatcher {

    private static final ApproximatelySignMatcher DEFAULT = new ApproximatelySignMatcher(false);
    private static final ApproximatelySignMatcher DEFAULT_ALLOW_TRAILING = new ApproximatelySignMatcher(true);

    public static ApproximatelySignMatcher getInstance(DecimalFormatSymbols symbols, boolean allowTrailing) {
        String symbolString = symbols.getApproximatelySignString();
        if (DEFAULT.uniSet.contains(symbolString)) {
            return allowTrailing ? DEFAULT_ALLOW_TRAILING : DEFAULT;
        } else {
            return new ApproximatelySignMatcher(symbolString, allowTrailing);
        }
    }

    private final boolean allowTrailing;

    private ApproximatelySignMatcher(String symbolString, boolean allowTrailing) {
        super(symbolString, DEFAULT.uniSet);
        this.allowTrailing = allowTrailing;
    }

    private ApproximatelySignMatcher(boolean allowTrailing) {
        super(StaticUnicodeSets.Key.APPROXIMATELY_SIGN);
        this.allowTrailing = allowTrailing;
    }

    @Override
    protected boolean isDisabled(ParsedNumber result) {
        return !allowTrailing && result.seenNumber();
    }

    @Override
    protected void accept(StringSegment segment, ParsedNumber result) {
        result.setCharsConsumed(segment);
    }

    @Override
    public String toString() {
        return "<ApproximatelySignMatcher>";
    }

}
