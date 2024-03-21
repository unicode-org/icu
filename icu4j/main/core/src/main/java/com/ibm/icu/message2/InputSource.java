// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

class InputSource {
    final String buffer;

    private int cursor;
    private int lastReadCursor = -1;
    private int lastReadCount = 0;

    InputSource(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string should not be null");
        }
        this.buffer = input;
        this.cursor = 0;
    }

    boolean atEnd() {
        return cursor >= buffer.length();
    }

    int peekChar() {
        if (atEnd()) {
            return -1;
        }
        return buffer.charAt(cursor);
    }

    int readCodePoint() {
        // TODO: remove this?
        // START Detect possible infinite loop
        if (lastReadCursor != cursor) {
            lastReadCursor = cursor;
            lastReadCount = 1;
        } else {
            lastReadCount++;
            if (lastReadCount >= 10) {
                throw new RuntimeException("Stuck in a loop!");
            }
        }
        // END Detect possible infinite loop

        if (atEnd()) {
            return -1;
        }

        char c = buffer.charAt(cursor++);
        if (Character.isHighSurrogate(c)) {
            if (!atEnd()) {
                char c2 = buffer.charAt(cursor++);
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c, c2);
                } else { // invalid, high surrogate followed by non-surrogate
                    cursor--;
                    return c;
                }
            }
        }
        return c;
    }

    // Backup a number of characters.
    void backup(int amount) {
        // TODO: validate
        cursor -= amount;
    }

    int getPosition() {
        return cursor;
    }

    void skip(int amount) {
        // TODO: validate
        cursor += amount;
    }

    void gotoPosition(int position) {
        // TODO: validate
        cursor = position;
    }
}
