// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityResult;
import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * The result of a number range formatting operation. This class allows the result to be exported in several data types,
 * including a String, an AttributedCharacterIterator, and a BigDecimal.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberRangeFormatter
 */
public class FormattedNumberRange {
    final NumberStringBuilder string;
    final DecimalQuantity quantity1;
    final DecimalQuantity quantity2;
    final RangeIdentityResult identityResult;

    FormattedNumberRange(NumberStringBuilder string, DecimalQuantity quantity1, DecimalQuantity quantity2,
            RangeIdentityResult identityResult) {
        this.string = string;
        this.quantity1 = quantity1;
        this.quantity2 = quantity2;
        this.identityResult = identityResult;
    }

    /**
     * Creates a String representation of the the formatted number range.
     *
     * @return a String containing the localized number range.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    @Override
    public String toString() {
        return string.toString();
    }

    /**
     * Append the formatted number range to an Appendable, such as a StringBuilder. This may be slightly more efficient
     * than creating a String.
     *
     * <p>
     * If an IOException occurs when appending to the Appendable, an unchecked {@link ICUUncheckedIOException} is thrown
     * instead.
     *
     * @param appendable
     *            The Appendable to which to append the formatted number range string.
     * @return The same Appendable, for chaining.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see Appendable
     * @see NumberRangeFormatter
     */
    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(string);
        } catch (IOException e) {
            // Throw as an unchecked exception to avoid users needing try/catch
            throw new ICUUncheckedIOException(e);
        }
        return appendable;
    }

    /**
     * Determines the start (inclusive) and end (exclusive) indices of the next occurrence of the given
     * <em>field</em> in the output string. This allows you to determine the locations of, for example,
     * the integer part, fraction part, or symbols.
     * <p>
     * If both sides of the range have the same field, the field will occur twice, once before the range separator and
     * once after the range separator, if applicable.
     * <p>
     * If a field occurs just once, calling this method will find that occurrence and return it. If a field occurs
     * multiple times, this method may be called repeatedly with the following pattern:
     *
     * <pre>
     * FieldPosition fpos = new FieldPosition(NumberFormat.Field.INTEGER);
     * while (formattedNumberRange.nextFieldPosition(fpos, status)) {
     *     // do something with fpos.
     * }
     * </pre>
     * <p>
     * This method is useful if you know which field to query. If you want all available field position information, use
     * {@link #toCharacterIterator()}.
     *
     * @param fieldPosition
     *            Input+output variable. See {@link FormattedNumber#nextFieldPosition(FieldPosition)}.
     * @return true if a new occurrence of the field was found; false otherwise.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see NumberRangeFormatter
     */
    public boolean nextFieldPosition(FieldPosition fieldPosition) {
        return string.nextFieldPosition(fieldPosition);
    }

    /**
     * Export the formatted number range as an AttributedCharacterIterator. This allows you to determine which
     * characters in the output string correspond to which <em>fields</em>, such as the integer part, fraction part, and
     * sign.
     * <p>
     * If information on only one field is needed, use {@link #nextFieldPosition(FieldPosition)} instead.
     *
     * @return An AttributedCharacterIterator, containing information on the field attributes of the number range
     *         string.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see com.ibm.icu.text.NumberFormat.Field
     * @see AttributedCharacterIterator
     * @see NumberRangeFormatter
     */
    public AttributedCharacterIterator toCharacterIterator() {
        return string.toCharacterIterator();
    }

    /**
     * Export the first formatted number as a BigDecimal. This endpoint is useful for obtaining the exact number being
     * printed after scaling and rounding have been applied by the number range formatting pipeline.
     *
     * @return A BigDecimal representation of the first formatted number.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     * @see #getSecondBigDecimal
     */
    public BigDecimal getFirstBigDecimal() {
        return quantity1.toBigDecimal();
    }

    /**
     * Export the second formatted number as a BigDecimal. This endpoint is useful for obtaining the exact number being
     * printed after scaling and rounding have been applied by the number range formatting pipeline.
     *
     * @return A BigDecimal representation of the second formatted number.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     * @see #getFirstBigDecimal
     */
    public BigDecimal getSecondBigDecimal() {
        return quantity2.toBigDecimal();
    }

    /**
     * Returns whether the pair of numbers was successfully formatted as a range or whether an identity fallback was
     * used. For example, if the first and second number were the same either before or after rounding occurred, an
     * identity fallback was used.
     *
     * @return A RangeIdentityType indicating the resulting identity situation in the formatted number range.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     * @see NumberRangeFormatter.RangeIdentityFallback
     */
    public RangeIdentityResult getIdentityResult() {
        return identityResult;
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int hashCode() {
        // NumberStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        return Arrays.hashCode(string.toCharArray()) ^ Arrays.hashCode(string.toFieldArray())
                ^ quantity1.toBigDecimal().hashCode() ^ quantity2.toBigDecimal().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (!(other instanceof FormattedNumberRange))
            return false;
        // NumberStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        FormattedNumberRange _other = (FormattedNumberRange) other;
        return Arrays.equals(string.toCharArray(), _other.string.toCharArray())
                && Arrays.equals(string.toFieldArray(), _other.string.toFieldArray())
                && quantity1.toBigDecimal().equals(_other.quantity1.toBigDecimal())
                && quantity2.toBigDecimal().equals(_other.quantity2.toBigDecimal());
    }
}
