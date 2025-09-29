// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.List;
import java.util.Map;

/**
 * The interface that must be implemented by all functions
 * that can be used from {@link MessageFormatter}.
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public interface Function {
    /**
     * A method that takes the object to format and returns
     * the i18n-aware string representation.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    String formatToString(Object toFormat, Map<String, Object> variableOptions);

    /**
     * A method that takes the object to format and returns
     * the i18n-aware formatted placeholder.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted placeholder.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions);

    /**
     * A method that is invoked for the object to match on each key.
     *
     * <p>For example, an English plural {@code matches} would return {@code true}
     * for {@code matches(1, "1")}, {@code matches(1, "one")}, and {@code matches(1, "*")}.</p>
     *
     * @param value the value to select on.
     * @param keys the key to test for matching.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    default List<String> matches(Object value, List<String> keys, Map<String, Object> variableOptions) {
        /* Options considered:
         * - return null (which will end up selecting `*`) => current solution
         * - throw (not something that ICU usually does)
         * - remove the `default` and force each class implementing this interface to also implement this method
         */
        return null;
    }
}
