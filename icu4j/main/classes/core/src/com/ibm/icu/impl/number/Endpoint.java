// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.number.Format.BeforeTargetAfterFormat;
import com.ibm.icu.impl.number.Format.SingularFormat;
import com.ibm.icu.impl.number.Format.TargetFormat;
import com.ibm.icu.impl.number.formatters.BigDecimalMultiplier;
import com.ibm.icu.impl.number.formatters.CompactDecimalFormat;
import com.ibm.icu.impl.number.formatters.CurrencyFormat;
import com.ibm.icu.impl.number.formatters.MagnitudeMultiplier;
import com.ibm.icu.impl.number.formatters.MeasureFormat;
import com.ibm.icu.impl.number.formatters.PaddingFormat;
import com.ibm.icu.impl.number.formatters.PositiveDecimalFormat;
import com.ibm.icu.impl.number.formatters.PositiveNegativeAffixFormat;
import com.ibm.icu.impl.number.formatters.RoundingFormat;
import com.ibm.icu.impl.number.formatters.ScientificFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

public class Endpoint {
  //  public static Format from(DecimalFormatSymbols symbols, Properties properties)
  //      throws ParseException {
  //    Format format = new PositiveIntegerFormat(symbols, properties);
  //    // TODO: integer-only format
  //    format = new PositiveDecimalFormat((SelfContainedFormat) format, symbols, properties);
  //    if (properties.useCompactDecimalFormat()) {
  //      format = CompactDecimalFormat.getInstance((SingularFormat) format, symbols, properties);
  //    } else {
  //      format =
  //          PositiveNegativeAffixFormat.getInstance((SingularFormat) format, symbols, properties);
  //    }
  //    if (properties.useRoundingInterval()) {
  //      format = new IntervalRoundingFormat((SingularFormat) format, properties);
  //    } else if (properties.useSignificantDigits()) {
  //      format = new SignificantDigitsFormat((SingularFormat) format, properties);
  //    } else if (properties.useFractionFormat()) {
  //      format = new RoundingFormat((SingularFormat) format, properties);
  //    }
  //    return format;
  //  }

  public static Format fromBTA(Properties properties) {
    return fromBTA(properties, getSymbols());
  }

  public static SingularFormat fromBTA(Properties properties, Locale locale) {
    return fromBTA(properties, getSymbols(locale));
  }

  public static SingularFormat fromBTA(Properties properties, ULocale uLocale) {
    return fromBTA(properties, getSymbols(uLocale));
  }

  public static SingularFormat fromBTA(String pattern) {
    return fromBTA(getProperties(pattern), getSymbols());
  }

  public static SingularFormat fromBTA(String pattern, Locale locale) {
    return fromBTA(getProperties(pattern), getSymbols(locale));
  }

  public static SingularFormat fromBTA(String pattern, ULocale uLocale) {
    return fromBTA(getProperties(pattern), getSymbols(uLocale));
  }

  public static SingularFormat fromBTA(String pattern, DecimalFormatSymbols symbols) {
    return fromBTA(getProperties(pattern), symbols);
  }

  public static SingularFormat fromBTA(Properties properties, DecimalFormatSymbols symbols) {

    if (symbols == null) throw new IllegalArgumentException("symbols must not be null");

    // TODO: This fast track results in an improvement of about 10ns during formatting.  See if
    // there is a way to implement it more elegantly.
    boolean canUseFastTrack = true;
    PluralRules rules = getPluralRules(symbols.getULocale(), properties);
    BeforeTargetAfterFormat format = new Format.BeforeTargetAfterFormat(rules);
    TargetFormat target = new PositiveDecimalFormat(symbols, properties);
    format.setTargetFormat(target);
    // TODO: integer-only format?
    if (MagnitudeMultiplier.useMagnitudeMultiplier(properties)) {
      canUseFastTrack = false;
      format.addBeforeFormat(MagnitudeMultiplier.getInstance(properties));
    }
    if (BigDecimalMultiplier.useMultiplier(properties)) {
      canUseFastTrack = false;
      format.addBeforeFormat(BigDecimalMultiplier.getInstance(properties));
    }
    if (MeasureFormat.useMeasureFormat(properties)) {
      canUseFastTrack = false;
      format.addBeforeFormat(MeasureFormat.getInstance(symbols, properties));
    }
    if (CurrencyFormat.useCurrency(properties)) {
      canUseFastTrack = false;
      if (CompactDecimalFormat.useCompactDecimalFormat(properties)) {
        format.addBeforeFormat(CompactDecimalFormat.getInstance(symbols, properties));
      } else if (ScientificFormat.useScientificNotation(properties)) {
          // TODO: Should the currency rounder or scientific rounder be used in this case?
          // For now, default to using the scientific rounder.
        format.addBeforeFormat(PositiveNegativeAffixFormat.getInstance(symbols, properties));
        format.addBeforeFormat(ScientificFormat.getInstance(symbols, properties));
      } else {
        format.addBeforeFormat(CurrencyFormat.getCurrencyRounder(symbols, properties));
        format.addBeforeFormat(CurrencyFormat.getCurrencyModifier(symbols, properties));
      }
    } else {
      if (CompactDecimalFormat.useCompactDecimalFormat(properties)) {
        canUseFastTrack = false;
        format.addBeforeFormat(CompactDecimalFormat.getInstance(symbols, properties));
      } else if (ScientificFormat.useScientificNotation(properties)) {
        canUseFastTrack = false;
        format.addBeforeFormat(PositiveNegativeAffixFormat.getInstance(symbols, properties));
        format.addBeforeFormat(ScientificFormat.getInstance(symbols, properties));
      } else {
        format.addBeforeFormat(PositiveNegativeAffixFormat.getInstance(symbols, properties));
        format.addBeforeFormat(RoundingFormat.getDefaultOrNoRounder(properties));
      }
    }
    if (PaddingFormat.usePadding(properties)) {
      canUseFastTrack = false;
      format.addAfterFormat(PaddingFormat.getInstance(properties));
    }
    if (canUseFastTrack) {
      return new Format.PositiveNegativeRounderTargetFormat(
          PositiveNegativeAffixFormat.getInstance(symbols, properties),
          RoundingFormat.getDefaultOrNoRounder(properties),
          target);
    } else {
      return format;
    }
  }

  public static String staticFormat(FormatQuantity input, Properties properties) {
    return staticFormat(input, properties, getSymbols());
  }

  public static String staticFormat(FormatQuantity input, Properties properties, Locale locale) {
    return staticFormat(input, properties, getSymbols(locale));
  }

  public static String staticFormat(FormatQuantity input, Properties properties, ULocale uLocale) {
    return staticFormat(input, properties, getSymbols(uLocale));
  }

  public static String staticFormat(FormatQuantity input, String pattern) {
    return staticFormat(input, getProperties(pattern), getSymbols());
  }

  public static String staticFormat(FormatQuantity input, String pattern, Locale locale) {
    return staticFormat(input, getProperties(pattern), getSymbols(locale));
  }

  public static String staticFormat(FormatQuantity input, String pattern, ULocale uLocale) {
    return staticFormat(input, getProperties(pattern), getSymbols(uLocale));
  }

  public static String staticFormat(
      FormatQuantity input, String pattern, DecimalFormatSymbols symbols) {
    return staticFormat(input, getProperties(pattern), symbols);
  }

  public static String staticFormat(
      FormatQuantity input, Properties properties, DecimalFormatSymbols symbols) {
    PluralRules rules = null;
    ModifierHolder mods = Format.threadLocalModifierHolder.get().clear();
    NumberStringBuilder sb = Format.threadLocalStringBuilder.get().clear();
    int length = 0;

    // Pre-processing
    if (!input.isNaN()) {
      if (MagnitudeMultiplier.useMagnitudeMultiplier(properties)) {
        MagnitudeMultiplier.getInstance(properties).before(input, mods, rules);
      }
      if (BigDecimalMultiplier.useMultiplier(properties)) {
        BigDecimalMultiplier.getInstance(properties).before(input, mods, rules);
      }
      if (MeasureFormat.useMeasureFormat(properties)) {
        rules = (rules != null) ? rules : getPluralRules(symbols.getULocale(), properties);
        MeasureFormat.getInstance(symbols, properties).before(input, mods, rules);
      }
      if (CompactDecimalFormat.useCompactDecimalFormat(properties)) {
        rules = (rules != null) ? rules : getPluralRules(symbols.getULocale(), properties);
        CompactDecimalFormat.apply(input, mods, rules, symbols, properties);
      } else if (CurrencyFormat.useCurrency(properties)) {
        rules = (rules != null) ? rules : getPluralRules(symbols.getULocale(), properties);
        CurrencyFormat.getCurrencyRounder(symbols, properties).before(input, mods, rules);
        CurrencyFormat.getCurrencyModifier(symbols, properties).before(input, mods, rules);
      } else if (ScientificFormat.useScientificNotation(properties)) {
        // TODO: Is it possible to combine significant digits with currency?
        PositiveNegativeAffixFormat.getInstance(symbols, properties).before(input, mods, rules);
        ScientificFormat.getInstance(symbols, properties).before(input, mods, rules);
      } else {
        PositiveNegativeAffixFormat.apply(input, mods, symbols, properties);
        RoundingFormat.getDefaultOrNoRounder(properties).before(input, mods, rules);
      }
    }

    // Primary format step
    length += new PositiveDecimalFormat(symbols, properties).target(input, sb, 0);
    length += mods.applyStrong(sb, 0, length);

    // Post-processing
    if (PaddingFormat.usePadding(properties)) {
      length += PaddingFormat.getInstance(properties).after(mods, sb, 0, length);
    }
    length += mods.applyAll(sb, 0, length);

    return sb.toString();
  }

  private static final ThreadLocal<Map<ULocale, DecimalFormatSymbols>> threadLocalSymbolsCache =
      new ThreadLocal<Map<ULocale, DecimalFormatSymbols>>() {
        @Override
        protected Map<ULocale, DecimalFormatSymbols> initialValue() {
          return new HashMap<ULocale, DecimalFormatSymbols>();
        }
      };

  private static DecimalFormatSymbols getSymbols() {
    ULocale uLocale = ULocale.getDefault();
    return getSymbols(uLocale);
  }

  private static DecimalFormatSymbols getSymbols(Locale locale) {
    ULocale uLocale = ULocale.forLocale(locale);
    return getSymbols(uLocale);
  }

  private static DecimalFormatSymbols getSymbols(ULocale uLocale) {
    if (uLocale == null) uLocale = ULocale.getDefault();
    DecimalFormatSymbols symbols = threadLocalSymbolsCache.get().get(uLocale);
    if (symbols == null) {
      symbols = DecimalFormatSymbols.getInstance(uLocale);
      threadLocalSymbolsCache.get().put(uLocale, symbols);
    }
    return symbols;
  }

  private static final ThreadLocal<Map<String, Properties>> threadLocalPropertiesCache =
      new ThreadLocal<Map<String, Properties>>() {
        @Override
        protected Map<String, Properties> initialValue() {
          return new HashMap<String, Properties>();
        }
      };

  private static Properties getProperties(String pattern) {
    if (pattern == null) pattern = "#";
    Properties properties = threadLocalPropertiesCache.get().get(pattern);
    if (properties == null) {
      properties = PatternString.parseToProperties(pattern);
      threadLocalPropertiesCache.get().put(pattern.intern(), properties);
    }
    return properties;
  }

  private static final ThreadLocal<Map<ULocale, PluralRules>> threadLocalRulesCache =
      new ThreadLocal<Map<ULocale, PluralRules>>() {
        @Override
        protected Map<ULocale, PluralRules> initialValue() {
          return new HashMap<ULocale, PluralRules>();
        }
      };

  private static PluralRules getPluralRules(ULocale uLocale, Properties properties) {
    // Backwards compatibility: CurrencyPluralInfo wraps its own copy of PluralRules
    if (properties.getCurrencyPluralInfo() != null) {
      return properties.getCurrencyPluralInfo().getPluralRules();
    }

    if (uLocale == null) uLocale = ULocale.getDefault();
    PluralRules rules = threadLocalRulesCache.get().get(uLocale);
    if (rules == null) {
      rules = PluralRules.forLocale(uLocale);
      threadLocalRulesCache.get().put(uLocale, rules);
    }
    return rules;
  }
}
