// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

/**
 * A mutable String wrapper with a variable offset and length and
 * support for case folding. The charAt, length, and subSequence methods all
 * operate relative to the fixed offset into the String.
 *
 * Intended to be useful for parsing.
 *
 * CAUTION: Since this class is mutable, it must not be used anywhere that an
 * immutable object is required, like in a cache or as the key of a hash map.
 *
 * @author sffc (Shane Carr)
 */
public class StringSegment implements CharSequence {
    private final String str;
    private int start;
    private int end;
    private boolean foldCase;

    public StringSegment(String str, boolean foldCase) {
        this.str = str;
        this.start = 0;
        this.end = str.length();
        this.foldCase = foldCase;
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
     * Number parsing note: This method is usually called by a Matcher to register that a char was
     * consumed. If the char is strong (it usually is, except for things like whitespace), follow this
     * with a call to ParsedNumber#setCharsConsumed(). For more information on strong chars, see that
     * method.
     */
    public void adjustOffset(int delta) {
        assert start + delta >= 0;
        assert start + delta <= end;
        start += delta;
    }

    /**
     * Adjusts the offset by the width of the current lead code point, either 1 or 2 chars.
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
        return str.subSequence(start + this.start, end + this.start);
    }

    /**
     * Returns the first code point in the string segment.
     *
     * <p>
     * <strong>Important:</strong> Most of the time, you should use {@link #startsWith}, which handles
     * case folding logic, instead of this method.
     */
    public int getCodePoint() {
        assert start < end;
        char lead = str.charAt(start);
        char trail;
        if (Character.isHighSurrogate(lead)
                && start + 1 < end
                && Character.isLowSurrogate(trail = str.charAt(start + 1))) {
            return Character.toCodePoint(lead, trail);
        }
        return lead;
    }

    /**
     * Returns the code point at the given index relative to the current offset.
     */
    public int codePointAt(int index) {
        return str.codePointAt(start + index);
    }

    /**
     * Returns true if the first code point of this StringSegment equals the given code point.
     *
     * <p>
     * This method will perform case folding if case folding is enabled for the parser.
     */
    public boolean startsWith(int otherCp) {
        return codePointsEqual(getCodePoint(), otherCp, foldCase);
    }

    /**
     * Returns true if the first code point of this StringSegment is in the given UnicodeSet.
     */
    public boolean startsWith(UnicodeSet uniset) {
        // TODO: Move UnicodeSet case-folding logic here.
        // TODO: Handle string matches here instead of separately.
        int cp = getCodePoint();
        if (cp == -1) {
            return false;
        }
        return uniset.contains(cp);
    }

    /**
     * Returns true if there is at least one code point of overlap between this StringSegment and the
     * given CharSequence. Null-safe.
     */
    public boolean startsWith(CharSequence other) {
        if (other == null || other.length() == 0 || length() == 0) {
            return false;
        }
        int cp1 = Character.codePointAt(this, 0);
        int cp2 = Character.codePointAt(other, 0);
        return codePointsEqual(cp1, cp2, foldCase);
    }

    /**
     * Returns the length of the prefix shared by this StringSegment and the given CharSequence. For
     * example, if this string segment is "aab", and the char sequence is "aac", this method returns 2,
     * since the first 2 characters are the same.
     *
     * <p>
     * This method only returns offsets along code point boundaries.
     *
     * <p>
     * This method will perform case folding if case folding was enabled in the constructor.
     *
     * <p>
     * IMPORTANT: The given CharSequence must not be empty! It is the caller's responsibility to check.
     */
    public int getCommonPrefixLength(CharSequence other) {
        return getPrefixLengthInternal(other, foldCase);
    }

    /**
     * Like {@link #getCommonPrefixLength}, but never performs case folding, even if case folding was
     * enabled in the constructor.
     */
    public int getCaseSensitivePrefixLength(CharSequence other) {
        return getPrefixLengthInternal(other, false);
    }

    private int getPrefixLengthInternal(CharSequence other, boolean foldCase) {
        assert other.length() != 0;
        int offset = 0;
        for (; offset < Math.min(length(), other.length());) {
            // TODO: case-fold code points, not chars
            int cp1 = Character.codePointAt(this, offset);
            int cp2 = Character.codePointAt(other, offset);
            if (!codePointsEqual(cp1, cp2, foldCase)) {
                break;
            }
            offset += Character.charCount(cp1);
        }
        return offset;
    }

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

    /**
     * Equals any CharSequence with the same chars as this segment.
     *
     * <p>
     * This method does not perform case folding; if you want case-insensitive equality, use
     * {@link #getCommonPrefixLength}.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CharSequence))
            return false;
        return Utility.charSequenceEquals(this, (CharSequence) other);
    }

    /** Returns a hash code equivalent to calling .toString().hashCode() */
    @Override
    public int hashCode() {
        return Utility.charSequenceHashCode(this);
    }

    /** Returns a string representation useful for debugging. */
    @Override
    public String toString() {
        return str.substring(0, start) + "[" + str.substring(start, end) + "]" + str.substring(end);
    }

    /** Returns a String that is equivalent to the CharSequence representation. */
    public String asString() {
        return str.substring(start, end);
    }
}
