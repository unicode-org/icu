// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.util.ICUUncheckedIOException;

import newapi.impl.MicroProps;

public class FormattedNumber {
    NumberStringBuilder nsb;
    FormatQuantity fq;
    MicroProps micros;

    FormattedNumber(NumberStringBuilder nsb, FormatQuantity fq, MicroProps micros) {
        this.nsb = nsb;
        this.fq = fq;
        this.micros = micros;
    }

    @Override
    public String toString() {
        return nsb.toString();
    }

    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(nsb);
        } catch (IOException e) {
            // Throw as an unchecked exception to avoid users needing try/catch
            throw new ICUUncheckedIOException(e);
        }
        return appendable;
    }

    public void populateFieldPosition(FieldPosition fieldPosition) {
        populateFieldPosition(fieldPosition, 0);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void populateFieldPosition(FieldPosition fieldPosition, int offset) {
        nsb.populateFieldPosition(fieldPosition, offset);
        fq.populateUFieldPosition(fieldPosition);
    }

    public AttributedCharacterIterator getAttributes() {
        return nsb.getIterator();
    }

    public BigDecimal toBigDecimal() {
        return fq.toBigDecimal();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an upcoming release.
     */
    @Deprecated
    public String getPrefix() {
        NumberStringBuilder temp = new NumberStringBuilder();
        int length = micros.modOuter.apply(temp, 0, 0);
        length += micros.modMiddle.apply(temp, 0, length);
        length += micros.modInner.apply(temp, 0, length);
        int prefixLength = micros.modOuter.getPrefixLength() + micros.modMiddle.getPrefixLength()
                + micros.modInner.getPrefixLength();
        return temp.subSequence(0, prefixLength).toString();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an upcoming release.
     */
    @Deprecated
    public String getSuffix() {
        NumberStringBuilder temp = new NumberStringBuilder();
        int length = micros.modOuter.apply(temp, 0, 0);
        length += micros.modMiddle.apply(temp, 0, length);
        length += micros.modInner.apply(temp, 0, length);
        int prefixLength = micros.modOuter.getPrefixLength() + micros.modMiddle.getPrefixLength()
                + micros.modInner.getPrefixLength();
        return temp.subSequence(prefixLength, length).toString();
    }

    /**
     * @internal
     * @deprecated This API a technology preview. It is not stable and may change or go away in an upcoming release.
     */
    @Deprecated
    public IFixedDecimal getFixedDecimal() {
        return fq;
    }

    @Override
    public int hashCode() {
        // NumberStringBuilder and BigDecimal are mutable, so we can't call
        // #equals() or #hashCode() on them directly.
        return Arrays.hashCode(nsb.toCharArray()) ^ Arrays.hashCode(nsb.toFieldArray()) ^ fq.toBigDecimal().hashCode();
    }

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
                ^ Arrays.equals(nsb.toFieldArray(), _other.nsb.toFieldArray())
                ^ fq.toBigDecimal().equals(_other.fq.toBigDecimal());
    }
}