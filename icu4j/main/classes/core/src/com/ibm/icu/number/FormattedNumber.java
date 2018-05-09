// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * The result of a number formatting operation. This class allows the result to be exported in several
 * data types, including a String, an AttributedCharacterIterator, and a BigDecimal.
 *
 * @draft ICU 60
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public class FormattedNumber {
    final NumberStringBuilder nsb;
    final DecimalQuantity fq;

    FormattedNumber(NumberStringBuilder nsb, DecimalQuantity fq) {
        this.nsb = nsb;
        this.fq = fq;
    }

    /**
     * Creates a String representation of the the formatted number.
     *
     * @return a String containing the localized number.
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    @Override
    public String toString() {
        return nsb.toString();
    }

    /**
     * Append the formatted number to an Appendable, such as a StringBuilder. This may be slightly more
     * efficient than creating a String.
     *
     * <p>
     * If an IOException occurs when appending to the Appendable, an unchecked
     * {@link ICUUncheckedIOException} is thrown instead.
     *
     * @param appendable
     *            The Appendable to which to append the formatted number string.
     * @return The same Appendable, for chaining.
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     * @see Appendable
     * @see NumberFormatter
     */
    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(nsb);
        } catch (IOException e) {
            // Throw as an unchecked exception to avoid users needing try/catch
            throw new ICUUncheckedIOException(e);
        }
        return appendable;
    }

    /**
     * Determine the start and end indices of the first occurrence of the given <em>field</em> in the
     * output string. This allows you to determine the locations of the integer part, fraction part, and
     * sign.
     *
     * <p>
     * If multiple different field attributes are needed, this method can be called repeatedly, or if
     * <em>all</em> field attributes are needed, consider using getFieldIterator().
     *
     * <p>
     * If a field occurs multiple times in an output string, such as a grouping separator, this method
     * will only ever return the first occurrence. Use getFieldIterator() to access all occurrences of an
     * attribute.
     *
     * @param fieldPosition
     *            The FieldPosition to populate with the start and end indices of the desired field.
     * @deprecated ICU 62 Use {@link #nextFieldPosition} instead. This method will be removed in a future
     *             release. See http://bugs.icu-project.org/trac/ticket/13746
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see NumberFormatter
     */
    @Deprecated
    public void populateFieldPosition(FieldPosition fieldPosition) {
        // in case any users were depending on the old behavior:
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        nextFieldPosition(fieldPosition);
    }

    /**
     * Determines the start and end indices of the next occurrence of the given <em>field</em> in the
     * output string. This allows you to determine the locations of, for example, the integer part,
     * fraction part, or symbols.
     * <p>
     * If a field occurs just once, calling this method will find that occurrence and return it. If a
     * field occurs multiple times, this method may be called repeatedly with the following pattern:
     * <p>
     *
     * <pre>
     * FieldPosition fpos = new FieldPosition(NumberFormat.Field.GROUPING_SEPARATOR);
     * while (formattedNumber.nextFieldPosition(fpos, status)) {
     *     // do something with fpos.
     * }
     * </pre>
     * <p>
     * This method is useful if you know which field to query. If you want all available field position
     * information, use {@link #toCharacterIterator()}.
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
        return nsb.nextFieldPosition(fieldPosition);
    }

    /**
     * Export the formatted number as an AttributedCharacterIterator. This allows you to determine which
     * characters in the output string correspond to which <em>fields</em>, such as the integer part,
     * fraction part, and sign.
     * <p>
     * If information on only one field is needed, consider using populateFieldPosition() instead.
     *
     * @return An AttributedCharacterIterator, containing information on the field attributes of the
     *         number string.
     * @deprecated ICU 62 Use {@link #toCharacterIterator} instead. This method will be removed in a future
     *             release. See http://bugs.icu-project.org/trac/ticket/13746
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see AttributedCharacterIterator
     * @see NumberFormatter
     */
    @Deprecated
    public AttributedCharacterIterator getFieldIterator() {
        return nsb.toCharacterIterator();
    }

    /**
     * Export the formatted number as an AttributedCharacterIterator. This allows you to determine which
     * characters in the output string correspond to which <em>fields</em>, such as the integer part,
     * fraction part, and sign.
     * <p>
     * If information on only one field is needed, use {@link #nextFieldPosition(FieldPosition)} instead.
     *
     * @return An AttributedCharacterIterator, containing information on the field attributes of the
     *         number string.
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see AttributedCharacterIterator
     * @see NumberFormatter
     */
    public AttributedCharacterIterator toCharacterIterator() {
        return nsb.toCharacterIterator();
    }

    /**
     * Export the formatted number as a BigDecimal. This endpoint is useful for obtaining the exact
     * number being printed after scaling and rounding have been applied by the number formatting
     * pipeline.
     *
     * @return A BigDecimal representation of the formatted number.
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
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
        // NumberStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        return Arrays.hashCode(nsb.toCharArray())
                ^ Arrays.hashCode(nsb.toFieldArray())
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
        // NumberStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        FormattedNumber _other = (FormattedNumber) other;
        return Arrays.equals(nsb.toCharArray(), _other.nsb.toCharArray())
                && Arrays.equals(nsb.toFieldArray(), _other.nsb.toFieldArray())
                && fq.toBigDecimal().equals(_other.fq.toBigDecimal());
    }
}