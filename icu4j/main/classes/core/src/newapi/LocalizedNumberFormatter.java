// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

import newapi.impl.MacroProps;
import newapi.impl.MicroProps;

public class LocalizedNumberFormatter extends NumberFormatterSettings<LocalizedNumberFormatter> {

    static final AtomicLongFieldUpdater<LocalizedNumberFormatter> callCount = AtomicLongFieldUpdater
            .newUpdater(LocalizedNumberFormatter.class, "callCountInternal");

    volatile long callCountInternal; // do not access directly; use callCount instead
    volatile LocalizedNumberFormatter savedWithUnit;
    volatile Worker1 compiled;

    LocalizedNumberFormatter(NumberFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    public FormattedNumber format(long input) {
        return format(new FormatQuantity4(input));
    }

    public FormattedNumber format(double input) {
        return format(new FormatQuantity4(input));
    }

    public FormattedNumber format(Number input) {
        return format(new FormatQuantity4(input));
    }

    public FormattedNumber format(Measure input) {
        MeasureUnit unit = input.getUnit();
        Number number = input.getNumber();
        // Use this formatter if possible
        if (Objects.equals(resolve().unit, unit)) {
            return format(number);
        }
        // This mechanism saves the previously used unit, so if the user calls this method with the
        // same unit multiple times in a row, they get a more efficient code path.
        LocalizedNumberFormatter withUnit = savedWithUnit;
        if (withUnit == null || !Objects.equals(withUnit.resolve().unit, unit)) {
            withUnit = new LocalizedNumberFormatter(this, KEY_UNIT, unit);
            savedWithUnit = withUnit;
        }
        return withUnit.format(number);
    }

    /**
     * This is the core entrypoint to the number formatting pipeline. It performs self-regulation: a static code path
     * for the first few calls, and compiling a more efficient data structure if called repeatedly.
     *
     * @param fq
     *            The quantity to be formatted.
     * @return The formatted number result.
     *
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public FormattedNumber format(FormatQuantity fq) {
        MacroProps macros = resolve();
        NumberStringBuilder string = new NumberStringBuilder();
        long currentCount = callCount.incrementAndGet(this);
        MicroProps micros;
        if (currentCount == macros.threshold.longValue()) {
            compiled = Worker1.fromMacros(macros);
            micros = compiled.apply(fq, string);
        } else if (compiled != null) {
            micros = compiled.apply(fq, string);
        } else {
            micros = Worker1.applyStatic(macros, fq, string);
        }
        return new FormattedNumber(string, fq, micros);
    }

    @Override
    protected LocalizedNumberFormatter create(int key, Object value) {
        return new LocalizedNumberFormatter(this, key, value);
    }

    /**
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public static class Internal extends LocalizedNumberFormatter {

        /**
         * @internal
         * @deprecated ICU 60 This API is ICU internal only.
         */
        @Deprecated
        public Internal(NumberFormatterSettings<?> parent, int key, Object value) {
            super(parent, key, value);
        }
    }
}