// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import com.ibm.icu.util.ULocale;
import java.util.Locale;

/**
 * Encodes info about the direction of the message.
 *
 * <p>It is used to implement the @code u:dir} functionality.
 *
 * @draft ICU 78
 */
public enum Directionality {
    /**
     * Not initialized or unknown.
     *
     * <p>No special processing will be used.
     *
     * @draft ICU 78
     */
    UNKNOWN,
    /**
     * Left-to-right directionality.
     *
     * @draft ICU 78
     */
    LTR,
    /**
     * Right-to-left directionality.
     *
     * @draft ICU 78
     */
    RTL,
    /**
     * Directionality determined from <i>expression</i> contents.
     *
     * @draft ICU 78
     */
    AUTO,
    /**
     * Directionality inherited from the <i>message</i> or from the <i>resolved value</i> of the
     * <i>operand</i> without requiring isolation of the <i>expression</i> value.
     *
     * @draft ICU 78
     */
    INHERIT;

    /**
     * Determines the directionality appropriate for a given locale.
     *
     * @param locale the locale to determine the directionality from.
     * @return the appropriate directionality for the locale given.
     * @draft ICU 78
     */
    public static Directionality of(Locale locale) {
        if (locale == null) {
            return Directionality.INHERIT;
        }
        return ULocale.forLocale(locale).isRightToLeft() ? Directionality.RTL : Directionality.LTR;
    }
}
