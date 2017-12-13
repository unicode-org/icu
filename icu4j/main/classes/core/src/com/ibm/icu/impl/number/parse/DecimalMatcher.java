// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.math.RoundingMode;

import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author sffc
 *
 */
public class DecimalMatcher implements NumberParseMatcher {

    /**
     * @return
     */
    public static DecimalMatcher getInstance(DecimalFormatSymbols symbols) {
        // TODO(sffc): Auto-generated method stub
        return new DecimalMatcher(symbols.getDigitStrings(),
                new UnicodeSet("[,]").freeze(),
                new UnicodeSet("[.]").freeze(),
                false);
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
    private final int grouping1 = 3;
    private final int grouping2 = 3;
    private final boolean isScientific;

    private DecimalMatcher(
            String[] digitStrings,
            UnicodeSet groupingUniSet,
            UnicodeSet decimalUniSet,
            boolean isScientific) {
        this.digitStrings = digitStrings;
        this.groupingUniSet = groupingUniSet;
        this.decimalUniSet = decimalUniSet;
        separatorSet = groupingUniSet.cloneAsThawed().addAll(decimalUniSet).freeze();
        this.isScientific = isScientific;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.quantity != null && !isScientific) {
            // A number has already been consumed.
            return false;
        }

        int currGroup = 0;
        int separator = -1;
        int lastSeparatorOffset = segment.getOffset();
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
                    result.scientificAdjustment = digit + result.scientificAdjustment * 10;
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
                } else if (separator == cp && groupingUniSet.contains(cp)) {
                    // Second or later grouping separator.
                    if (requireGroupingMatch && currGroup != grouping2) {
                        break;
                    }
                } else if (separator != cp && decimalUniSet.contains(cp)) {
                    // Decimal separator.
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

        if (seenBothSeparators || (separator != -1 && decimalUniSet.contains(separator))) {
            result.quantity.adjustMagnitude(-currGroup);
        } else if (requireGroupingMatch && separator != -1 && groupingUniSet.contains(separator)
                && currGroup != grouping1) {
            result.quantity.adjustMagnitude(-currGroup);
            result.quantity.roundToMagnitude(0, RoundingUtils.mathContextUnlimited(RoundingMode.FLOOR));
            segment.setOffset(lastSeparatorOffset);
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
