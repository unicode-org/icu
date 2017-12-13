// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;

/**
 * @author sffc
 *
 */
public class ParsedNumber {

    public DecimalQuantity_DualStorageBCD quantity = null;
    public int charsConsumed = 0;
    public int flags = 0;
    public String prefix = null;
    public String suffix = null;
    public int scientificAdjustment = 0;
    public String currencyCode = null;

    public static final int FLAG_NEGATIVE = 0x0001;
    public static final int FLAG_PERCENT = 0x0002;
    public static final int FLAG_PERMILLE = 0x0004;

    /**
     * @param other
     */
    public void copyFrom(ParsedNumber other) {
        quantity = other.quantity;
        charsConsumed = other.charsConsumed;
        flags = other.flags;
        prefix = other.prefix;
        suffix = other.suffix;
        scientificAdjustment = other.scientificAdjustment;
        currencyCode = other.currencyCode;
    }

    public void setCharsConsumed(StringSegment segment) {
        charsConsumed = segment.getOffset();
    }

    public double getDouble() {
        DecimalQuantity copy = quantity.createCopy();
        copy.adjustMagnitude(scientificAdjustment);
        double d = copy.toDouble();
        if (0 != (flags & FLAG_NEGATIVE)) {
            d = -d;
        }
        return d;
    }
}
