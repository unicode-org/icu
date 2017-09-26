// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

public interface AffixPatternProvider {
  public static final class Flags {
    public static final int PLURAL_MASK = 0xff;
    public static final int PREFIX = 0x100;
    public static final int NEGATIVE_SUBPATTERN = 0x200;
    public static final int PADDING = 0x400;
  }

  public char charAt(int flags, int i);

  public int length(int flags);

  public boolean hasCurrencySign();

  public boolean positiveHasPlusSign();

  public boolean hasNegativeSubpattern();

  public boolean negativeHasMinusSign();

  public boolean containsSymbolType(int type);
}
