// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Creates a {@link Selector} doing literal selection, similar to <code>{exp, select}</code>
 * in {@link com.ibm.icu.text.MessageFormat}.
 */
class TextSelectorFactory implements SelectorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Selector createSelector(Locale locale, Map<String, Object> fixedOptions) {
        return new TextSelector();
    }

    private static class TextSelector implements Selector {
        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> matches(
                Object value, List<String> keys, Map<String, Object> variableOptions) {
            List<String> result = new ArrayList<>();
            if (value == null) {
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
            if ("*".equals(key)) {
                return true;
            }
            return key.equals(Objects.toString(value));
        }
    }
}
