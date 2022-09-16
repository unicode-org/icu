// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;


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
        public boolean matches(Object value, String key, Map<String, Object> variableOptions) {
            if ("*".equals(key)) {
                return true;
            }
            return key.equals(value);
        }
    }
}
