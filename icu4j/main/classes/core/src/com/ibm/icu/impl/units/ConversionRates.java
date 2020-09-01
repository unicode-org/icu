/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.UResourceBundle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.TreeMap;

public class ConversionRates {

    public ConversionRates() {
        // Read the conversion rates from the data (units.txt).
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        ConversionRatesSink sink = new ConversionRatesSink();
        resource.getAllItemsWithFallback(UnitsData.Constants.CONVERSION_UNIT_TABLE_NAME, sink);
        this.mapToConversionRate = sink.getMapToConversionRate();
    }

    /**
     * Extracts the factor from a `SingleUnitImpl` to its Basic Unit.
     *
     * @param singleUnit
     * @return
     */
    private UnitConverter.Factor getFactorToBase(SingleUnitImpl singleUnit) {
        int power = singleUnit.getDimensionality();
        MeasureUnit.SIPrefix siPrefix = singleUnit.getSiPrefix();
        UnitConverter.Factor result = UnitConverter.Factor.precessFactor(mapToConversionRate.get(singleUnit.getSimpleUnit()).getConversionRate());

        return result.applySiPrefix(siPrefix).power(power); // NOTE: you must apply the SI prefixes before the power.
    }

    public UnitConverter.Factor getFactorToBase(MeasureUnitImpl measureUnit) {
        UnitConverter.Factor result = new UnitConverter.Factor();
        for (SingleUnitImpl singleUnit :
                measureUnit.getSingleUnits()) {
            result = result.multiply(getFactorToBase(singleUnit));
        }

        return result;
    }

    protected BigDecimal getOffset(MeasureUnitImpl source, MeasureUnitImpl target, UnitConverter.Factor
            sourceToBase, UnitConverter.Factor targetToBase, UnitConverter.Convertibility convertibility) {
        if (convertibility != UnitConverter.Convertibility.CONVERTIBLE) return BigDecimal.valueOf(0);
        if (!(checkSimpleUnit(source) && checkSimpleUnit(target))) return BigDecimal.valueOf(0);

        String sourceSimpleIdentifier = source.getSingleUnits().get(0).getSimpleUnit();
        String targetSimpleIdentifier = target.getSingleUnits().get(0).getSimpleUnit();

        BigDecimal sourceOffset = this.mapToConversionRate.get(sourceSimpleIdentifier).getOffset();
        BigDecimal targetOffset = this.mapToConversionRate.get(targetSimpleIdentifier).getOffset();
        return sourceOffset
                .subtract(targetOffset)
                .divide(targetToBase.getConversionRate(), MathContext.DECIMAL128);


    }

    public MeasureUnitImpl getBasicMeasureUnitImplWithoutSIPrefix(MeasureUnitImpl measureUnit) {
        ArrayList<SingleUnitImpl> baseUnits =  this.getBasicUnitsWithoutSIPrefix(measureUnit);

        MeasureUnitImpl result = new MeasureUnitImpl();
        for (SingleUnitImpl baseUnit :
                baseUnits) {
            result.appendSingleUnit(baseUnit);
        }

        return result;
    }


    public ArrayList<SingleUnitImpl> getBasicUnitsWithoutSIPrefix(MeasureUnitImpl measureUnitImpl) {
        ArrayList<SingleUnitImpl> result = new ArrayList<>();
        ArrayList<SingleUnitImpl> singleUnits = measureUnitImpl.getSingleUnits();
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            result.addAll(getBasicUnitsWithoutSIPrefix(singleUnit));
        }

        return result;
    }

    /**
     * @param singleUnit
     * @return The bese units in the `SingleUnitImpl` with applying the dimensionality only and not the SI prefix.
     * <p>
     * NOTE:
     * This method is helpful when checking the convertibility because no need to check convertibility.
     */
    public ArrayList<SingleUnitImpl> getBasicUnitsWithoutSIPrefix(SingleUnitImpl singleUnit) {
        String target = mapToConversionRate.get(singleUnit.getSimpleUnit()).getTarget();
        MeasureUnitImpl targetImpl = MeasureUnitImpl.UnitsParser.parseForIdentifier(target);

        // Each unit must be powered by the same dimension
        targetImpl.applyDimensionality(singleUnit.getDimensionality());

        // NOTE: we do not apply SI prefixes.

        return targetImpl.getSingleUnits();
    }

    /**
     * Checks if the `MeasureUnitImpl` is simple or not.
     *
     * @param measureUnitImpl
     * @return true if the `MeasureUnitImpl` is simple, false otherwise.
     */
    private boolean checkSimpleUnit(MeasureUnitImpl measureUnitImpl) {
        if (measureUnitImpl.getComplexity() != MeasureUnit.Complexity.SINGLE) return false;
        SingleUnitImpl singleUnit = measureUnitImpl.getSingleUnits().get(0);

        if (singleUnit.getSiPrefix() != MeasureUnit.SIPrefix.ONE) return false;
        if (singleUnit.getDimensionality() != 1) return false;

        return true;
    }

    /**
     * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
     */
    private TreeMap<String, ConversionRate> mapToConversionRate;

    public static class ConversionRatesSink extends UResource.Sink {
        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            Assert.assrt(UnitsData.Constants.CONVERSION_UNIT_TABLE_NAME.equals(key.toString()));

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

    public static class ConversionRate {

        public ConversionRate(String simpleUnit, String target, String conversionRate, String offset) {
            this.simpleUnit = simpleUnit;
            this.target = target;
            this.conversionRate = conversionRate;
            this.offset = forNumberWithDivision(offset);
        }

        /**
         * @return the base unit.
         * <p>
         * For example:
         * ("meter", "foot", "inch", "mile" ... etc.) have "meter" as a base/root unit.
         */
        public String getTarget() {
            return this.target;
        }

        /**
         *
         * @return The offset from this unit to the base unit.
         */
        public BigDecimal getOffset() {
            return this.offset;
        }

        /**
         *
         * @return The conversion rate from this unit to the base unit.
         */
        public String getConversionRate() {
            return conversionRate;
        }

        private final String simpleUnit;
        private final String target;
        private final String conversionRate;
        private final BigDecimal offset;

        private static BigDecimal forNumberWithDivision(String numberWithDivision) {
            String[] numbers = numberWithDivision.split("/");
            Assert.assrt(numbers.length <= 2);

            if (numbers.length == 1) {
                return new BigDecimal(numbers[0]);
            }

            return new BigDecimal(numbers[0]).divide(new BigDecimal(numbers[1]), MathContext.DECIMAL128);
        }


    }
}
