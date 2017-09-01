// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Objects;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.Grouper;
import newapi.IntegerWidth;
import newapi.Notation;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.Rounder;

public class MacroProps implements Cloneable {
  public Notation notation;
  public MeasureUnit unit;
  public Rounder rounder;
  public Grouper grouper;
  public Padder padder;
  public IntegerWidth integerWidth;
  public Object symbols;
  public UnitWidth unitWidth;
  public SignDisplay sign;
  public DecimalMarkDisplay decimal;
  public AffixPatternProvider affixProvider; // not in API; for JDK compatibility mode only
  public MultiplierImpl multiplier; // not in API; for JDK compatibility mode only
  public PluralRules rules; // not in API; could be made public in the future
  public Long threshold; // not in API; controls internal self-regulation threshold
  public ULocale loc;

  /**
   * Copies values from fallback into this instance if they are null in this instance.
   *
   * @param fallback The instance to copy from; not modified by this operation.
   */
  public void fallback(MacroProps fallback) {
    if (notation == null) notation = fallback.notation;
    if (unit == null) unit = fallback.unit;
    if (rounder == null) rounder = fallback.rounder;
    if (grouper == null) grouper = fallback.grouper;
    if (padder == null) padder = fallback.padder;
    if (integerWidth == null) integerWidth = fallback.integerWidth;
    if (symbols == null) symbols = fallback.symbols;
    if (unitWidth == null) unitWidth = fallback.unitWidth;
    if (sign == null) sign = fallback.sign;
    if (decimal == null) decimal = fallback.decimal;
    if (affixProvider == null) affixProvider = fallback.affixProvider;
    if (multiplier == null) multiplier = fallback.multiplier;
    if (rules == null) rules = fallback.rules;
    if (loc == null) loc = fallback.loc;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        notation,
        unit,
        rounder,
        grouper,
        padder,
        integerWidth,
        symbols,
        unitWidth,
        sign,
        decimal,
        affixProvider,
        multiplier,
        rules,
        loc);
  }

  @Override
  public boolean equals(Object _other) {
    MacroProps other = (MacroProps) _other;
    return Objects.equals(notation, other.notation)
        && Objects.equals(unit, other.unit)
        && Objects.equals(rounder, other.rounder)
        && Objects.equals(grouper, other.grouper)
        && Objects.equals(padder, other.padder)
        && Objects.equals(integerWidth, other.integerWidth)
        && Objects.equals(symbols, other.symbols)
        && Objects.equals(unitWidth, other.unitWidth)
        && Objects.equals(sign, other.sign)
        && Objects.equals(decimal, other.decimal)
        && Objects.equals(affixProvider, other.affixProvider)
        && Objects.equals(multiplier, other.multiplier)
        && Objects.equals(rules, other.rules)
        && Objects.equals(loc, other.loc);
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
