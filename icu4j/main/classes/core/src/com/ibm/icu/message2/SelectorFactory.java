// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;

/**
 * The interface that must be implemented for each selection function
 * that can be used from {@link MessageFormatter}.
 *
 * <p>The we use it to create and cache various selectors with various options.</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public interface SelectorFactory {
    /**
     * The method that is called to create a selector.
     *
     * @param locale the locale to use for selection.
     * @param fixedOptions the options to use for selection. The keys and values are function dependent.
     * @return The Selector.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    Selector createSelector(Locale locale, Map<String, Object> fixedOptions);
}
