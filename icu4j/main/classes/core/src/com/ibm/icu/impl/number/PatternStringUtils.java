// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * Assorted utilities relating to decimal formatting pattern strings.
 */
public class PatternStringUtils {

    // Note: the order of fields in this enum matters for parsing.
    public static enum PatternSignType {
        // Render using normal positive subpattern rules
        POS,
        // Render using rules to force the display of a plus sign
        POS_SIGN,
        // Render using negative subpattern rules
        NEG;

        public static final PatternSignType[] VALUES = PatternSignType.values();
    };

    /**
     * Determine whether a given roundingIncrement should be ignored for formatting
     * based on the current maxFrac value (maximum fraction digits). For example a
     * roundingIncrement of 0.01 should be ignored if maxFrac is 1, but not if maxFrac
     * is 2 or more. Note that roundingIncrements are rounded up in significance, so
     * a roundingIncrement of 0.006 is treated like 0.01 for this determination, i.e.
     * it should not be ignored if maxFrac is 2 or more (but a roundingIncrement of
     * 0.005 is treated like 0.001 for significance).
     *
     * This test is needed for both NumberPropertyMapper.oldToNew and
     * PatternStringUtils.propertiesToPatternString, but NumberPropertyMapper
     * is package-private so we have it here.
     *
     * @param roundIncrDec
     *            The roundingIncrement to be checked. Must be non-null.
     * @param maxFrac
     *            The current maximum fraction digits value.
     * @return true if roundIncr should be ignored for formatting.
     */
    public static boolean ignoreRoundingIncrement(BigDecimal roundIncrDec, int maxFrac) {
        double roundIncr = roundIncrDec.doubleValue();
        if (roundIncr == 0.0) {
            return true;
        }
        if (maxFrac < 0) {
            return false;
        }
        int frac = 0;
        roundIncr *= 2.0; // This handles the rounding up of values above e.g. 0.005 or 0.0005
        for (frac = 0; frac <= maxFrac && roundIncr <= 1.0; frac++, roundIncr *= 10.0);
        return (frac > maxFrac);
    }

    /**
     * Creates a pattern string from a property bag.
     *
     * <p>
     * Since pattern strings support only a subset of the functionality available in a property bag, a
     * new property bag created from the string returned by this function may not be the same as the
     * original property bag.
     *
     * @param properties
     *            The property bag to serialize.
     * @return A pattern string approximately serializing the property bag.
     */
    public static String propertiesToPatternString(DecimalFormatProperties properties) {
        StringBuilder sb = new StringBuilder();

        // Convenience references
        // The Math.min() calls prevent DoS
        int dosMax = 100;
        int grouping1 = Math.max(0, Math.min(properties.getGroupingSize(), dosMax));
        int grouping2 = Math.max(0, Math.min(properties.getSecondaryGroupingSize(), dosMax));
        boolean useGrouping = properties.getGroupingUsed();
        int paddingWidth = Math.min(properties.getFormatWidth(), dosMax);
        PadPosition paddingLocation = properties.getPadPosition();
        String paddingString = properties.getPadString();
        int minInt = Math.max(0, Math.min(properties.getMinimumIntegerDigits(), dosMax));
        int maxInt = Math.min(properties.getMaximumIntegerDigits(), dosMax);
        int minFrac = Math.max(0, Math.min(properties.getMinimumFractionDigits(), dosMax));
        int maxFrac = Math.min(properties.getMaximumFractionDigits(), dosMax);
        int minSig = Math.min(properties.getMinimumSignificantDigits(), dosMax);
        int maxSig = Math.min(properties.getMaximumSignificantDigits(), dosMax);
        boolean alwaysShowDecimal = properties.getDecimalSeparatorAlwaysShown();
        int exponentDigits = Math.min(properties.getMinimumExponentDigits(), dosMax);
        boolean exponentShowPlusSign = properties.getExponentSignAlwaysShown();
        AffixPatternProvider affixes = PropertiesAffixPatternProvider.forProperties(properties);

        // Prefixes
        sb.append(affixes.getString(AffixPatternProvider.FLAG_POS_PREFIX));
        int afterPrefixPos = sb.length();

        // Figure out the grouping sizes.
        if (!useGrouping) {
            grouping1 = 0;
            grouping2 = 0;
        } else if (grouping1 == grouping2) {
            grouping1 = 0;
        }
        int groupingLength = grouping1 + grouping2 + 1;

        // Figure out the digits we need to put in the pattern.
        BigDecimal roundingInterval = properties.getRoundingIncrement();
        StringBuilder digitsString = new StringBuilder();
        int digitsStringScale = 0;
        if (maxSig != Math.min(dosMax, -1)) {
            // Significant Digits.
            while (digitsString.length() < minSig) {
                digitsString.append('@');
            }
            while (digitsString.length() < maxSig) {
                digitsString.append('#');
            }
        } else if (roundingInterval != null && !ignoreRoundingIncrement(roundingInterval,maxFrac)) {
            // Rounding Interval.
            digitsStringScale = -roundingInterval.scale();
            // TODO: Check for DoS here?
            String str = roundingInterval.scaleByPowerOfTen(roundingInterval.scale()).toPlainString();
            if (str.charAt(0) == '-') {
                // TODO: Unsupported operation exception or fail silently?
                digitsString.append(str, 1, str.length());
            } else {
                digitsString.append(str);
            }
        }
        while (digitsString.length() + digitsStringScale < minInt) {
            digitsString.insert(0, '0');
        }
        while (-digitsStringScale < minFrac) {
            digitsString.append('0');
            digitsStringScale--;
        }

        // Write the digits to the string builder
        int m0 = Math.max(groupingLength, digitsString.length() + digitsStringScale);
        m0 = (maxInt != dosMax) ? Math.max(maxInt, m0) - 1 : m0 - 1;
        int mN = (maxFrac != dosMax) ? Math.min(-maxFrac, digitsStringScale) : digitsStringScale;
        for (int magnitude = m0; magnitude >= mN; magnitude--) {
            int di = digitsString.length() + digitsStringScale - magnitude - 1;
            if (di < 0 || di >= digitsString.length()) {
                sb.append('#');
            } else {
                sb.append(digitsString.charAt(di));
            }
            // Decimal separator
            if (magnitude == 0 && (alwaysShowDecimal || mN < 0)) {
                sb.append('.');
            }
            if (!useGrouping) {
                continue;
            }
            // Least-significant grouping separator
            if (magnitude > 0 && magnitude == grouping1) {
                sb.append(',');
            }
            // All other grouping separators
            if (magnitude > grouping1 && grouping2 > 0 && (magnitude - grouping1) % grouping2 == 0) {
                sb.append(',');
            }
        }

        // Exponential notation
        if (exponentDigits != Math.min(dosMax, -1)) {
            sb.append('E');
            if (exponentShowPlusSign) {
                sb.append('+');
            }
            for (int i = 0; i < exponentDigits; i++) {
                sb.append('0');
            }
        }

        // Suffixes
        int beforeSuffixPos = sb.length();
        sb.append(affixes.getString(AffixPatternProvider.FLAG_POS_SUFFIX));

        // Resolve Padding
        if (paddingWidth > 0) {
            while (paddingWidth - sb.length() > 0) {
                sb.insert(afterPrefixPos, '#');
                beforeSuffixPos++;
            }
            int addedLength;
            switch (paddingLocation) {
            case BEFORE_PREFIX:
                addedLength = PatternStringUtils.escapePaddingString(paddingString, sb, 0);
                sb.insert(0, '*');
                afterPrefixPos += addedLength + 1;
                beforeSuffixPos += addedLength + 1;
                break;
            case AFTER_PREFIX:
                addedLength = PatternStringUtils.escapePaddingString(paddingString, sb, afterPrefixPos);
                sb.insert(afterPrefixPos, '*');
                afterPrefixPos += addedLength + 1;
                beforeSuffixPos += addedLength + 1;
                break;
            case BEFORE_SUFFIX:
                PatternStringUtils.escapePaddingString(paddingString, sb, beforeSuffixPos);
                sb.insert(beforeSuffixPos, '*');
                break;
            case AFTER_SUFFIX:
                sb.append('*');
                PatternStringUtils.escapePaddingString(paddingString, sb, sb.length());
                break;
            }
        }

        // Negative affixes
        // Ignore if the negative prefix pattern is "-" and the negative suffix is empty
        if (affixes.hasNegativeSubpattern()) {
            sb.append(';');
            sb.append(affixes.getString(AffixPatternProvider.FLAG_NEG_PREFIX));
            // Copy the positive digit format into the negative.
            // This is optional; the pattern is the same as if '#' were appended here instead.
            sb.append(sb, afterPrefixPos, beforeSuffixPos);
            sb.append(affixes.getString(AffixPatternProvider.FLAG_NEG_SUFFIX));
        }

        return sb.toString();
    }

    /** @return The number of chars inserted. */
    private static int escapePaddingString(CharSequence input, StringBuilder output, int startIndex) {
        if (input == null || input.length() == 0)
            input = Padder.FALLBACK_PADDING_STRING;
        int startLength = output.length();
        if (input.length() == 1) {
            if (input.equals("'")) {
                output.insert(startIndex, "''");
            } else {
                output.insert(startIndex, input);
            }
        } else {
            output.insert(startIndex, '\'');
            int offset = 1;
            for (int i = 0; i < input.length(); i++) {
                // it's okay to deal in chars here because the quote mark is the only interesting thing.
                char ch = input.charAt(i);
                if (ch == '\'') {
                    output.insert(startIndex + offset, "''");
                    offset += 2;
                } else {
                    output.insert(startIndex + offset, ch);
                    offset += 1;
                }
            }
            output.insert(startIndex + offset, '\'');
        }
        return output.length() - startLength;
    }

    /**
     * Converts a pattern between standard notation and localized notation. Localized notation means that
     * instead of using generic placeholders in the pattern, you use the corresponding locale-specific
     * characters instead. For example, in locale <em>fr-FR</em>, the period in the pattern "0.000" means
     * "decimal" in standard notation (as it does in every other locale), but it means "grouping" in
     * localized notation.
     *
     * <p>
     * A greedy string-substitution strategy is used to substitute locale symbols. If two symbols are
     * ambiguous or have the same prefix, the result is not well-defined.
     *
     * <p>
     * Locale symbols are not allowed to contain the ASCII quote character.
     *
     * <p>
     * This method is provided for backwards compatibility and should not be used in any new code.
     *
     * @param input
     *            The pattern to convert.
     * @param symbols
     *            The symbols corresponding to the localized pattern.
     * @param toLocalized
     *            true to convert from standard to localized notation; false to convert from localized to
     *            standard notation.
     * @return The pattern expressed in the other notation.
     */
    public static String convertLocalized(
            String input,
            DecimalFormatSymbols symbols,
            boolean toLocalized) {
        if (input == null)
            return null;

        // Construct a table of strings to be converted between localized and standard.
        String[][] table = new String[21][2];
        int standIdx = toLocalized ? 0 : 1;
        int localIdx = toLocalized ? 1 : 0;
        table[0][standIdx] = "%";
        table[0][localIdx] = symbols.getPercentString();
        table[1][standIdx] = "‰";
        table[1][localIdx] = symbols.getPerMillString();
        table[2][standIdx] = ".";
        table[2][localIdx] = symbols.getDecimalSeparatorString();
        table[3][standIdx] = ",";
        table[3][localIdx] = symbols.getGroupingSeparatorString();
        table[4][standIdx] = "-";
        table[4][localIdx] = symbols.getMinusSignString();
        table[5][standIdx] = "+";
        table[5][localIdx] = symbols.getPlusSignString();
        table[6][standIdx] = ";";
        table[6][localIdx] = Character.toString(symbols.getPatternSeparator());
        table[7][standIdx] = "@";
        table[7][localIdx] = Character.toString(symbols.getSignificantDigit());
        table[8][standIdx] = "E";
        table[8][localIdx] = symbols.getExponentSeparator();
        table[9][standIdx] = "*";
        table[9][localIdx] = Character.toString(symbols.getPadEscape());
        table[10][standIdx] = "#";
        table[10][localIdx] = Character.toString(symbols.getDigit());
        for (int i = 0; i < 10; i++) {
            table[11 + i][standIdx] = Character.toString((char) ('0' + i));
            table[11 + i][localIdx] = symbols.getDigitStringsLocal()[i];
        }

        // Special case: quotes are NOT allowed to be in any localIdx strings.
        // Substitute them with '’' instead.
        for (int i = 0; i < table.length; i++) {
            table[i][localIdx] = table[i][localIdx].replace('\'', '’');
        }

        // Iterate through the string and convert.
        // State table:
        // 0 => base state
        // 1 => first char inside a quoted sequence in input and output string
        // 2 => inside a quoted sequence in input and output string
        // 3 => first char after a close quote in input string;
        // close quote still needs to be written to output string
        // 4 => base state in input string; inside quoted sequence in output string
        // 5 => first char inside a quoted sequence in input string;
        // inside quoted sequence in output string
        StringBuilder result = new StringBuilder();
        int state = 0;
        outer: for (int offset = 0; offset < input.length(); offset++) {
            char ch = input.charAt(offset);

            // Handle a quote character (state shift)
            if (ch == '\'') {
                if (state == 0) {
                    result.append('\'');
                    state = 1;
                    continue;
                } else if (state == 1) {
                    result.append('\'');
                    state = 0;
                    continue;
                } else if (state == 2) {
                    state = 3;
                    continue;
                } else if (state == 3) {
                    result.append('\'');
                    result.append('\'');
                    state = 1;
                    continue;
                } else if (state == 4) {
                    state = 5;
                    continue;
                } else {
                    assert state == 5;
                    result.append('\'');
                    result.append('\'');
                    state = 4;
                    continue;
                }
            }

            if (state == 0 || state == 3 || state == 4) {
                for (String[] pair : table) {
                    // Perform a greedy match on this symbol string
                    if (input.regionMatches(offset, pair[0], 0, pair[0].length())) {
                        // Skip ahead past this region for the next iteration
                        offset += pair[0].length() - 1;
                        if (state == 3 || state == 4) {
                            result.append('\'');
                            state = 0;
                        }
                        result.append(pair[1]);
                        continue outer;
                    }
                }
                // No replacement found. Check if a special quote is necessary
                for (String[] pair : table) {
                    if (input.regionMatches(offset, pair[1], 0, pair[1].length())) {
                        if (state == 0) {
                            result.append('\'');
                            state = 4;
                        }
                        result.append(ch);
                        continue outer;
                    }
                }
                // Still nothing. Copy the char verbatim. (Add a close quote if necessary)
                if (state == 3 || state == 4) {
                    result.append('\'');
                    state = 0;
                }
                result.append(ch);
            } else {
                assert state == 1 || state == 2 || state == 5;
                result.append(ch);
                state = 2;
            }
        }
        // Resolve final quotes
        if (state == 3 || state == 4) {
            result.append('\'');
            state = 0;
        }
        if (state != 0) {
            throw new IllegalArgumentException("Malformed localized pattern: unterminated quote");
        }
        return result.toString();
    }

    /**
     * This method contains the heart of the logic for rendering LDML affix strings. It handles
     * sign-always-shown resolution, whether to use the positive or negative subpattern, permille
     * substitution, and plural forms for CurrencyPluralInfo.
     */
    public static void patternInfoToStringBuilder(
            AffixPatternProvider patternInfo,
            boolean isPrefix,
            PatternSignType patternSignType,
            StandardPlural plural,
            boolean perMilleReplacesPercent,
            StringBuilder output) {

        boolean plusReplacesMinusSign = (patternSignType == PatternSignType.POS_SIGN)
                && !patternInfo.positiveHasPlusSign();

        // Should we use the affix from the negative subpattern?
        // (If not, we will use the positive subpattern.)
        boolean useNegativeAffixPattern = patternInfo.hasNegativeSubpattern()
                && (patternSignType == PatternSignType.NEG
                    || (patternInfo.negativeHasMinusSign() && plusReplacesMinusSign));

        // Resolve the flags for the affix pattern.
        int flags = 0;
        if (useNegativeAffixPattern) {
            flags |= AffixPatternProvider.Flags.NEGATIVE_SUBPATTERN;
        }
        if (isPrefix) {
            flags |= AffixPatternProvider.Flags.PREFIX;
        }
        if (plural != null) {
            assert plural.ordinal() == (AffixPatternProvider.Flags.PLURAL_MASK & plural.ordinal());
            flags |= plural.ordinal();
        }

        // Should we prepend a sign to the pattern?
        boolean prependSign;
        if (!isPrefix || useNegativeAffixPattern) {
            prependSign = false;
        } else if (patternSignType == PatternSignType.NEG) {
            prependSign = true;
        } else {
            prependSign = plusReplacesMinusSign;
        }

        // Compute the length of the affix pattern.
        int length = patternInfo.length(flags) + (prependSign ? 1 : 0);

        // Finally, set the result into the StringBuilder.
        output.setLength(0);
        for (int index = 0; index < length; index++) {
            char candidate;
            if (prependSign && index == 0) {
                candidate = '-';
            } else if (prependSign) {
                candidate = patternInfo.charAt(flags, index - 1);
            } else {
                candidate = patternInfo.charAt(flags, index);
            }
            if (plusReplacesMinusSign && candidate == '-') {
                candidate = '+';
            }
            if (perMilleReplacesPercent && candidate == '%') {
                candidate = '‰';
            }
            output.append(candidate);
        }
    }

    public static PatternSignType resolveSignDisplay(SignDisplay signDisplay, Signum signum) {
        switch (signDisplay) {
            case AUTO:
            case ACCOUNTING:
                switch (signum) {
                    case NEG:
                    case NEG_ZERO:
                        return PatternSignType.NEG;
                    case POS_ZERO:
                    case POS:
                        return PatternSignType.POS;
                }
                break;

            case ALWAYS:
            case ACCOUNTING_ALWAYS:
                switch (signum) {
                    case NEG:
                    case NEG_ZERO:
                        return PatternSignType.NEG;
                    case POS_ZERO:
                    case POS:
                        return PatternSignType.POS_SIGN;
                }
                break;

            case EXCEPT_ZERO:
            case ACCOUNTING_EXCEPT_ZERO:
                switch (signum) {
                    case NEG:
                        return PatternSignType.NEG;
                    case NEG_ZERO:
                    case POS_ZERO:
                        return PatternSignType.POS;
                    case POS:
                        return PatternSignType.POS_SIGN;
                }
                break;

            case NEGATIVE:
            case ACCOUNTING_NEGATIVE:
                switch (signum) {
                    case NEG:
                        return PatternSignType.NEG;
                    case NEG_ZERO:
                    case POS_ZERO:
                    case POS:
                        return PatternSignType.POS;
                }
                break;

            case NEVER:
                return PatternSignType.POS;

            default:
                break;
        }

        throw new AssertionError("Unreachable");
    }

}
