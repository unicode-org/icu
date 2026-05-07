// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

class StringView implements CharSequence {
    final int offset;
    final String text;

    StringView(String text, int offset) {
        this.offset = offset;
        this.text = text;
    }

    StringView(String text) {
        this(text, 0);
    }

    @Override
    public int length() {
        return text.length() - offset;
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index + offset);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return text.subSequence(start + offset, end + offset);
    }

    @Override
    public String toString() {
        return text.substring(offset);
    }
}
