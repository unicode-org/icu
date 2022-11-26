// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.text.AttributedCharacterIterator;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.FormattedValue;

/**
 * Not yet implemented: The result of a message formatting operation.
 *
 * <p>This contains information about where the various fields and placeholders
 * ended up in the final result.</p>
 * <p>This class allows the result to be exported in several data types,
 * including a {@link String}, {@link AttributedCharacterIterator}, more (TBD).</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for ICU internal use only.
 */
@Deprecated
public class FormattedMessage implements FormattedValue {

    /**
     * Not yet implemented.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public FormattedMessage() {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public int length() {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public char charAt(int index) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public CharSequence subSequence(int start, int end) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public <A extends Appendable> A appendTo(A appendable) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public boolean nextPosition(ConstrainedFieldPosition cfpos) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Not yet implemented.
     *
     * {@inheritDoc}
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    @Override
    public AttributedCharacterIterator toCharacterIterator() {
        throw new RuntimeException("Not yet implemented.");
    }

}
