// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

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

    public static ScientificMatcher getInstance(
            DecimalFormatSymbols symbols,
            Grouper grouper,
            int parseFlags) {
        // TODO: Static-initialize most common instances?
        return new ScientificMatcher(symbols, grouper, parseFlags);
    }

    private ScientificMatcher(DecimalFormatSymbols symbols, Grouper grouper, int parseFlags) {
        exponentSeparatorString = ParsingUtils.maybeFold(symbols.getExponentSeparator(), parseFlags);
        exponentMatcher = DecimalMatcher.getInstance(symbols,
                grouper,
                ParsingUtils.PARSE_FLAG_DECIMAL_SCIENTIFIC | ParsingUtils.PARSE_FLAG_INTEGER_ONLY);
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
            segment.adjustOffset(overlap1);
            if (segment.length() == 0) {
                return true;
            }
            int leadCp = segment.getCodePoint();
            if (leadCp == -1) {
                // Partial code point match
                return true;
            }

            // Allow a sign, and then try to match digits.
            boolean minusSign = false;
            if (UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.MINUS_SIGN).contains(leadCp)) {
                minusSign = true;
                segment.adjustOffset(Character.charCount(leadCp));
            } else if (UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.PLUS_SIGN).contains(leadCp)) {
                segment.adjustOffset(Character.charCount(leadCp));
            }

            int digitsOffset = segment.getOffset();
            boolean digitsReturnValue = exponentMatcher.match(segment, result, minusSign);
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
    public UnicodeSet getLeadCodePoints() {
        int leadCp = exponentSeparatorString.codePointAt(0);
        UnicodeSet s = UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.SCIENTIFIC_LEAD);
        if (s.contains(leadCp)) {
            return s;
        } else {
            return new UnicodeSet().add(leadCp).freeze();
        }
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
