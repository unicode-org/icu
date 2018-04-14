// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.Scale;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Rounder;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class MacroProps implements Cloneable {
    public Notation notation;
    public MeasureUnit unit;
    public MeasureUnit perUnit;
    public Rounder rounder;
    public Object grouping;
    public Padder padder;
    public IntegerWidth integerWidth;
    public Object symbols;
    public UnitWidth unitWidth;
    public SignDisplay sign;
    public DecimalSeparatorDisplay decimal;
    public Scale scale;
    public AffixPatternProvider affixProvider; // not in API; for JDK compatibility mode only
    public PluralRules rules; // not in API; could be made public in the future
    public Long threshold; // not in API; controls internal self-regulation threshold
    public ULocale loc;

    /**
     * Copies values from fallback into this instance if they are null in this instance.
     *
     * @param fallback
     *            The instance to copy from; not modified by this operation.
     */
    public void fallback(MacroProps fallback) {
        if (notation == null)
            notation = fallback.notation;
        if (unit == null)
            unit = fallback.unit;
        if (perUnit == null)
            perUnit = fallback.perUnit;
        if (rounder == null)
            rounder = fallback.rounder;
        if (grouping == null)
            grouping = fallback.grouping;
        if (padder == null)
            padder = fallback.padder;
        if (integerWidth == null)
            integerWidth = fallback.integerWidth;
        if (symbols == null)
            symbols = fallback.symbols;
        if (unitWidth == null)
            unitWidth = fallback.unitWidth;
        if (sign == null)
            sign = fallback.sign;
        if (decimal == null)
            decimal = fallback.decimal;
        if (affixProvider == null)
            affixProvider = fallback.affixProvider;
        if (scale == null)
            scale = fallback.scale;
        if (rules == null)
            rules = fallback.rules;
        if (loc == null)
            loc = fallback.loc;
    }

    @Override
    public int hashCode() {
        return Utility.hash(notation,
                unit,
                perUnit,
                rounder,
                grouping,
                padder,
                integerWidth,
                symbols,
                unitWidth,
                sign,
                decimal,
                affixProvider,
                scale,
                rules,
                loc);
    }

    @Override
    public boolean equals(Object _other) {
        if (_other == null)
            return false;
        if (this == _other)
            return true;
        if (!(_other instanceof MacroProps))
            return false;
        MacroProps other = (MacroProps) _other;
        return Utility.equals(notation, other.notation)
                && Utility.equals(unit, other.unit)
                && Utility.equals(perUnit, other.perUnit)
                && Utility.equals(rounder, other.rounder)
                && Utility.equals(grouping, other.grouping)
                && Utility.equals(padder, other.padder)
                && Utility.equals(integerWidth, other.integerWidth)
                && Utility.equals(symbols, other.symbols)
                && Utility.equals(unitWidth, other.unitWidth)
                && Utility.equals(sign, other.sign)
                && Utility.equals(decimal, other.decimal)
                && Utility.equals(affixProvider, other.affixProvider)
                && Utility.equals(scale, other.scale)
                && Utility.equals(rules, other.rules)
                && Utility.equals(loc, other.loc);
    }

    @Override
    public Object clone() {
        // TODO: Remove this method?
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
