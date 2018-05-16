// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.StaticUnicodeSets;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class ScientificMatcher implements NumberParseMatcher {

    private final String exponentSeparatorString;
    private final DecimalMatcher exponentMatcher;
    private final String customMinusSign;
    private final String customPlusSign;

    public static ScientificMatcher getInstance(DecimalFormatSymbols symbols, Grouper grouper) {
        // TODO: Static-initialize most common instances?
        return new ScientificMatcher(symbols, grouper);
    }

    private ScientificMatcher(DecimalFormatSymbols symbols, Grouper grouper) {
        exponentSeparatorString = symbols.getExponentSeparator();
        exponentMatcher = DecimalMatcher.getInstance(symbols,
                grouper,
                ParsingUtils.PARSE_FLAG_INTEGER_ONLY | ParsingUtils.PARSE_FLAG_GROUPING_DISABLED);

        String minusSign = symbols.getMinusSignString();
        customMinusSign = minusSignSet().contains(minusSign) ? null : minusSign;
        String plusSign = symbols.getPlusSignString();
        customPlusSign = plusSignSet().contains(plusSign) ? null : plusSign;
    }

    private static UnicodeSet minusSignSet() {
        return StaticUnicodeSets.get(StaticUnicodeSets.Key.MINUS_SIGN);
    }

    private static UnicodeSet plusSignSet() {
        return StaticUnicodeSets.get(StaticUnicodeSets.Key.PLUS_SIGN);
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
            // Full exponent separator match.

            // First attempt to get a code point, returning true if we can't get one.
            if (segment.length() == overlap1) {
                return true;
            }
            segment.adjustOffset(overlap1);

            // Allow a sign, and then try to match digits.
            int exponentSign = 1;
            if (segment.startsWith(minusSignSet())) {
                exponentSign = -1;
                segment.adjustOffsetByCodePoint();
            } else if (segment.startsWith(plusSignSet())) {
                segment.adjustOffsetByCodePoint();
            } else if (segment.startsWith(customMinusSign)) {
                int overlap2 = segment.getCommonPrefixLength(customMinusSign);
                if (overlap2 != customMinusSign.length()) {
                    // Partial custom sign match; un-match the exponent separator.
                    segment.adjustOffset(-overlap1);
                    return true;
                }
                exponentSign = -1;
                segment.adjustOffset(overlap2);
            } else if (segment.startsWith(customPlusSign)) {
                int overlap2 = segment.getCommonPrefixLength(customPlusSign);
                if (overlap2 != customPlusSign.length()) {
                    // Partial custom sign match; un-match the exponent separator.
                    segment.adjustOffset(-overlap1);
                    return true;
                }
                segment.adjustOffset(overlap2);
            }

            // We are supposed to accept E0 after NaN, so we need to make sure result.quantity is available.
            boolean wasNull = (result.quantity == null);
            if (wasNull) {
                result.quantity = new DecimalQuantity_DualStorageBCD();
            }
            int digitsOffset = segment.getOffset();
            boolean digitsReturnValue = exponentMatcher.match(segment, result, exponentSign);
            if (wasNull) {
                result.quantity = null;
            }

            if (segment.getOffset() != digitsOffset) {
                // At least one exponent digit was matched.
                result.flags |= ParsedNumber.FLAG_HAS_EXPONENT;
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
    public boolean smokeTest(StringSegment segment) {
        return segment.startsWith(exponentSeparatorString);
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
