// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.text.DisplayOptions.NounClass;

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
    final MeasureUnit outputUnit;

    // Grammatical gender of the formatted result.
    final String gender;

    FormattedNumber(FormattedStringBuilder nsb, DecimalQuantity fq, MeasureUnit outputUnit, String gender) {
        this.string = nsb;
        this.fq = fq;
        this.outputUnit = outputUnit;
        this.gender = gender;
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
     * Gets the resolved output unit.
     * <p>
     * The output unit is dependent upon the localized preferences for the usage
     * specified via NumberFormatterSettings.usage(), and may be a unit with
     * MeasureUnit.Complexity.MIXED unit complexity (MeasureUnit.getComplexity()), such
     * as "foot-and-inch" or "hour-and-minute-and-second".
     *
     * @return `MeasureUnit`.
     * @stable ICU 68
     */
    public MeasureUnit getOutputUnit() {
        return this.outputUnit;
    }

    /**
     * Gets the noun class of the formatted output. Returns `UNDEFINED` when the noun class is not
     * supported yet.
     *
     * @return NounClass
     * @draft ICU 71.
     */
    public NounClass getNounClass() {
        return NounClass.fromIdentifier(this.gender);
    }

    /**
     * The gender of the formatted output.
     *
     * @internal ICU 69 technology preview
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public String getGender() {
        if (this.gender == null) {
            return "";
        }
        return this.gender;
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
