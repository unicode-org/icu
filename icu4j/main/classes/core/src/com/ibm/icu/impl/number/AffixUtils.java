// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

/**
 * Performs manipulations on affix patterns: the prefix and suffix strings associated with a decimal
 * format pattern. For example:
 *
 * <table>
 * <tr>
 * <th>Affix Pattern</th>
 * <th>Example Unescaped (Formatted) String</th>
 * </tr>
 * <tr>
 * <td>abc</td>
 * <td>abc</td>
 * </tr>
 * <tr>
 * <td>ab-</td>
 * <td>ab−</td>
 * </tr>
 * <tr>
 * <td>ab'-'</td>
 * <td>ab-</td>
 * </tr>
 * <tr>
 * <td>ab''</td>
 * <td>ab'</td>
 * </tr>
 * </table>
 *
 * To manually iterate over tokens in a literal string, use the following pattern, which is designed to
 * be efficient.
 *
 * <pre>
 * long tag = 0L;
 * while (AffixPatternUtils.hasNext(tag, patternString)) {
 *     tag = AffixPatternUtils.nextToken(tag, patternString);
 *     int typeOrCp = AffixPatternUtils.getTypeOrCp(tag);
 *     switch (typeOrCp) {
 *     case AffixPatternUtils.TYPE_MINUS_SIGN:
 *         // Current token is a minus sign.
 *         break;
 *     case AffixPatternUtils.TYPE_PLUS_SIGN:
 *         // Current token is a plus sign.
 *         break;
 *     case AffixPatternUtils.TYPE_PERCENT:
 *         // Current token is a percent sign.
 *         break;
 *     // ... other types ...
 *     default:
 *         // Current token is an arbitrary code point.
 *         // The variable typeOrCp is the code point.
 *         break;
 *     }
 * }
 * </pre>
 */
public class AffixUtils {

    private static final int STATE_BASE = 0;
    private static final int STATE_FIRST_QUOTE = 1;
    private static final int STATE_INSIDE_QUOTE = 2;
    private static final int STATE_AFTER_QUOTE = 3;
    private static final int STATE_FIRST_CURR = 4;
    private static final int STATE_SECOND_CURR = 5;
    private static final int STATE_THIRD_CURR = 6;
    private static final int STATE_FOURTH_CURR = 7;
    private static final int STATE_FIFTH_CURR = 8;
    private static final int STATE_OVERFLOW_CURR = 9;

    /** Represents a literal character; the value is stored in the code point field. */
    private static final int TYPE_CODEPOINT = 0;

    /** Represents a minus sign symbol '-'. */
    public static final int TYPE_MINUS_SIGN = -1;

    /** Represents a plus sign symbol '+'. */
    public static final int TYPE_PLUS_SIGN = -2;

    /** Represents a percent sign symbol '%'. */
    public static final int TYPE_PERCENT = -3;

    /** Represents a permille sign symbol '‰'. */
    public static final int TYPE_PERMILLE = -4;

    /** Represents a single currency symbol '¤'. */
    public static final int TYPE_CURRENCY_SINGLE = -5;

    /** Represents a double currency symbol '¤¤'. */
    public static final int TYPE_CURRENCY_DOUBLE = -6;

    /** Represents a triple currency symbol '¤¤¤'. */
    public static final int TYPE_CURRENCY_TRIPLE = -7;

    /** Represents a quadruple currency symbol '¤¤¤¤'. */
    public static final int TYPE_CURRENCY_QUAD = -8;

    /** Represents a quintuple currency symbol '¤¤¤¤¤'. */
    public static final int TYPE_CURRENCY_QUINT = -9;

    /** Represents a sequence of six or more currency symbols. */
    public static final int TYPE_CURRENCY_OVERFLOW = -15;

    public static interface SymbolProvider {
        public CharSequence getSymbol(int type);
    }

    public static interface TokenConsumer {
        public void consumeToken(int typeOrCp);
    }

    /**
     * Estimates the number of code points present in an unescaped version of the affix pattern string
     * (one that would be returned by {@link #unescape}), assuming that all interpolated symbols consume
     * one code point and that currencies consume as many code points as their symbol width. Used for
     * computing padding width.
     *
     * @param patternString
     *            The original string whose width will be estimated.
     * @return The length of the unescaped string.
     */
    public static int estimateLength(CharSequence patternString) {
        if (patternString == null)
            return 0;
        int state = STATE_BASE;
        int offset = 0;
        int length = 0;
        for (; offset < patternString.length();) {
            int cp = Character.codePointAt(patternString, offset);

            switch (state) {
            case STATE_BASE:
                if (cp == '\'') {
                    // First quote
                    state = STATE_FIRST_QUOTE;
                } else {
                    // Unquoted symbol
                    length++;
                }
                break;
            case STATE_FIRST_QUOTE:
                if (cp == '\'') {
                    // Repeated quote
                    length++;
                    state = STATE_BASE;
                } else {
                    // Quoted code point
                    length++;
                    state = STATE_INSIDE_QUOTE;
                }
                break;
            case STATE_INSIDE_QUOTE:
                if (cp == '\'') {
                    // End of quoted sequence
                    state = STATE_AFTER_QUOTE;
                } else {
                    // Quoted code point
                    length++;
                }
                break;
            case STATE_AFTER_QUOTE:
                if (cp == '\'') {
                    // Double quote inside of quoted sequence
                    length++;
                    state = STATE_INSIDE_QUOTE;
                } else {
                    // Unquoted symbol
                    length++;
                }
                break;
            default:
                throw new AssertionError();
            }

            offset += Character.charCount(cp);
        }

        switch (state) {
        case STATE_FIRST_QUOTE:
        case STATE_INSIDE_QUOTE:
            throw new IllegalArgumentException("Unterminated quote: \"" + patternString + "\"");
        default:
            break;
        }

        return length;
    }

    /**
     * Takes a string and escapes (quotes) characters that have special meaning in the affix pattern
     * syntax. This function does not reverse-lookup symbols.
     *
     * <p>
     * Example input: "-$x"; example output: "'-'$x"
     *
     * @param input
     *            The string to be escaped.
     * @param output
     *            The string builder to which to append the escaped string.
     * @return The number of chars (UTF-16 code units) appended to the output.
     */
    public static int escape(CharSequence input, StringBuilder output) {
        if (input == null)
            return 0;
        int state = STATE_BASE;
        int offset = 0;
        int startLength = output.length();
        for (; offset < input.length();) {
            int cp = Character.codePointAt(input, offset);

            switch (cp) {
            case '\'':
                output.append("''");
                break;

            case '-':
            case '+':
            case '%':
            case '‰':
            case '¤':
                if (state == STATE_BASE) {
                    output.append('\'');
                    output.appendCodePoint(cp);
                    state = STATE_INSIDE_QUOTE;
                } else {
                    output.appendCodePoint(cp);
                }
                break;

            default:
                if (state == STATE_INSIDE_QUOTE) {
                    output.append('\'');
                    output.appendCodePoint(cp);
                    state = STATE_BASE;
                } else {
                    output.appendCodePoint(cp);
                }
                break;
            }
            offset += Character.charCount(cp);
        }

        if (state == STATE_INSIDE_QUOTE) {
            output.append('\'');
        }

        return output.length() - startLength;
    }

    /** Version of {@link #escape} that returns a String, or null if input is null. */
    public static String escape(CharSequence input) {
        if (input == null)
            return null;
        StringBuilder sb = new StringBuilder();
        escape(input, sb);
        return sb.toString();
    }

    public static final NumberFormat.Field getFieldForType(int type) {
        switch (type) {
        case TYPE_MINUS_SIGN:
            return NumberFormat.Field.SIGN;
        case TYPE_PLUS_SIGN:
            return NumberFormat.Field.SIGN;
        case TYPE_PERCENT:
            return NumberFormat.Field.PERCENT;
        case TYPE_PERMILLE:
            return NumberFormat.Field.PERMILLE;
        case TYPE_CURRENCY_SINGLE:
            return NumberFormat.Field.CURRENCY;
        case TYPE_CURRENCY_DOUBLE:
            return NumberFormat.Field.CURRENCY;
        case TYPE_CURRENCY_TRIPLE:
            return NumberFormat.Field.CURRENCY;
        case TYPE_CURRENCY_QUAD:
            return NumberFormat.Field.CURRENCY;
        case TYPE_CURRENCY_QUINT:
            return NumberFormat.Field.CURRENCY;
        case TYPE_CURRENCY_OVERFLOW:
            return NumberFormat.Field.CURRENCY;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Executes the unescape state machine. Replaces the unquoted characters "-", "+", "%", "‰", and "¤"
     * with the corresponding symbols provided by the {@link SymbolProvider}, and inserts the result into
     * the FormattedStringBuilder at the requested location.
     *
     * <p>
     * Example input: "'-'¤x"; example output: "-$x"
     *
     * @param affixPattern
     *            The original string to be unescaped.
     * @param output
     *            The FormattedStringBuilder to mutate with the result.
     * @param position
     *            The index into the FormattedStringBuilder to insert the the string.
     * @param provider
     *            An object to generate locale symbols.
     * @return The length of the string added to affixPattern.
     */
    public static int unescape(
            CharSequence affixPattern,
            FormattedStringBuilder output,
            int position,
            SymbolProvider provider,
            NumberFormat.Field field) {
        assert affixPattern != null;
        int length = 0;
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            int typeOrCp = getTypeOrCp(tag);
            if (typeOrCp == TYPE_CURRENCY_OVERFLOW) {
                // Don't go to the provider for this special case
                length += output.insertCodePoint(position + length, 0xFFFD, NumberFormat.Field.CURRENCY);
            } else if (typeOrCp < 0) {
                length += output.insert(position + length,
                        provider.getSymbol(typeOrCp),
                        getFieldForType(typeOrCp));
            } else {
                length += output.insertCodePoint(position + length, typeOrCp, field);
            }
        }
        return length;
    }

    /**
     * Sames as {@link #unescape}, but only calculates the length or code point count. More efficient
     * than {@link #unescape} if you only need the length but not the string itself.
     *
     * @param affixPattern
     *            The original string to be unescaped.
     * @param lengthOrCount
     *            true to count length (UTF-16 code units); false to count code points
     * @param provider
     *            An object to generate locale symbols.
     * @return The number of code points in the unescaped string.
     */
    public static int unescapedCount(
            CharSequence affixPattern,
            boolean lengthOrCount,
            SymbolProvider provider) {
        int length = 0;
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            int typeOrCp = getTypeOrCp(tag);
            if (typeOrCp == TYPE_CURRENCY_OVERFLOW) {
                // U+FFFD is one char
                length += 1;
            } else if (typeOrCp < 0) {
                CharSequence symbol = provider.getSymbol(typeOrCp);
                length += lengthOrCount ? symbol.length()
                        : Character.codePointCount(symbol, 0, symbol.length());
            } else {
                length += lengthOrCount ? Character.charCount(typeOrCp) : 1;
            }
        }
        return length;
    }

    /**
     * Checks whether the given affix pattern contains at least one token of the given type, which is one
     * of the constants "TYPE_" in {@link AffixUtils}.
     *
     * @param affixPattern
     *            The affix pattern to check.
     * @param type
     *            The token type.
     * @return true if the affix pattern contains the given token type; false otherwise.
     */
    public static boolean containsType(CharSequence affixPattern, int type) {
        if (affixPattern == null || affixPattern.length() == 0) {
            return false;
        }
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            if (getTypeOrCp(tag) == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified affix pattern has any unquoted currency symbols ("¤").
     *
     * @param affixPattern
     *            The string to check for currency symbols.
     * @return true if the literal has at least one unquoted currency symbol; false otherwise.
     */
    public static boolean hasCurrencySymbols(CharSequence affixPattern) {
        if (affixPattern == null || affixPattern.length() == 0)
            return false;
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            int typeOrCp = getTypeOrCp(tag);
            if (typeOrCp < 0 && getFieldForType(typeOrCp) == NumberFormat.Field.CURRENCY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces all occurrences of tokens with the given type with the given replacement char.
     *
     * @param affixPattern
     *            The source affix pattern (does not get modified).
     * @param type
     *            The token type.
     * @param replacementChar
     *            The char to substitute in place of chars of the given token type.
     * @return A string containing the new affix pattern.
     */
    public static String replaceType(CharSequence affixPattern, int type, char replacementChar) {
        if (affixPattern == null || affixPattern.length() == 0)
            return "";
        char[] chars = affixPattern.toString().toCharArray();
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            if (getTypeOrCp(tag) == type) {
                int offset = getOffset(tag);
                chars[offset - 1] = replacementChar;
            }
        }
        return new String(chars);
    }

    /**
     * Returns whether the given affix pattern contains only symbols and ignorables as defined by the
     * given ignorables set.
     */
    public static boolean containsOnlySymbolsAndIgnorables(
            CharSequence affixPattern,
            UnicodeSet ignorables) {
        if (affixPattern == null) {
            return true;
        }
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            int typeOrCp = getTypeOrCp(tag);
            if (typeOrCp >= 0 && !ignorables.contains(typeOrCp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Iterates over the affix pattern, calling the TokenConsumer for each token.
     */
    public static void iterateWithConsumer(CharSequence affixPattern, TokenConsumer consumer) {
        assert affixPattern != null;
        long tag = 0L;
        while (hasNext(tag, affixPattern)) {
            tag = nextToken(tag, affixPattern);
            int typeOrCp = getTypeOrCp(tag);
            consumer.consumeToken(typeOrCp);
        }
    }

    /**
     * Returns the next token from the affix pattern.
     *
     * @param tag
     *            A bitmask used for keeping track of state from token to token. The initial value should
     *            be 0L.
     * @param patternString
     *            The affix pattern.
     * @return The bitmask tag to pass to the next call of this method to retrieve the following token
     *         (never negative), or -1 if there were no more tokens in the affix pattern.
     * @see #hasNext
     */
    private static long nextToken(long tag, CharSequence patternString) {
        int offset = getOffset(tag);
        int state = getState(tag);
        for (; offset < patternString.length();) {
            int cp = Character.codePointAt(patternString, offset);
            int count = Character.charCount(cp);

            switch (state) {
            case STATE_BASE:
                switch (cp) {
                case '\'':
                    state = STATE_FIRST_QUOTE;
                    offset += count;
                    // continue to the next code point
                    break;
                case '-':
                    return makeTag(offset + count, TYPE_MINUS_SIGN, STATE_BASE, 0);
                case '+':
                    return makeTag(offset + count, TYPE_PLUS_SIGN, STATE_BASE, 0);
                case '%':
                    return makeTag(offset + count, TYPE_PERCENT, STATE_BASE, 0);
                case '‰':
                    return makeTag(offset + count, TYPE_PERMILLE, STATE_BASE, 0);
                case '¤':
                    state = STATE_FIRST_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                default:
                    return makeTag(offset + count, TYPE_CODEPOINT, STATE_BASE, cp);
                }
                break;
            case STATE_FIRST_QUOTE:
                if (cp == '\'') {
                    return makeTag(offset + count, TYPE_CODEPOINT, STATE_BASE, cp);
                } else {
                    return makeTag(offset + count, TYPE_CODEPOINT, STATE_INSIDE_QUOTE, cp);
                }
            case STATE_INSIDE_QUOTE:
                if (cp == '\'') {
                    state = STATE_AFTER_QUOTE;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset + count, TYPE_CODEPOINT, STATE_INSIDE_QUOTE, cp);
                }
            case STATE_AFTER_QUOTE:
                if (cp == '\'') {
                    return makeTag(offset + count, TYPE_CODEPOINT, STATE_INSIDE_QUOTE, cp);
                } else {
                    state = STATE_BASE;
                    // re-evaluate this code point
                    break;
                }
            case STATE_FIRST_CURR:
                if (cp == '¤') {
                    state = STATE_SECOND_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_SINGLE, STATE_BASE, 0);
                }
            case STATE_SECOND_CURR:
                if (cp == '¤') {
                    state = STATE_THIRD_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_DOUBLE, STATE_BASE, 0);
                }
            case STATE_THIRD_CURR:
                if (cp == '¤') {
                    state = STATE_FOURTH_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_TRIPLE, STATE_BASE, 0);
                }
            case STATE_FOURTH_CURR:
                if (cp == '¤') {
                    state = STATE_FIFTH_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_QUAD, STATE_BASE, 0);
                }
            case STATE_FIFTH_CURR:
                if (cp == '¤') {
                    state = STATE_OVERFLOW_CURR;
                    offset += count;
                    // continue to the next code point
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_QUINT, STATE_BASE, 0);
                }
            case STATE_OVERFLOW_CURR:
                if (cp == '¤') {
                    offset += count;
                    // continue to the next code point and loop back to this state
                    break;
                } else {
                    return makeTag(offset, TYPE_CURRENCY_OVERFLOW, STATE_BASE, 0);
                }
            default:
                throw new AssertionError();
            }
        }
        // End of string
        switch (state) {
        case STATE_BASE:
            // No more tokens in string.
            return -1L;
        case STATE_FIRST_QUOTE:
        case STATE_INSIDE_QUOTE:
            // For consistent behavior with the JDK and ICU 58, throw an exception here.
            throw new IllegalArgumentException(
                    "Unterminated quote in pattern affix: \"" + patternString + "\"");
        case STATE_AFTER_QUOTE:
            // No more tokens in string.
            return -1L;
        case STATE_FIRST_CURR:
            return makeTag(offset, TYPE_CURRENCY_SINGLE, STATE_BASE, 0);
        case STATE_SECOND_CURR:
            return makeTag(offset, TYPE_CURRENCY_DOUBLE, STATE_BASE, 0);
        case STATE_THIRD_CURR:
            return makeTag(offset, TYPE_CURRENCY_TRIPLE, STATE_BASE, 0);
        case STATE_FOURTH_CURR:
            return makeTag(offset, TYPE_CURRENCY_QUAD, STATE_BASE, 0);
        case STATE_FIFTH_CURR:
            return makeTag(offset, TYPE_CURRENCY_QUINT, STATE_BASE, 0);
        case STATE_OVERFLOW_CURR:
            return makeTag(offset, TYPE_CURRENCY_OVERFLOW, STATE_BASE, 0);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns whether the affix pattern string has any more tokens to be retrieved from a call to
     * {@link #nextToken}.
     *
     * @param tag
     *            The bitmask tag of the previous token, as returned by {@link #nextToken}.
     * @param string
     *            The affix pattern.
     * @return true if there are more tokens to consume; false otherwise.
     */
    private static boolean hasNext(long tag, CharSequence string) {
        assert tag >= 0;
        int state = getState(tag);
        int offset = getOffset(tag);
        // Special case: the last character in string is an end quote.
        if (state == STATE_INSIDE_QUOTE
                && offset == string.length() - 1
                && string.charAt(offset) == '\'') {
            return false;
        } else if (state != STATE_BASE) {
            return true;
        } else {
            return offset < string.length();
        }
    }

    /**
     * This function helps determine the identity of the token consumed by {@link #nextToken}. Converts
     * from a bitmask tag, based on a call to {@link #nextToken}, to its corresponding symbol type or
     * code point.
     *
     * @param tag
     *            The bitmask tag of the current token, as returned by {@link #nextToken}.
     * @return If less than zero, a symbol type corresponding to one of the <code>TYPE_</code> constants,
     *         such as {@link #TYPE_MINUS_SIGN}. If greater than or equal to zero, a literal code point.
     */
    private static int getTypeOrCp(long tag) {
        assert tag >= 0;
        int type = getType(tag);
        return (type == TYPE_CODEPOINT) ? getCodePoint(tag) : -type;
    }

    /**
     * Encodes the given values into a 64-bit tag.
     *
     * <ul>
     * <li>Bits 0-31 => offset (int32)
     * <li>Bits 32-35 => type (uint4)
     * <li>Bits 36-39 => state (uint4)
     * <li>Bits 40-60 => code point (uint21)
     * <li>Bits 61-63 => unused
     * </ul>
     */
    private static long makeTag(int offset, int type, int state, int cp) {
        long tag = 0L;
        tag |= offset;
        tag |= (-(long) type) << 32;
        tag |= ((long) state) << 36;
        tag |= ((long) cp) << 40;
        assert tag >= 0;
        return tag;
    }

    private static int getOffset(long tag) {
        return (int) (tag & 0xffffffff);
    }

    private static int getType(long tag) {
        return (int) ((tag >>> 32) & 0xf);
    }

    private static int getState(long tag) {
        return (int) ((tag >>> 36) & 0xf);
    }

    private static int getCodePoint(long tag) {
        return (int) (tag >>> 40);
    }
}
