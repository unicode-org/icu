// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.Key;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.number.Grouper;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class DecimalMatcher implements NumberParseMatcher {

    /** If true, only accept strings whose grouping sizes match the locale */
    private final boolean requireGroupingMatch;

    /** If true, do not accept grouping separators at all */
    private final boolean groupingDisabled;

    /** If true, do not accept numbers in the fraction */
    private final boolean integerOnly;

    /** If true, save the result as an exponent instead of a quantity in the ParsedNumber */
    private final boolean isScientific;

    private final int grouping1;
    private final int grouping2;

    // Assumption: these sets all consist of single code points. If this assumption needs to be broken,
    // fix getLeadCodePoints() as well as matching logic. Be careful of the performance impact.
    private final UnicodeSet groupingUniSet;
    private final UnicodeSet decimalUniSet;
    private final UnicodeSet separatorSet;
    private final UnicodeSet leadSet;
    private final String[] digitStrings;

    public static DecimalMatcher getInstance(
            DecimalFormatSymbols symbols,
            Grouper grouper,
            int parseFlags) {
        // TODO: Cache popular instances?
        return new DecimalMatcher(symbols, grouper, parseFlags);
    }

    private DecimalMatcher(DecimalFormatSymbols symbols, Grouper grouper, int parseFlags) {
        Key groupingKey, decimalKey;
        String groupingSeparator, decimalSeparator;
        if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_MONETARY_SEPARATORS)) {
            groupingSeparator = symbols.getMonetaryGroupingSeparatorString();
            decimalSeparator = symbols.getMonetaryDecimalSeparatorString();
        } else {
            groupingSeparator = symbols.getGroupingSeparatorString();
            decimalSeparator = symbols.getDecimalSeparatorString();
        }

        // Attempt to find values in the static cache
        if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_STRICT_SEPARATORS)) {
            decimalKey = UnicodeSetStaticCache
                    .chooseFrom(decimalSeparator, Key.STRICT_COMMA, Key.STRICT_PERIOD);
            if (decimalKey == Key.STRICT_COMMA) {
                // Decimal is comma; grouping should be period or custom
                groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator,
                        Key.STRICT_PERIOD_OR_OTHER);
            } else if (decimalKey == Key.STRICT_PERIOD) {
                // Decimal is period; grouping should be comma or custom
                groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator,
                        Key.STRICT_COMMA_OR_OTHER);
            } else {
                // Decimal is custom; grouping can be either comma or period or custom
                groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator,
                        Key.STRICT_COMMA_OR_OTHER,
                        Key.STRICT_PERIOD_OR_OTHER);
            }
        } else {
            decimalKey = UnicodeSetStaticCache.chooseFrom(decimalSeparator, Key.COMMA, Key.PERIOD);
            if (decimalKey == Key.COMMA) {
                // Decimal is comma; grouping should be period or custom
                groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator, Key.PERIOD_OR_OTHER);
            } else if (decimalKey == Key.PERIOD) {
                // Decimal is period; grouping should be comma or custom
                groupingKey = UnicodeSetStaticCache.chooseFrom(groupingSeparator, Key.COMMA_OR_OTHER);
            } else {
                // Decimal is custom; grouping can be either comma or period or custom
                groupingKey = UnicodeSetStaticCache
                        .chooseFrom(groupingSeparator, Key.COMMA_OR_OTHER, Key.PERIOD_OR_OTHER);
            }
        }

        // Get the sets from the static cache if they were found
        UnicodeSet _groupingUniSet = null, _decimalUniSet = null, _separatorSet = null, _leadSet = null;
        if (groupingKey != null && decimalKey != null) {
            _groupingUniSet = UnicodeSetStaticCache.get(groupingKey);
            _decimalUniSet = UnicodeSetStaticCache.get(decimalKey);
            Key separatorKey = UnicodeSetStaticCache.unionOf(groupingKey, decimalKey);
            if (separatorKey != null) {
                _separatorSet = UnicodeSetStaticCache.get(separatorKey);
                Key leadKey = UnicodeSetStaticCache.unionOf(Key.DIGITS, separatorKey);
                if (leadKey != null) {
                    _leadSet = UnicodeSetStaticCache.get(leadKey);
                }
            }
        } else if (groupingKey != null) {
            _groupingUniSet = UnicodeSetStaticCache.get(groupingKey);
        } else if (decimalKey != null) {
            _decimalUniSet = UnicodeSetStaticCache.get(decimalKey);
        }

        // Finish resolving fallbacks
        groupingUniSet = _groupingUniSet != null ? _groupingUniSet
                : new UnicodeSet().add(groupingSeparator.codePointAt(0)).freeze();
        decimalUniSet = _decimalUniSet != null ? _decimalUniSet
                : new UnicodeSet().add(decimalSeparator.codePointAt(0)).freeze();
        separatorSet = _separatorSet != null ? _separatorSet
                : new UnicodeSet().addAll(groupingUniSet).addAll(decimalUniSet).freeze();
        leadSet = _leadSet; // null if not available

        int cpZero = symbols.getCodePointZero();
        if (cpZero == -1 || !UCharacter.isDigit(cpZero) || UCharacter.digit(cpZero) != 0) {
            digitStrings = symbols.getDigitStringsLocal();
        } else {
            digitStrings = null;
        }

        requireGroupingMatch = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_STRICT_GROUPING_SIZE);
        groupingDisabled = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_GROUPING_DISABLED);
        integerOnly = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_INTEGER_ONLY);
        isScientific = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_DECIMAL_SCIENTIFIC);
        grouping1 = grouper.getPrimary();
        grouping2 = grouper.getSecondary();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        return match(segment, result, false);
    }

    public boolean match(StringSegment segment, ParsedNumber result, boolean negativeExponent) {
        if (result.seenNumber() && !isScientific) {
            // A number has already been consumed.
            return false;
        }

        ParsedNumber backup = null;
        if (requireGroupingMatch) {
            backup = new ParsedNumber();
            backup.copyFrom(result);
        }

        int firstGroup = 0;
        int prevGroup = 0;
        int currGroup = 0;
        int separator = -1;
        int initialOffset = segment.getOffset();
        int exponent = 0;
        boolean hasPartialPrefix = false;
        boolean seenBothSeparators = false;
        boolean illegalGrouping = false;
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
                        segment.adjustOffset(overlap);
                        digit = (byte) i;
                        break;
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
                    if (groupingDisabled && !decimalUniSet.contains(cp)) {
                        break;
                    }
                    if (integerOnly && !groupingUniSet.contains(cp)) {
                        break;
                    }
                    separator = cp;
                    firstGroup = currGroup;
                    if (requireGroupingMatch && currGroup == 0 && !decimalUniSet.contains(cp)) {
                        illegalGrouping = true;
                    }
                } else if (!groupingDisabled && separator == cp && groupingUniSet.contains(cp)) {
                    // Second or later grouping separator.
                    prevGroup = currGroup;
                    if (requireGroupingMatch && currGroup == 0) {
                        break;
                    }
                    if (requireGroupingMatch && currGroup != grouping2) {
                        if (currGroup == grouping1) {
                            break;
                        } else {
                            illegalGrouping = true;
                            break;
                        }
                    }
                } else if (!integerOnly && separator != cp && decimalUniSet.contains(cp)) {
                    // Decimal separator after a grouping separator.
                    if (requireGroupingMatch && currGroup != grouping1) {
                        illegalGrouping = true;
                    }
                    seenBothSeparators = true;
                } else {
                    // Invalid separator.
                    break;
                }
                currGroup = 0;
                segment.adjustOffset(Character.charCount(cp));
                continue;
            }

            break;
        }

        // Unless the first group directly precedes the grouping separator, check it for validity
        if (seenBothSeparators || (separator != -1 && !decimalUniSet.contains(separator))) {
            if (currGroup > 0 && firstGroup > grouping2) {
                illegalGrouping = true;
            }
        }

        // Check the final grouping size for validity
        if (requireGroupingMatch
                && separator != -1
                && !seenBothSeparators
                && !decimalUniSet.contains(separator)) {
            if (currGroup > 0 && currGroup != grouping1) {
                illegalGrouping = true;
            }
            if (currGroup == 0 && prevGroup > 0 && prevGroup != grouping1) {
                illegalGrouping = true;
            }
        }

        if (requireGroupingMatch && illegalGrouping) {
            result.copyFrom(backup);
            segment.setOffset(initialOffset);

        } else if (isScientific) {
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

        } else if (result.quantity == null && segment.getOffset() != initialOffset) {
            // Strings that start with a separator but have no digits.
            // We don't need a backup of ParsedNumber because no changes could have been made to it.
            segment.setOffset(initialOffset);
            hasPartialPrefix = true;

        } else if (seenBothSeparators || (separator != -1 && decimalUniSet.contains(separator))) {
            // The final separator was a decimal separator.
            result.quantity.adjustMagnitude(-currGroup);
            result.flags |= ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR;

        }

        return segment.length() == 0 || hasPartialPrefix;
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        if (digitStrings == null && leadSet != null) {
            return leadSet;
        }

        UnicodeSet leadCodePoints = new UnicodeSet();
        // Assumption: the sets are all single code points.
        leadCodePoints.addAll(UnicodeSetStaticCache.get(Key.DIGITS));
        leadCodePoints.addAll(separatorSet);
        if (digitStrings != null) {
            for (int i = 0; i < digitStrings.length; i++) {
                ParsingUtils.putLeadCodePoint(digitStrings[i], leadCodePoints);
            }
        }
        return leadCodePoints.freeze();
    }

    @Override
    public boolean matchesEmpty() {
        return false;
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
