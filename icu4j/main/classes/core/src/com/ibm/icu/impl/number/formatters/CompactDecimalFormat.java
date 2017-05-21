// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.Modifier.PositiveNegativeModifier;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.PNAffixGenerator;
import com.ibm.icu.impl.number.PatternString;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.Rounder;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.impl.number.modifiers.PositiveNegativeAffixModifier;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormat.SignificantDigitsMode;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class CompactDecimalFormat extends Format.BeforeFormat {
  public static interface IProperties
      extends RoundingFormat.IProperties, CurrencyFormat.ICurrencyProperties {

    static CompactStyle DEFAULT_COMPACT_STYLE = null;

    /** @see #setCompactStyle */
    public CompactStyle getCompactStyle();

    /**
     * Use compact decimal formatting with the specified {@link CompactStyle}. CompactStyle.SHORT
     * produces output like "10K" in locale <em>en-US</em>, whereas CompactStyle.LONG produces
     * output like "10 thousand" in that locale.
     *
     * @param compactStyle The style of prefixes/suffixes to append.
     * @return The property bag, for chaining.
     */
    public IProperties setCompactStyle(CompactStyle compactStyle);

    static Map<String, Map<String, String>> DEFAULT_COMPACT_CUSTOM_DATA = null;

    /** @see #setCompactCustomData */
    public Map<String, Map<String, String>> getCompactCustomData();

    /**
     * Specifies custom data to be used instead of CLDR data when constructing a
     * CompactDecimalFormat. The argument should be a map with the following structure:
     *
     * <pre>
     * {
     *   "1000": {
     *     "one": "0 thousand",
     *     "other": "0 thousand"
     *   },
     *   "10000": {
     *     "one": "00 thousand",
     *     "other": "00 thousand"
     *   },
     *   // ...
     * }
     * </pre>
     *
     * This API endpoint is used by the CLDR Survey Tool.
     *
     * @param compactCustomData A map with the above structure.
     * @return The property bag, for chaining.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public IProperties setCompactCustomData(Map<String, Map<String, String>> compactCustomData);
  }

  public static boolean useCompactDecimalFormat(IProperties properties) {
    return properties.getCompactStyle() != IProperties.DEFAULT_COMPACT_STYLE;
  }

  static final int MAX_DIGITS = 15;

  // Properties
  private final CompactDecimalData data;
  private final Rounder rounder;
  private final PositiveNegativeModifier defaultMod;
  private final CompactStyle style; // retained for exporting only

  public static CompactDecimalFormat getInstance(
      DecimalFormatSymbols symbols, IProperties properties) {
    return new CompactDecimalFormat(symbols, properties);
  }

  private static final int DEFAULT_MIN_SIG = 1;
  private static final int DEFAULT_MAX_SIG = 2;
  private static final SignificantDigitsMode DEFAULT_SIG_MODE =
      SignificantDigitsMode.OVERRIDE_MAXIMUM_FRACTION;

  private static final ThreadLocal<Properties> threadLocalProperties =
      new ThreadLocal<Properties>() {
        @Override
        protected Properties initialValue() {
          return new Properties();
        }
      };

  private static Rounder getRounder(IProperties properties) {
    // Use rounding settings if they were specified, or else use the default CDF rounder.
    // TODO: Detecting and overriding significant digits here is a bit of a hack, since detection
    // is also performed in the "RoundingFormat.getDefaultOrNull" method.
    // It would be more elegant to call some sort of "fallback" copy method.
    Rounder rounder = null;
    if (!SignificantDigitsRounder.useSignificantDigits(properties)) {
      rounder = RoundingFormat.getDefaultOrNull(properties);
    }
    if (rounder == null) {
      int _minSig = properties.getMinimumSignificantDigits();
      int _maxSig = properties.getMaximumSignificantDigits();
      SignificantDigitsMode _mode = properties.getSignificantDigitsMode();
      Properties rprops = threadLocalProperties.get().clear();
      // Settings needing possible override:
      rprops.setMinimumSignificantDigits(_minSig > 0 ? _minSig : DEFAULT_MIN_SIG);
      rprops.setMaximumSignificantDigits(_maxSig > 0 ? _maxSig : DEFAULT_MAX_SIG);
      rprops.setSignificantDigitsMode(_mode != null ? _mode : DEFAULT_SIG_MODE);
      // TODO: Should copyFrom() be used instead?  It requires a cast.
      // Settings to copy verbatim:
      rprops.setRoundingMode(properties.getRoundingMode());
      rprops.setMinimumFractionDigits(properties.getMinimumFractionDigits());
      rprops.setMaximumFractionDigits(properties.getMaximumFractionDigits());
      rprops.setMinimumIntegerDigits(properties.getMinimumIntegerDigits());
      rprops.setMaximumIntegerDigits(properties.getMaximumIntegerDigits());
      rounder = SignificantDigitsRounder.getInstance(rprops);
    }
    return rounder;
  }

  protected static final ThreadLocal<Map<CompactDecimalFingerprint, CompactDecimalData>>
      threadLocalDataCache =
          new ThreadLocal<Map<CompactDecimalFingerprint, CompactDecimalData>>() {
            @Override
            protected Map<CompactDecimalFingerprint, CompactDecimalData> initialValue() {
              return new HashMap<CompactDecimalFingerprint, CompactDecimalData>();
            }
          };

  private static CompactDecimalData getData(
      DecimalFormatSymbols symbols, CompactDecimalFingerprint fingerprint) {
    // See if we already have a data object based on the fingerprint
    CompactDecimalData data = threadLocalDataCache.get().get(fingerprint);
    if (data != null) return data;

    // Make data bundle object
    data = new CompactDecimalData();
    ULocale ulocale = symbols.getULocale();
    CompactDecimalDataSink sink = new CompactDecimalDataSink(data, symbols, fingerprint);
    String nsName = NumberingSystem.getInstance(ulocale).getName();
    ICUResourceBundle rb =
        (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ulocale);
    internalPopulateData(nsName, rb, sink, data);
    if (data.isEmpty() && fingerprint.compactStyle == CompactStyle.LONG) {
      // No long data is available; load short data instead
      sink.compactStyle = CompactStyle.SHORT;
      internalPopulateData(nsName, rb, sink, data);
    }
    threadLocalDataCache.get().put(fingerprint, data);
    return data;
  }

  private static void internalPopulateData(
      String nsName, ICUResourceBundle rb, CompactDecimalDataSink sink, CompactDecimalData data) {
    try {
      rb.getAllItemsWithFallback("NumberElements/" + nsName, sink);
    } catch (MissingResourceException e) {
      // Fall back to latn
    }
    if (data.isEmpty() && !nsName.equals("latn")) {
      rb.getAllItemsWithFallback("NumberElements/latn", sink);
    }
    if (sink.exception != null) {
      throw sink.exception;
    }
  }

  private static PositiveNegativeModifier getDefaultMod(
      DecimalFormatSymbols symbols, CompactDecimalFingerprint fingerprint) {
    ULocale uloc = symbols.getULocale();
    String pattern;
    if (fingerprint.compactType == CompactType.CURRENCY) {
      pattern = NumberFormat.getPatternForStyle(uloc, NumberFormat.CURRENCYSTYLE);
    } else {
      pattern = NumberFormat.getPatternForStyle(uloc, NumberFormat.NUMBERSTYLE);
    }
    // TODO: Clean this up; avoid the extra object creations.
    // TODO: Currency may also need to override grouping settings, not just affixes.
    Properties properties = PatternString.parseToProperties(pattern);
    PNAffixGenerator pnag = PNAffixGenerator.getThreadLocalInstance();
    PNAffixGenerator.Result result =
        pnag.getModifiers(symbols, fingerprint.currencySymbol, properties);
    return new PositiveNegativeAffixModifier(result.positive, result.negative);
  }

  private CompactDecimalFormat(DecimalFormatSymbols symbols, IProperties properties) {
    CompactDecimalFingerprint fingerprint = new CompactDecimalFingerprint(symbols, properties);
    this.rounder = getRounder(properties);
    // Short-circuit and use custom data if provided
    if (properties.getCompactCustomData() != null) {
      this.data = createDataFromCustom(symbols, fingerprint, properties.getCompactCustomData());
    } else {
      this.data = getData(symbols, fingerprint);
    }
    this.defaultMod = getDefaultMod(symbols, fingerprint);
    this.style = properties.getCompactStyle(); // for exporting only
  }

  @Override
  public void before(FormatQuantity input, ModifierHolder mods, PluralRules rules) {
    apply(input, mods, rules, rounder, data, defaultMod);
  }

  @Override
  protected void before(FormatQuantity input, ModifierHolder mods) {
    throw new UnsupportedOperationException();
  }

  public static void apply(
      FormatQuantity input,
      ModifierHolder mods,
      PluralRules rules,
      DecimalFormatSymbols symbols,
      IProperties properties) {
    CompactDecimalFingerprint fingerprint = new CompactDecimalFingerprint(symbols, properties);
    Rounder rounder = getRounder(properties);
    CompactDecimalData data = getData(symbols, fingerprint);
    PositiveNegativeModifier defaultMod = getDefaultMod(symbols, fingerprint);
    apply(input, mods, rules, rounder, data, defaultMod);
  }

  private static void apply(
      FormatQuantity input,
      ModifierHolder mods,
      PluralRules rules,
      Rounder rounder,
      CompactDecimalData data,
      PositiveNegativeModifier defaultMod) {

    // Treat zero as if it had magnitude 0
    int magnitude;
    if (input.isZero()) {
      magnitude = 0;
      rounder.apply(input);
    } else {
      // TODO: Revisit chooseMultiplierAndApply
      int multiplier = rounder.chooseMultiplierAndApply(input, data);
      magnitude = input.isZero() ? 0 : input.getMagnitude();
      magnitude -= multiplier;
    }

    StandardPlural plural = input.getStandardPlural(rules);
    boolean isNegative = input.isNegative();
    Modifier mod = data.getModifier(magnitude, plural, isNegative);
    if (mod == null) {
      // Use the default (non-compact) modifier.
      mod = defaultMod.getModifier(isNegative);
    }
    mods.add(mod);
  }

  @Override
  public void export(Properties properties) {
    properties.setCompactStyle(style);
    rounder.export(properties);
  }

  static class CompactDecimalData implements Rounder.MultiplierGenerator {

    // A dummy object used when a "0" compact decimal entry is encountered.  This is necessary
    // in order to prevent falling back to root.
    private static final Modifier USE_FALLBACK = new ConstantAffixModifier();

    final Modifier[] mods;
    final byte[] multipliers;
    boolean isEmpty;
    int largestMagnitude;

    CompactDecimalData() {
      mods = new Modifier[(MAX_DIGITS + 1) * StandardPlural.COUNT * 2];
      multipliers = new byte[MAX_DIGITS + 1];
      isEmpty = true;
      largestMagnitude = -1;
    }

    boolean isEmpty() {
      return isEmpty;
    }

    @Override
    public int getMultiplier(int magnitude) {
      if (magnitude < 0) {
        return 0;
      }
      if (magnitude > largestMagnitude) {
        magnitude = largestMagnitude;
      }
      return multipliers[magnitude];
    }

    int setOrGetMultiplier(int magnitude, byte multiplier) {
      if (multipliers[magnitude] != 0) {
        return multipliers[magnitude];
      }
      multipliers[magnitude] = multiplier;
      isEmpty = false;
      if (magnitude > largestMagnitude) largestMagnitude = magnitude;
      return multiplier;
    }

    Modifier getModifier(int magnitude, StandardPlural plural, boolean isNegative) {
      if (magnitude < 0) {
        return null;
      }
      if (magnitude > largestMagnitude) {
        magnitude = largestMagnitude;
      }
      Modifier mod = mods[modIndex(magnitude, plural, isNegative)];
      if (mod == null && plural != StandardPlural.OTHER) {
        // Fall back to "other" plural variant
        mod = mods[modIndex(magnitude, StandardPlural.OTHER, isNegative)];
      }
      if (mod == USE_FALLBACK) {
        // Return null if USE_FALLBACK is present
        mod = null;
      }
      return mod;
    }

    public boolean has(int magnitude, StandardPlural plural) {
      // Return true if USE_FALLBACK is present
      return mods[modIndex(magnitude, plural, false)] != null;
    }

    void setModifiers(Modifier positive, Modifier negative, int magnitude, StandardPlural plural) {
      mods[modIndex(magnitude, plural, false)] = positive;
      mods[modIndex(magnitude, plural, true)] = negative;
      isEmpty = false;
      if (magnitude > largestMagnitude) largestMagnitude = magnitude;
    }

    void setNoFallback(int magnitude, StandardPlural plural) {
      setModifiers(USE_FALLBACK, USE_FALLBACK, magnitude, plural);
    }

    private static final int modIndex(int magnitude, StandardPlural plural, boolean isNegative) {
      return magnitude * StandardPlural.COUNT * 2 + plural.ordinal() * 2 + (isNegative ? 1 : 0);
    }
  }

  // Should this be public or internal?
  static enum CompactType {
    DECIMAL,
    CURRENCY
  }

  static class CompactDecimalFingerprint {
    // TODO: Add more stuff to the fingerprint, like the symbols used by PNAffixGenerator
    final CompactStyle compactStyle;
    final CompactType compactType;
    final ULocale uloc;
    final String currencySymbol;

    CompactDecimalFingerprint(DecimalFormatSymbols symbols, IProperties properties) {
      // CompactDecimalFormat does not need to worry about the same constraints as non-compact
      // currency formatting needs to consider, like the currency rounding mode and the currency
      // long names with plural forms.
      if (properties.getCurrency() != CurrencyFormat.ICurrencyProperties.DEFAULT_CURRENCY) {
        compactType = CompactType.CURRENCY;
        currencySymbol = CurrencyFormat.getCurrencySymbol(symbols, properties);
      } else {
        compactType = CompactType.DECIMAL;
        currencySymbol = ""; // fallback; should remain unused
      }
      compactStyle = properties.getCompactStyle();
      uloc = symbols.getULocale();
    }

    @Override
    public boolean equals(Object _other) {
      if (_other == null) return false;
      CompactDecimalFingerprint other = (CompactDecimalFingerprint) _other;
      if (this == other) return true;
      if (compactStyle != other.compactStyle) return false;
      if (compactType != other.compactType) return false;
      if (currencySymbol != other.currencySymbol) {
        // String comparison with null handling
        if (currencySymbol == null || other.currencySymbol == null) return false;
        if (!currencySymbol.equals(other.currencySymbol)) return false;
      }
      if (!uloc.equals(other.uloc)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = 0;
      if (compactStyle != null) hashCode ^= compactStyle.hashCode();
      if (compactType != null) hashCode ^= compactType.hashCode();
      if (uloc != null) hashCode ^= uloc.hashCode();
      if (currencySymbol != null) hashCode ^= currencySymbol.hashCode();
      return hashCode;
    }
  }

  private static final class CompactDecimalDataSink extends UResource.Sink {

    CompactDecimalData data;
    DecimalFormatSymbols symbols;
    CompactStyle compactStyle;
    CompactType compactType;
    String currencySymbol;
    PNAffixGenerator pnag;
    IllegalArgumentException exception;

    /*
     * NumberElements{              <-- top (numbering system table)
     *  latn{                       <-- patternsTable (one per numbering system)
     *    patternsLong{             <-- formatsTable (one per pattern)
     *      decimalFormat{          <-- powersOfTenTable (one per format)
     *        1000{                 <-- pluralVariantsTable (one per power of ten)
     *          one{"0 thousand"}   <-- plural variant and template
     */

    public CompactDecimalDataSink(
        CompactDecimalData data,
        DecimalFormatSymbols symbols,
        CompactDecimalFingerprint fingerprint) {
      this.data = data;
      this.symbols = symbols;
      compactType = fingerprint.compactType;
      currencySymbol = fingerprint.currencySymbol;
      compactStyle = fingerprint.compactStyle;
      pnag = PNAffixGenerator.getThreadLocalInstance();
    }

    @Override
    public void put(UResource.Key key, UResource.Value value, boolean isRoot) {
      UResource.Table patternsTable = value.getTable();
      for (int i1 = 0; patternsTable.getKeyAndValue(i1, key, value); ++i1) {
        if (key.contentEquals("patternsShort") && compactStyle == CompactStyle.SHORT) {
        } else if (key.contentEquals("patternsLong") && compactStyle == CompactStyle.LONG) {
        } else {
          continue;
        }

        // traverse into the table of formats
        UResource.Table formatsTable = value.getTable();
        for (int i2 = 0; formatsTable.getKeyAndValue(i2, key, value); ++i2) {
          if (key.contentEquals("decimalFormat") && compactType == CompactType.DECIMAL) {
          } else if (key.contentEquals("currencyFormat") && compactType == CompactType.CURRENCY) {
          } else {
            continue;
          }

          // traverse into the table of powers of ten
          UResource.Table powersOfTenTable = value.getTable();
          for (int i3 = 0; powersOfTenTable.getKeyAndValue(i3, key, value); ++i3) {
            try {

              // Assumes that the keys are always of the form "10000" where the magnitude is the
              // length of the key minus one
              byte magnitude = (byte) (key.length() - 1);

              // Silently ignore divisors that are too big.
              if (magnitude >= MAX_DIGITS) continue;

              // Iterate over the plural variants ("one", "other", etc)
              UResource.Table pluralVariantsTable = value.getTable();
              for (int i4 = 0; pluralVariantsTable.getKeyAndValue(i4, key, value); ++i4) {

                // Skip this magnitude/plural if we already have it from a child locale.
                StandardPlural plural = StandardPlural.fromString(key.toString());
                if (data.has(magnitude, plural)) {
                  continue;
                }

                // The value "0" means that we need to use the default pattern and not fall back
                // to parent locales.  Example locale where this is relevant: 'it'.
                String patternString = value.toString();
                if (patternString.equals("0")) {
                  data.setNoFallback(magnitude, plural);
                  continue;
                }

                // The magnitude multiplier is the difference between the magnitude and the number
                // of zeros in the pattern, getMinimumIntegerDigits.
                Properties properties = PatternString.parseToProperties(patternString);
                byte _multiplier = (byte) -(magnitude - properties.getMinimumIntegerDigits() + 1);
                if (_multiplier != data.setOrGetMultiplier(magnitude, _multiplier)) {
                  throw new IllegalArgumentException(
                      String.format(
                          "Different number of zeros for same power of ten in compact decimal format data for locale '%s', style '%s', type '%s'",
                          symbols.getULocale().toString(),
                          compactStyle.toString(),
                          compactType.toString()));
                }

                PNAffixGenerator.Result result =
                    pnag.getModifiers(symbols, currencySymbol, properties);
                data.setModifiers(result.positive, result.negative, magnitude, plural);
              }

            } catch (IllegalArgumentException e) {
              exception = e;
              continue;
            }
          }

          // We want only one table of compact decimal formats, so if we get here, stop consuming.
          // The data.isEmpty() check will prevent further bundles from being traversed.
          return;
        }
      }
    }
  }

  /**
   * Uses data from the custom powersToPluralsToPatterns map instead of an ICUResourceBundle to
   * populate an instance of CompactDecimalData.
   */
  static CompactDecimalData createDataFromCustom(
      DecimalFormatSymbols symbols,
      CompactDecimalFingerprint fingerprint,
      Map<String, Map<String, String>> powersToPluralsToPatterns) {
    CompactDecimalData data = new CompactDecimalData();
    PNAffixGenerator pnag = PNAffixGenerator.getThreadLocalInstance();
    for (Map.Entry<String, Map<String, String>> magnitudeEntry :
        powersToPluralsToPatterns.entrySet()) {
      byte magnitude = (byte) (magnitudeEntry.getKey().length() - 1);
      for (Map.Entry<String, String> pluralEntry : magnitudeEntry.getValue().entrySet()) {
        StandardPlural plural = StandardPlural.fromString(pluralEntry.getKey().toString());
        String patternString = pluralEntry.getValue().toString();
        Properties properties = PatternString.parseToProperties(patternString);
        byte _multiplier = (byte) -(magnitude - properties.getMinimumIntegerDigits() + 1);
        if (_multiplier != data.setOrGetMultiplier(magnitude, _multiplier)) {
          throw new IllegalArgumentException(
              "Different number of zeros for same power of ten in custom compact decimal format data");
        }
        PNAffixGenerator.Result result =
            pnag.getModifiers(symbols, fingerprint.currencySymbol, properties);
        data.setModifiers(result.positive, result.negative, magnitude, plural);
      }
    }
    return data;
  }
}
