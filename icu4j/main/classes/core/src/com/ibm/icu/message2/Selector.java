// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Map;

/**
 * The interface that must be implemented by all selectors
 * that can be used from {@link MessageFormatter}.
 *
 * <p>Selectors are used to choose between different message variants,
 * similar to <code>plural</code>, <code>selectordinal</code>,
 * and <code>select</code> in {@link com.ibm.icu.text.MessageFormat}.</p>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public interface Selector {
    /**
     * A method that is invoked for the object to match and each key.
     *
     * <p>For example an English plural {@code matches} would return {@code true}
     * for {@code matches(1, "1")}, {@code matches(1, "one")}, and {@code matches(1, "*")}.</p>
     *
     * @param value the value to select on.
     * @param key the key to test for matching.
     * @param variableOptions options that are not know at build time.
     * @return the formatted string.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    boolean matches(Object value, String key, Map<String, Object> variableOptions);
}
