// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
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

    /** If true, do not accept fraction grouping separators */
    private final boolean fractionGroupingDisabled;

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
        } else {
            decimalUniSet = new UnicodeSet().add(decimalSeparator.codePointAt(0)).freeze();
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
        fractionGroupingDisabled = 0 != (parseFlags
                & ParsingUtils.PARSE_FLAG_FRACTION_GROUPING_DISABLED);
        integerOnly = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_INTEGER_ONLY);
        grouping1 = grouper.getPrimary();
        grouping2 = grouper.getSecondary();
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

        ParsedNumber backupResult = null;
        if (requireGroupingMatch) {
            backupResult = new ParsedNumber();
            backupResult.copyFrom(result);
        }

        // strict parsing
        boolean strictFail = false; // did we exit with a strict parse failure?
        String actualGroupingString = groupingSeparator;
        String actualDecimalString = decimalSeparator;
        int groupedDigitCount = 0; // tracking count of digits delimited by grouping separator
        int backupOffset = -1; // used for preserving the last confirmed position
        int smallGroupBackupOffset = -1; // used to back up behind groups of size 1
        boolean afterFirstGrouping = false;
        boolean seenGrouping = false;
        boolean seenDecimal = false;
        int digitsAfterDecimal = 0;
        int initialOffset = segment.getOffset();
        int exponent = 0;
        boolean hasPartialPrefix = false;
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

            if (digit >= 0) {
                // Digit was found.
                // Check for grouping size violation
                if (backupOffset != -1) {
                    smallGroupBackupOffset = backupOffset;
                    backupOffset = -1;
                    if (requireGroupingMatch) {
                        // comma followed by digit, so group before comma is a secondary
                        // group. If there was a group separator before that, the group
                        // must == the secondary group length, else it can be <= the the
                        // secondary group length.
                        if ((afterFirstGrouping && groupedDigitCount != grouping2)
                                || (!afterFirstGrouping && groupedDigitCount > grouping2)) {
                            strictFail = true;
                            break;
                        }
                    } else {
                        // #11230: don't accept groups after the first with only 1 digit.
                        // The logic to back up and remove the lone digit is lower down.
                        if (afterFirstGrouping && groupedDigitCount == 1) {
                            break;
                        }
                    }
                    afterFirstGrouping = true;
                    groupedDigitCount = 0;
                }

                // Save the digit in the DecimalQuantity or scientific adjustment.
                if (exponentSign != 0) {
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
                groupedDigitCount++;
                if (seenDecimal) {
                    digitsAfterDecimal++;
                }
                continue;
            }

            // Attempt to match a literal grouping or decimal separator
            int decimalOverlap = segment.getCommonPrefixLength(actualDecimalString);
            boolean decimalStringMatch = decimalOverlap == actualDecimalString.length();
            int groupingOverlap = segment.getCommonPrefixLength(actualGroupingString);
            boolean groupingStringMatch = groupingOverlap == actualGroupingString.length();

            hasPartialPrefix = (decimalOverlap == segment.length())
                    || (groupingOverlap == segment.length());

            if (!seenDecimal
                    && !groupingStringMatch
                    && (decimalStringMatch || (!seenDecimal && decimalUniSet.contains(cp)))) {
                // matched a decimal separator
                if (requireGroupingMatch) {
                    if (backupOffset != -1 || (seenGrouping && groupedDigitCount != grouping1)) {
                        strictFail = true;
                        break;
                    }
                }

                // If we're only parsing integers, then don't parse this one.
                if (integerOnly) {
                    break;
                }

                seenDecimal = true;
                if (!decimalStringMatch) {
                    actualDecimalString = UCharacter.toString(cp);
                }
                segment.adjustOffset(actualDecimalString.length());
                result.setCharsConsumed(segment);
                result.flags |= ParsedNumber.FLAG_HAS_DECIMAL_SEPARATOR;
                continue;
            }

            if (!groupingDisabled
                    && !decimalStringMatch
                    && (groupingStringMatch || (!seenGrouping && groupingUniSet.contains(cp)))) {
                // matched a grouping separator
                if (requireGroupingMatch) {
                    if (groupedDigitCount == 0) {
                        // leading group
                        strictFail = true;
                        break;
                    } else if (backupOffset != -1) {
                        // two group separators in a row
                        break;
                    }
                }

                if (fractionGroupingDisabled && seenDecimal) {
                    // Stop parsing here.
                    break;
                }

                seenGrouping = true;
                if (!groupingStringMatch) {
                    actualGroupingString = UCharacter.toString(cp);
                }
                backupOffset = segment.getOffset();
                segment.adjustOffset(actualGroupingString.length());
                // Note: do NOT set charsConsumed
                continue;
            }

            // Not a digit and not a separator
            break;
        }

        // Back up if there was a trailing grouping separator
        if (backupOffset != -1) {
            segment.setOffset(backupOffset);
            hasPartialPrefix = true; // redundant with `groupingOverlap == segment.length()`
        }

        // Check the final grouping for validity
        if (requireGroupingMatch
                && !seenDecimal
                && seenGrouping
                && afterFirstGrouping
                && groupedDigitCount != grouping1) {
            strictFail = true;
        }

        // #11230: don't accept groups after the first with only 1 digit.
        // Behavior in this case is to back up before that 1-digit group.
        if (!seenDecimal && afterFirstGrouping && groupedDigitCount == 1) {
            if (segment.length() == 0) {
                // Strings like "9,999" where we looked at only the first 3 chars.
                // Ask for a longer segment.
                hasPartialPrefix = true;
            }
            segment.setOffset(smallGroupBackupOffset);
            result.setCharsConsumed(segment);
            if (smallGroupBackupOffset == initialOffset) {
                // Strings like ",9"
                // Reset to no quantity seen.
                result.quantity = null;
            } else {
                // Strings like "9,9"
                // Remove the lone digit from the result quantity.
                assert result.quantity != null;
                result.quantity.adjustMagnitude(-1);
                result.quantity.truncate();
            }
        }

        if (requireGroupingMatch && strictFail) {
            result.copyFrom(backupResult);
            segment.setOffset(initialOffset);
        }

        if (result.quantity == null && segment.getOffset() != initialOffset) {
            // Strings that start with a separator but have no digits.
            // We don't need a backup of ParsedNumber because no changes could have been made to it.
            segment.setOffset(initialOffset);
            hasPartialPrefix = true;
        }

        if (result.quantity != null) {
            // The final separator was a decimal separator.
            result.quantity.adjustMagnitude(-digitsAfterDecimal);
        }

        if (exponentSign != 0 && segment.getOffset() != initialOffset) {
            boolean overflow = (exponent == Integer.MAX_VALUE);
            if (!overflow) {
                try {
                    result.quantity.adjustMagnitude(exponentSign * exponent);
                } catch (ArithmeticException e) {
                    overflow = true;
                }
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
        }

        return segment.length() == 0 || hasPartialPrefix;
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
