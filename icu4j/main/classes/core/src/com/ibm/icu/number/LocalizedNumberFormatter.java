// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/**
 * A NumberFormatter that has a locale associated with it; this means .format() methods are available.
 *
 * @see NumberFormatter
 * @draft ICU 60
 * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public FormattedNumber format(Measure input) {
        MeasureUnit unit = input.getUnit();
        Number number = input.getNumber();
        // Use this formatter if possible
        if (Utility.equals(resolve().unit, unit)) {
            return format(number);
        }
        // This mechanism saves the previously used unit, so if the user calls this method with the
        // same unit multiple times in a row, they get a more efficient code path.
        LocalizedNumberFormatter withUnit = savedWithUnit;
        if (withUnit == null || !Utility.equals(withUnit.resolve().unit, unit)) {
            withUnit = new LocalizedNumberFormatter(this, KEY_UNIT, unit);
            savedWithUnit = withUnit;
        }
        return withUnit.format(number);
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
     * @return The formatted number result.
     *
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public FormattedNumber format(DecimalQuantity fq) {
        MacroProps macros = resolve();
        // NOTE: In Java, the atomic increment logic is slightly different than ICU4C.
        // It seems to be more efficient to make just one function call instead of two.
        // Further benchmarking is required.
        long currentCount = callCount.incrementAndGet(this);
        NumberStringBuilder string = new NumberStringBuilder();
        if (currentCount == macros.threshold.longValue()) {
            compiled = NumberFormatterImpl.fromMacros(macros);
            compiled.apply(fq, string);
        } else if (compiled != null) {
            compiled.apply(fq, string);
        } else {
            NumberFormatterImpl.applyStatic(macros, fq, string);
        }
        return new FormattedNumber(string, fq);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only. Use {@link FormattedNumber#populateFieldPosition} or
     *             {@link FormattedNumber#getFieldIterator} for similar functionality.
     */
    @Deprecated
    public String getAffix(boolean isPrefix, boolean isNegative) {
        MacroProps macros = resolve();
        NumberStringBuilder nsb = new NumberStringBuilder();
        DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(isNegative ? -1 : 1);
        int prefixLength = NumberFormatterImpl.getPrefixSuffix(macros, dq, nsb);
        if (isPrefix) {
            return nsb.subSequence(0, prefixLength).toString();
        } else {
            return nsb.subSequence(prefixLength, nsb.length()).toString();
        }
    }

    @Override
    LocalizedNumberFormatter create(int key, Object value) {
        return new LocalizedNumberFormatter(this, key, value);
    }
}