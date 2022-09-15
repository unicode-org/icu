// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Map;

/**
 * The interface that must be implemented by all formatters
 * that can be used from {@link MessageFormatter}.
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for ICU internal use only.
 */
@Deprecated
public interface Formatter {
    /**
     * A method that takes the object to format and returns
     * the i18n-aware string representation.
     *
     * @param toFormat the object to format.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for ICU internal use only.
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
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions);
}
