// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.number.range.RangeMacroProps;
import com.ibm.icu.number.NumberRangeFormatter.RangeCollapse;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityFallback;
import com.ibm.icu.util.ULocale;

/**
 * An abstract base class for specifying settings related to number formatting. This class is implemented by
 * {@link UnlocalizedNumberRangeFormatter} and {@link LocalizedNumberRangeFormatter}. This class is not intended for
 * public subclassing.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberRangeFormatter
 */
public abstract class NumberRangeFormatterSettings<T extends NumberRangeFormatterSettings<?>> {

    static final int KEY_MACROS = 0; // not used
    static final int KEY_LOCALE = 1;
    static final int KEY_FORMATTER_1 = 2;
    static final int KEY_FORMATTER_2 = 3;
    static final int KEY_COLLAPSE = 4;
    static final int KEY_IDENTITY_FALLBACK = 5;
    static final int KEY_MAX = 6;

    final NumberRangeFormatterSettings<?> parent;
    final int key;
    final Object value;
    volatile RangeMacroProps resolvedMacros;

    NumberRangeFormatterSettings(NumberRangeFormatterSettings<?> parent, int key, Object value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    public T numberFormatter(UnlocalizedNumberFormatter formatter) {
        return numberFormatters(formatter, formatter);
    }

    public T numberFormatters(UnlocalizedNumberFormatter formatterFirst, UnlocalizedNumberFormatter formatterSecond) {
        T intermediate = create(KEY_FORMATTER_1, formatterFirst);
        return (T) intermediate.create(KEY_FORMATTER_2, formatterSecond);
    }

    public T collapse(RangeCollapse collapse) {
        return create(KEY_COLLAPSE, collapse);
    }

    public T identityFallback(RangeIdentityFallback identityFallback) {
        return create(KEY_IDENTITY_FALLBACK, identityFallback);
    }

    /* package-protected */ abstract T create(int key, Object value);

    RangeMacroProps resolve() {
        if (resolvedMacros != null) {
            return resolvedMacros;
        }
        // Although the linked-list fluent storage approach requires this method,
        // my benchmarks show that linked-list is still faster than a full clone
        // of a MacroProps object at each step.
        // TODO: Remove the reference to the parent after the macros are resolved?
        RangeMacroProps macros = new RangeMacroProps();
        NumberRangeFormatterSettings<?> current = this;
        while (current != null) {
            switch (current.key) {
            case KEY_MACROS:
                // ignored for now
                break;
            case KEY_LOCALE:
                if (macros.loc == null) {
                    macros.loc = (ULocale) current.value;
                }
                break;
            case KEY_FORMATTER_1:
                if (macros.formatter1 == null) {
                    macros.formatter1 = (UnlocalizedNumberFormatter) current.value;
                }
                break;
            case KEY_FORMATTER_2:
                if (macros.formatter2 == null) {
                    macros.formatter2 = (UnlocalizedNumberFormatter) current.value;
                }
                break;
            case KEY_COLLAPSE:
                if (macros.collapse == null) {
                    macros.collapse = (RangeCollapse) current.value;
                }
                break;
            case KEY_IDENTITY_FALLBACK:
                if (macros.identityFallback == null) {
                    macros.identityFallback = (RangeIdentityFallback) current.value;
                }
                break;
            default:
                throw new AssertionError("Unknown key: " + current.key);
            }
            current = current.parent;
        }
        resolvedMacros = macros;
        return macros;
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int hashCode() {
        return resolve().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof NumberRangeFormatterSettings)) {
            return false;
        }
        return resolve().equals(((NumberRangeFormatterSettings<?>) other).resolve());
    }
}
