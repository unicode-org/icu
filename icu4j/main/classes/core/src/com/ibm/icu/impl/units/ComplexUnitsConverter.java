// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Measure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

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
    public static final BigDecimal EPSILON = BigDecimal.valueOf(Math.ulp(1.0));
    public static final BigDecimal EPSILON_MULTIPLIER = BigDecimal.valueOf(1).add(EPSILON);
    private ArrayList<UnitConverter> unitConverters_;
    // Individual units of mixed units, sorted big to small
    private List<MeasureUnitImpl> units_;
    private MeasureUnitImpl inputUnit_;

    /**
     * Constructs <code>ComplexUnitsConverter</code> for an <code>inputUnit</code> that could be Single, Compound or Mixed.
     * In case of:
     * 1- Single and Compound units,
     * the conversion will not perform anything, the input will be equal to the output.
     * 2- Mixed Unit
     * the conversion will consider the input in the biggest unit. and will convert it to be spread throw
     * the input units. For example: if input unit is "inch-and-foot", and the input is 2.5. The converter
     * will consider the input value in "foot", because foot is the biggest unit. Then, it will convert
     * 2.5 feet to "inch-and-foot".
     *
     * @param inputUnit represents the input unit. could be any type. (single, compound or mixed).
     */
    public ComplexUnitsConverter(MeasureUnitImpl inputUnit, ConversionRates conversionRates) {
        this.units_ = inputUnit.extractIndividualUnitsWithIndex();
        // Sort the units in a descending order.
        Collections.sort(
                this.units_,
                Collections.reverseOrder(new MeasureUnitImpl.MeasureUnitImplComparator(conversionRates)));
        assert (!this.units_.isEmpty());

        this.inputUnit_ = this.units_.get(0);
        this.init(conversionRates);
    }

    /**
     * Constructs <code>ComplexUnitsConverter</code>
     * NOTE:
     * - inputUnit and outputUnits must be under the same category
     * - e.g. meter to feet and inches --> all of them are length units.
     *
     * @param inputUnit   represents the source unit. (should be single or compound unit).
     * @param outputUnits represents the output unit. could be any type. (single, compound or mixed).
     */
    public ComplexUnitsConverter(MeasureUnitImpl inputUnit, MeasureUnitImpl outputUnits,
                                 ConversionRates conversionRates) {
        this.inputUnit_ = inputUnit;
        this.units_ = outputUnits.extractIndividualUnitsWithIndex();
        assert (!this.units_.isEmpty());

        // Sort the units in a descending order.
        Collections.sort(
                this.units_,
                Collections.reverseOrder(new MeasureUnitImpl.MeasureUnitImplComparator(conversionRates)));

        this.init(conversionRates);
    }

    private void init(ConversionRates conversionRates) {

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
                unitConverters_.add(new UnitConverter(this.inputUnit_, units_.get(i), conversionRates));
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
        assert !units_.isEmpty();

        // NOTE: First converter converts to the biggest quantity.
        return unitConverters_.get(0).convert(quantity).multiply(EPSILON_MULTIPLIER).compareTo(limit) >= 0;
    }

    /**
     * Returns outputMeasures which is an array with the corresponding values.
     * - E.g. converting meters to feet and inches.
     * 1 meter --> 3 feet, 3.3701 inches
     * NOTE:
     * the smallest element is the only element that could have fractional values. And all
     * other elements are floored to the nearest integer
     */
    public List<Measure> convert(BigDecimal quantity, Precision rounder) {
        List<Measure> result = new ArrayList<>(unitConverters_.size());
        BigDecimal sign = BigDecimal.ONE;
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            quantity = quantity.abs();
            sign = sign.negate();
        }

        // For N converters:
        // - the first converter converts from the input unit to the largest
        //   unit,
        // - N-1 converters convert to bigger units for which we want integers,
        // - the Nth converter (index N-1) converts to the smallest unit, which
        //   isn't (necessarily) an integer.
        List<BigDecimal> intValues = new ArrayList<>(unitConverters_.size() - 1);

        for (int i = 0, n = unitConverters_.size(); i < n; ++i) {
            quantity = (unitConverters_.get(i)).convert(quantity);

            if (i < n - 1) {
                // The double type has 15 decimal digits of precision. For choosing
                // whether to use the current unit or the next smaller unit, we
                // therefore nudge up the number with which the thresholding
                // decision is made. However after the thresholding, we use the
                // original values to ensure unbiased accuracy (to the extent of
                // double's capabilities).
                BigDecimal roundedQuantity =
                        quantity.multiply(EPSILON_MULTIPLIER).setScale(0, RoundingMode.FLOOR);
                intValues.add(roundedQuantity);

                // Keep the residual of the quantity.
                //   For example: `3.6 feet`, keep only `0.6 feet`
                quantity = quantity.subtract(roundedQuantity);
                if (quantity.compareTo(BigDecimal.ZERO) == -1) {
                    quantity = BigDecimal.ZERO;
                }
            } else { // LAST ELEMENT
                if (rounder == null) {
                    // Nothing to do for the last element.
                    break;
                }

                // Round the last value
                // TODO(ICU-21288): get smarter about precision for mixed units.
                DecimalQuantity quant = new DecimalQuantity_DualStorageBCD(quantity);
                rounder.apply(quant);
                quantity = quant.toBigDecimal();
                if (i == 0) {
                    // Last element is also the first element, so we're done
                    break;
                }

                // Check if there's a carry, and bubble it back up the resulting intValues.
                BigDecimal carry = unitConverters_.get(i)
                        .convertInverse(quantity)
                        .multiply(EPSILON_MULTIPLIER)
                        .setScale(0, RoundingMode.FLOOR);
                if (carry.compareTo(BigDecimal.ZERO) <= 0) { // carry is not greater than zero
                    break;
                }
                quantity = quantity.subtract(unitConverters_.get(i).convert(carry));
                intValues.set(i - 1, intValues.get(i - 1).add(carry));

                // We don't use the first converter: that one is for the input unit
                for (int j = i - 1; j > 0; j--) {
                    carry = unitConverters_.get(j)
                            .convertInverse(intValues.get(j))
                            .multiply(EPSILON_MULTIPLIER)
                            .setScale(0, RoundingMode.FLOOR);
                    if (carry.compareTo(BigDecimal.ZERO) <= 0) { // carry is not greater than zero
                        break;
                    }
                    intValues.set(j, intValues.get(j).subtract(unitConverters_.get(j).convert(carry)));
                    intValues.set(j - 1, intValues.get(j - 1).add(carry));
                }
            }
        }

        // Initialize empty result.
        List<Measure> result = new ArrayList<>(unitConverters_.size());
        for (int i = 0; i < unitConverters_.size(); i++) {
            result.add(null);
        }

        // Package values into Measure instances in result:
        for (int i = 0, n = unitConverters_.size(); i < n; ++i) {
            if (i < n - 1) {
                result.set (units_.get(i).index , new Measure(intValues.get(i).multiply(sign), units_.get(i).build()));
            } else {
                result.set (units_.get(i).index , new Measure(quantity.multiply(sign), units_.get(i).build()));
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "ComplexUnitsConverter [unitConverters_=" + unitConverters_ + ", units_=" + units_ + "]";
    }
}
