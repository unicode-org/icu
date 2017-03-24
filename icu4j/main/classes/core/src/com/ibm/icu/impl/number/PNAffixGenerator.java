// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.number.Modifier.AffixModifier;
import com.ibm.icu.impl.number.formatters.CompactDecimalFormat;
import com.ibm.icu.impl.number.formatters.PositiveNegativeAffixFormat;
import com.ibm.icu.impl.number.formatters.PositiveNegativeAffixFormat.IProperties;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.impl.number.modifiers.ConstantMultiFieldModifier;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat.Field;

/**
 * A class to convert from a bag of prefix/suffix properties into a positive and negative {@link
 * Modifier}. This is a standard implementation used by {@link PositiveNegativeAffixFormat}, {@link
 * CompactDecimalFormat}, {@link Parse}, and others.
 *
 * <p>This class is is intended to be an efficient generator for instances of Modifier by a single
 * thread during construction of a formatter or during static formatting. It uses internal caching
 * to avoid creating new Modifier objects when possible. It is NOT THREAD SAFE and NOT IMMUTABLE.
 *
 * <p>The thread-local instance of this class provided by {@link #getThreadLocalInstance} should be
 * used in most cases instead of constructing a new instance of the object.
 *
 * <p>This class also handles the logic of assigning positive signs, negative signs, and currency
 * signs according to the LDML specification.
 */
public class PNAffixGenerator {
  public static class Result {
    public AffixModifier positive = null;
    public AffixModifier negative = null;
  }

  protected static final ThreadLocal<PNAffixGenerator> threadLocalInstance =
      new ThreadLocal<PNAffixGenerator>() {
        @Override
        protected PNAffixGenerator initialValue() {
          return new PNAffixGenerator();
        }
      };

  public static PNAffixGenerator getThreadLocalInstance() {
    return threadLocalInstance.get();
  }

  // These instances are used internally and cached to avoid object creation.  The resultInstance
  // also serves as a 1-element cache to avoid creating objects when subsequent calls have
  // identical prefixes and suffixes.  This happens, for example, when consuming CDF data.
  private Result resultInstance = new Result();
  private NumberStringBuilder sb1 = new NumberStringBuilder();
  private NumberStringBuilder sb2 = new NumberStringBuilder();
  private NumberStringBuilder sb3 = new NumberStringBuilder();
  private NumberStringBuilder sb4 = new NumberStringBuilder();
  private NumberStringBuilder sb5 = new NumberStringBuilder();
  private NumberStringBuilder sb6 = new NumberStringBuilder();

  /**
   * Generates modifiers using default currency symbols.
   *
   * @param symbols The symbols to interpolate for minus, plus, percent, permille, and currency.
   * @param properties The bag of properties to convert.
   * @return The positive and negative {@link Modifier}.
   */
  public Result getModifiers(
      DecimalFormatSymbols symbols, PositiveNegativeAffixFormat.IProperties properties) {
    // If this method is used, the user doesn't care about currencies. Default the currency symbols
    // to the information we can get from the DecimalFormatSymbols instance.
    return getModifiers(
        symbols,
        symbols.getCurrencySymbol(),
        symbols.getInternationalCurrencySymbol(),
        symbols.getCurrencySymbol(),
        properties);
  }

  /**
   * Generates modifiers using the specified currency symbol for all three lengths of currency
   * placeholders in the pattern string.
   *
   * @param symbols The symbols to interpolate for minus, plus, percent, and permille.
   * @param currencySymbol The currency symbol.
   * @param properties The bag of properties to convert.
   * @return The positive and negative {@link Modifier}.
   */
  public Result getModifiers(
      DecimalFormatSymbols symbols,
      String currencySymbol,
      PositiveNegativeAffixFormat.IProperties properties) {
    // If this method is used, the user doesn't cares about currencies but doesn't care about
    // supporting all three sizes of currency placeholders.  Use the one provided string for all
    // three sizes of placeholders.
    return getModifiers(symbols, currencySymbol, currencySymbol, currencySymbol, properties);
  }

  /**
   * Generates modifiers using the three specified strings to replace the three lengths of currency
   * placeholders: "¤", "¤¤", and "¤¤¤".
   *
   * @param symbols The symbols to interpolate for minus, plus, percent, and permille.
   * @param curr1 The string to replace "¤".
   * @param curr2 The string to replace "¤¤".
   * @param curr3 The string to replace "¤¤¤".
   * @param properties The bag of properties to convert.
   * @return The positive and negative {@link Modifier}.
   */
  public Result getModifiers(
      DecimalFormatSymbols symbols,
      String curr1,
      String curr2,
      String curr3,
      PositiveNegativeAffixFormat.IProperties properties) {

    // Use a different code path for handling affixes with "always show plus sign"
    if (properties.getSignAlwaysShown()) {
      return getModifiersWithPlusSign(symbols, curr1, curr2, curr3, properties);
    }

    CharSequence ppp = properties.getPositivePrefixPattern();
    CharSequence psp = properties.getPositiveSuffixPattern();
    CharSequence npp = properties.getNegativePrefixPattern();
    CharSequence nsp = properties.getNegativeSuffixPattern();

    // Set sb1/sb2 to the positive prefix/suffix.
    sb1.clear();
    sb2.clear();
    AffixPatternUtils.unescape(ppp, symbols, curr1, curr2, curr3, null, sb1);
    AffixPatternUtils.unescape(psp, symbols, curr1, curr2, curr3, null, sb2);
    setPositiveResult(sb1, sb2, properties);

    // Set sb1/sb2 to the negative prefix/suffix.
    if (npp == null && nsp == null) {
      // Negative prefix defaults to positive prefix prepended with the minus sign.
      // Negative suffix defaults to positive suffix.
      sb1.insert(0, symbols.getMinusSignString(), Field.SIGN);
    } else {
      sb1.clear();
      sb2.clear();
      AffixPatternUtils.unescape(npp, symbols, curr1, curr2, curr3, null, sb1);
      AffixPatternUtils.unescape(nsp, symbols, curr1, curr2, curr3, null, sb2);
    }
    setNegativeResult(sb1, sb2, properties);

    return resultInstance;
  }

  private Result getModifiersWithPlusSign(
      DecimalFormatSymbols symbols,
      String curr1,
      String curr2,
      String curr3,
      IProperties properties) {

    CharSequence ppp = properties.getPositivePrefixPattern();
    CharSequence psp = properties.getPositiveSuffixPattern();
    CharSequence npp = properties.getNegativePrefixPattern();
    CharSequence nsp = properties.getNegativeSuffixPattern();

    // There are three cases, listed below with their expected outcomes.
    // TODO: Should we handle the cases when the positive subpattern has a '+' already?
    //
    //   1) No negative subpattern.
    //        Positive => Positive subpattern prepended with '+'
    //        Negative => Positive subpattern prepended with '-'
    //   2) Negative subpattern does not have '-'.
    //        Positive => Positive subpattern prepended with '+'
    //        Negative => Negative subpattern
    //   3) Negative subpattern has '-'.
    //        Positive => Negative subpattern with '+' substituted for '-'
    //        Negative => Negative subpattern

    if (npp != null || nsp != null) {
      // Case 2 or Case 3
      sb1.clear();
      sb2.clear();
      sb3.clear();
      sb4.clear();
      AffixPatternUtils.unescape(npp, symbols, curr1, curr2, curr3, null, sb1);
      AffixPatternUtils.unescape(nsp, symbols, curr1, curr2, curr3, null, sb2);
      AffixPatternUtils.unescape(
          npp, symbols, curr1, curr2, curr3, symbols.getPlusSignString(), sb3);
      AffixPatternUtils.unescape(
          nsp, symbols, curr1, curr2, curr3, symbols.getPlusSignString(), sb4);
      if (!charSequenceEquals(sb1, sb3) || !charSequenceEquals(sb2, sb4)) {
        // Case 3. The plus sign substitution was successful.
        setPositiveResult(sb3, sb4, properties);
        setNegativeResult(sb1, sb2, properties);
        return resultInstance;
      } else {
        // Case 2. There was no minus sign. Set the negative result and fall through.
        setNegativeResult(sb1, sb2, properties);
      }
    }

    // Case 1 or 2. Set sb1/sb2 to the positive prefix/suffix.
    sb1.clear();
    sb2.clear();
    AffixPatternUtils.unescape(ppp, symbols, curr1, curr2, curr3, null, sb1);
    AffixPatternUtils.unescape(psp, symbols, curr1, curr2, curr3, null, sb2);

    if (npp == null && nsp == null) {
      // Case 1. Compute the negative result from the positive subpattern.
      sb3.clear();
      sb3.append(symbols.getMinusSignString(), Field.SIGN);
      sb3.append(sb1);
      setNegativeResult(sb3, sb2, properties);
    }

    // Case 1 or 2. Prepend a '+' sign to the positive prefix.
    sb1.insert(0, symbols.getPlusSignString(), Field.SIGN);
    setPositiveResult(sb1, sb2, properties);

    return resultInstance;
  }

  private void setPositiveResult(
      NumberStringBuilder prefix, NumberStringBuilder suffix, IProperties properties) {
    // Override with custom affixes. We need to put these into NumberStringBuilders so that they
    // have the same datatype as the incoming prefix and suffix (important when testing for field
    // equality in contentEquals).
    // TODO: It is a little inefficient that we copy String -> NumberStringBuilder -> Modifier.
    // Consider re-working the logic so that fewer copies are required.
    String _prefix = properties.getPositivePrefix();
    String _suffix = properties.getPositiveSuffix();
    if (_prefix != null) {
      prefix = sb5.clear();
      prefix.append(_prefix, null);
    }
    if (_suffix != null) {
      suffix = sb6.clear();
      suffix.append(_suffix, null);
    }
    if (prefix.length() == 0 && suffix.length() == 0) {
      resultInstance.positive = ConstantAffixModifier.EMPTY;
      return;
    }
    if (resultInstance.positive != null
        && (resultInstance.positive instanceof ConstantMultiFieldModifier)
        && ((ConstantMultiFieldModifier) resultInstance.positive).contentEquals(prefix, suffix)) {
      // Use the cached modifier
      return;
    }
    resultInstance.positive = new ConstantMultiFieldModifier(prefix, suffix, false);
  }

  private void setNegativeResult(
      NumberStringBuilder prefix, NumberStringBuilder suffix, IProperties properties) {
    String _prefix = properties.getNegativePrefix();
    String _suffix = properties.getNegativeSuffix();
    if (_prefix != null) {
      prefix = sb5.clear();
      prefix.append(_prefix, null);
    }
    if (_suffix != null) {
      suffix = sb6.clear();
      suffix.append(_suffix, null);
    }
    if (prefix.length() == 0 && suffix.length() == 0) {
      resultInstance.negative = ConstantAffixModifier.EMPTY;
      return;
    }
    if (resultInstance.negative != null
        && (resultInstance.negative instanceof ConstantMultiFieldModifier)
        && ((ConstantMultiFieldModifier) resultInstance.negative).contentEquals(prefix, suffix)) {
      // Use the cached modifier
      return;
    }
    resultInstance.negative = new ConstantMultiFieldModifier(prefix, suffix, false);
  }

  /** A null-safe equals method for CharSequences. */
  private static boolean charSequenceEquals(CharSequence a, CharSequence b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a.length() != b.length()) return false;
    for (int i = 0; i < a.length(); i++) {
      if (a.charAt(i) != b.charAt(i)) return false;
    }
    return true;
  }
}
