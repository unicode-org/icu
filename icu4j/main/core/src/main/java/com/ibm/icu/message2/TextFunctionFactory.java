// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ibm.icu.message2.MFDataModel.CatchallKey;

/**
 * Creates a {@link Function} doing literal selection, similar to <code>{exp, select}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 * TODO: TZU
 */
class TextFunctionFactory implements FunctionFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public Function create(Locale locale, Map<String, Object> fixedOptions) {
        return new TextFunctionImpl(OptUtils.getDirectionality(fixedOptions));
    }

    private static class TextFunctionImpl implements Function {
        private final Directionality directionality;

        public TextFunctionImpl(Directionality directionality) {
            this.directionality = directionality == null ? Directionality.INHERIT : directionality;
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
            return new FormattedPlaceholder(
                    toFormat, new PlainStringFormattedValue(Objects.toString(toFormat)),
                    directionality, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> matches(
                Object value, List<String> keys, Map<String, Object> variableOptions) {
            List<String> result = new ArrayList<>();
            if (value == null) {
                if (OptUtils.reportErrors(variableOptions)) {
                    throw new IllegalArgumentException("unresolved-variable: argument to match on can't be null");
                }
                return result;
            }
            for (String key : keys) {
                if (matches(value, key)) {
                    result.add(key);
                }
            }
            result.sort(String::compareTo);
            return result;
        }

        @SuppressWarnings("static-method")
        private boolean matches(Object value, String key) {
            if (CatchallKey.isCatchAll(key)) {
                return true;
            }
            return key.equals(Objects.toString(value));
        }
    }
}
