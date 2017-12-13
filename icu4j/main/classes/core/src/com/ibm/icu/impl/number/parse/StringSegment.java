// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * A mutable class allowing for a String with a variable offset and length. The charAt, length, and subSequence methods
 * all operate relative to the fixed offset into the String.
 *
 * @author sffc
 */
public class StringSegment implements CharSequence {
    private final String str;
    private int start;
    private int end;

    public StringSegment(String str) {
        this.str = str;
        this.start = 0;
        this.end = str.length();
    }

    public int getOffset() {
        return start;
    }

    public void setOffset(int start) {
        assert start <= end;
        this.start = start;
    }

    public void adjustOffset(int delta) {
        assert start + delta >= 0;
        assert start + delta <= end;
        start += delta;
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
     * Returns the first code point in the string segment, or -1 if the string starts with an invalid code point.
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
     * Returns whether the segment is one char in length, and that the char is a leading surrogate.
     */
    public boolean isLeadingSurrogate() {
        return (end - start == 1) && Character.isHighSurrogate(str.charAt(start));
    }

    /**
     * Returns the length of the prefix shared by this StringSegment and the given CharSequence. For example, if this
     * string segment is "aab", and the char sequence is "aac", this method returns 2, since the first 2 characters are
     * the same.
     */
    public int getCommonPrefixLength(CharSequence other) {
        int offset = 0;
        for (; offset < Math.min(length(), other.length()); offset++) {
            if (charAt(offset) != other.charAt(offset)) {
                break;
            }
        }
        return offset;
    }

    @Override
    public String toString() {
        return str.substring(0, start) + "[" + str.substring(start, end) + "]" + str.substring(end);
    }
}
