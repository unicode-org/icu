// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import newapi.impl.AffixPatternProvider;
import newapi.impl.Padder.PadPosition;

/** Implements a recursive descent parser for decimal format patterns. */
public class PatternParser {

    /**
     * Runs the recursive descent parser on the given pattern string, returning a data structure with raw information
     * about the pattern string.
     *
     * <p>
     * To obtain a more useful form of the data, consider using {@link PatternAndPropertyUtils#parse} instead.
     *
     * @param patternString
     *            The LDML decimal format pattern (Excel-style pattern) to parse.
     * @return The results of the parse.
     */
    public static ParsedPatternInfo parse(String patternString) {
        ParserState state = new ParserState(patternString);
        ParsedPatternInfo result = new ParsedPatternInfo(patternString);
        consumePattern(state, result);
        return result;
    }

    /**
     * Contains information about
     * @author sffc
     *
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
            return AffixPatternUtils.containsType(pattern, type);
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
        public FormatQuantity4 rounding = null;
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

    private static void consumePadding(ParserState state, ParsedSubpatternInfo result, PadPosition paddingLocation) {
        if (state.peek() != '*') {
            return;
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
                    result.rounding = new FormatQuantity4();
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
                        result.rounding = new FormatQuantity4();
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
}
