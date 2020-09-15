// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * A NumberFormatter that does not yet have a locale. In order to format numbers, a locale must be
 * specified.
 *
 * Instances of this class are immutable and thread-safe.
 *
 * @see NumberFormatter
 * @stable ICU 60
 */
public class UnlocalizedNumberFormatter extends NumberFormatterSettings<UnlocalizedNumberFormatter> {

    /** Base constructor; called during startup only. Sets the threshold to the default value of 3. */
    UnlocalizedNumberFormatter() {
        super(null, KEY_THRESHOLD, new Long(3));
    }

    UnlocalizedNumberFormatter(NumberFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    /**
     * Associate the given locale with the number formatter. The locale is used for picking the
     * appropriate symbols, formats, and other data for number display.
     *
     * <p>
     * To use the Java default locale, call Locale.getDefault():
     *
     * <pre>
     * NumberFormatter.with(). ... .locale(Locale.getDefault())
     * </pre>
     *
     * @param locale
     *            The locale to use when loading data for number formatting.
     * @return The fluent chain
     * @stable ICU 60
     */
    public LocalizedNumberFormatter locale(Locale locale) {
        return new LocalizedNumberFormatter(this, KEY_LOCALE, ULocale.forLocale(locale));
    }

    /**
     * ULocale version of the {@link #locale(Locale)} setter above.
     *
     * @param locale
     *            The locale to use when loading data for number formatting.
     * @return The fluent chain
     * @see #locale(Locale)
     * @stable ICU 60
     */
    public LocalizedNumberFormatter locale(ULocale locale) {
        return new LocalizedNumberFormatter(this, KEY_LOCALE, locale);
    }

    @Override
    UnlocalizedNumberFormatter create(int key, Object value) {
        return new UnlocalizedNumberFormatter(this, key, value);
    }
}
