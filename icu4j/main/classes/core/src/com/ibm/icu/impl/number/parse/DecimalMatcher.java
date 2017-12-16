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

    private UnicodeSet groupingUniSet = null;
    private UnicodeSet decimalUniSet = null;
    private UnicodeSet separatorSet = null;
    private UnicodeSet separatorLeadChars = null;
    private String[] digitStrings = null;
    private boolean frozen;

    public DecimalMatcher() {
        frozen = false;
    }

    public void freeze(DecimalFormatSymbols symbols, boolean isStrict) {
        assert !frozen;
        frozen = true;

        String groupingSeparator = symbols.getGroupingSeparatorString();
        String decimalSeparator = symbols.getDecimalSeparatorString();
        UnicodeSetStaticCache.Key groupingKey, decimalKey;

        // Attempt to find values in the static cache
        if (isStrict) {
            groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator,
                    UnicodeSetStaticCache.Key.OTHER_GROUPING_SEPARATORS,
                    UnicodeSetStaticCache.Key.STRICT_COMMA_OR_OTHER,
                    UnicodeSetStaticCache.Key.STRICT_PERIOD_OR_OTHER);
            decimalKey = UnicodeSetStaticCache.chooseFrom(decimalSeparator,
                    UnicodeSetStaticCache.Key.STRICT_COMMA,
                    UnicodeSetStaticCache.Key.STRICT_PERIOD);
        } else {
            groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator,
                    UnicodeSetStaticCache.Key.OTHER_GROUPING_SEPARATORS,
                    UnicodeSetStaticCache.Key.COMMA_OR_OTHER,
                    UnicodeSetStaticCache.Key.PERIOD_OR_OTHER);
            decimalKey = UnicodeSetStaticCache.chooseFrom(decimalSeparator,
                    UnicodeSetStaticCache.Key.COMMA,
                    UnicodeSetStaticCache.Key.PERIOD);
        }

        // Get the sets from the static cache if they were found
        if (groupingKey != null && decimalKey != null) {
            groupingUniSet = UnicodeSetStaticCache.get(groupingKey);
            decimalUniSet = UnicodeSetStaticCache.get(decimalKey);
            UnicodeSetStaticCache.Key separatorKey = UnicodeSetStaticCache.unionOf(groupingKey, decimalKey);
            if (separatorKey != null) {
                separatorSet = UnicodeSetStaticCache.get(separatorKey);
                separatorLeadChars = UnicodeSetStaticCache.getLeadChars(separatorKey);
            }
        } else if (groupingKey != null) {
            groupingUniSet = UnicodeSetStaticCache.get(groupingKey);
        } else if (decimalKey != null) {
            decimalUniSet = UnicodeSetStaticCache.get(decimalKey);
        }

        // Resolve fallbacks if we don't have sets from the static cache
        if (groupingUniSet == null) {
            groupingUniSet = new UnicodeSet().add(groupingSeparator).freeze();
        }
        if (decimalUniSet == null) {
            decimalUniSet = new UnicodeSet().add(decimalSeparator).freeze();
        }
        if (separatorSet == null) {
            separatorSet = new UnicodeSet().addAll(groupingUniSet).addAll(decimalUniSet).freeze();
        }

        int cpZero = symbols.getCodePointZero();
        if (cpZero == -1 || !UCharacter.isDigit(cpZero) || UCharacter.digit(cpZero) != 0) {
            digitStrings = symbols.getDigitStrings();
        }
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
            if (digit == -1 && digitStrings != null) {
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

    @Override
    public UnicodeSet getLeadChars(boolean ignoreCase) {
        UnicodeSet leadChars = new UnicodeSet();
        leadChars.addAll(UnicodeSetStaticCache.getLeadChars(UnicodeSetStaticCache.Key.DIGITS));
        if (digitStrings != null) {
            for (int i = 0; i < digitStrings.length; i++) {
                ParsingUtils.putLeadingChar(digitStrings[i], leadChars, ignoreCase);
            }
        }
        if (separatorLeadChars != null) {
            leadChars.addAll(separatorLeadChars);
        } else {
            ParsingUtils.putLeadSurrogates(separatorSet, leadChars);
        }
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
