// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Creates a {@link Formatter} that simply returns the String non-i18n aware representation of an object.
 */
class IdentityFormatterFactory implements FormatterFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        return new IdentityFormatterImpl();
    }

    private static class IdentityFormatterImpl implements Formatter {
        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(Objects.toString(toFormat)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }
    }
}
