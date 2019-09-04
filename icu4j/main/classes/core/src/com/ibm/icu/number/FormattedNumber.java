// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int length() {
        return string.length();
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public char charAt(int index) {
        return string.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 64
     * @provisional This API might change or be removed in a future release.
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
     * Determines the start (inclusive) and end (exclusive) indices of the next occurrence of the
     * given <em>field</em> in the output string. This allows you to determine the locations of,
     * for example, the integer part, fraction part, or symbols.
     * <p>
     * This is a simpler but less powerful alternative to {@link #nextPosition}.
     * <p>
     * If a field occurs just once, calling this method will find that occurrence and return it. If a
     * field occurs multiple times, this method may be called repeatedly with the following pattern:
     *
     * <pre>
     * FieldPosition fpos = new FieldPosition(NumberFormat.Field.GROUPING_SEPARATOR);
     * while (formattedNumber.nextFieldPosition(fpos, status)) {
     *     // do something with fpos.
     * }
     * </pre>
     * <p>
     * This method is useful if you know which field to query. If you want all available field position
     * information, use {@link #nextPosition} or {@link #toCharacterIterator()}.
     *
     * @param fieldPosition
     *            Input+output variable. On input, the "field" property determines which field to look
     *            up, and the "beginIndex" and "endIndex" properties determine where to begin the search.
     *            On output, the "beginIndex" is set to the beginning of the first occurrence of the
     *            field with either begin or end indices after the input indices, "endIndex" is set to
     *            the end of that occurrence of the field (exclusive index). If a field position is not
     *            found, the method returns FALSE and the FieldPosition may or may not be changed.
     * @return true if a new occurrence of the field was found; false otherwise.
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see NumberFormatter
     */
    public boolean nextFieldPosition(FieldPosition fieldPosition) {
        fq.populateUFieldPosition(fieldPosition);
        return FormattedValueStringBuilderImpl.nextFieldPosition(string, fieldPosition);
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

    /**
     * {@inheritDoc}
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int hashCode() {
        // FormattedStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        return Arrays.hashCode(string.toCharArray())
                ^ Arrays.hashCode(string.toFieldArray())
                ^ fq.toBigDecimal().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (!(other instanceof FormattedNumber))
            return false;
        // FormattedStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        FormattedNumber _other = (FormattedNumber) other;
        return Arrays.equals(string.toCharArray(), _other.string.toCharArray())
                && Arrays.equals(string.toFieldArray(), _other.string.toFieldArray())
                && fq.toBigDecimal().equals(_other.fq.toBigDecimal());
    }
}