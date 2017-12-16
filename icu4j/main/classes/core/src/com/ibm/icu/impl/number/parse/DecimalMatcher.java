// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class DecimalMatcher implements NumberParseMatcher {

    public boolean requireGroupingMatch = false;
    public boolean decimalEnabled = true;
    public boolean groupingEnabled = true;
    public int grouping1 = 3;
    public int grouping2 = 3;
    public boolean integerOnly = false;
    public boolean isScientific = false;

    private UnicodeSet groupingUniSet;
    private UnicodeSet decimalUniSet;
    private UnicodeSet separatorSet;
    private String[] digitStrings;
    private boolean frozen;

    public DecimalMatcher() {
        frozen = false;
    }

    public void freeze(DecimalFormatSymbols symbols, boolean isStrict) {
        assert !frozen;
        frozen = true;

        groupingUniSet = SeparatorSetUtils.getGroupingUnicodeSet(symbols, isStrict);
        decimalUniSet = SeparatorSetUtils.getDecimalUnicodeSet(symbols, isStrict);
        separatorSet = SeparatorSetUtils.unionUnicodeSets(groupingUniSet, decimalUniSet);
        digitStrings = symbols.getDigitStringsLocal();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        return match(segment, result, false);
    }

    public boolean match(StringSegment segment, ParsedNumber result, boolean negativeExponent) {
        assert frozen;
        if (result.seenNumber() && !isScientific) {
            // A number has already been consumed.
            return false;
        }

        int initialOffset = segment.getOffset();
        int currGroup = 0;
        int separator = -1;
        int lastSeparatorOffset = segment.getOffset();
        int exponent = 0;
        boolean hasPartialPrefix = false;
        boolean seenBothSeparators = false;
        while (segment.length() > 0) {
            hasPartialPrefix = false;

            // Attempt to match a digit.
            byte digit = -1;

            // Try by code point digit value.
            int cp = segment.getCodePoint();
            if (UCharacter.isDigit(cp)) {
                segment.adjustOffset(Character.charCount(cp));
                digit = (byte) UCharacter.digit(cp);
            }

            // Try by digit string.
            if (digit == -1) {
                for (int i = 0; i < digitStrings.length; i++) {
                    String str = digitStrings[i];
                    int overlap = segment.getCommonPrefixLength(str);
                    if (overlap == str.length()) {
                        segment.adjustOffset(str.length());
                        digit = (byte) i;
                    } else if (overlap == segment.length()) {
                        hasPartialPrefix = true;
                    }
                }
            }

            // If found, save it in the DecimalQuantity or scientific adjustment.
            if (digit >= 0) {
                if (isScientific) {
                    int nextExponent = digit + exponent * 10;
                    if (nextExponent < exponent) {
                        // Overflow
                        exponent = Integer.MAX_VALUE;
                    } else {
                        exponent = nextExponent;
                    }
                } else {
                    if (result.quantity == null) {
                        result.quantity = new DecimalQuantity_DualStorageBCD();
                    }
                    result.quantity.appendDigit(digit, 0, true);
                }
                result.setCharsConsumed(segment);
                currGroup++;
                continue;
            }

            // Attempt to match a separator.
            if (!seenBothSeparators && cp != -1 && separatorSet.contains(cp)) {
                if (separator == -1) {
                    // First separator; could be either grouping or decimal.
                    separator = cp;
                    if (requireGroupingMatch && currGroup == 0) {
                        break;
                    }
                } else if (groupingEnabled && separator == cp && groupingUniSet.contains(cp)) {
                    // Second or later grouping separator.
                    if (requireGroupingMatch && currGroup != grouping2) {
                        break;
                    }
                } else if (groupingEnabled && separator != cp && decimalUniSet.contains(cp)) {
                    // Decimal separator after a grouping separator.
                    if (requireGroupingMatch && currGroup != grouping1) {
                        break;
                    }
                    seenBothSeparators = true;
                } else {
                    // Invalid separator.
                    break;
                }
                currGroup = 0;
                lastSeparatorOffset = segment.getOffset();
                segment.adjustOffset(Character.charCount(cp));
                continue;
            }

            break;
        }

        if (isScientific) {
            boolean overflow = (exponent == Integer.MAX_VALUE);
            if (!overflow) {
                try {
                    result.quantity.adjustMagnitude(negativeExponent ? -exponent : exponent);
                } catch (ArithmeticException e) {
                    overflow = true;
                }
            }
            if (overflow) {
                if (negativeExponent) {
                    // Set to zero
                    result.quantity.clear();
                } else {
                    // Set to infinity
                    result.quantity = null;
                    result.flags |= ParsedNumber.FLAG_INFINITY;
                }
            }
        } else if (result.quantity == null) {
            // No-op: strings that start with a separator without any other digits
        } else if (seenBothSeparators || (separator != -1 && decimalUniSet.contains(separator))) {
            // The final separator was a decimal separator.
            result.flags |= ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR;
            result.quantity.adjustMagnitude(-currGroup);
            if (integerOnly) {
                result.quantity.truncate();
                segment.setOffset(lastSeparatorOffset);
            }
        } else if (separator != -1 && !groupingEnabled) {
            // The final separator was a grouping separator, but we aren't accepting grouping.
            // Reset the offset to immediately before that grouping separator.
            result.quantity.adjustMagnitude(-currGroup);
            result.quantity.truncate();
            segment.setOffset(lastSeparatorOffset);
        } else if (separator != -1 && requireGroupingMatch && groupingUniSet.contains(separator)
                && currGroup != grouping1) {
            // The final separator was a grouping separator, and we have a mismatched grouping size.
            // Reset the offset to the beginning of the number.
            // TODO
            result.quantity.adjustMagnitude(-currGroup);
            result.quantity.truncate();
            segment.setOffset(lastSeparatorOffset);
            // result.quantity = null;
            // segment.setOffset(initialOffset);
        }

        return segment.length() == 0 || hasPartialPrefix || segment.isLeadingSurrogate();
    }

    private static final UnicodeSet UNISET_DIGITS = new UnicodeSet("[:digit:]");

    @Override
    public UnicodeSet getLeadChars(boolean ignoreCase) {
        UnicodeSet leadChars = new UnicodeSet();
        ParsingUtils.putLeadSurrogates(UNISET_DIGITS, leadChars);
        for (int i = 0; i < digitStrings.length; i++) {
            ParsingUtils.putLeadingChar(digitStrings[i], leadChars, ignoreCase);
        }
        ParsingUtils.putLeadSurrogates(separatorSet, leadChars);
        return leadChars.freeze();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<DecimalMatcher>";
    }
}
