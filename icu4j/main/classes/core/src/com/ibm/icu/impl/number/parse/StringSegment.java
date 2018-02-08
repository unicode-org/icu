// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

/**
 * A mutable class allowing for a String with a variable offset and length. The charAt, length, and
 * subSequence methods all operate relative to the fixed offset into the String.
 *
 * @author sffc
 */
public class StringSegment implements CharSequence {
    private final String str;
    private int start;
    private int end;
    private boolean foldCase;

    public StringSegment(String str, int parseFlags) {
        this.str = str;
        this.start = 0;
        this.end = str.length();
        this.foldCase = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_IGNORE_CASE);
    }

    public int getOffset() {
        return start;
    }

    public void setOffset(int start) {
        assert start <= end;
        this.start = start;
    }

    /**
     * Equivalent to <code>setOffset(getOffset()+delta)</code>.
     *
     * <p>
     * This method is usually called by a Matcher to register that a char was consumed. If the char is
     * strong (it usually is, except for things like whitespace), follow this with a call to
     * {@link ParsedNumber#setCharsConsumed}. For more information on strong chars, see that method.
     */
    public void adjustOffset(int delta) {
        assert start + delta >= 0;
        assert start + delta <= end;
        start += delta;
    }

    /**
     * Adjusts the offset by the width of the current code point, either 1 or 2 chars.
     */
    public void adjustOffsetByCodePoint() {
        start += Character.charCount(getCodePoint());
    }

    public void setLength(int length) {
        assert length >= 0;
        assert start + length <= str.length();
        end = start + length;
    }

    public void resetLength() {
        end = str.length();
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        return str.charAt(index + start);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new AssertionError(); // Never used
        // Possible implementation:
        // return str.subSequence(start + this.start, end + this.start);
    }

    /**
     * Returns the first code point in the string segment, or -1 if the string starts with an invalid
     * code point.
     *
     * <p>
     * <strong>Important:</strong> Most of the time, you should use {@link #matches}, which handles case
     * folding logic, instead of this method.
     */
    public int getCodePoint() {
        assert start < end;
        char lead = str.charAt(start);
        if (Character.isHighSurrogate(lead) && start + 1 < end) {
            return Character.toCodePoint(lead, str.charAt(start + 1));
        } else if (Character.isSurrogate(lead)) {
            return -1;
        } else {
            return lead;
        }
    }

    /**
     * Returns true if the first code point of this StringSegment equals the given code point.
     *
     * <p>
     * This method will perform case folding if case folding is enabled for the parser.
     */
    public boolean matches(int otherCp) {
        return codePointsEqual(getCodePoint(), otherCp, foldCase);
    }

    /**
     * Returns true if the first code point of this StringSegment is in the given UnicodeSet.
     */
    public boolean matches(UnicodeSet uniset) {
        // TODO: Move UnicodeSet case-folding logic here.
        // TODO: Handle string matches here instead of separately.
        int cp = getCodePoint();
        if (cp == -1) {
            return false;
        }
        return uniset.contains(cp);
    }

    /**
     * Returns the length of the prefix shared by this StringSegment and the given CharSequence. For
     * example, if this string segment is "aab", and the char sequence is "aac", this method returns 2,
     * since the first 2 characters are the same.
     *
     * <p>
     * This method will perform case folding if case folding is enabled for the parser.
     */
    public int getCommonPrefixLength(CharSequence other) {
        return getPrefixLengthInternal(other, foldCase);
    }

    /**
     * Like {@link #getCommonPrefixLength}, but never performs case folding, even if case folding is
     * enabled for the parser.
     */
    public int getCaseSensitivePrefixLength(CharSequence other) {
        return getPrefixLengthInternal(other, false);
    }

    private int getPrefixLengthInternal(CharSequence other, boolean foldCase) {
        int offset = 0;
        for (; offset < Math.min(length(), other.length());) {
            // TODO: case-fold code points, not chars
            char c1 = charAt(offset);
            char c2 = other.charAt(offset);
            if (!codePointsEqual(c1, c2, foldCase)) {
                break;
            }
            offset++;
        }
        return offset;
    }

    // /**
    // * Case-folds the string if IGNORE_CASE flag is set; otherwise, returns the same string.
    // */
    // public static String maybeFold(String input, int parseFlags) {
    // UnicodeSet cwcf = UnicodeSetStaticCache.get(UnicodeSetStaticCache.Key.CWCF);
    // if (0 != (parseFlags & ParsingUtils.PARSE_FLAG_IGNORE_CASE) && cwcf.containsSome(input)) {
    // return UCharacter.foldCase(input, true);
    // } else {
    // return input;
    // }
    // }

    private static final boolean codePointsEqual(int cp1, int cp2, boolean foldCase) {
        if (cp1 == cp2) {
            return true;
        }
        if (!foldCase) {
            return false;
        }
        cp1 = UCharacter.foldCase(cp1, true);
        cp2 = UCharacter.foldCase(cp2, true);
        return cp1 == cp2;
    }

    @Override
    public String toString() {
        return str.substring(0, start) + "[" + str.substring(start, end) + "]" + str.substring(end);
    }
}
