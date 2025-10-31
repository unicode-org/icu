// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.FormattedValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.AttributedCharacterIterator;

/**
 * Very-very rough implementation of FormattedValue, packaging a string. Expect it to change.
 *
 * @draft ICU 78
 */
public class PlainStringFormattedValue implements FormattedValue {
    private final String value;

    /**
     * Constructor, taking the string to store.
     *
     * @param value the string value to store
     * @draft ICU 78
     */
    public PlainStringFormattedValue(String value) {
        if (value == null) {
            throw new IllegalAccessError("Should not try to wrap a null in a formatted value");
        }
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public int length() {
        return value == null ? 0 : value.length();
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(value);
        } catch (IOException e) {
            throw new UncheckedIOException("problem appending", e);
        }
        return appendable;
    }

    /**
     * Not yet implemented.
     *
     * <p>{@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public boolean nextPosition(ConstrainedFieldPosition cfpos) {
        throw new RuntimeException("nextPosition not yet implemented");
    }

    /**
     * Not yet implemented.
     *
     * <p>{@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public AttributedCharacterIterator toCharacterIterator() {
        throw new RuntimeException("toCharacterIterator not yet implemented");
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 78
     */
    @Override
    public String toString() {
        return value;
    }
}
