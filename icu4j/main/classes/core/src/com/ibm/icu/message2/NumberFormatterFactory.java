// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.util.CurrencyAmount;


/**
 * Creates a {@link Formatter} doing numeric formatting, similar to <code>{exp, number}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 */
class NumberFormatterFactory implements FormatterFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        return new NumberFormatterImpl(locale, fixedOptions);
    }

    static class NumberFormatterImpl implements Formatter {
        private final Locale locale;
        private final Map<String, Object> fixedOptions;
        private final LocalizedNumberFormatter icuFormatter;
        final boolean advanced;

        private static LocalizedNumberFormatter formatterForOptions(Locale locale, Map<String, Object> fixedOptions) {
            UnlocalizedNumberFormatter nf;
            String skeleton = OptUtils.getString(fixedOptions, "skeleton");
            if (skeleton != null) {
                nf = NumberFormatter.forSkeleton(skeleton);
            } else {
                nf = NumberFormatter.with();
                Integer minFractionDigits = OptUtils.getInteger(fixedOptions, "minimumFractionDigits");
                if (minFractionDigits != null) {
                    nf = nf.precision(Precision.minFraction(minFractionDigits));
                }
            }
            return nf.locale(locale);
        }

        NumberFormatterImpl(Locale locale, Map<String, Object> fixedOptions) {
            this.locale = locale;
            this.fixedOptions = new HashMap<>(fixedOptions);
            String skeleton = OptUtils.getString(fixedOptions, "skeleton");
            boolean fancy = skeleton != null;
            this.icuFormatter = formatterForOptions(locale, fixedOptions);
            this.advanced = fancy;
        }

        LocalizedNumberFormatter getIcuFormatter() {
            return icuFormatter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            LocalizedNumberFormatter realFormatter;
            if (variableOptions.isEmpty()) {
                realFormatter = this.icuFormatter;
            } else {
                Map<String, Object> mergedOptions = new HashMap<>(fixedOptions);
                mergedOptions.putAll(variableOptions);
                // This is really wasteful, as we don't use the existing
                // formatter if even one option is variable.
                // We can optimize, but for now will have to do.
                realFormatter = formatterForOptions(locale, mergedOptions);
            }

            Integer offset = OptUtils.getInteger(variableOptions, "offset");
            if (offset == null && fixedOptions != null) {
                offset = OptUtils.getInteger(fixedOptions, "offset");
            }
            if (offset == null) {
                offset = 0;
            }

            FormattedValue result = null;
            if (toFormat == null) {
                // This is also what MessageFormat does.
                throw new NullPointerException("Argument to format can't be null");
            } else if (toFormat instanceof Double) {
                result = realFormatter.format((double) toFormat - offset);
            } else if (toFormat instanceof Long) {
                result = realFormatter.format((long) toFormat - offset);
            } else if (toFormat instanceof Integer) {
                result = realFormatter.format((int) toFormat - offset);
            } else if (toFormat instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) toFormat;
                result = realFormatter.format(bd.subtract(BigDecimal.valueOf(offset)));
            } else if (toFormat instanceof Number) {
                result = realFormatter.format(((Number) toFormat).doubleValue() - offset);
            } else if (toFormat instanceof CurrencyAmount) {
                result = realFormatter.format((CurrencyAmount) toFormat);
            } else {
                // The behavior is not in the spec, will be in the registry.
                // We can return "NaN", or try to parse the string as a number
                String strValue = Objects.toString(toFormat);
                Number nrValue = OptUtils.asNumber(strValue);
                if (nrValue != null) {
                    result = realFormatter.format(nrValue.doubleValue() - offset);
                } else {
                    result = new PlainStringFormattedValue("NaN");
                }
            }
            return new FormattedPlaceholder(toFormat, result);
        }
    }
}
