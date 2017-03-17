// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import com.ibm.icu.impl.number.Format.AfterFormat;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;

public class PaddingFormat implements AfterFormat {
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

  public static interface IProperties {

    static int DEFAULT_FORMAT_WIDTH = 0;

    /** @see #setFormatWidth */
    public int getFormatWidth();

    /**
     * Sets the minimum width of the string output by the formatting pipeline. For example, if
     * padding is enabled and paddingWidth is set to 6, formatting the number "3.14159" with the
     * pattern "0.00" will result in "··3.14" if '·' is your padding string.
     *
     * <p>If the number is longer than your padding width, the number will display as if no padding
     * width had been specified, which may result in strings longer than the padding width.
     *
     * <p>Width is counted in UTF-16 code units.
     *
     * @param formatWidth The output width.
     * @return The property bag, for chaining.
     * @see #setPadPosition
     * @see #setPadString
     */
    public IProperties setFormatWidth(int formatWidth);

    static String DEFAULT_PAD_STRING = null;

    /** @see #setPadString */
    public String getPadString();

    /**
     * Sets the string used for padding. The string should contain a single character or grapheme
     * cluster.
     *
     * <p>Must be used in conjunction with {@link #setFormatWidth}.
     *
     * @param paddingString The padding string. Defaults to an ASCII space (U+0020).
     * @return The property bag, for chaining.
     * @see #setFormatWidth
     */
    public IProperties setPadString(String paddingString);

    static PadPosition DEFAULT_PAD_POSITION = null;

    /** @see #setPadPosition */
    public PadPosition getPadPosition();

    /**
     * Sets the location where the padding string is to be inserted to maintain the padding width:
     * one of BEFORE_PREFIX, AFTER_PREFIX, BEFORE_SUFFIX, or AFTER_SUFFIX.
     *
     * <p>Must be used in conjunction with {@link #setFormatWidth}.
     *
     * @param padPosition The output width.
     * @return The property bag, for chaining.
     * @see #setFormatWidth
     */
    public IProperties setPadPosition(PadPosition padPosition);
  }

  public static final String FALLBACK_PADDING_STRING = "\u0020"; // i.e. a space

  public static boolean usePadding(IProperties properties) {
    return properties.getFormatWidth() != IProperties.DEFAULT_FORMAT_WIDTH;
  }

  public static AfterFormat getInstance(IProperties properties) {
    return new PaddingFormat(
        properties.getFormatWidth(),
        properties.getPadString(),
        properties.getPadPosition());
  }

  // Properties
  private final int paddingWidth;
  private final String paddingString;
  private final PadPosition paddingLocation;

  private PaddingFormat(
      int paddingWidth, String paddingString, PadPosition paddingLocation) {
    this.paddingWidth = paddingWidth > 0 ? paddingWidth : 10; // TODO: Is this a sensible default?
    this.paddingString = paddingString != null ? paddingString : FALLBACK_PADDING_STRING;
    this.paddingLocation =
        paddingLocation != null ? paddingLocation : PadPosition.BEFORE_PREFIX;
  }

  @Override
  public int after(ModifierHolder mods, NumberStringBuilder string, int leftIndex, int rightIndex) {

    // TODO: Count code points instead of code units?
    int requiredPadding = paddingWidth - (rightIndex - leftIndex) - mods.totalLength();

    if (requiredPadding <= 0) {
      // Skip padding, but still apply modifiers to be consistent
      return mods.applyAll(string, leftIndex, rightIndex);
    }

    int length = 0;
    if (paddingLocation == PadPosition.AFTER_PREFIX) {
      length += addPadding(requiredPadding, string, leftIndex);
    } else if (paddingLocation == PadPosition.BEFORE_SUFFIX) {
      length += addPadding(requiredPadding, string, rightIndex);
    }
    length += mods.applyAll(string, leftIndex, rightIndex + length);
    if (paddingLocation == PadPosition.BEFORE_PREFIX) {
      length += addPadding(requiredPadding, string, leftIndex);
    } else if (paddingLocation == PadPosition.AFTER_SUFFIX) {
      length += addPadding(requiredPadding, string, rightIndex + length);
    }

    return length;
  }

  private int addPadding(int requiredPadding, NumberStringBuilder string, int index) {
    for (int i = 0; i < requiredPadding; i++) {
      string.insert(index, paddingString, null);
    }
    return paddingString.length() * requiredPadding;
  }

  @Override
  public void export(Properties properties) {
    properties.setFormatWidth(paddingWidth);
    properties.setPadString(paddingString);
    properties.setPadPosition(paddingLocation);
  }
}
