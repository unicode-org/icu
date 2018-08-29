// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * The main entrypoint to the formatting of ranges of numbers, including currencies and other units of measurement.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public abstract class NumberRangeFormatter {

    public static enum RangeCollapse {}

    public static enum RangeIdentityFallback {}

    private static final UnlocalizedNumberRangeFormatter BASE = new UnlocalizedNumberRangeFormatter();

    public static UnlocalizedNumberRangeFormatter with() {
        return BASE;
    }

    public static LocalizedNumberRangeFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    public static LocalizedNumberRangeFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }

}
