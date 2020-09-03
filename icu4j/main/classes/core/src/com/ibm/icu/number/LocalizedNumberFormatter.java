// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.math.BigInteger;
import java.text.Format;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.LocalizedNumberFormatterAsFormat;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/**
 * A NumberFormatter that has a locale associated with it; this means .format() methods are available.
 *
 * Instances of this class are immutable and thread-safe.
 *
 * @see NumberFormatter
 * @stable ICU 60
 * @see NumberFormatter
 */
public class LocalizedNumberFormatter extends NumberFormatterSettings<LocalizedNumberFormatter> {

    static final AtomicLongFieldUpdater<LocalizedNumberFormatter> callCount = AtomicLongFieldUpdater
            .newUpdater(LocalizedNumberFormatter.class, "callCountInternal");

    volatile long callCountInternal; // do not access directly; use callCount instead
    volatile LocalizedNumberFormatter savedWithUnit;
    volatile NumberFormatterImpl compiled;

    LocalizedNumberFormatter(NumberFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    /**
     * Format the given byte, short, int, or long to a string using the settings specified in the
     * NumberFormatter fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public FormattedNumber format(long input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given float or double to a string using the settings specified in the NumberFormatter
     * fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public FormattedNumber format(double input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given {@link BigInteger}, {@link BigDecimal}, or other {@link Number} to a string using
     * the settings specified in the NumberFormatter fluent setting chain.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public FormattedNumber format(Number input) {
        return format(new DecimalQuantity_DualStorageBCD(input));
    }

    /**
     * Format the given {@link Measure} or {@link CurrencyAmount} to a string using the settings
     * specified in the NumberFormatter fluent setting chain.
     *
     * <p>
     * The unit specified here overrides any unit that may have been specified in the setter chain. This
     * method is intended for cases when each input to the number formatter has a different unit.
     *
     * @param input
     *            The number to format.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public FormattedNumber format(Measure input) {
        DecimalQuantity fq = new DecimalQuantity_DualStorageBCD(input.getNumber());
        MeasureUnit unit = input.getUnit();
        FormattedStringBuilder string = new FormattedStringBuilder();
        MicroProps micros = formatImpl(fq, unit, string);
        return new FormattedNumber(string, fq, micros.outputUnit);
    }

    /**
     * Creates a representation of this LocalizedNumberFormat as a {@link java.text.Format}, enabling the
     * use of this number formatter with APIs that need an object of that type, such as MessageFormat.
     * <p>
     * This API is not intended to be used other than for enabling API compatibility. The {@link #format}
     * methods should normally be used when formatting numbers, not the Format object returned by this
     * method.
     *
     * @return A Format wrapping this LocalizedNumberFormatter.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public Format toFormat() {
        return new LocalizedNumberFormatterAsFormat(this, resolve().loc);
    }

    /**
     *  Helper method that creates a FormattedStringBuilder and formats.
     */
    private FormattedNumber format(DecimalQuantity fq) {
        FormattedStringBuilder string = new FormattedStringBuilder();
        MicroProps micros = formatImpl(fq, string);
        return new FormattedNumber(string, fq, micros.outputUnit);
    }

    /**
     * This is the core entrypoint to the number formatting pipeline. It performs self-regulation: a
     * static code path for the first few calls, and compiling a more efficient data structure if called
     * repeatedly.
     *
     * <p>
     * This function is very hot, being called in every call to the number formatting pipeline.
     *
     * @param fq
     *            The quantity to be formatted.
     * @param string
     *            The string builder into which to insert the result.
     *
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public MicroProps formatImpl(DecimalQuantity fq, FormattedStringBuilder string) {
        if (computeCompiled()) {
            return compiled.format(fq, string);
        }
        return NumberFormatterImpl.formatStatic(resolve(), fq, string);
    }

    /**
     * Version of above for unit override.
     *
     * @internal
     * @deprecated ICU 67 This API is ICU internal only.
     */
    @Deprecated
    public MicroProps formatImpl(DecimalQuantity fq, MeasureUnit unit, FormattedStringBuilder string) {
        // Use this formatter if possible
        if (Objects.equals(resolve().unit, unit)) {
            return formatImpl(fq, string);

        }
        // This mechanism saves the previously used unit, so if the user calls this method with the
        // same unit multiple times in a row, they get a more efficient code path.
        LocalizedNumberFormatter withUnit = savedWithUnit;
        if (withUnit == null || !Objects.equals(withUnit.resolve().unit, unit)) {
            withUnit = new LocalizedNumberFormatter(this, KEY_UNIT, unit);
            savedWithUnit = withUnit;
        }
        return withUnit.formatImpl(fq, string);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only. Use {@link FormattedNumber#nextPosition}
     *             for related functionality.
     */
    @Deprecated
    public String getAffixImpl(boolean isPrefix, boolean isNegative) {
        FormattedStringBuilder string = new FormattedStringBuilder();
        byte signum = (byte) (isNegative ? -1 : 1);
        // Always return affixes for plural form OTHER.
        StandardPlural plural = StandardPlural.OTHER;
        int prefixLength;
        if (computeCompiled()) {
            prefixLength = compiled.getPrefixSuffix(signum, plural, string);
        } else {
            prefixLength = NumberFormatterImpl.getPrefixSuffixStatic(resolve(), signum, plural, string);
        }
        if (isPrefix) {
            return string.subSequence(0, prefixLength).toString();
        } else {
            return string.subSequence(prefixLength, string.length()).toString();
        }
    }

    private boolean computeCompiled() {
        MacroProps macros = resolve();
        // NOTE: In Java, the atomic increment logic is slightly different than ICU4C.
        // It seems to be more efficient to make just one function call instead of two.
        // Further benchmarking is required.
        long currentCount = callCount.incrementAndGet(this);
        if (currentCount == macros.threshold.longValue()) {
            compiled = new NumberFormatterImpl(macros);
            return true;
        } else if (compiled != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    LocalizedNumberFormatter create(int key, Object value) {
        return new LocalizedNumberFormatter(this, key, value);
    }
}
