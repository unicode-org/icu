// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.BigDecimal;
import java.math.MathContext;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.AffixPatternUtils;
import com.ibm.icu.impl.number.LdmlPatternInfo;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.CurrencyRounding;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.IntegerWidth;
import newapi.NumberFormatter.Notation;
import newapi.NumberFormatter.Rounding;
import newapi.NumberFormatter.SignDisplay;
import newapi.impl.NotationImpl.NotationScientificImpl;
import newapi.impl.RoundingImpl.RoundingImplCurrency;
import newapi.impl.RoundingImpl.RoundingImplFraction;
import newapi.impl.RoundingImpl.RoundingImplIncrement;
import newapi.impl.RoundingImpl.RoundingImplSignificant;

/** @author sffc */
public final class NumberPropertyMapper {

  /**
   * Creates a new {@link MacroProps} object based on the content of a {@link Properties} object. In
   * other words, maps Properties to MacroProps. This function is used by the JDK-compatibility API
   * to call into the ICU 60 fluent number formatting pipeline.
   *
   * @param properties The property bag to be mapped.
   * @param symbols The symbols associated with the property bag.
   * @param exportedProperties A property bag in which to store validated properties.
   * @return A new MacroProps containing all of the information in the Properties.
   */
  public static MacroProps oldToNew(
      Properties properties, DecimalFormatSymbols symbols, Properties exportedProperties) {
    MacroProps macros = new MacroProps();
    ULocale locale = symbols.getULocale();

    /////////////
    // SYMBOLS //
    /////////////

    macros.symbols = symbols;

    //////////////////
    // PLURAL RULES //
    //////////////////

    macros.rules = properties.getPluralRules();

    /////////////
    // AFFIXES //
    /////////////

    AffixPatternProvider affixProvider;
    if (properties.getCurrencyPluralInfo() == null) {
      affixProvider =
          new PropertiesAffixPatternProvider(
              properties.getPositivePrefix() != null
                  ? AffixPatternUtils.escape(properties.getPositivePrefix())
                  : properties.getPositivePrefixPattern(),
              properties.getPositiveSuffix() != null
                  ? AffixPatternUtils.escape(properties.getPositiveSuffix())
                  : properties.getPositiveSuffixPattern(),
              properties.getNegativePrefix() != null
                  ? AffixPatternUtils.escape(properties.getNegativePrefix())
                  : properties.getNegativePrefixPattern(),
              properties.getNegativeSuffix() != null
                  ? AffixPatternUtils.escape(properties.getNegativeSuffix())
                  : properties.getNegativeSuffixPattern());
    } else {
      affixProvider = new CurrencyPluralInfoAffixProvider(properties.getCurrencyPluralInfo());
    }
    macros.affixProvider = affixProvider;

    ///////////
    // UNITS //
    ///////////

    boolean useCurrency =
        ((properties.getCurrency() != null)
            || properties.getCurrencyPluralInfo() != null
            || properties.getCurrencyUsage() != null
            || affixProvider.hasCurrencySign());
    Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
    CurrencyUsage currencyUsage = properties.getCurrencyUsage();
    boolean explicitCurrencyUsage = currencyUsage != Properties.DEFAULT_CURRENCY_USAGE;
    if (!explicitCurrencyUsage) {
      currencyUsage = CurrencyUsage.STANDARD;
    }
    if (useCurrency) {
      macros.unit = currency;
    }

    ///////////////////////
    // ROUNDING STRATEGY //
    ///////////////////////

    int maxInt = properties.getMaximumIntegerDigits();
    int minInt = properties.getMinimumIntegerDigits();
    int maxFrac = properties.getMaximumFractionDigits();
    int minFrac = properties.getMinimumFractionDigits();
    int minSig = properties.getMinimumSignificantDigits();
    int maxSig = properties.getMaximumSignificantDigits();
    BigDecimal roundingIncrement = properties.getRoundingIncrement();
    MathContext mathContext = RoundingUtils.getMathContextOrUnlimited(properties);
    boolean explicitMinMaxFrac =
        minFrac != Properties.DEFAULT_MINIMUM_FRACTION_DIGITS
            || maxFrac != Properties.DEFAULT_MAXIMUM_FRACTION_DIGITS;
    boolean explicitMinMaxSig =
        minSig != Properties.DEFAULT_MINIMUM_SIGNIFICANT_DIGITS
            || maxSig != Properties.DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS;
    // Validate min/max int/frac.
    // For backwards compatibility, minimum overrides maximum if the two conflict.
    // The following logic ensures that there is always a minimum of at least one digit.
    if (minInt == 0 && maxFrac != 0) {
      // Force a digit after the decimal point.
      minFrac = minFrac <= 0 ? 1 : minFrac;
      maxFrac = maxFrac < 0 ? Integer.MAX_VALUE : maxFrac < minFrac ? minFrac : maxFrac;
      minInt = 0;
      maxInt = maxInt < 0 ? Integer.MAX_VALUE : maxInt;
    } else {
      // Force a digit before the decimal point.
      minFrac = minFrac < 0 ? 0 : minFrac;
      maxFrac = maxFrac < 0 ? Integer.MAX_VALUE : maxFrac < minFrac ? minFrac : maxFrac;
      minInt = minInt <= 0 ? 1 : minInt;
      maxInt = maxInt < 0 ? Integer.MAX_VALUE : maxInt < minInt ? minInt : maxInt;
    }
    Rounding rounding = null;
    if (explicitCurrencyUsage) {
      rounding = RoundingImplCurrency.getInstance(currencyUsage).withCurrency(currency);
    } else if (roundingIncrement != null) {
      rounding = RoundingImplIncrement.getInstance(roundingIncrement);
    } else if (explicitMinMaxSig) {
      minSig = minSig < 1 ? 1 : minSig > 1000 ? 1000 : minSig;
      maxSig = maxSig < 0 ? 1000 : maxSig < minSig ? minSig : maxSig > 1000 ? 1000 : maxSig;
      rounding = RoundingImplSignificant.getInstance(minSig, maxSig);
    } else if (explicitMinMaxFrac) {
      rounding = RoundingImplFraction.getInstance(minFrac, maxFrac);
    } else if (useCurrency) {
      rounding = RoundingImplCurrency.getInstance(currencyUsage);
    }
    if (rounding != null) {
      rounding = rounding.withMode(mathContext);
      macros.rounding = rounding;
    }

    ///////////////////
    // INTEGER WIDTH //
    ///////////////////

    macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);

    ///////////////////////
    // GROUPING STRATEGY //
    ///////////////////////

    int grouping1 = properties.getGroupingSize();
    int grouping2 = properties.getSecondaryGroupingSize();
    int minGrouping = properties.getMinimumGroupingDigits();
    assert grouping1 >= -2; // value of -2 means to forward no grouping information
    grouping1 = grouping1 > 0 ? grouping1 : grouping2 > 0 ? grouping2 : grouping1;
    grouping2 = grouping2 > 0 ? grouping2 : grouping1;
    // TODO: Is it important to handle minGrouping > 2?
    macros.grouping =
        GroupingImpl.getInstance((byte) grouping1, (byte) grouping2, minGrouping == 2);

    /////////////
    // PADDING //
    /////////////

    if (properties.getFormatWidth() != Properties.DEFAULT_FORMAT_WIDTH) {
      macros.padding =
          PaddingImpl.getInstance(
              properties.getPadString(), properties.getFormatWidth(), properties.getPadPosition());
    }

    ///////////////////////////////
    // DECIMAL MARK ALWAYS SHOWN //
    ///////////////////////////////

    macros.decimal =
        properties.getDecimalSeparatorAlwaysShown()
            ? DecimalMarkDisplay.ALWAYS
            : DecimalMarkDisplay.AUTO;

    ///////////////////////
    // SIGN ALWAYS SHOWN //
    ///////////////////////

    macros.sign = properties.getSignAlwaysShown() ? SignDisplay.ALWAYS : SignDisplay.AUTO;

    /////////////////////////
    // SCIENTIFIC NOTATION //
    /////////////////////////

    if (properties.getMinimumExponentDigits() != Properties.DEFAULT_MINIMUM_EXPONENT_DIGITS) {
      // Scientific notation is required.
      // The mapping from property bag to scientific notation is nontrivial due to LDML rules.
      // The maximum of 8 engineering digits has unknown origins and is not in the spec.
      int engineering =
          (maxInt != Integer.MAX_VALUE) ? maxInt : properties.getMaximumIntegerDigits();
      engineering = (engineering < 0) ? 0 : (engineering > 8) ? minInt : engineering;
      macros.notation =
          new NotationScientificImpl(
              // Engineering interval:
              engineering,
              // Enforce minimum integer digits (for patterns like "000.00E0"):
              (engineering == minInt),
              // Minimum exponent digits:
              properties.getMinimumExponentDigits(),
              // Exponent sign always shown:
              properties.getExponentSignAlwaysShown()
                  ? SignDisplay.ALWAYS
                  : SignDisplay.AUTO);
      // Scientific notation also involves overriding the rounding mode.
      if (macros.rounding instanceof RoundingImplFraction) {
        int minInt_ = properties.getMinimumIntegerDigits();
        int minFrac_ = properties.getMinimumFractionDigits();
        int maxFrac_ = properties.getMaximumFractionDigits();
        if (minInt_ == 0 && maxFrac_ == 0) {
          // Patterns like "#E0" and "##E0", which mean no rounding!
          macros.rounding = Rounding.NONE.withMode(mathContext);
        } else if (minInt_ == 0 && minFrac_ == 0) {
          // Patterns like "#.##E0" (no zeros in the mantissa), which mean round to maxFrac+1
          macros.rounding = new RoundingImplSignificant(1, maxFrac_ + 1).withMode(mathContext);
        } else {
          // All other scientific patterns, which mean round to minInt+maxFrac
          macros.rounding =
              new RoundingImplSignificant(minInt_ + minFrac_, minInt_ + maxFrac_)
                  .withMode(mathContext);
        }
      }
    }

    //////////////////////
    // COMPACT NOTATION //
    //////////////////////

    if (properties.getCompactStyle() != Properties.DEFAULT_COMPACT_STYLE) {
      if (properties.getCompactCustomData() != null) {
        macros.notation = new NotationImpl.NotationCompactImpl(properties.getCompactCustomData());
      } else if (properties.getCompactStyle() == CompactStyle.LONG) {
        macros.notation = Notation.COMPACT_LONG;
      } else {
        macros.notation = Notation.COMPACT_SHORT;
      }
      // Do not forward the affix provider.
      macros.affixProvider = null;
    }

    /////////////////
    // MULTIPLIERS //
    /////////////////

    if (properties.getMagnitudeMultiplier() != Properties.DEFAULT_MAGNITUDE_MULTIPLIER) {
      macros.multiplier = new MultiplierImpl(properties.getMagnitudeMultiplier());
    } else if (properties.getMultiplier() != Properties.DEFAULT_MULTIPLIER) {
      macros.multiplier = new MultiplierImpl(properties.getMultiplier());
    }

    //////////////////////
    // PROPERTY EXPORTS //
    //////////////////////

    if (exportedProperties != null) {

      exportedProperties.setMathContext(mathContext);
      exportedProperties.setRoundingMode(mathContext.getRoundingMode());
      exportedProperties.setMinimumIntegerDigits(minInt);
      exportedProperties.setMaximumIntegerDigits(maxInt);

      Rounding rounding_;
      if (rounding instanceof CurrencyRounding) {
        rounding_ = ((CurrencyRounding) rounding).withCurrency(currency);
      } else {
        rounding_ = rounding;
      }
      int minFrac_ = minFrac;
      int maxFrac_ = maxFrac;
      int minSig_ = minSig;
      int maxSig_ = maxSig;
      BigDecimal increment_ = null;
      if (rounding_ instanceof RoundingImplFraction) {
        minFrac_ = ((RoundingImplFraction) rounding_).minFrac;
        maxFrac_ = ((RoundingImplFraction) rounding_).maxFrac;
      } else if (rounding_ instanceof RoundingImplIncrement) {
        increment_ = ((RoundingImplIncrement) rounding_).increment;
        minFrac_ = increment_.scale();
        maxFrac_ = increment_.scale();
      } else if (rounding_ instanceof RoundingImplSignificant) {
        minSig_ = ((RoundingImplSignificant) rounding_).minSig;
        maxSig_ = ((RoundingImplSignificant) rounding_).maxSig;
      }

      exportedProperties.setMinimumFractionDigits(minFrac_);
      exportedProperties.setMaximumFractionDigits(maxFrac_);
      exportedProperties.setMinimumSignificantDigits(minSig_);
      exportedProperties.setMaximumSignificantDigits(maxSig_);
      exportedProperties.setRoundingIncrement(increment_);
    }

    return macros;
  }

  private static class PropertiesAffixPatternProvider implements AffixPatternProvider {
    private final String posPrefixPattern;
    private final String posSuffixPattern;
    private final String negPrefixPattern;
    private final String negSuffixPattern;

    public PropertiesAffixPatternProvider(String ppp, String psp, String npp, String nsp) {
      if (ppp == null) ppp = "";
      if (psp == null) psp = "";
      if (npp == null && nsp != null) npp = "-"; // TODO: This is a hack.
      if (nsp == null && npp != null) nsp = "";
      posPrefixPattern = ppp;
      posSuffixPattern = psp;
      negPrefixPattern = npp;
      negSuffixPattern = nsp;
    }

    @Override
    public char charAt(int flags, int i) {
      boolean prefix = (flags & Flags.PREFIX) != 0;
      boolean negative = (flags & Flags.NEGATIVE_SUBPATTERN) != 0;
      if (prefix && negative) {
        return negPrefixPattern.charAt(i);
      } else if (prefix) {
        return posPrefixPattern.charAt(i);
      } else if (negative) {
        return negSuffixPattern.charAt(i);
      } else {
        return posSuffixPattern.charAt(i);
      }
    }

    @Override
    public int length(int flags) {
      boolean prefix = (flags & Flags.PREFIX) != 0;
      boolean negative = (flags & Flags.NEGATIVE_SUBPATTERN) != 0;
      if (prefix && negative) {
        return negPrefixPattern.length();
      } else if (prefix) {
        return posPrefixPattern.length();
      } else if (negative) {
        return negSuffixPattern.length();
      } else {
        return posSuffixPattern.length();
      }
    }

    @Override
    public boolean positiveHasPlusSign() {
      return AffixPatternUtils.containsType(posPrefixPattern, AffixPatternUtils.TYPE_PLUS_SIGN)
          || AffixPatternUtils.containsType(posSuffixPattern, AffixPatternUtils.TYPE_PLUS_SIGN);
    }

    @Override
    public boolean hasNegativeSubpattern() {
      return negPrefixPattern != null;
    }

    @Override
    public boolean negativeHasMinusSign() {
      return AffixPatternUtils.containsType(negPrefixPattern, AffixPatternUtils.TYPE_MINUS_SIGN)
          || AffixPatternUtils.containsType(negSuffixPattern, AffixPatternUtils.TYPE_MINUS_SIGN);
    }

    @Override
    public boolean hasCurrencySign() {
      return AffixPatternUtils.hasCurrencySymbols(posPrefixPattern)
          || AffixPatternUtils.hasCurrencySymbols(posSuffixPattern)
          || AffixPatternUtils.hasCurrencySymbols(negPrefixPattern)
          || AffixPatternUtils.hasCurrencySymbols(negSuffixPattern);
    }

    @Override
    public boolean containsSymbolType(int type) {
      return AffixPatternUtils.containsType(posPrefixPattern, type)
          || AffixPatternUtils.containsType(posSuffixPattern, type)
          || AffixPatternUtils.containsType(negPrefixPattern, type)
          || AffixPatternUtils.containsType(negSuffixPattern, type);
    }
  }

  private static class CurrencyPluralInfoAffixProvider implements AffixPatternProvider {
    private final AffixPatternProvider[] affixesByPlural;

    public CurrencyPluralInfoAffixProvider(CurrencyPluralInfo cpi) {
      affixesByPlural = new PatternParseResult[StandardPlural.COUNT];
      for (StandardPlural plural : StandardPlural.VALUES) {
        affixesByPlural[plural.ordinal()] =
            LdmlPatternInfo.parse(cpi.getCurrencyPluralPattern(plural.getKeyword()));
      }
    }

    @Override
    public char charAt(int flags, int i) {
      int pluralOrdinal = (flags & Flags.PLURAL_MASK);
      return affixesByPlural[pluralOrdinal].charAt(flags, i);
    }

    @Override
    public int length(int flags) {
      int pluralOrdinal = (flags & Flags.PLURAL_MASK);
      return affixesByPlural[pluralOrdinal].length(flags);
    }

    @Override
    public boolean positiveHasPlusSign() {
      return affixesByPlural[StandardPlural.OTHER.ordinal()].positiveHasPlusSign();
    }

    @Override
    public boolean hasNegativeSubpattern() {
      return affixesByPlural[StandardPlural.OTHER.ordinal()].hasNegativeSubpattern();
    }

    @Override
    public boolean negativeHasMinusSign() {
      return affixesByPlural[StandardPlural.OTHER.ordinal()].negativeHasMinusSign();
    }

    @Override
    public boolean hasCurrencySign() {
      return affixesByPlural[StandardPlural.OTHER.ordinal()].hasCurrencySign();
    }

    @Override
    public boolean containsSymbolType(int type) {
      return affixesByPlural[StandardPlural.OTHER.ordinal()].containsSymbolType(type);
    }
  }
}
