/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

public class UnitConverter {
    /**
     * Constructor of `UnitConverter`.
     * NOTE:
     * - source and target must be under the same category
     * - e.g. meter to mile --> both of them are length units.
     *
     * @param source          represents the source unit.
     * @param target          represents the target unit.
     * @param conversionRates contains all the needed conversion rates.
     */
    public UnitConverter(MeasureUnitImpl source, MeasureUnitImpl target, ConversionRates conversionRates) {
        // TODO: calculate offset
        this.offset = BigDecimal.valueOf(0);

        Convertibility convertibility = extractConvertibility(source, target, conversionRates);
        Assert.assrt(convertibility == Convertibility.CONVERTIBLE || convertibility == Convertibility.RECIPROCAL);

        Factor sourceToBase = conversionRates.getFactorToBase(source);
        Factor targetToBase = conversionRates.getFactorToBase(target);

        if (convertibility == Convertibility.CONVERTIBLE) {
            this.conversionRate = sourceToBase.divide(targetToBase).getConversionRate();
        } else {
            this.conversionRate = sourceToBase.multiply(targetToBase).getConversionRate();
        }
    }

    public BigDecimal convert(BigDecimal inputValue) {
        return inputValue.multiply(this.conversionRate).add(offset);
    }

    static public Convertibility extractConvertibility(MeasureUnitImpl source, MeasureUnitImpl target, ConversionRates conversionRates) {
        ArrayList<SingleUnitImpl> sourceSingleUnits = conversionRates.getBasicUnitsWithoutSIPrefix(source);
        ArrayList<SingleUnitImpl> targetSingleUnits = conversionRates.getBasicUnitsWithoutSIPrefix(target);

        HashMap dimensionMap = new HashMap<String, Integer>();

        insertInMap(dimensionMap, sourceSingleUnits, 1);
        insertInMap(dimensionMap, targetSingleUnits, -1);

        if (areDimensionsZeroes(dimensionMap)) return Convertibility.CONVERTIBLE;

        insertInMap(dimensionMap, targetSingleUnits, 2);
        if (areDimensionsZeroes(dimensionMap)) return Convertibility.RECIPROCAL;

        return Convertibility.INCONVERTIBLE;
    }

    private BigDecimal conversionRate;
    private BigDecimal offset;


    /**
     * Helpers
     */
    private static void insertInMap(HashMap<String, Integer> dimensionMap, ArrayList<SingleUnitImpl> singleUnits, int multiplier) {
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            if (dimensionMap.containsKey(singleUnit.getSimpleUnit())) {
                dimensionMap.put(singleUnit.getSimpleUnit(), dimensionMap.get(singleUnit.getSimpleUnit()) + singleUnit.getDimensionality() * multiplier);
            } else {
                dimensionMap.put(singleUnit.getSimpleUnit(), singleUnit.getDimensionality() * multiplier);
            }
        }
    }

    private static boolean areDimensionsZeroes(HashMap<String, Integer> dimensionMap) {
        for (Integer value :
                dimensionMap.values()) {
            if (!value.equals(0)) return false;
        }

        return true;
    }

}


