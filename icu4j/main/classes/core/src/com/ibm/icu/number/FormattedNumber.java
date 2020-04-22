// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.FormattedValueStringBuilderImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.PluralRules.IFixedDecimal;

/**
 * The result of a number formatting operation. This class allows the result to be exported in several
 * data types, including a String, an AttributedCharacterIterator, and a BigDecimal.
 *
 * Instances of this class are immutable and thread-safe.
 *
 * @stable ICU 60
 * @see NumberFormatter
 */
public class FormattedNumber implements FormattedValue {
    final FormattedStringBuilder string;
    final DecimalQuantity fq;

    FormattedNumber(FormattedStringBuilder nsb, DecimalQuantity fq) {
        this.string = nsb;
        this.fq = fq;
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 60
     */
    @Override
    public String toString() {
        return string.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 64
     */
    @Override
    public int length() {
        return string.length();
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 64
     */
    @Override
    public char charAt(int index) {
        return string.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 64
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return string.subString(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 60
     */
    @Override
    public <A extends Appendable> A appendTo(A appendable) {
        return Utility.appendTo(string, appendable);
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 64
     */
    @Override
    public boolean nextPosition(ConstrainedFieldPosition cfpos) {
        return FormattedValueStringBuilderImpl.nextPosition(string, cfpos, null);
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 62
     */
    @Override
    public AttributedCharacterIterator toCharacterIterator() {
        return FormattedValueStringBuilderImpl.toCharacterIterator(string, null);
    }

    /**
     * Export the formatted number as a BigDecimal. This endpoint is useful for obtaining the exact
     * number being printed after scaling and rounding have been applied by the number formatting
     * pipeline.
     *
     * @return A BigDecimal representation of the formatted number.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public BigDecimal toBigDecimal() {
        return fq.toBigDecimal();
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public IFixedDecimal getFixedDecimal() {
        return fq;
    }
}