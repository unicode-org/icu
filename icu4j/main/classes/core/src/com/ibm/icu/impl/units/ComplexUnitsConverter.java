package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.util.Measure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Converts from single or compound unit to single, compound or mixed units.
 * For example, from `meter` to `foot+inch`.
 * <p>
 * DESIGN:
 * This class uses `UnitConverter` in order to perform the single converter (i.e. converters from a
 * single unit to another single unit). Therefore, `ComplexUnitsConverter` class contains multiple
 * instances of the `UnitConverter` to perform the conversion.
 */
public class ComplexUnitsConverter {
    /**
     * Constructor of `ComplexUnitsConverter`.
     * NOTE:
     * - inputUnit and outputUnits must be under the same category
     * - e.g. meter to feet and inches --> all of them are length units.
     *
     * @param inputUnit   represents the source unit. (should be single or compound unit).
     * @param outputUnits represents the output unit. could be any type. (single, compound or mixed).
     */
    public ComplexUnitsConverter(MeasureUnitImpl inputUnit, MeasureUnitImpl outputUnits,
                                 ConversionRates conversionRates) {


        units_ = outputUnits.getMeasureUnits();
        Assert.assrt(!units_.isEmpty());

        // Sort the units in a descending order.
        UnitsComparator.setConversionRates(conversionRates);
        Collections.sort(this.units_, Collections.reverseOrder(new UnitsComparator()));


        // If the `outputUnits` is `UMEASURE_UNIT_MIXED` such as `foot+inch`. Thus means there is more than one unit
        //  and In this case we need more converters to convert from the `inputUnit` to the first unit in the
        //  `outputUnits`. Then, a converter from the first unit in the `outputUnits` to the second unit and so on.
        //      For Example:
        //          - inputUnit is `meter`
        //          - outputUnits is `foot+inch`
        //              - Therefore, we need to have two converters:
        //                      1. a converter from `meter` to `foot`
        //                      2. a converter from `foot` to `inch`
        //          - Therefore, if the input is `2 meter`:
        //              1. convert `meter` to `foot` --> 2 meter to 6.56168 feet
        //              2. convert the residual of 6.56168 feet (0.56168) to inches, which will be (6.74016
        //              inches)
        //              3. then, the final result will be (6 feet and 6.74016 inches)
        unitConverters_ = new ArrayList<>();
        for (int i = 0, n = units_.size(); i < n; i++) {
            if (i == 0) { // first element
                unitConverters_.add(new UnitConverter(inputUnit, units_.get(i), conversionRates));
            } else {
                unitConverters_.add(new UnitConverter(units_.get(i - 1), units_.get(i), conversionRates));
            }
        }
    }

    /**
     * Returns true if the specified `quantity` of the `inputUnit`, expressed in terms of the biggest
     * unit in the MeasureUnit `outputUnit`, is greater than or equal to `limit`.
     * <p>
     * For example, if the input unit is `meter` and the target unit is `foot+inch`. Therefore, this
     * function will convert the `quantity` from `meter` to `foot`, then, it will compare the value in
     * `foot` with the `limit`.
     */
    public boolean greaterThanOrEqual(BigDecimal quantity, BigDecimal limit) {
        Assert.assrt(!units_.isEmpty());

        // NOTE: First converter converts to the biggest quantity.
        return unitConverters_.get(0).convert(quantity).compareTo(limit) >= 0;
    }

    /**
     * Returns outputMeasures which is an array with the corresponding values.
     * - E.g. converting meters to feet and inches.
     * 1 meter --> 3 feet, 3.3701 inches
     * NOTE:
     * the smallest element is the only element that could have fractional values. And all
     * other elements are floored to the nearest integer
     */
    public Pair<ArrayList<Measure>, ArrayList<Pair<MeasureUnitImpl, BigDecimal>>> convert(BigDecimal quantity) {
        ArrayList<Measure> result = new ArrayList<>();
        ArrayList<Pair<MeasureUnitImpl, BigDecimal>> tempResult = new ArrayList<>();

        for (int i = 0, n = unitConverters_.size(); i < n; ++i) {
            quantity = (unitConverters_.get(i)).convert(quantity);

            if (i < n - 1) {
                Number newQuantity = Math.floor(quantity.doubleValue());

                // NOTE: Measure would own its MeasureUnit. // TODO?
              //  result.add(new Measure(newQuantity, units_.get(i).build()));
                tempResult.add(Pair.of(units_.get(i), BigDecimal.valueOf(newQuantity.doubleValue())));

                // Keep the residual of the quantity.
                //   For example: `3.6 feet`, keep only `0.6 feet`
                quantity = quantity.subtract(BigDecimal.valueOf(newQuantity.longValue()));
            } else { // LAST ELEMENT
                // NOTE: Measure would own its MeasureUnit. // TODO?
               // result.add(new Measure(quantity, units_.get(i).build()));
                tempResult.add(Pair.of(units_.get(i), BigDecimal.valueOf(quantity.doubleValue())));
            }
        }

        return Pair.of(result, tempResult);
    }

    private ArrayList<UnitConverter> unitConverters_;
    private ArrayList<MeasureUnitImpl> units_;
}

