// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

/** @author sffc */
public class ThingsNeedingNewHome {
  public static final String FALLBACK_PADDING_STRING = "\u0020"; // i.e. a space

  public enum PadPosition {
    BEFORE_PREFIX,
    AFTER_PREFIX,
    BEFORE_SUFFIX,
    AFTER_SUFFIX;

    public static PadPosition fromOld(int old) {
      switch (old) {
        case com.ibm.icu.text.DecimalFormat.PAD_BEFORE_PREFIX:
          return PadPosition.BEFORE_PREFIX;
        case com.ibm.icu.text.DecimalFormat.PAD_AFTER_PREFIX:
          return PadPosition.AFTER_PREFIX;
        case com.ibm.icu.text.DecimalFormat.PAD_BEFORE_SUFFIX:
          return PadPosition.BEFORE_SUFFIX;
        case com.ibm.icu.text.DecimalFormat.PAD_AFTER_SUFFIX:
          return PadPosition.AFTER_SUFFIX;
        default:
          throw new IllegalArgumentException("Don't know how to map " + old);
      }
    }

    public int toOld() {
      switch (this) {
        case BEFORE_PREFIX:
          return com.ibm.icu.text.DecimalFormat.PAD_BEFORE_PREFIX;
        case AFTER_PREFIX:
          return com.ibm.icu.text.DecimalFormat.PAD_AFTER_PREFIX;
        case BEFORE_SUFFIX:
          return com.ibm.icu.text.DecimalFormat.PAD_BEFORE_SUFFIX;
        case AFTER_SUFFIX:
          return com.ibm.icu.text.DecimalFormat.PAD_AFTER_SUFFIX;
        default:
          return -1; // silence compiler errors
      }
    }
  }

  /**
   * Returns true if the currency is set in The property bag or if currency symbols are present in
   * the prefix/suffix pattern.
   */
  public static boolean useCurrency(Properties properties) {
    return ((properties.getCurrency() != null)
        || properties.getCurrencyPluralInfo() != null
        || properties.getCurrencyUsage() != null
        || AffixPatternUtils.hasCurrencySymbols(properties.getPositivePrefixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getPositiveSuffixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getNegativePrefixPattern())
        || AffixPatternUtils.hasCurrencySymbols(properties.getNegativeSuffixPattern()));
  }
}
