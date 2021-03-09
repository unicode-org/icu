// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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
 * @stable ICU 63
 * @see NumberRangeFormatter
 */
public abstract class NumberRangeFormatterSettings<T extends NumberRangeFormatterSettings<?>> {

    static final int KEY_MACROS = 0; // not used
    static final int KEY_LOCALE = 1;
    static final int KEY_FORMATTER_1 = 2;
    static final int KEY_FORMATTER_2 = 3;
    static final int KEY_SAME_FORMATTERS = 4;
    static final int KEY_COLLAPSE = 5;
    static final int KEY_IDENTITY_FALLBACK = 6;
    static final int KEY_MAX = 7;

    private final NumberRangeFormatterSettings<?> parent;
    private final int key;
    private final Object value;
    private volatile RangeMacroProps resolvedMacros;

    NumberRangeFormatterSettings(NumberRangeFormatterSettings<?> parent, int key, Object value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    /**
     * Sets the NumberFormatter instance to use for the numbers in the range. The same formatter is applied to both
     * sides of the range.
     * <p>
     * The NumberFormatter instances must not have a locale applied yet; the locale specified on the
     * NumberRangeFormatter will be used.
     *
     * @param formatter
     *            The formatter to use for both numbers in the range.
     * @return The fluent chain.
     * @stable ICU 63
     * @see NumberFormatter
     * @see NumberRangeFormatter
     */
    @SuppressWarnings("unchecked")
    public T numberFormatterBoth(UnlocalizedNumberFormatter formatter) {
        return (T) create(KEY_SAME_FORMATTERS, true).create(KEY_FORMATTER_1, formatter);
    }

    /**
     * Sets the NumberFormatter instance to use for the first number in the range.
     * <p>
     * The NumberFormatter instance must not have a locale applied yet; the locale specified on the
     * NumberRangeFormatter will be used.
     *
     * @param formatterFirst
     *            The formatter to use for the first number in the range.
     * @return The fluent chain.
     * @stable ICU 63
     * @see NumberFormatter
     * @see NumberRangeFormatter
     */
    @SuppressWarnings("unchecked")
    public T numberFormatterFirst(UnlocalizedNumberFormatter formatterFirst) {
        return (T) create(KEY_SAME_FORMATTERS, false).create(KEY_FORMATTER_1, formatterFirst);
    }

    /**
     * Sets the NumberFormatter instances to use for the second number in the range.
     * <p>
     * The NumberFormatter instance must not have a locale applied yet; the locale specified on the
     * NumberRangeFormatter will be used.
     *
     * @param formatterSecond
     *            The formatter to use for the second number in the range.
     * @return The fluent chain.
     * @stable ICU 63
     * @see NumberFormatter
     * @see NumberRangeFormatter
     */
    @SuppressWarnings("unchecked")
    public T numberFormatterSecond(UnlocalizedNumberFormatter formatterSecond) {
        return (T) create(KEY_SAME_FORMATTERS, false).create(KEY_FORMATTER_2, formatterSecond);
    }

    /**
     * Sets the aggressiveness of "collapsing" fields across the range separator. Possible values:
     * <ul>
     * <li>ALL: "3-5K miles"</li>
     * <li>UNIT: "3K - 5K miles"</li>
     * <li>NONE: "3K miles - 5K miles"</li>
     * <li>AUTO: usually UNIT or NONE, depending on the locale and formatter settings</li>
     * </ul>
     * <p>
     * The default value is AUTO.
     *
     * @param collapse
     *            The collapsing strategy to use for this range.
     * @return The fluent chain.
     * @stable ICU 63
     * @see NumberRangeFormatter
     */
    public T collapse(RangeCollapse collapse) {
        return create(KEY_COLLAPSE, collapse);
    }

    /**
     * Sets the behavior when the two sides of the range are the same. This could happen if the same two numbers are
     * passed to the formatRange function, or if different numbers are passed to the function but they become the same
     * after rounding rules are applied. Possible values:
     * <ul>
     * <li>SINGLE_VALUE: "5 miles"</li>
     * <li>APPROXIMATELY_OR_SINGLE_VALUE: "~5 miles" or "5 miles", depending on whether the number was the same before
     * rounding was applied</li>
     * <li>APPROXIMATELY: "~5 miles"</li>
     * <li>RANGE: "5-5 miles" (with collapse=UNIT)</li>
     * </ul>
     * <p>
     * The default value is APPROXIMATELY.
     *
     * @param identityFallback
     *            The strategy to use when formatting two numbers that end up being the same.
     * @return The fluent chain.
     * @stable ICU 63
     * @see NumberRangeFormatter
     */
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
        // Bitmap: 1 if seen; 0 if unseen
        long seen = 0;
        NumberRangeFormatterSettings<?> current = this;
        while (current != null) {
            long keyBitmask = (1L << current.key);
            if (0 != (seen & keyBitmask)) {
                current = current.parent;
                continue;
            }
            seen |= keyBitmask;
            switch (current.key) {
            case KEY_MACROS:
                // ignored for now
                break;
            case KEY_LOCALE:
                macros.loc = (ULocale) current.value;
                break;
            case KEY_FORMATTER_1:
                macros.formatter1 = (UnlocalizedNumberFormatter) current.value;
                break;
            case KEY_FORMATTER_2:
                macros.formatter2 = (UnlocalizedNumberFormatter) current.value;
                break;
            case KEY_SAME_FORMATTERS:
                macros.sameFormatters = (boolean) current.value ? 1 : 0;
                break;
            case KEY_COLLAPSE:
                macros.collapse = (RangeCollapse) current.value;
                break;
            case KEY_IDENTITY_FALLBACK:
                macros.identityFallback = (RangeIdentityFallback) current.value;
                break;
            default:
                throw new AssertionError("Unknown key: " + current.key);
            }
            current = current.parent;
        }
        // Copy the locale into the children (see touchRangeLocales in C++)
        if (macros.formatter1 != null) {
            macros.formatter1.resolve().loc = macros.loc;
        }
        if (macros.formatter2 != null) {
            macros.formatter2.resolve().loc = macros.loc;
        }
        resolvedMacros = macros;
        return macros;
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 63
     */
    @Override
    public int hashCode() {
        return resolve().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @stable ICU 63
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
