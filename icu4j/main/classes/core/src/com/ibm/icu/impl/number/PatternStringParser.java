// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.number.Padder.PadPosition;

/** Implements a recursive descent parser for decimal format patterns. */
public class PatternStringParser {

    public static final int IGNORE_ROUNDING_NEVER = 0;
    public static final int IGNORE_ROUNDING_IF_CURRENCY = 1;
    public static final int IGNORE_ROUNDING_ALWAYS = 2;

    /**
     * Runs the recursive descent parser on the given pattern string, returning a data structure with raw
     * information about the pattern string.
     *
     * <p>
     * To obtain a more useful form of the data, consider using {@link #parseToProperties} instead.
     *
     * @param patternString
     *            The LDML decimal format pattern (Excel-style pattern) to parse.
     * @return The results of the parse.
     */
    public static ParsedPatternInfo parseToPatternInfo(String patternString) {
        ParserState state = new ParserState(patternString);
        ParsedPatternInfo result = new ParsedPatternInfo(patternString);
        consumePattern(state, result);
        return result;
    }

    /**
     * Parses a pattern string into a new property bag.
     *
     * @param pattern
     *            The pattern string, like "#,##0.00"
     * @param ignoreRounding
     *            Whether to leave out rounding information (minFrac, maxFrac, and rounding increment)
     *            when parsing the pattern. This may be desirable if a custom rounding mode, such as
     *            CurrencyUsage, is to be used instead. One of
     *            {@link PatternStringParser#IGNORE_ROUNDING_ALWAYS},
     *            {@link PatternStringParser#IGNORE_ROUNDING_IF_CURRENCY}, or
     *            {@link PatternStringParser#IGNORE_ROUNDING_NEVER}.
     * @return A property bag object.
     * @throws IllegalArgumentException
     *             If there is a syntax error in the pattern string.
     */
    public static DecimalFormatProperties parseToProperties(String pattern, int ignoreRounding) {
        DecimalFormatProperties properties = new DecimalFormatProperties();
        parseToExistingPropertiesImpl(pattern, properties, ignoreRounding);
        return properties;
    }

    public static DecimalFormatProperties parseToProperties(String pattern) {
        return parseToProperties(pattern, PatternStringParser.IGNORE_ROUNDING_NEVER);
    }

    /**
     * Parses a pattern string into an existing property bag. All properties that can be encoded into a
     * pattern string will be overwritten with either their default value or with the value coming from
     * the pattern string. Properties that cannot be encoded into a pattern string, such as rounding
     * mode, are not modified.
     *
     * @param pattern
     *            The pattern string, like "#,##0.00"
     * @param properties
     *            The property bag object to overwrite.
     * @param ignoreRounding
     *            See {@link #parseToProperties(String pattern, int ignoreRounding)}.
     * @throws IllegalArgumentException
     *             If there was a syntax error in the pattern string.
     */
    public static void parseToExistingProperties(
            String pattern,
            DecimalFormatProperties properties,
            int ignoreRounding) {
        parseToExistingPropertiesImpl(pattern, properties, ignoreRounding);
    }

    public static void parseToExistingProperties(String pattern, DecimalFormatProperties properties) {
        parseToExistingProperties(pattern, properties, PatternStringParser.IGNORE_ROUNDING_NEVER);
    }

    /**
     * Contains raw information about the parsed decimal format pattern string.
     */
    public static class ParsedPatternInfo implements AffixPatternProvider {
        public String pattern;
        public ParsedSubpatternInfo positive;
        public ParsedSubpatternInfo negative;

        private ParsedPatternInfo(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public char charAt(int flags, int index) {
            long endpoints = getEndpoints(flags);
            int left = (int) (endpoints & 0xffffffff);
            int right = (int) (endpoints >>> 32);
            if (index < 0 || index >= right - left) {
                throw new IndexOutOfBoundsException();
            }
            return pattern.charAt(left + index);
        }

        @Override
        public int length(int flags) {
            return getLengthFromEndpoints(getEndpoints(flags));
        }

        public static int getLengthFromEndpoints(long endpoints) {
            int left = (int) (endpoints & 0xffffffff);
            int right = (int) (endpoints >>> 32);
            return right - left;
        }

        @Override
        public String getString(int flags) {
            long endpoints = getEndpoints(flags);
            int left = (int) (endpoints & 0xffffffff);
            int right = (int) (endpoints >>> 32);
            if (left == right) {
                return "";
            }
            return pattern.substring(left, right);
        }

        private long getEndpoints(int flags) {
            boolean prefix = (flags & Flags.PREFIX) != 0;
            boolean isNegative = (flags & Flags.NEGATIVE_SUBPATTERN) != 0;
            boolean padding = (flags & Flags.PADDING) != 0;
            if (isNegative && padding) {
                return negative.paddingEndpoints;
            } else if (padding) {
                return positive.paddingEndpoints;
            } else if (prefix && isNegative) {
                return negative.prefixEndpoints;
            } else if (prefix) {
                return positive.prefixEndpoints;
            } else if (isNegative) {
                return negative.suffixEndpoints;
            } else {
                return positive.suffixEndpoints;
            }
        }

        @Override
        public boolean positiveHasPlusSign() {
            return positive.hasPlusSign;
        }

        @Override
        public boolean hasNegativeSubpattern() {
            return negative != null;
        }

        @Override
        public boolean negativeHasMinusSign() {
            return negative.hasMinusSign;
        }

        @Override
        public boolean hasCurrencySign() {
            return positive.hasCurrencySign || (negative != null && negative.hasCurrencySign);
        }

        @Override
        public boolean containsSymbolType(int type) {
            return AffixUtils.containsType(pattern, type);
        }

        @Override
        public boolean hasBody() {
            return positive.integerTotal > 0;
        }
    }

    public static class ParsedSubpatternInfo {
        public long groupingSizes = 0x0000ffffffff0000L;
        public int integerLeadingHashSigns = 0;
        public int integerTrailingHashSigns = 0;
        public int integerNumerals = 0;
        public int integerAtSigns = 0;
        public int integerTotal = 0; // for convenience
        public int fractionNumerals = 0;
        public int fractionHashSigns = 0;
        public int fractionTotal = 0; // for convenience
        public boolean hasDecimal = false;
        public int widthExceptAffixes = 0;
        public PadPosition paddingLocation = null;
        public DecimalQuantity_DualStorageBCD rounding = null;
        public boolean exponentHasPlusSign = false;
        public int exponentZeros = 0;
        public boolean hasPercentSign = false;
        public boolean hasPerMilleSign = false;
        public boolean hasCurrencySign = false;
        public boolean hasMinusSign = false;
        public boolean hasPlusSign = false;

        public long prefixEndpoints = 0;
        public long suffixEndpoints = 0;
        public long paddingEndpoints = 0;
    }

    /////////////////////////////////////////////////////
    /// BEGIN RECURSIVE DESCENT PARSER IMPLEMENTATION ///
    /////////////////////////////////////////////////////

    /** An internal class used for tracking the cursor during parsing of a pattern string. */
    private static class ParserState {
        final String pattern;
        int offset;

        ParserState(String pattern) {
            this.pattern = pattern;
            this.offset = 0;
        }

        int peek() {
            if (offset == pattern.length()) {
                return -1;
            } else {
                return pattern.codePointAt(offset);
            }
        }

        int next() {
            int codePoint = peek();
            offset += Character.charCount(codePoint);
            return codePoint;
        }

        IllegalArgumentException toParseException(String message) {
            StringBuilder sb = new StringBuilder();
            sb.append("Malformed pattern for ICU DecimalFormat: \"");
            sb.append(pattern);
            sb.append("\": ");
            sb.append(message);
            sb.append(" at position ");
            sb.append(offset);
            return new IllegalArgumentException(sb.toString());
        }
    }

    private static void consumePattern(ParserState state, ParsedPatternInfo result) {
        // pattern := subpattern (';' subpattern)?
        result.positive = new ParsedSubpatternInfo();
        consumeSubpattern(state, result.positive);
        if (state.peek() == ';') {
            state.next(); // consume the ';'
            // Don't consume the negative subpattern if it is empty (trailing ';')
            if (state.peek() != -1) {
                result.negative = new ParsedSubpatternInfo();
                consumeSubpattern(state, result.negative);
            }
        }
        if (state.peek() != -1) {
            throw state.toParseException("Found unquoted special character");
        }
    }

    private static void consumeSubpattern(ParserState state, ParsedSubpatternInfo result) {
        // subpattern := literals? number exponent? literals?
        consumePadding(state, result, PadPosition.BEFORE_PREFIX);
        result.prefixEndpoints = consumeAffix(state, result);
        consumePadding(state, result, PadPosition.AFTER_PREFIX);
        consumeFormat(state, result);
        consumeExponent(state, result);
        consumePadding(state, result, PadPosition.BEFORE_SUFFIX);
        result.suffixEndpoints = consumeAffix(state, result);
        consumePadding(state, result, PadPosition.AFTER_SUFFIX);
    }

    private static void consumePadding(
            ParserState state,
            ParsedSubpatternInfo result,
            PadPosition paddingLocation) {
        if (state.peek() != '*') {
            return;
        }
        if (result.paddingLocation != null) {
            throw state.toParseException("Cannot have multiple pad specifiers");
        }
        result.paddingLocation = paddingLocation;
        state.next(); // consume the '*'
        result.paddingEndpoints |= state.offset;
        consumeLiteral(state);
        result.paddingEndpoints |= ((long) state.offset) << 32;
    }

    private static long consumeAffix(ParserState state, ParsedSubpatternInfo result) {
        // literals := { literal }
        long endpoints = state.offset;
        outer: while (true) {
            switch (state.peek()) {
            case '#':
            case '@':
            case ';':
            case '*':
            case '.':
            case ',':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case -1:
                // Characters that cannot appear unquoted in a literal
                break outer;

            case '%':
                result.hasPercentSign = true;
                break;

            case '‰':
                result.hasPerMilleSign = true;
                break;

            case '¤':
                result.hasCurrencySign = true;
                break;

            case '-':
                result.hasMinusSign = true;
                break;

            case '+':
                result.hasPlusSign = true;
                break;
            }
            consumeLiteral(state);
        }
        endpoints |= ((long) state.offset) << 32;
        return endpoints;
    }

    private static void consumeLiteral(ParserState state) {
        if (state.peek() == -1) {
            throw state.toParseException("Expected unquoted literal but found EOL");
        } else if (state.peek() == '\'') {
            state.next(); // consume the starting quote
            while (state.peek() != '\'') {
                if (state.peek() == -1) {
                    throw state.toParseException("Expected quoted literal but found EOL");
                } else {
                    state.next(); // consume a quoted character
                }
            }
            state.next(); // consume the ending quote
        } else {
            // consume a non-quoted literal character
            state.next();
        }
    }

    private static void consumeFormat(ParserState state, ParsedSubpatternInfo result) {
        consumeIntegerFormat(state, result);
        if (state.peek() == '.') {
            state.next(); // consume the decimal point
            result.hasDecimal = true;
            result.widthExceptAffixes += 1;
            consumeFractionFormat(state, result);
        }
    }

    private static void consumeIntegerFormat(ParserState state, ParsedSubpatternInfo result) {
        outer: while (true) {
            switch (state.peek()) {
            case ',':
                result.widthExceptAffixes += 1;
                result.groupingSizes <<= 16;
                break;

            case '#':
                if (result.integerNumerals > 0) {
                    throw state.toParseException("# cannot follow 0 before decimal point");
                }
                result.widthExceptAffixes += 1;
                result.groupingSizes += 1;
                if (result.integerAtSigns > 0) {
                    result.integerTrailingHashSigns += 1;
                } else {
                    result.integerLeadingHashSigns += 1;
                }
                result.integerTotal += 1;
                break;

            case '@':
                if (result.integerNumerals > 0) {
                    throw state.toParseException("Cannot mix 0 and @");
                }
                if (result.integerTrailingHashSigns > 0) {
                    throw state.toParseException("Cannot nest # inside of a run of @");
                }
                result.widthExceptAffixes += 1;
                result.groupingSizes += 1;
                result.integerAtSigns += 1;
                result.integerTotal += 1;
                break;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (result.integerAtSigns > 0) {
                    throw state.toParseException("Cannot mix @ and 0");
                }
                result.widthExceptAffixes += 1;
                result.groupingSizes += 1;
                result.integerNumerals += 1;
                result.integerTotal += 1;
                if (state.peek() != '0' && result.rounding == null) {
                    result.rounding = new DecimalQuantity_DualStorageBCD();
                }
                if (result.rounding != null) {
                    result.rounding.appendDigit((byte) (state.peek() - '0'), 0, true);
                }
                break;

            default:
                break outer;
            }
            state.next(); // consume the symbol
        }

        // Disallow patterns with a trailing ',' or with two ',' next to each other
        short grouping1 = (short) (result.groupingSizes & 0xffff);
        short grouping2 = (short) ((result.groupingSizes >>> 16) & 0xffff);
        short grouping3 = (short) ((result.groupingSizes >>> 32) & 0xffff);
        if (grouping1 == 0 && grouping2 != -1) {
            throw state.toParseException("Trailing grouping separator is invalid");
        }
        if (grouping2 == 0 && grouping3 != -1) {
            throw state.toParseException("Grouping width of zero is invalid");
        }
    }

    private static void consumeFractionFormat(ParserState state, ParsedSubpatternInfo result) {
        int zeroCounter = 0;
        while (true) {
            switch (state.peek()) {
            case '#':
                result.widthExceptAffixes += 1;
                result.fractionHashSigns += 1;
                result.fractionTotal += 1;
                zeroCounter++;
                break;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (result.fractionHashSigns > 0) {
                    throw state.toParseException("0 cannot follow # after decimal point");
                }
                result.widthExceptAffixes += 1;
                result.fractionNumerals += 1;
                result.fractionTotal += 1;
                if (state.peek() == '0') {
                    zeroCounter++;
                } else {
                    if (result.rounding == null) {
                        result.rounding = new DecimalQuantity_DualStorageBCD();
                    }
                    result.rounding.appendDigit((byte) (state.peek() - '0'), zeroCounter, false);
                    zeroCounter = 0;
                }
                break;

            default:
                return;
            }
            state.next(); // consume the symbol
        }
    }

    private static void consumeExponent(ParserState state, ParsedSubpatternInfo result) {
        if (state.peek() != 'E') {
            return;
        }
        if ((result.groupingSizes & 0xffff0000L) != 0xffff0000L) {
            throw state.toParseException("Cannot have grouping separator in scientific notation");
        }
        state.next(); // consume the E
        result.widthExceptAffixes++;
        if (state.peek() == '+') {
            state.next(); // consume the +
            result.exponentHasPlusSign = true;
            result.widthExceptAffixes++;
        }
        while (state.peek() == '0') {
            state.next(); // consume the 0
            result.exponentZeros += 1;
            result.widthExceptAffixes++;
        }
    }

    ///////////////////////////////////////////////////
    /// END RECURSIVE DESCENT PARSER IMPLEMENTATION ///
    ///////////////////////////////////////////////////

    private static void parseToExistingPropertiesImpl(
            String pattern,
            DecimalFormatProperties properties,
            int ignoreRounding) {
        if (pattern == null || pattern.length() == 0) {
            // Backwards compatibility requires that we reset to the default values.
            // TODO: Only overwrite the properties that "saveToProperties" normally touches?
            properties.clear();
            return;
        }

        // TODO: Use thread locals here?
        ParsedPatternInfo patternInfo = parseToPatternInfo(pattern);
        patternInfoToProperties(properties, patternInfo, ignoreRounding);
    }

    /** Finalizes the temporary data stored in the ParsedPatternInfo to the Properties. */
    private static void patternInfoToProperties(
            DecimalFormatProperties properties,
            ParsedPatternInfo patternInfo,
            int _ignoreRounding) {
        // Translate from PatternParseResult to Properties.
        // Note that most data from "negative" is ignored per the specification of DecimalFormat.

        ParsedSubpatternInfo positive = patternInfo.positive;

        boolean ignoreRounding;
        if (_ignoreRounding == PatternStringParser.IGNORE_ROUNDING_NEVER) {
            ignoreRounding = false;
        } else if (_ignoreRounding == PatternStringParser.IGNORE_ROUNDING_IF_CURRENCY) {
            ignoreRounding = positive.hasCurrencySign;
        } else {
            assert _ignoreRounding == PatternStringParser.IGNORE_ROUNDING_ALWAYS;
            ignoreRounding = true;
        }

        // Grouping settings
        short grouping1 = (short) (positive.groupingSizes & 0xffff);
        short grouping2 = (short) ((positive.groupingSizes >>> 16) & 0xffff);
        short grouping3 = (short) ((positive.groupingSizes >>> 32) & 0xffff);
        if (grouping2 != -1) {
            properties.setGroupingSize(grouping1);
            properties.setGroupingUsed(true);
        } else {
            properties.setGroupingSize(-1);
            properties.setGroupingUsed(false);
        }
        if (grouping3 != -1) {
            properties.setSecondaryGroupingSize(grouping2);
        } else {
            properties.setSecondaryGroupingSize(-1);
        }

        // For backwards compatibility, require that the pattern emit at least one min digit.
        int minInt, minFrac;
        if (positive.integerTotal == 0 && positive.fractionTotal > 0) {
            // patterns like ".##"
            minInt = 0;
            minFrac = Math.max(1, positive.fractionNumerals);
        } else if (positive.integerNumerals == 0 && positive.fractionNumerals == 0) {
            // patterns like "#.##"
            minInt = 1;
            minFrac = 0;
        } else {
            minInt = positive.integerNumerals;
            minFrac = positive.fractionNumerals;
        }

        // Rounding settings
        // Don't set basic rounding when there is a currency sign; defer to CurrencyUsage
        if (positive.integerAtSigns > 0) {
            properties.setMinimumFractionDigits(-1);
            properties.setMaximumFractionDigits(-1);
            properties.setRoundingIncrement(null);
            properties.setMinimumSignificantDigits(positive.integerAtSigns);
            properties.setMaximumSignificantDigits(
                    positive.integerAtSigns + positive.integerTrailingHashSigns);
        } else if (positive.rounding != null) {
            if (!ignoreRounding) {
                properties.setMinimumFractionDigits(minFrac);
                properties.setMaximumFractionDigits(positive.fractionTotal);
                properties.setRoundingIncrement(
                        positive.rounding.toBigDecimal().setScale(positive.fractionNumerals));
            } else {
                properties.setMinimumFractionDigits(-1);
                properties.setMaximumFractionDigits(-1);
                properties.setRoundingIncrement(null);
            }
            properties.setMinimumSignificantDigits(-1);
            properties.setMaximumSignificantDigits(-1);
        } else {
            if (!ignoreRounding) {
                properties.setMinimumFractionDigits(minFrac);
                properties.setMaximumFractionDigits(positive.fractionTotal);
                properties.setRoundingIncrement(null);
            } else {
                properties.setMinimumFractionDigits(-1);
                properties.setMaximumFractionDigits(-1);
                properties.setRoundingIncrement(null);
            }
            properties.setMinimumSignificantDigits(-1);
            properties.setMaximumSignificantDigits(-1);
        }

        // If the pattern ends with a '.' then force the decimal point.
        if (positive.hasDecimal && positive.fractionTotal == 0) {
            properties.setDecimalSeparatorAlwaysShown(true);
        } else {
            properties.setDecimalSeparatorAlwaysShown(false);
        }

        // Scientific notation settings
        if (positive.exponentZeros > 0) {
            properties.setExponentSignAlwaysShown(positive.exponentHasPlusSign);
            properties.setMinimumExponentDigits(positive.exponentZeros);
            if (positive.integerAtSigns == 0) {
                // patterns without '@' can define max integer digits, used for engineering notation
                properties.setMinimumIntegerDigits(positive.integerNumerals);
                properties.setMaximumIntegerDigits(positive.integerTotal);
            } else {
                // patterns with '@' cannot define max integer digits
                properties.setMinimumIntegerDigits(1);
                properties.setMaximumIntegerDigits(-1);
            }
        } else {
            properties.setExponentSignAlwaysShown(false);
            properties.setMinimumExponentDigits(-1);
            properties.setMinimumIntegerDigits(minInt);
            properties.setMaximumIntegerDigits(-1);
        }

        // Compute the affix patterns (required for both padding and affixes)
        String posPrefix = patternInfo.getString(AffixPatternProvider.Flags.PREFIX);
        String posSuffix = patternInfo.getString(0);

        // Padding settings
        if (positive.paddingLocation != null) {
            // The width of the positive prefix and suffix templates are included in the padding
            int paddingWidth = positive.widthExceptAffixes
                    + AffixUtils.estimateLength(posPrefix)
                    + AffixUtils.estimateLength(posSuffix);
            properties.setFormatWidth(paddingWidth);
            String rawPaddingString = patternInfo.getString(AffixPatternProvider.Flags.PADDING);
            if (rawPaddingString.length() == 1) {
                properties.setPadString(rawPaddingString);
            } else if (rawPaddingString.length() == 2) {
                if (rawPaddingString.charAt(0) == '\'') {
                    properties.setPadString("'");
                } else {
                    properties.setPadString(rawPaddingString);
                }
            } else {
                properties.setPadString(rawPaddingString.substring(1, rawPaddingString.length() - 1));
            }
            assert positive.paddingLocation != null;
            properties.setPadPosition(positive.paddingLocation);
        } else {
            properties.setFormatWidth(-1);
            properties.setPadString(null);
            properties.setPadPosition(null);
        }

        // Set the affixes
        // Always call the setter, even if the prefixes are empty, especially in the case of the
        // negative prefix pattern, to prevent default values from overriding the pattern.
        properties.setPositivePrefixPattern(posPrefix);
        properties.setPositiveSuffixPattern(posSuffix);
        if (patternInfo.negative != null) {
            properties.setNegativePrefixPattern(patternInfo.getString(
                    AffixPatternProvider.Flags.NEGATIVE_SUBPATTERN | AffixPatternProvider.Flags.PREFIX));
            properties.setNegativeSuffixPattern(
                    patternInfo.getString(AffixPatternProvider.Flags.NEGATIVE_SUBPATTERN));
        } else {
            properties.setNegativePrefixPattern(null);
            properties.setNegativeSuffixPattern(null);
        }

        // Set the magnitude multiplier
        if (positive.hasPercentSign) {
            properties.setMagnitudeMultiplier(2);
        } else if (positive.hasPerMilleSign) {
            properties.setMagnitudeMultiplier(3);
        } else {
            properties.setMagnitudeMultiplier(0);
        }
    }
}
