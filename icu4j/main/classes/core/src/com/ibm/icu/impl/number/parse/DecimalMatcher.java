// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StaticUnicodeSets;
import com.ibm.icu.impl.StaticUnicodeSets.Key;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.lang.UCharacter;
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

    // Fraction grouping parsing is disabled for now but could be enabled later.
    // See http://bugs.icu-project.org/trac/ticket/10794
    // private final boolean fractionGrouping;

    /** If true, do not accept numbers in the fraction */
    private final boolean integerOnly;

    private final int grouping1;
    private final int grouping2;

    private final String groupingSeparator;
    private final String decimalSeparator;

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
        if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_MONETARY_SEPARATORS)) {
            groupingSeparator = symbols.getMonetaryGroupingSeparatorString();
            decimalSeparator = symbols.getMonetaryDecimalSeparatorString();
        } else {
            groupingSeparator = symbols.getGroupingSeparatorString();
            decimalSeparator = symbols.getDecimalSeparatorString();
        }
        boolean strictSeparators = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_STRICT_SEPARATORS);
        Key groupingKey = strictSeparators ? Key.STRICT_ALL_SEPARATORS : Key.ALL_SEPARATORS;

        // Attempt to find separators in the static cache

        groupingUniSet = StaticUnicodeSets.get(groupingKey);
        Key decimalKey = StaticUnicodeSets.chooseFrom(decimalSeparator,
                strictSeparators ? Key.STRICT_COMMA : Key.COMMA,
                strictSeparators ? Key.STRICT_PERIOD : Key.PERIOD);
        if (decimalKey != null) {
            decimalUniSet = StaticUnicodeSets.get(decimalKey);
        } else if (!decimalSeparator.isEmpty()) {
            decimalUniSet = new UnicodeSet().add(decimalSeparator.codePointAt(0)).freeze();
        } else {
            decimalUniSet = UnicodeSet.EMPTY;
        }

        if (groupingKey != null && decimalKey != null) {
            // Everything is available in the static cache
            separatorSet = groupingUniSet;
            leadSet = StaticUnicodeSets.get(strictSeparators ? Key.DIGITS_OR_ALL_SEPARATORS
                    : Key.DIGITS_OR_STRICT_ALL_SEPARATORS);
        } else {
            separatorSet = new UnicodeSet().addAll(groupingUniSet).addAll(decimalUniSet).freeze();
            leadSet = null;
        }

        int cpZero = symbols.getCodePointZero();
        if (cpZero == -1 || !UCharacter.isDigit(cpZero) || UCharacter.digit(cpZero) != 0) {
            digitStrings = symbols.getDigitStringsLocal();
        } else {
            digitStrings = null;
        }

        requireGroupingMatch = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_STRICT_GROUPING_SIZE);
        groupingDisabled = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_GROUPING_DISABLED);
        integerOnly = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_INTEGER_ONLY);
        grouping1 = grouper.getPrimary();
        grouping2 = grouper.getSecondary();

        // Fraction grouping parsing is disabled for now but could be enabled later.
        // See http://bugs.icu-project.org/trac/ticket/10794
        // fractionGrouping = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_FRACTION_GROUPING_ENABLED);
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        return match(segment, result, 0);
    }

    /**
     * @param exponentSign
     *            -1 means a negative exponent; +1 means a positive exponent; 0 means NO exponent. If -1
     *            or +1, the number will be saved by scaling the pre-existing DecimalQuantity in the
     *            ParsedNumber. If 0, a new DecimalQuantity will be created to store the number.
     */
    public boolean match(StringSegment segment, ParsedNumber result, int exponentSign) {
        if (result.seenNumber() && exponentSign == 0) {
            // A number has already been consumed.
            return false;
        } else if (exponentSign != 0) {
            // scientific notation always comes after the number
            assert result.quantity != null;
        }

        // Initial offset before any character consumption.
        int initialOffset = segment.getOffset();

        // Return value: whether to ask for more characters.
        boolean maybeMore = false;

        // All digits consumed so far.
        DecimalQuantity_DualStorageBCD digitsConsumed = null;

        // The total number of digits after the decimal place, used for scaling the result.
        int digitsAfterDecimalPlace = 0;

        // The actual grouping and decimal separators used in the string.
        // If non-null, we have seen that token.
        String actualGroupingString = null;
        String actualDecimalString = null;

        // Information for two groups: the previous group and the current group.
        //
        // Each group has three pieces of information:
        //
        // Offset: the string position of the beginning of the group, including a leading separator
        // if there was a leading separator. This is needed in case we need to rewind the parse to
        // that position.
        //
        // Separator type:
        // 0 => beginning of string
        // 1 => lead separator is a grouping separator
        // 2 => lead separator is a decimal separator
        //
        // Count: the number of digits in the group. If -1, the group has been validated.
        int currGroupOffset = 0;
        int currGroupSepType = 0;
        int currGroupCount = 0;
        int prevGroupOffset = -1;
        int prevGroupSepType = -1;
        int prevGroupCount = -1;

        while (segment.length() > 0) {
            maybeMore = false;

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
                    if (str.isEmpty()) {
                        continue;
                    }
                    int overlap = segment.getCommonPrefixLength(str);
                    if (overlap == str.length()) {
                        segment.adjustOffset(overlap);
                        digit = (byte) i;
                        break;
                    }
                    maybeMore = maybeMore || (overlap == segment.length());
                }
            }

            if (digit >= 0) {
                // Digit was found.
                if (digitsConsumed == null) {
                    digitsConsumed = new DecimalQuantity_DualStorageBCD();
                }
                digitsConsumed.appendDigit(digit, 0, true);
                currGroupCount++;
                if (actualDecimalString != null) {
                    digitsAfterDecimalPlace++;
                }
                continue;
            }

            // Attempt to match a literal grouping or decimal separator.
            boolean isDecimal = false;
            boolean isGrouping = false;

            // 1) Attempt the decimal separator string literal.
            // if (we have not seen a decimal separator yet) { ... }
            if (actualDecimalString == null && !decimalSeparator.isEmpty()) {
                int overlap = segment.getCommonPrefixLength(decimalSeparator);
                maybeMore = maybeMore || (overlap == segment.length());
                if (overlap == decimalSeparator.length()) {
                    isDecimal = true;
                    actualDecimalString = decimalSeparator;
                }
            }

            // 2) Attempt to match the actual grouping string literal.
            if (actualGroupingString != null) {
                int overlap = segment.getCommonPrefixLength(actualGroupingString);
                maybeMore = maybeMore || (overlap == segment.length());
                if (overlap == actualGroupingString.length()) {
                    isGrouping = true;
                }
            }

            // 2.5) Attempt to match a new the grouping separator string literal.
            // if (we have not seen a grouping or decimal separator yet) { ... }
            if (!groupingDisabled
                    && actualGroupingString == null
                    && actualDecimalString == null
                    && !groupingSeparator.isEmpty()) {
                int overlap = segment.getCommonPrefixLength(groupingSeparator);
                maybeMore = maybeMore || (overlap == segment.length());
                if (overlap == groupingSeparator.length()) {
                    isGrouping = true;
                    actualGroupingString = groupingSeparator;
                }
            }

            // 3) Attempt to match a decimal separator from the equivalence set.
            // if (we have not seen a decimal separator yet) { ... }
            // The !isGrouping is to confirm that we haven't yet matched the current character.
            if (!isGrouping && actualDecimalString == null) {
                if (decimalUniSet.contains(cp)) {
                    isDecimal = true;
                    actualDecimalString = UCharacter.toString(cp);
                }
            }

            // 4) Attempt to match a grouping separator from the equivalence set.
            // if (we have not seen a grouping or decimal separator yet) { ... }
            if (!groupingDisabled && actualGroupingString == null && actualDecimalString == null) {
                if (groupingUniSet.contains(cp)) {
                    isGrouping = true;
                    actualGroupingString = UCharacter.toString(cp);
                }
            }

            // Leave if we failed to match this as a separator.
            if (!isDecimal && !isGrouping) {
                break;
            }

            // Check for conditions when we don't want to accept the separator.
            if (isDecimal && integerOnly) {
                break;
            } else if (currGroupSepType == 2 && isGrouping) {
                // Fraction grouping
                break;
            }

            // Validate intermediate grouping sizes.
            boolean prevValidSecondary = validateGroup(prevGroupSepType, prevGroupCount, false);
            boolean currValidPrimary = validateGroup(currGroupSepType, currGroupCount, true);
            if (!prevValidSecondary || (isDecimal && !currValidPrimary)) {
                // Invalid grouping sizes.
                if (isGrouping && currGroupCount == 0) {
                    // Trailing grouping separators: these are taken care of below
                    assert currGroupSepType == 1;
                } else if (requireGroupingMatch) {
                    // Strict mode: reject the parse
                    digitsConsumed = null;
                }
                break;
            } else if (requireGroupingMatch && currGroupCount == 0 && currGroupSepType == 1) {
                break;
            } else {
                // Grouping sizes OK so far.
                prevGroupOffset = currGroupOffset;
                prevGroupCount = currGroupCount;
                if (isDecimal) {
                    // Do not validate this group any more.
                    prevGroupSepType = -1;
                } else {
                    prevGroupSepType = currGroupSepType;
                }
            }

            // OK to accept the separator.
            // Special case: don't update currGroup if it is empty. This is to allow
            // adjacent grouping separators in lenient mode: "1,,234"
            if (currGroupCount != 0) {
                currGroupOffset = segment.getOffset();
            }
            currGroupSepType = isGrouping ? 1 : 2;
            currGroupCount = 0;
            if (isGrouping) {
                segment.adjustOffset(actualGroupingString.length());
            } else {
                segment.adjustOffset(actualDecimalString.length());
            }
        }

        // End of main loop.
        // Back up if there was a trailing grouping separator.
        // Shift prev -> curr so we can check it as a final group.
        if (currGroupSepType != 2 && currGroupCount == 0) {
            maybeMore = true;
            segment.setOffset(currGroupOffset);
            currGroupOffset = prevGroupOffset;
            currGroupSepType = prevGroupSepType;
            currGroupCount = prevGroupCount;
            prevGroupOffset = -1;
            prevGroupSepType = 0;
            prevGroupCount = 1;
        }

        // Validate final grouping sizes.
        boolean prevValidSecondary = validateGroup(prevGroupSepType, prevGroupCount, false);
        boolean currValidPrimary = validateGroup(currGroupSepType, currGroupCount, true);
        if (!requireGroupingMatch) {
            // The cases we need to handle here are lone digits.
            // Examples: "1,1"  "1,1,"  "1,1,1"  "1,1,1,"  ",1" (all parse as 1)
            // See more examples in numberformattestspecification.txt
            int digitsToRemove = 0;
            if (!prevValidSecondary) {
                segment.setOffset(prevGroupOffset);
                digitsToRemove += prevGroupCount;
                digitsToRemove += currGroupCount;
            } else if (!currValidPrimary && (prevGroupSepType != 0 || prevGroupCount != 0)) {
                maybeMore = true;
                segment.setOffset(currGroupOffset);
                digitsToRemove += currGroupCount;
            }
            if (digitsToRemove != 0) {
                digitsConsumed.adjustMagnitude(-digitsToRemove);
                digitsConsumed.truncate();
            }
            prevValidSecondary = true;
            currValidPrimary = true;
        }
        if (currGroupSepType != 2 && (!prevValidSecondary || !currValidPrimary)) {
            // Grouping failure.
            digitsConsumed = null;
        }

        // Strings that start with a separator but have no digits,
        // or strings that failed a grouping size check.
        if (digitsConsumed == null) {
            maybeMore = maybeMore || (segment.length() == 0);
            segment.setOffset(initialOffset);
            return maybeMore;
        }

        // We passed all inspections. Start post-processing.

        // Adjust for fraction part.
        digitsConsumed.adjustMagnitude(-digitsAfterDecimalPlace);

        // Set the digits, either normal or exponent.
        if (exponentSign != 0 && segment.getOffset() != initialOffset) {
            boolean overflow = false;
            if (digitsConsumed.fitsInLong()) {
                long exponentLong = digitsConsumed.toLong(false);
                assert exponentLong >= 0;
                if (exponentLong <= Integer.MAX_VALUE) {
                    int exponentInt = (int) exponentLong;
                    try {
                        result.quantity.adjustMagnitude(exponentSign * exponentInt);
                    } catch (ArithmeticException e) {
                        overflow = true;
                    }
                } else {
                    overflow = true;
                }
            } else {
                overflow = true;
            }
            if (overflow) {
                if (exponentSign == -1) {
                    // Set to zero
                    result.quantity.clear();
                } else {
                    // Set to infinity
                    result.quantity = null;
                    result.flags |= ParsedNumber.FLAG_INFINITY;
                }
            }
        } else {
            result.quantity = digitsConsumed;
        }

        // Set other information into the result and return.
        if (actualDecimalString != null) {
            result.flags |= ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR;
        }
        result.setCharsConsumed(segment);
        return segment.length() == 0 || maybeMore;
    }

    private boolean validateGroup(int sepType, int count, boolean isPrimary) {
        if (requireGroupingMatch) {
            if (sepType == -1) {
                // No such group (prevGroup before first shift).
                return true;
            } else if (sepType == 0) {
                // First group.
                if (isPrimary) {
                    // No grouping separators is OK.
                    return true;
                } else {
                    return count != 0 && count <= grouping2;
                }
            } else if (sepType == 1) {
                // Middle group.
                if (isPrimary) {
                    return count == grouping1;
                } else {
                    return count == grouping2;
                }
            } else {
                assert sepType == 2;
                // After the decimal separator.
                return true;
            }
        } else {
            if (sepType == 1) {
                // #11230: don't accept middle groups with only 1 digit.
                return count != 1;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        // The common case uses a static leadSet for efficiency.
        if (digitStrings == null && leadSet != null) {
            return segment.startsWith(leadSet);
        }
        if (segment.startsWith(separatorSet) || UCharacter.isDigit(segment.getCodePoint())) {
            return true;
        }
        if (digitStrings == null) {
            return false;
        }
        for (int i = 0; i < digitStrings.length; i++) {
            if (segment.startsWith(digitStrings[i])) {
                return true;
            }
        }
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
