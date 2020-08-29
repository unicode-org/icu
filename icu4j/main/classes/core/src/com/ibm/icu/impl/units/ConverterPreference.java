/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import java.math.BigDecimal;

/**
 * Contains the complex unit converter and the limit which representing the smallest value that the
 * converter should accept. For example, if the converter is converting to `foot+inch` and the limit
 * equals 3.0, thus means the converter should not convert to a value less than `3.0 feet`.
 *
 * NOTE:
 *    if the limit doest not has a value `i.e. (std::numeric_limits<double>::lowest())`, this mean there
 *    is no limit for the converter.
 */
public class ConverterPreference {
    ComplexUnitsConverter converter;
    BigDecimal limit;
    String precision;

    // In case there is no limit, the limit will be -inf.
    public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl outputUnits,
                               String precision, ConversionRates conversionRates) {
        this(source, outputUnits, BigDecimal.valueOf(Double.MIN_VALUE), precision,
                conversionRates);
    }

    public ConverterPreference(MeasureUnitImpl source, MeasureUnitImpl outputUnits,
                               BigDecimal limit, String precision, ConversionRates conversionRates) {
        this.converter = new ComplexUnitsConverter(source, outputUnits, conversionRates);
        this.limit = limit;
        this.precision = precision;
    }
}
