// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.util;

import com.ibm.icu.number.NumberFormatter;

/**
 * Dimensionless unit for percent and permille.
 * @see NumberFormatter
 * @draft ICU 60
 * @provisional This API might change or be removed in a future release.
 */
public class NoUnit extends MeasureUnit {
    private static final long serialVersionUID = 2467174286237024095L;

    /**
     * Constant for the base unit (dimensionless and no scaling).
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    public static final NoUnit BASE =
        (NoUnit) MeasureUnit.internalGetInstance("none", "base");

    /**
     * Constant for the percent unit, or 1/100 of a base unit.
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    public static final NoUnit PERCENT =
        (NoUnit) MeasureUnit.internalGetInstance("none", "percent");

    /**
     * Constant for the permille unit, or 1/100 of a base unit.
     *
     * @draft ICU 60
     * @provisional This API might change or be removed in a future release.
     */
    public static final NoUnit PERMILLE =
        (NoUnit) MeasureUnit.internalGetInstance("none", "permille");


    /**
     * Package local constructor. This class is not designed for subclassing
     * by ICU users.
     *
     * @param subType   The unit subtype.
     */
    NoUnit(String subType) {
        super("none", subType);
    }
}
