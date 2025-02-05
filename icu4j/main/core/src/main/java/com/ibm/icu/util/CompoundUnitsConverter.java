// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.math.BigDecimal;

import com.ibm.icu.impl.units.ConversionRates;

/**
 * Represents a compound units converter.
 * 
 * @draft ICU 77
 */
public class CompoundUnitsConverter {
    private com.ibm.icu.impl.units.UnitsConverter converter;

    private CompoundUnitsConverter(com.ibm.icu.impl.units.UnitsConverter converter) {
        this.converter = converter;
    }

    protected CompoundUnitsConverter(MeasureUnit from, MeasureUnit to) {
        // TODO: extract conversion rates from the data only once.
        ConversionRates conversionRates = new ConversionRates();
        this.converter = new com.ibm.icu.impl.units.UnitsConverter(from.getCopyOfMeasureUnitImpl(),
                to.getCopyOfMeasureUnitImpl(), conversionRates);
    }

    /**
     * Converts a value from the source unit to the target unit.
     * 
     * @param value The value to convert.
     * @return The converted value.
     * @draft ICU 77
     */
    public BigDecimal convert(BigDecimal value) {
        return this.converter.convert(value);
    }

    /**
     * Clones the CompoundUnitsConverter.
     * 
     * @return A new CompoundUnitsConverter object.
     * @draft ICU 77
     */
    public CompoundUnitsConverter clone() {
        return new CompoundUnitsConverter(this.converter.clone());
    }

}
