package com.ibm.icu.message2;

import com.ibm.icu.util.ULocale;

// TODO 77: document
public enum Directionality {
    UNKNOWN, LTR, RTL, AUTO, INHERIT;

    public static Directionality of(ULocale ulocale) {
        if (ulocale == null ) {
            return Directionality.INHERIT;
        }
        return ulocale.isRightToLeft() ? Directionality.RTL : Directionality.LTR;
    }
}
