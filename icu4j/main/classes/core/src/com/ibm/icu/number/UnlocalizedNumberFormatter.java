// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

public class UnlocalizedNumberFormatter extends NumberFormatterSettings<UnlocalizedNumberFormatter> {

    /** Base constructor; called during startup only. Sets the threshold to the default value of 3. */
    UnlocalizedNumberFormatter() {
        super(null, KEY_THRESHOLD, new Long(3));
    }

    UnlocalizedNumberFormatter(NumberFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    public LocalizedNumberFormatter locale(Locale locale) {
        return new LocalizedNumberFormatter(this, KEY_LOCALE, ULocale.forLocale(locale));
    }

    public LocalizedNumberFormatter locale(ULocale locale) {
        return new LocalizedNumberFormatter(this, KEY_LOCALE, locale);
    }

    @Override
    protected UnlocalizedNumberFormatter create(int key, Object value) {
        return new UnlocalizedNumberFormatter(this, key, value);
    }
}