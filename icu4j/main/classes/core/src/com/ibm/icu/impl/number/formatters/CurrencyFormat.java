// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import java.math.BigDecimal;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixPatternUtils;
import com.ibm.icu.impl.number.PNAffixGenerator;
import com.ibm.icu.impl.number.PatternString;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.Rounder;
import com.ibm.icu.impl.number.modifiers.GeneralPluralModifier;
import com.ibm.icu.impl.number.rounders.IncrementRounder;
import com.ibm.icu.impl.number.rounders.MagnitudeRounder;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;

public class CurrencyFormat {

  public enum CurrencyStyle {
    SYMBOL,
    ISO_CODE;
  }

  public static interface ICurrencyProperties {
    static Currency DEFAULT_CURRENCY = null;

    /** @see #setCurrency */
    public Currency getCurrency();

    /**
     * Use the specified currency to substitute currency placeholders ('¤') in the pattern string.
     *
     * @param currency The currency.
     * @return The property bag, for chaining.
     */
    public IProperties setCurrency(Currency currency);

    static CurrencyStyle DEFAULT_CURRENCY_STYLE = null;

    /** @see #setCurrencyStyle */
    public CurrencyStyle getCurrencyStyle();

    /**
     * Use the specified {@link CurrencyStyle} to replace currency placeholders ('¤').
     * CurrencyStyle.SYMBOL will use the short currency symbol, like "$" or "€", whereas
     * CurrencyStyle.ISO_CODE will use the ISO 4217 currency code, like "USD" or "EUR".
     *
     * <p>For long currency names, use {@link MeasureFormat.IProperties#setMeasureUnit}.
     *
     * @param currencyStyle The currency style. Defaults to CurrencyStyle.SYMBOL.
     * @return The property bag, for chaining.
     */
    public IProperties setCurrencyStyle(CurrencyStyle currencyStyle);

    /**
     * An old enum that specifies how currencies should be rounded. It contains a subset of the
     * functionality supported by RoundingInterval.
     */
    static Currency.CurrencyUsage DEFAULT_CURRENCY_USAGE = null;

    /** @see #setCurrencyUsage */
    public Currency.CurrencyUsage getCurrencyUsage();

    /**
     * Use the specified {@link CurrencyUsage} instance, which provides default rounding rules for
     * the currency in two styles, CurrencyUsage.CASH and CurrencyUsage.STANDARD.
     *
     * <p>The CurrencyUsage specified here will not be used unless there is a currency placeholder
     * in the pattern.
     *
     * @param currencyUsage The currency usage. Defaults to CurrencyUsage.STANDARD.
     * @return The property bag, for chaining.
     */
    public IProperties setCurrencyUsage(Currency.CurrencyUsage currencyUsage);

    static CurrencyPluralInfo DEFAULT_CURRENCY_PLURAL_INFO = null;

    /** @see #setCurrencyPluralInfo */
    @Deprecated
    public CurrencyPluralInfo getCurrencyPluralInfo();

    /**
     * Use the specified {@link CurrencyPluralInfo} instance when formatting currency long names.
     *
     * @param currencyPluralInfo The currency plural info object.
     * @return The property bag, for chaining.
     * @deprecated Use {@link MeasureFormat.IProperties#setMeasureUnit} with a Currency instead.
     */
    @Deprecated
    public IProperties setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo);
  }

  public static interface IProperties
      extends ICurrencyProperties,
          RoundingFormat.IProperties,
          PositiveNegativeAffixFormat.IProperties {}

  /**
   * Returns true if the currency is set in The property bag or if currency symbols are present in
   * the prefix/suffix pattern.
   */
  public static boolean useCurrency(IProperties properties) {
    return ((properties.getCurrency() != null)
        || properties.getCurrencyPluralInfo() != null
        || properties.getCurrencyUsage() != null
        || AffixPatternUtils.hasCurrencySymbols(properties.getPositivePrefixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getPositiveSuffixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getNegativePrefixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getNegativeSuffixPattern()));
  }

  /**
   * Returns the effective currency symbol based on the input. If {@link
   * ICurrencyProperties#setCurrencyStyle} was set to {@link CurrencyStyle#ISO_CODE}, the ISO Code
   * will be returned; otherwise, the currency symbol, like "$", will be returned.
   *
   * @param symbols The current {@link DecimalFormatSymbols} instance
   * @param properties The current property bag
   * @return The currency symbol string, e.g., to substitute '¤' in a decimal pattern string.
   */
  public static String getCurrencySymbol(
      DecimalFormatSymbols symbols, ICurrencyProperties properties) {
    // If the user asked for ISO Code, return the ISO Code instead of the symbol
    CurrencyStyle style = properties.getCurrencyStyle();
    if (style == CurrencyStyle.ISO_CODE) {
      return getCurrencyIsoCode(symbols, properties);
    }

    // Get the currency symbol
    Currency currency = properties.getCurrency();
    if (currency == null) {
      return symbols.getCurrencySymbol();
    } else if (currency.equals(symbols.getCurrency())) {
      // The user may have set a custom currency symbol in DecimalFormatSymbols.
      return symbols.getCurrencySymbol();
    } else {
      // Use the canonical symbol.
      return currency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, null);
    }
  }

  /**
   * Returns the currency ISO code based on the input, like "USD".
   *
   * @param symbols The current {@link DecimalFormatSymbols} instance
   * @param properties The current property bag
   * @return The currency ISO code string, e.g., to substitute '¤¤' in a decimal pattern string.
   */
  public static String getCurrencyIsoCode(
      DecimalFormatSymbols symbols, ICurrencyProperties properties) {
    Currency currency = properties.getCurrency();
    if (currency == null) {
      // If a currency object was not provided, use the string from symbols
      // Note: symbols.getCurrency().getCurrencyCode() won't work here because
      // DecimalFormatSymbols#setInternationalCurrencySymbol() does not update the
      // immutable internal currency instance.
      return symbols.getInternationalCurrencySymbol();
    } else if (currency.equals(symbols.getCurrency())) {
      // The user may have set a custom currency symbol in DecimalFormatSymbols.
      return symbols.getInternationalCurrencySymbol();
    } else {
      // Use the canonical currency code.
      return currency.getCurrencyCode();
    }
  }

  /**
   * Returns the currency long name on the input, like "US dollars".
   *
   * @param symbols The current {@link DecimalFormatSymbols} instance
   * @param properties The current property bag
   * @param plural The plural form
   * @return The currency long name string, e.g., to substitute '¤¤¤' in a decimal pattern string.
   */
  public static String getCurrencyLongName(
      DecimalFormatSymbols symbols, ICurrencyProperties properties, StandardPlural plural) {
    // Attempt to get a currency object first from properties then from symbols
    Currency currency = properties.getCurrency();
    if (currency == null) {
      currency = symbols.getCurrency();
    }

    // If no currency object is available, fall back to the currency symbol
    if (currency == null) {
      return getCurrencySymbol(symbols, properties);
    }

    // Get the long name
    return currency.getName(
        symbols.getULocale(), Currency.PLURAL_LONG_NAME, plural.getKeyword(), null);
  }

  public static GeneralPluralModifier getCurrencyModifier(
      DecimalFormatSymbols symbols, IProperties properties) {

    PNAffixGenerator pnag = PNAffixGenerator.getThreadLocalInstance();
    String sym = getCurrencySymbol(symbols, properties);
    String iso = getCurrencyIsoCode(symbols, properties);

    // Previously, the user was also able to specify '¤¤' and '¤¤¤' directly into the prefix or
    // suffix, which is how the user specified whether they wanted the ISO code or long name.
    // For backwards compatibility support, that feature is implemented here.

    CurrencyPluralInfo info = properties.getCurrencyPluralInfo();
    GeneralPluralModifier mod = new GeneralPluralModifier();
    Properties temp = new Properties();
    for (StandardPlural plural : StandardPlural.VALUES) {
      String longName = getCurrencyLongName(symbols, properties, plural);

      PNAffixGenerator.Result result;
      if (info == null) {
        // CurrencyPluralInfo is not available.
        result = pnag.getModifiers(symbols, sym, iso, longName, properties);
      } else {
        // CurrencyPluralInfo is available. Use it to generate affixes for long name support.
        String pluralPattern = info.getCurrencyPluralPattern(plural.getKeyword());
        PatternString.parseToExistingProperties(
            pluralPattern, temp, PatternString.IGNORE_ROUNDING_ALWAYS);
        result = pnag.getModifiers(symbols, sym, iso, longName, temp);
      }
      mod.put(plural, result.positive, result.negative);
    }
    return mod;
  }

  private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

  public static void populateCurrencyRounderProperties(
      Properties destination, DecimalFormatSymbols symbols, IProperties properties) {

    Currency currency = properties.getCurrency();
    if (currency == null) {
      // Fall back to the DecimalFormatSymbols currency instance.
      currency = symbols.getCurrency();
    }
    if (currency == null) {
      // There is a currency symbol in the pattern, but we have no currency available to use.
      // Use the default currency instead so that we can still apply currency usage rules.
      currency = DEFAULT_CURRENCY;
    }

    CurrencyUsage _currencyUsage = properties.getCurrencyUsage();
    int _minFrac = properties.getMinimumFractionDigits();
    int _maxFrac = properties.getMaximumFractionDigits();

    CurrencyUsage effectiveCurrencyUsage =
        (_currencyUsage != null) ? _currencyUsage : CurrencyUsage.STANDARD;
    double incrementDouble = currency.getRoundingIncrement(effectiveCurrencyUsage);
    int fractionDigits = currency.getDefaultFractionDigits(effectiveCurrencyUsage);

    destination.setRoundingMode(properties.getRoundingMode());
    destination.setMinimumIntegerDigits(properties.getMinimumIntegerDigits());
    destination.setMaximumIntegerDigits(properties.getMaximumIntegerDigits());

    if (_currencyUsage == null && (_minFrac >= 0 || _maxFrac >= 0)) {
      // User override of fraction length
      if (_minFrac < 0) {
        destination.setMinimumFractionDigits(fractionDigits < _maxFrac ? fractionDigits : _maxFrac);
        destination.setMaximumFractionDigits(_maxFrac);
      } else if (_maxFrac < 0) {
        destination.setMinimumFractionDigits(_minFrac);
        destination.setMaximumFractionDigits(fractionDigits > _minFrac ? fractionDigits : _minFrac);
      } else {
        destination.setMinimumFractionDigits(_minFrac);
        destination.setMaximumFractionDigits(_maxFrac);
      }
    } else {
      // Currency rounding
      destination.setMinimumFractionDigits(fractionDigits);
      destination.setMaximumFractionDigits(fractionDigits);
    }

    if (incrementDouble > 0.0) {
      BigDecimal incrementBigDecimal;
      BigDecimal _roundingIncrement = properties.getRoundingIncrement();
      if (_roundingIncrement != null) {
        incrementBigDecimal = _roundingIncrement;
      } else {
        incrementBigDecimal = BigDecimal.valueOf(incrementDouble);
      }
      destination.setRoundingIncrement(incrementBigDecimal);
    } else {
    }
  }

  private static final ThreadLocal<Properties> threadLocalProperties =
      new ThreadLocal<Properties>() {
        @Override
        protected Properties initialValue() {
          return new Properties();
        }
      };

  public static Rounder getCurrencyRounder(DecimalFormatSymbols symbols, IProperties properties) {
    if (SignificantDigitsRounder.useSignificantDigits(properties)) {
      return SignificantDigitsRounder.getInstance(properties);
    }
    Properties cprops = threadLocalProperties.get().clear();
    populateCurrencyRounderProperties(cprops, symbols, properties);
    if (cprops.getRoundingIncrement() != null) {
      return IncrementRounder.getInstance(cprops);
    } else {
      return MagnitudeRounder.getInstance(cprops);
    }
  }
}
