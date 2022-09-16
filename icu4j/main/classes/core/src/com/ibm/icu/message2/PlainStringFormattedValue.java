// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.AttributedCharacterIterator;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.FormattedValue;

/**
 * Very-very rough implementation of FormattedValue, packaging a string.
 * Expect it to change.
 *
 * @internal ICU 72 technology preview. Visible For Testing.
 * @deprecated This API is for ICU internal use only.
 */
@Deprecated
public class PlainStringFormattedValue implements FormattedValue {
    private final String value;

    /**
     * Constructor, taking the string to store.
     *
     * @param value the string value to store
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public PlainStringFormattedValue(String value) {
        if (value == null) {
            throw new IllegalAccessError("Should not try to wrap a null in a formatted value");
        }
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public int length() {
        return value == null ? 0 : value.length();
    }

    /**
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
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
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public boolean nextPosition(ConstrainedFieldPosition cfpos) {
        throw new RuntimeException("nextPosition not yet implemented");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public AttributedCharacterIterator toCharacterIterator() {
        throw new RuntimeException("toCharacterIterator not yet implemented");
    }

    /**
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview. Visible For Testing.
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public String toString() {
        return value;
    }
}
