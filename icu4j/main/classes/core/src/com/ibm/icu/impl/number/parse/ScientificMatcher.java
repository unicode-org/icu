// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * @author sffc
 *
 */
public class ScientificMatcher implements NumberParseMatcher {

    private final String exponentSeparatorString;
    private final DecimalMatcher exponentMatcher;

    public ScientificMatcher(DecimalFormatSymbols symbols) {
        exponentSeparatorString = symbols.getExponentSeparator();
        exponentMatcher = DecimalMatcher.getExponentInstance(symbols);
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        // Only accept scientific notation after the mantissa.
        if (result.quantity == null) {
            return false;
        }

        // First match the scientific separator, and then match another number after it.
        int overlap = segment.getCommonPrefixLength(exponentSeparatorString);
        if (overlap == exponentSeparatorString.length()) {
            // Full exponent separator match; try to match digits.
            segment.adjustOffset(overlap);
            int digitsOffset = segment.getOffset();
            boolean digitsReturnValue = exponentMatcher.match(segment, result);
            if (segment.getOffset() == digitsOffset) {
                // No digits were matched; un-match the exponent separator.
                segment.adjustOffset(-overlap);
            }
            return digitsReturnValue;

        } else if (overlap == segment.length()) {
            // Partial exponent separator match
            return true;
        }

        // No match
        return false;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<ScientificMatcher " + exponentSeparatorString + ">";
    }
}
