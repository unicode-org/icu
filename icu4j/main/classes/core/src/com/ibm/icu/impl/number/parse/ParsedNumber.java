// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.Comparator;

import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;

/**
 * @author sffc
 *
 */
public class ParsedNumber {

    public DecimalQuantity_DualStorageBCD quantity;

    /**
     * The number of chars accepted during parsing. This is NOT necessarily the same as the StringSegment offset; "weak"
     * chars, like whitespace, change the offset, but the charsConsumed is not touched until a "strong" char is
     * encountered.
     */
    public int charsConsumed;

    /**
     * Boolean flags (see constants below).
     */
    public int flags;

    /**
     * The prefix string that got consumed.
     */
    public String prefix;

    /**
     * The suffix string that got consumed.
     */
    public String suffix;

    /**
     * The currency that got consumed.
     */
    public String currencyCode;

    public static final int FLAG_NEGATIVE = 0x0001;
    public static final int FLAG_PERCENT = 0x0002;
    public static final int FLAG_PERMILLE = 0x0004;
    public static final int FLAG_HAS_EXPONENT = 0x0008;
    public static final int FLAG_HAS_DEFAULT_CURRENCY = 0x0010;
    public static final int FLAG_HAS_DECIMAL_SEPARATOR = 0x0020;
    public static final int FLAG_NAN = 0x0040;

    /** A Comparator that favors ParsedNumbers with the most chars consumed. */
    public static final Comparator<ParsedNumber> COMPARATOR = new Comparator<ParsedNumber>() {
        @Override
        public int compare(ParsedNumber o1, ParsedNumber o2) {
            return o1.charsConsumed - o2.charsConsumed;
        }
    };

    public ParsedNumber() {
        clear();
    }

    /**
     * Clears the data from this ParsedNumber, in effect failing the current parse.
     */
    public void clear() {
        quantity = null;
        charsConsumed = 0;
        flags = 0;
        prefix = null;
        suffix = null;
        currencyCode = null;
    }

    public void copyFrom(ParsedNumber other) {
        quantity = other.quantity == null ? null : (DecimalQuantity_DualStorageBCD) other.quantity.createCopy();
        charsConsumed = other.charsConsumed;
        flags = other.flags;
        prefix = other.prefix;
        suffix = other.suffix;
        currencyCode = other.currencyCode;
    }

    public void setCharsConsumed(StringSegment segment) {
        charsConsumed = segment.getOffset();
    }

    public boolean seenNumber() {
        return quantity != null || 0 != (flags & FLAG_NAN);
    }

    public double getDouble() {
        if (0 != (flags & FLAG_NAN)) {
            return Double.NaN;
        }
        double d = quantity.toDouble();
        if (0 != (flags & FLAG_NEGATIVE)) {
            d = -d;
        }
        return d;
    }
}
