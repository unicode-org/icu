// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.number.FormattedNumberRange.RangeIdentityType;
import com.ibm.icu.text.NumberFormat;

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
        // TODO: This is a placeholder implementation.
        DecimalQuantity dq1 = new DecimalQuantity_DualStorageBCD(first);
        DecimalQuantity dq2 = new DecimalQuantity_DualStorageBCD(second);
        NumberStringBuilder nsb = new NumberStringBuilder();
        nsb.append(dq1.toPlainString(), NumberFormat.Field.INTEGER);
        nsb.append(" --- ", null);
        nsb.append(dq2.toPlainString(), NumberFormat.Field.INTEGER);
        RangeIdentityType identityType = (first == second) ? RangeIdentityType.EQUAL_BEFORE_ROUNDING
                : RangeIdentityType.NOT_EQUAL;
        return new FormattedNumberRange(nsb, dq1, dq2, identityType);
    }

    @Override
    LocalizedNumberRangeFormatter create(int key, Object value) {
        return new LocalizedNumberRangeFormatter(this, key, value);
    }

}
