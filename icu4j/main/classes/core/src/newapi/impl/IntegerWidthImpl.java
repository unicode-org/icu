// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import newapi.NumberFormatter.IntegerWidth;
import newapi.NumberFormatter.Rounding;

public final class IntegerWidthImpl extends IntegerWidth.Internal {
  public final int minInt;
  public final int maxInt;

  public static final IntegerWidthImpl DEFAULT = new IntegerWidthImpl();

  /** Default constructor */
  public IntegerWidthImpl() {
    this(1, Integer.MAX_VALUE);
  }

  public IntegerWidthImpl(int minInt, int maxInt) {
    this.minInt = minInt;
    this.maxInt = maxInt;
  }

  @Override
  public IntegerWidthImpl truncateAt(int maxInt) {
    if (maxInt == this.maxInt) {
      return this;
    } else if (maxInt >= 0 && maxInt < Rounding.MAX_VALUE) {
      return new IntegerWidthImpl(minInt, maxInt);
    } else if (maxInt == Integer.MAX_VALUE) {
      return new IntegerWidthImpl(minInt, maxInt);
    } else {
      throw new IllegalArgumentException(
          "Integer digits must be between 0 and " + Rounding.MAX_VALUE);
    }
  }
}
