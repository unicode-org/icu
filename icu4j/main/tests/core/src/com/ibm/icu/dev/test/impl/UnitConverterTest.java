package com.ibm.icu.dev.test.impl;


import com.ibm.icu.impl.units.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class UnitConverterTest {

    @Test
    public void testExtractConvertibility() {
        MeasureUnitImpl source = UnitsParser.parseForIdentifier("square-meter-per-square-hour");
        MeasureUnitImpl target = UnitsParser.parseForIdentifier("hectare-per-square-second");
        ConversionRates conversionRates = new ConversionRates();

        assertEquals(UnitConverter.extractConvertibility(source, target, conversionRates), Convertibility.CONVERTIBLE);
    }
}
