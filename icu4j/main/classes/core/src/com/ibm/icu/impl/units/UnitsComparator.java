package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;

import java.math.BigDecimal;
import java.util.Comparator;

class UnitsComparator implements Comparator<MeasureUnitImpl> {
    @Override
    public int compare(MeasureUnitImpl o1, MeasureUnitImpl o2) {
        Assert.assrt(conversionRates != null);

        UnitConverter fromO1toO2 = new UnitConverter(o1, o2, conversionRates);
        return fromO1toO2.convert(BigDecimal.valueOf(1)).compareTo(BigDecimal.valueOf(1));
    }

    public static void setConversionRates(ConversionRates conversionRates) {
        UnitsComparator.conversionRates = conversionRates;
    }

    private static ConversionRates conversionRates = null;
}
