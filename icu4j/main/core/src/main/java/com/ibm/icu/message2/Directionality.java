// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import com.ibm.icu.util.ULocale;

/**
 * Encodes info about the direction of the message.
 *
 * <p>It is used to implement the @code u:dir} functionality.</p>
 *
 * @internal ICU 77 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public enum Directionality {
    /**
     * Not initialized or unknown.
     *
     * <p>No special processing will be used.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    UNKNOWN,
    /**
     * Left-to-right directionality.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    LTR,
    /**
     * Right-to-left directionality.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    RTL,
    /**
     * Directionality determined from <i>expression</i> contents.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    AUTO,
    /**
     * Directionality inherited from the <i>message</i> or from the <i>resolved value</i>
     * of the <i>operand</i> without requiring isolation of the <i>expression</i> value.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    INHERIT;

    /**
     * Determines the directionality appropriate for a given locale.
     *
     * @param ulocale the locale to determine the directionality from.
     * @return the appropriate directionality for the locale given.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static Directionality of(ULocale ulocale) {
        if (ulocale == null ) {
            return Directionality.INHERIT;
        }
        return ulocale.isRightToLeft() ? Directionality.RTL : Directionality.LTR;
    }
}
