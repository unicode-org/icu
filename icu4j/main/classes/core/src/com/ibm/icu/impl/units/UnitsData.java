package com.ibm.icu.impl.units;

public class UnitsData {

    public UnitsData () {
        this.conversionRates = new ConversionRates();
    }

    public ConversionRates getConversionRates() {
        return conversionRates;
    }

    private ConversionRates conversionRates;

}
