// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

public final class NumberFormatter {

    private static final UnlocalizedNumberFormatter BASE = new UnlocalizedNumberFormatter();

    public static enum UnitWidth {
        NARROW, // ¤¤¤¤¤ or narrow measure unit
        SHORT, // ¤ or short measure unit (DEFAULT)
        ISO_CODE, // ¤¤; undefined for measure unit
        FULL_NAME, // ¤¤¤ or wide unit
        HIDDEN, // no unit is displayed, but other unit effects are obeyed (like currency rounding)
        // TODO: For hidden, what to do if currency symbol appears in the middle, as in Portugal ?
    }

    public static enum DecimalMarkDisplay {
        AUTO, ALWAYS,
    }

    public static enum SignDisplay {
        AUTO, ALWAYS, NEVER, ACCOUNTING, ACCOUNTING_ALWAYS,
    }

    public static UnlocalizedNumberFormatter fromSkeleton(String skeleton) {
        // FIXME
        throw new UnsupportedOperationException();
    }

    public static UnlocalizedNumberFormatter with() {
        return BASE;
    }

    public static LocalizedNumberFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    public static LocalizedNumberFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }
}
