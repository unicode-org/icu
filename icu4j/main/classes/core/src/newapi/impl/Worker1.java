// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Map;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.LdmlPatternInfo;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.Dimensionless;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.Grouping;
import newapi.NumberFormatter.NotationCompact;
import newapi.NumberFormatter.NotationScientific;
import newapi.NumberFormatter.Rounding;
import newapi.NumberFormatter.SignDisplay;

public class Worker1 {

  public static Worker1 fromMacros(MacroProps macros) {
    return new Worker1(make(macros, true));
  }

  public static MicroProps applyStatic(
      MacroProps macros, FormatQuantity inValue, NumberStringBuilder outString) {
    MicroProps micros = make(macros, false).withQuantity(inValue);
    applyStatic(micros, inValue, outString);
    return micros;
  }

  private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

  final QuantityChain microsGenerator;

  private Worker1(QuantityChain microsGenerator) {
    this.microsGenerator = microsGenerator;
  }

  public MicroProps apply(FormatQuantity inValue, NumberStringBuilder outString) {
    MicroProps micros = microsGenerator.withQuantity(inValue);
    applyStatic(micros, inValue, outString);
    return micros;
  }

  //////////

  private static QuantityChain make(MacroProps input, boolean build) {

    String innerPattern = null;
    Map<StandardPlural, Modifier> outerMods = null;
    Rounding defaultRounding = Rounding.NONE;
    Currency currency = DEFAULT_CURRENCY;
    FormatWidth unitWidth = null;
    boolean perMille = false;
    PluralRules rules = input.rules;

    MicroProps micros = new MicroProps();
    QuantityChain chain = micros;

    // Copy over the simple settings
    micros.sign = input.sign == null ? SignDisplay.AUTO : input.sign;
    micros.decimal = input.decimal == null ? DecimalMarkDisplay.AUTO : input.decimal;
    micros.multiplier = 0;
    micros.integerWidth =
        input.integerWidth == null
            ? IntegerWidthImpl.DEFAULT
            : (IntegerWidthImpl) input.integerWidth;

    if (input.unit == null || input.unit == Dimensionless.BASE) {
      // No units; default format
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
    } else if (input.unit == Dimensionless.PERCENT) {
      // Percent
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.PERCENTSTYLE);
      micros.multiplier += 2;
    } else if (input.unit == Dimensionless.PERMILLE) {
      // Permille
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.PERCENTSTYLE);
      micros.multiplier += 3;
      perMille = true;
    } else if (input.unit instanceof Currency && input.unitWidth != FormatWidth.WIDE) {
      // Narrow, short, or ISO currency.
      // TODO: Accounting style?
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.CURRENCYSTYLE);
      defaultRounding = Rounding.currency(CurrencyUsage.STANDARD);
      currency = (Currency) input.unit;
      micros.useCurrency = true;
      unitWidth = (input.unitWidth == null) ? FormatWidth.NARROW : input.unitWidth;
    } else if (input.unit instanceof Currency) {
      // Currency long name
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
      outerMods = DataUtils.getCurrencyLongNameModifiers(input.loc, (Currency) input.unit);
      defaultRounding = Rounding.currency(CurrencyUsage.STANDARD);
      currency = (Currency) input.unit;
      micros.useCurrency = true;
      unitWidth = input.unitWidth = FormatWidth.WIDE;
    } else {
      // MeasureUnit
      innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
      unitWidth = (input.unitWidth == null) ? FormatWidth.SHORT : input.unitWidth;
      outerMods = DataUtils.getMeasureUnitModifiers(input.loc, input.unit, unitWidth);
    }

    // Parse the pattern, which is used for grouping and affixes only.
    PatternParseResult patternInfo = LdmlPatternInfo.parse(innerPattern);

    // Symbols
    if (input.symbols == null) {
      micros.symbols = DecimalFormatSymbols.getInstance(input.loc);
    } else if (input.symbols instanceof DecimalFormatSymbols) {
      micros.symbols = (DecimalFormatSymbols) input.symbols;
    } else if (input.symbols instanceof NumberingSystem) {
      // TODO: Do this more efficiently. Will require modifying DecimalFormatSymbols.
      NumberingSystem ns = (NumberingSystem) input.symbols;
      ULocale temp = input.loc.setKeywordValue("numbers", ns.getName());
      micros.symbols = DecimalFormatSymbols.getInstance(temp);
    } else {
      throw new AssertionError();
    }

    // TODO: Normalize the currency (accept symbols from DecimalFormatSymbols)?
    // currency = CustomSymbolCurrency.resolve(currency, input.loc, micros.symbols);

    // Multiplier (compatibility mode value).
    // An int magnitude multiplier is used when not in compatibility mode to
    // reduce object creations.
    if (input.multiplier != null) {
      // TODO: Make sure this is thread safe.
      chain = input.multiplier.chain(chain);
    }

    // Rounding strategy
    if (input.rounding != null) {
      micros.rounding = RoundingImpl.normalizeType(input.rounding, currency);
    } else if (input.notation instanceof NotationCompact) {
      micros.rounding = RoundingImpl.RoundingImplFractionSignificant.COMPACT_STRATEGY;
    } else {
      micros.rounding = RoundingImpl.normalizeType(defaultRounding, currency);
    }

    // Grouping strategy
    if (input.grouping != null) {
      micros.grouping = GroupingImpl.normalizeType(input.grouping, patternInfo);
    } else if (input.notation instanceof NotationCompact) {
      // Compact notation uses minGrouping by default since ICU 59
      micros.grouping = GroupingImpl.normalizeType(Grouping.DEFAULT_MIN_2_DIGITS, patternInfo);
    } else {
      micros.grouping = GroupingImpl.normalizeType(Grouping.DEFAULT, patternInfo);
    }

    // Inner modifier (scientific notation)
    if (input.notation instanceof NotationScientific) {
      assert input.notation instanceof NotationImpl.NotationScientificImpl;
      chain =
          ScientificImpl.getInstance(
                  (NotationImpl.NotationScientificImpl) input.notation, micros.symbols, build)
              .chain(chain);
    } else {
      // No inner modifier required
      micros.modInner = ConstantAffixModifier.EMPTY;
    }

    // Middle modifier (patterns, positive/negative, currency symbols, percent)
    MurkyModifier murkyMod = new MurkyModifier(false);
    murkyMod.setPatternInfo((input.affixProvider != null) ? input.affixProvider : patternInfo);
    murkyMod.setPatternAttributes(micros.sign, perMille);
    if (murkyMod.needsPlurals()) {
      if (rules == null) {
        // Lazily create PluralRules
        rules = PluralRules.forLocale(input.loc);
      }
      murkyMod.setSymbols(micros.symbols, currency, unitWidth, rules);
    } else {
      murkyMod.setSymbols(micros.symbols, currency, unitWidth, null);
    }
    if (build) {
      chain = murkyMod.createImmutable().chain(chain);
    } else {
      chain = murkyMod.chain(chain);
    }

    // Outer modifier (CLDR units and currency long names)
    if (outerMods != null) {
      if (rules == null) {
        // Lazily create PluralRules
        rules = PluralRules.forLocale(input.loc);
      }
      chain = new QuantityDependentModOuter(outerMods, rules).chain(chain);
    } else {
      // No outer modifier required
      micros.modOuter = ConstantAffixModifier.EMPTY;
    }

    // Padding strategy
    if (input.padding != null) {
      micros.padding = (PaddingImpl) input.padding;
    } else {
      micros.padding = PaddingImpl.NONE;
    }

    // Compact notation
    // NOTE: Compact notation can (but might not) override the middle modifier and rounding.
    // It therefore needs to go at the end of the chain.
    if (input.notation instanceof NotationCompact) {
      assert input.notation instanceof NotationImpl.NotationCompactImpl;
      if (rules == null) {
        // Lazily create PluralRules
        rules = PluralRules.forLocale(input.loc);
      }
      CompactStyle compactStyle = ((NotationImpl.NotationCompactImpl) input.notation).compactStyle;
      CompactImpl worker;
      if (compactStyle == null) {
        // Use compact custom data
        worker =
            CompactImpl.getInstance(
                ((NotationImpl.NotationCompactImpl) input.notation).compactCustomData, rules);
      } else {
        CompactType compactType =
            (input.unit instanceof Currency) ? CompactType.CURRENCY : CompactType.DECIMAL;
        worker = CompactImpl.getInstance(input.loc, compactType, compactStyle, rules);
      }
      if (build) {
        worker.precomputeAllModifiers(murkyMod);
      }
      chain = worker.chain(chain);
    }

    if (build) {
      micros.enableCloneInChain();
    }

    return chain;
  }

  //////////

  private static int applyStatic(
      MicroProps micros, FormatQuantity inValue, NumberStringBuilder outString) {
    inValue.adjustMagnitude(micros.multiplier);
    micros.rounding.apply(inValue);
    inValue.setIntegerLength(micros.integerWidth.minInt, micros.integerWidth.maxInt);
    int length = PositiveDecimalImpl.apply(micros, inValue, outString);
    // NOTE: When range formatting is added, these modifiers can bubble up.
    // For now, apply them all here at once.
    length += micros.padding.applyModsAndMaybePad(micros, outString, 0, length);
    return length;
  }
}
