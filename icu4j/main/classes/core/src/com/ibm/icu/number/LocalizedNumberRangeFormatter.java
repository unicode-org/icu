// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.range.RangeMacroProps;
import com.ibm.icu.number.FormattedNumberRange.RangeIdentityType;

/**
 * A NumberRangeFormatter that has a locale associated with it; this means .formatRange() methods are available.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberRangeFormatter
 */
public class LocalizedNumberRangeFormatter extends NumberRangeFormatterSettings<LocalizedNumberRangeFormatter> {

    LocalizedNumberRangeFormatter(NumberRangeFormatterSettings<?> parent, int key, Object value) {
        super(parent, key, value);
    }

    /**
     * Format the given integers to a string using the settings specified in the NumberRangeFormatter fluent setting
     * chain.
     *
     * @param first
     *            The first number in the range, usually to the left in LTR locales.
     * @param second
     *            The second number in the range, usually to the right in LTR locales.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    public FormattedNumberRange formatRange(int first, int second) {
        DecimalQuantity dq1 = new DecimalQuantity_DualStorageBCD(first);
        DecimalQuantity dq2 = new DecimalQuantity_DualStorageBCD(second);
        return formatImpl(dq1, dq2, first == second);
    }

    /**
     * Format the given doubles to a string using the settings specified in the NumberRangeFormatter fluent setting
     * chain.
     *
     * @param first
     *            The first number in the range, usually to the left in LTR locales.
     * @param second
     *            The second number in the range, usually to the right in LTR locales.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    public FormattedNumberRange formatRange(double first, double second) {
        DecimalQuantity dq1 = new DecimalQuantity_DualStorageBCD(first);
        DecimalQuantity dq2 = new DecimalQuantity_DualStorageBCD(second);
        // Note: double equality could be changed to epsilon equality later if there is demand.
        // The epsilon should be set via an API method.
        return formatImpl(dq1, dq2, first == second);
    }

    /**
     * Format the given Numbers to a string using the settings specified in the NumberRangeFormatter fluent setting
     * chain.
     *
     * @param first
     *            The first number in the range, usually to the left in LTR locales.
     * @param second
     *            The second number in the range, usually to the right in LTR locales.
     * @return A FormattedNumber object; call .toString() to get the string.
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    public FormattedNumberRange formatRange(Number first, Number second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Cannot format null values in range");
        }
        DecimalQuantity dq1 = new DecimalQuantity_DualStorageBCD(first);
        DecimalQuantity dq2 = new DecimalQuantity_DualStorageBCD(second);
        return formatImpl(dq1, dq2, first.equals(second));
    }

    FormattedNumberRange formatImpl(DecimalQuantity first, DecimalQuantity second, boolean equalBeforeRounding) {
        // TODO: This is a placeholder implementation.
        RangeMacroProps macros = resolve();
        LocalizedNumberFormatter f1 , f2;
        if (macros.formatter1 != null) {
            f1 = macros.formatter1.locale(macros.loc);
        } else {
            f1 = NumberFormatter.withLocale(macros.loc);
        }
        if (macros.formatter2 != null) {
            f2 = macros.formatter2.locale(macros.loc);
        } else {
            f2 = NumberFormatter.withLocale(macros.loc);
        }
        FormattedNumber r1 = f1.format(first);
        FormattedNumber r2 = f2.format(second);
        NumberStringBuilder nsb = new NumberStringBuilder();
        nsb.append(r1.nsb);
        nsb.append(" --- ", null);
        nsb.append(r2.nsb);
        RangeIdentityType identityType = equalBeforeRounding ? RangeIdentityType.EQUAL_BEFORE_ROUNDING
                : RangeIdentityType.NOT_EQUAL;
        return new FormattedNumberRange(nsb, first, second, identityType);
    }

    @Override
    LocalizedNumberRangeFormatter create(int key, Object value) {
        return new LocalizedNumberRangeFormatter(this, key, value);
    }

}
