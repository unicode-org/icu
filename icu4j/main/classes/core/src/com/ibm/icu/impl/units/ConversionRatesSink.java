package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.TreeMap;

public class ConversionRatesSink extends UResource.Sink {
    @Override
    public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
        Assert.assrt(Constants.CONVERSION_UNIT_TABLE_NAME.equals(key.toString()));

        UResource.Table conversionRateTable = value.getTable();
        for (int i = 0; conversionRateTable.getKeyAndValue(i, key, value); i++) {
            Assert.assrt(value.getType() == UResourceBundle.TABLE);

            String simpleUnit = key.toString();

            UResource.Table simpleUnitConversionInfo = value.getTable();
            String target = null;
            String factor = null;
            String offset = "0";
            for (int j = 0; simpleUnitConversionInfo.getKeyAndValue(j, key, value); j++) {
                Assert.assrt(value.getType() == UResourceBundle.STRING);


                String keyString = key.toString();
                String valueString = value.toString().replaceAll(" ","");
                if ("target".equals(keyString)) {
                    target = valueString;
                } else if ("factor".equals(keyString)) {
                    factor = valueString;
                } else if ("offset".equals(keyString)) {
                    offset = valueString;
                } else {
                    Assert.fail("The key must be target, factor or offset");
                }
            }

            // HERE a single conversion rate data should be loaded
            Assert.assrt(target != null);
            Assert.assrt(factor != null);

            mapToConversionRate.put(simpleUnit, new ConversionRate(simpleUnit, target, factor, offset));
        }


    }

    public TreeMap<String, ConversionRate> getMapToConversionRate() {
        return mapToConversionRate;
    }

    /**
     * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
     */
    private TreeMap<String, ConversionRate> mapToConversionRate = new TreeMap<>();
}
