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

    public DecimalQuantity_DualStorageBCD quantity = null;

    /**
     * The number of chars accepted during parsing. This is NOT necessarily the same as the StringSegment offset; "weak"
     * chars, like whitespace, change the offset, but the charsConsumed is not touched until a "strong" char is
     * encountered.
     */
    public int charsConsumed = 0;

    /**
     * Boolean flags (see constants below).
     */
    public int flags = 0;

    /**
     * The prefix string that got consumed.
     */
    public String prefix = null;

    /**
     * The suffix string that got consumed.
     */
    public String suffix = null;

    /**
     * The currency that got consumed.
     */
    public String currencyCode = null;

    public static final int FLAG_NEGATIVE = 0x0001;
    public static final int FLAG_PERCENT = 0x0002;
    public static final int FLAG_PERMILLE = 0x0004;

    /** A Comparator that favors ParsedNumbers with the most chars consumed. */
    public static final Comparator<ParsedNumber> COMPARATOR = new Comparator<ParsedNumber>() {
        @Override
        public int compare(ParsedNumber o1, ParsedNumber o2) {
            return o1.charsConsumed - o2.charsConsumed;
        }
    };

    /**
     * @param other
     */
    public void copyFrom(ParsedNumber other) {
        quantity = other.quantity;
        charsConsumed = other.charsConsumed;
        flags = other.flags;
        prefix = other.prefix;
        suffix = other.suffix;
        currencyCode = other.currencyCode;
    }

    public void setCharsConsumed(StringSegment segment) {
        charsConsumed = segment.getOffset();
    }

    public double getDouble() {
        double d = quantity.toDouble();
        if (0 != (flags & FLAG_NEGATIVE)) {
            d = -d;
        }
        return d;
    }
}
