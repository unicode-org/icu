// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import newapi.NumberFormatter.IntegerWidth;

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
    return new IntegerWidthImpl(minInt, maxInt);
  }
}
