// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.util;

import com.ibm.icu.number.NumberFormatter;

/**
 * Dimensionless unit for percent and permille.
 * @see NumberFormatter
 * @draft ICU 68
 * @provisional This API might change or be removed in a future release.
 */
public final class NoUnit {
    /**
     * Constant for the base unit (dimensionless and no scaling).
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant equals null.
     *
     * @draft ICU 68
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit BASE = null;

    /**
     * Constant for the percent unit, or 1/100 of a base unit.
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant is equivalent to MeasureUnit.PERCENT.
     *
     * @draft ICU 68
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit PERCENT = MeasureUnit.PERCENT;

    /**
     * Constant for the permille unit, or 1/100 of a base unit.
     *
     * Prior to ICU 68, this constant equaled an instance of NoUnit.
     *
     * Since ICU 68, this constant is equivalent to MeasureUnit.PERMILLE.
     *
     * @draft ICU 68
     * @provisional This API might change or be removed in a future release.
     */
    public static final MeasureUnit PERMILLE = MeasureUnit.PERMILLE;

    // This class is a namespace not intended to be instantiated:
    private NoUnit() {}
}
