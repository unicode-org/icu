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

    // TODO: Re-generate these sets from the database. They probably haven't been updated in a while.
    private static final UnicodeSet UNISET_PERIOD_LIKE = new UnicodeSet("[.\\u2024\\u3002\\uFE12\\uFE52\\uFF0E\\uFF61]")
            .freeze();
    private static final UnicodeSet UNISET_STRICT_PERIOD_LIKE = new UnicodeSet("[.\\u2024\\uFE52\\uFF0E\\uFF61]")
            .freeze();
    private static final UnicodeSet UNISET_COMMA_LIKE = new UnicodeSet(
            "[,\\u060C\\u066B\\u3001\\uFE10\\uFE11\\uFE50\\uFE51\\uFF0C\\uFF64]").freeze();
    private static final UnicodeSet UNISET_STRICT_COMMA_LIKE = new UnicodeSet("[,\\u066B\\uFE10\\uFE50\\uFF0C]")
            .freeze();
    private static final UnicodeSet UNISET_OTHER_GROUPING_SEPARATORS = new UnicodeSet(
            "[\\ '\\u00A0\\u066C\\u2000-\\u200A\\u2018\\u2019\\u202F\\u205F\\u3000\\uFF07]").freeze();

    public static DecimalMatcher getInstance(DecimalFormatSymbols symbols) {
        String groupingSeparator = symbols.getGroupingSeparatorString();
        UnicodeSet groupingSet = UNISET_COMMA_LIKE.contains(groupingSeparator)
                ? UNISET_COMMA_LIKE.cloneAsThawed().addAll(UNISET_OTHER_GROUPING_SEPARATORS).freeze()
                : UNISET_PERIOD_LIKE.contains(groupingSeparator)
                        ? UNISET_PERIOD_LIKE.cloneAsThawed().addAll(UNISET_OTHER_GROUPING_SEPARATORS).freeze()
                        : UNISET_OTHER_GROUPING_SEPARATORS.contains(groupingSeparator)
                                ? UNISET_OTHER_GROUPING_SEPARATORS
                                : new UnicodeSet().addAll(groupingSeparator).freeze();

        String decimalSeparator = symbols.getDecimalSeparatorString();
        UnicodeSet decimalSet = UNISET_COMMA_LIKE.contains(decimalSeparator) ? UNISET_COMMA_LIKE
                : UNISET_PERIOD_LIKE.contains(decimalSeparator) ? UNISET_PERIOD_LIKE
                        : new UnicodeSet().addAll(decimalSeparator).freeze();

        return new DecimalMatcher(symbols.getDigitStrings(), groupingSet, decimalSet, false);
    }

    public static DecimalMatcher getExponentInstance(DecimalFormatSymbols symbols) {
        return new DecimalMatcher(symbols.getDigitStrings(),
                new UnicodeSet("[,]").freeze(),
                new UnicodeSet("[.]").freeze(),
                true);
    }

    private final String[] digitStrings;
    private final UnicodeSet groupingUniSet;
    private final UnicodeSet decimalUniSet;
    private final UnicodeSet separatorSet;
    public boolean requireGroupingMatch = false;
    public boolean groupingEnabled = true;
    public int grouping1 = 3;
    public int grouping2 = 3;
    public boolean integerOnly = false;
    private final boolean isScientific;

    private DecimalMatcher(
            String[] digitStrings,
            UnicodeSet groupingUniSet,
            UnicodeSet decimalUniSet,
            boolean isScientific) {
        this.digitStrings = digitStrings;
        this.groupingUniSet = groupingUniSet;
        this.decimalUniSet = decimalUniSet;
        if (groupingEnabled) {
            separatorSet = groupingUniSet.cloneAsThawed().addAll(decimalUniSet).freeze();
        } else {
            separatorSet = decimalUniSet;
        }
        this.isScientific = isScientific;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.quantity != null && !isScientific) {
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
                    exponent = digit + exponent * 10;
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
            result.quantity.adjustMagnitude(exponent);
        } else if (seenBothSeparators || (separator != -1 && decimalUniSet.contains(separator))) {
            // The final separator was a decimal separator.
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
//            result.quantity = null;
//            segment.setOffset(initialOffset);
        }

        return segment.length() == 0 || hasPartialPrefix || segment.isLeadingSurrogate();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // No-op
    }

    @Override
    public String toString() {
        return "<MantissaMatcher>";
    }
}
