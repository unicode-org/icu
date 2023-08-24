// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.range;

import java.util.Objects;

import com.ibm.icu.number.NumberRangeFormatter.RangeCollapse;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityFallback;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class RangeMacroProps {
    public UnlocalizedNumberFormatter formatter1;
    public UnlocalizedNumberFormatter formatter2;
    public int sameFormatters = -1; // -1 for unset, 0 for false, 1 for true
    public RangeCollapse collapse;
    public RangeIdentityFallback identityFallback;
    public ULocale loc;

    @Override
    public int hashCode() {
        return Objects.hash(formatter1,
                formatter2,
                collapse,
                identityFallback,
                loc);
    }

    @Override
    public boolean equals(Object _other) {
        if (_other == null)
            return false;
        if (this == _other)
            return true;
        if (!(_other instanceof RangeMacroProps))
            return false;
        RangeMacroProps other = (RangeMacroProps) _other;
        return Objects.equals(formatter1, other.formatter1)
                && Objects.equals(formatter2, other.formatter2)
                && Objects.equals(collapse, other.collapse)
                && Objects.equals(identityFallback, other.identityFallback)
                && Objects.equals(loc, other.loc);
    }
}
