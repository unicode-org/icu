// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;

/**
 * The interface that must be implemented for each formatting function name that can be used from
 * {@link MessageFormatter}.
 *
 * <p>We use it to create and cache various functions with various options.
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public interface FunctionFactory {
    /**
     * The method that is called to create a function.
     *
     * @param locale the locale to use for formatting / selection.
     * @param fixedOptions the options to use for formatting / selection. The keys and values are
     *     function dependent.
     * @return the function.
     * @throws IllegalArgumentException in case there is a problem with the arguments.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    Function create(Locale locale, Map<String, Object> fixedOptions);
}
