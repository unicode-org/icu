// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Objects;

import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.IGrouping;
import newapi.NumberFormatter.IRounding;
import newapi.NumberFormatter.IntegerWidth;
import newapi.NumberFormatter.Notation;
import newapi.NumberFormatter.Padding;
import newapi.NumberFormatter.SignDisplay;

public class MacroProps implements Cloneable {
  public Notation notation;
  public MeasureUnit unit;
  public IRounding rounding;
  public IGrouping grouping;
  public Padding padding;
  public IntegerWidth integerWidth;
  public Object symbols;
  public FormatWidth unitWidth;
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
    if (rounding == null) rounding = fallback.rounding;
    if (grouping == null) grouping = fallback.grouping;
    if (padding == null) padding = fallback.padding;
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
        rounding,
        grouping,
        padding,
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
        && Objects.equals(rounding, other.rounding)
        && Objects.equals(grouping, other.grouping)
        && Objects.equals(padding, other.padding)
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
