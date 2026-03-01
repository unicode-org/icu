// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

public interface AffixPatternProvider {
    public static final class Flags {
        public static final int PLURAL_MASK = 0xff;
        public static final int PREFIX = 0x100;
        public static final int NEGATIVE_SUBPATTERN = 0x200;
        public static final int PADDING = 0x400;
    }

    // Convenience compound flags
    public static final int FLAG_POS_PREFIX = Flags.PREFIX;
    public static final int FLAG_POS_SUFFIX = 0;
    public static final int FLAG_NEG_PREFIX = Flags.PREFIX | Flags.NEGATIVE_SUBPATTERN;
    public static final int FLAG_NEG_SUFFIX = Flags.NEGATIVE_SUBPATTERN;

    public char charAt(int flags, int i);

    public int length(int flags);

    public String getString(int flags);

    public boolean hasCurrencySign();

    public boolean positiveHasPlusSign();

    public boolean hasNegativeSubpattern();

    public boolean negativeHasMinusSign();

    public boolean containsSymbolType(int type);

    /**
     * True if the pattern has a number placeholder like "0" or "#,##0.00"; false if the pattern does not
     * have one. This is used in cases like compact notation, where the pattern replaces the entire
     * number instead of rendering the number.
     */
    public boolean hasBody();
}
