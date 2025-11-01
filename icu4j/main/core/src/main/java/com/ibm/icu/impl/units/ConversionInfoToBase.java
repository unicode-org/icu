// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.units;

/**
 * Holds information for converting a unit to its base unit.
 */
public class ConversionInfoToBase {
    private final double conversionRateToBaseUnit;
    private final double offsetToBaseUnit;
    private final String baseUnit;
    private final String reciprocalBaseUnit;

    public ConversionInfoToBase(double conversionRateToBaseUnit, double offsetToBaseUnit, String baseUnit,
            String reciprocalBaseUnit) {

        this.conversionRateToBaseUnit = conversionRateToBaseUnit;
        this.offsetToBaseUnit = offsetToBaseUnit;
        this.baseUnit = baseUnit;
        this.reciprocalBaseUnit = reciprocalBaseUnit;
    }

    public double getConversionRateToBaseUnit() {
        return conversionRateToBaseUnit;
    }

    public double getOffsetToBaseUnit() {
        return offsetToBaseUnit;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public String getReciprocalBaseUnit() {
        return reciprocalBaseUnit;
    }

}
