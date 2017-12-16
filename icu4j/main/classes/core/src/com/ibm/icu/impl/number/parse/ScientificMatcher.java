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
    private final String minusSignString;
    private final DecimalMatcher exponentMatcher;

    public ScientificMatcher(DecimalFormatSymbols symbols) {
        exponentSeparatorString = symbols.getExponentSeparator();
        minusSignString = symbols.getMinusSignString();
        exponentMatcher = new DecimalMatcher();
        exponentMatcher.isScientific = true;
        exponentMatcher.groupingEnabled = false;
        exponentMatcher.decimalEnabled = false;
        exponentMatcher.freeze(symbols, false);
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        // Only accept scientific notation after the mantissa.
        if (!result.seenNumber()) {
            return false;
        }

        // First match the scientific separator, and then match another number after it.
        int overlap1 = segment.getCommonPrefixLength(exponentSeparatorString);
        if (overlap1 == exponentSeparatorString.length()) {
            // Full exponent separator match; allow a sign, and then try to match digits.
            segment.adjustOffset(overlap1);
            int overlap2 = segment.getCommonPrefixLength(minusSignString);
            boolean minusSign = false;
            if (overlap2 == minusSignString.length()) {
                minusSign = true;
                segment.adjustOffset(overlap2);
            } else if (overlap2 == segment.length()) {
                // Partial sign match
                return true;
            }

            int digitsOffset = segment.getOffset();
            int oldMagnitude = result.quantity.getMagnitude();
            boolean digitsReturnValue = exponentMatcher.match(segment, result);
            if (segment.getOffset() != digitsOffset) {
                // At least one exponent digit was matched.
                result.flags |= ParsedNumber.FLAG_HAS_EXPONENT;
                if (minusSign) {
                    // Switch the magnitude adjustment from positive to negative. Extracting the exponent from the
                    // change in the quantity's magnitude feels a bit like a hack; better would be for the
                    // DecimalMatcher to somehow return the exponent directly.
                    int exponent = result.quantity.getMagnitude() - oldMagnitude;
                    result.quantity.adjustMagnitude(-2 * exponent);
                }
            } else {
                // No exponent digits were matched; un-match the exponent separator.
                segment.adjustOffset(-overlap1);
            }
            return digitsReturnValue;

        } else if (overlap1 == segment.length()) {
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
