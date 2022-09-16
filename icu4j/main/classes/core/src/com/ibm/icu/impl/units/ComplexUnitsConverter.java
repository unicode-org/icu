// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Measure;

/**
 * Converts from single or compound unit to single, compound or mixed units. For example, from `meter` to `foot+inch`.
 * <p>
 * DESIGN: This class uses <code>UnitsConverter</code> in order to perform the single converter (i.e. converters from
 * a single unit to another single unit). Therefore, <code>ComplexUnitsConverter</code> class contains multiple
 * instances of the <code>UnitsConverter</code> to perform the conversion.
 */
public class ComplexUnitsConverter {
    public static final BigDecimal EPSILON = BigDecimal.valueOf(Math.ulp(1.0));
    public static final BigDecimal EPSILON_MULTIPLIER = BigDecimal.valueOf(1).add(EPSILON);

    // TODO(ICU-21937): Make it private after submitting the public units conversion API.
    public ArrayList<UnitsConverter> unitsConverters_;
    /**
     * Individual units of mixed units, sorted big to small, with indices
     * indicating the requested output mixed unit order.
     */
    // TODO(ICU-21937): Make it private after submitting the public units conversion API.
    public List<MeasureUnitImpl.MeasureUnitImplWithIndex> units_;
    private MeasureUnitImpl inputUnit_;

    /**
     * Constructs <code>ComplexUnitsConverter</code> for an <code>inputUnit</code> that could be Single, Compound or
     * Mixed. In case of: 1- Single and Compound units, the conversion will not perform anything, the input will be
     * equal to the output. 2- Mixed Unit the conversion will consider the input in the biggest unit. and will convert
     * it to be spread throw the input units. For example: if input unit is "inch-and-foot", and the input is 2.5. The
     * converter will consider the input value in "foot", because foot is the biggest unit. Then, it will convert 2.5
     * feet to "inch-and-foot".
     *
     * @param targetUnit
     *            represents the input unit. could be any type. (single, compound or mixed).
     */
    public ComplexUnitsConverter(MeasureUnitImpl targetUnit, ConversionRates conversionRates) {
        this.units_ = targetUnit.extractIndividualUnitsWithIndices();
        assert (!this.units_.isEmpty());

        // Assign the biggest unit to inputUnit_.
        this.inputUnit_ = this.units_.get(0).unitImpl;
        MeasureUnitImpl.MeasureUnitImplComparator comparator = new MeasureUnitImpl.MeasureUnitImplComparator(
                conversionRates);
        for (MeasureUnitImpl.MeasureUnitImplWithIndex unitWithIndex : this.units_) {
            if (comparator.compare(unitWithIndex.unitImpl, this.inputUnit_) > 0) {
                this.inputUnit_ = unitWithIndex.unitImpl;
            }
        }

        this.init(conversionRates);
    }

    /**
     * Constructs <code>ComplexUnitsConverter</code> NOTE: - inputUnit and outputUnits must be under the same category -
     * e.g. meter to feet and inches --> all of them are length units.
     *
     * @param inputUnitIdentifier
     *              represents the source unit identifier. (should be single or compound unit).
     * @param outputUnitsIdentifier
     *              represents the output unit identifier. could be any type. (single, compound or mixed).
     */
    public ComplexUnitsConverter(String inputUnitIdentifier, String outputUnitsIdentifier) {
        this(
                MeasureUnitImpl.forIdentifier(inputUnitIdentifier),
                MeasureUnitImpl.forIdentifier(outputUnitsIdentifier),
                new ConversionRates()
        );
    }

    /**
     * Constructs <code>ComplexUnitsConverter</code> NOTE: - inputUnit and outputUnits must be under the same category -
     * e.g. meter to feet and inches --> all of them are length units.
     *
     * @param inputUnit
     *            represents the source unit. (should be single or compound unit).
     * @param outputUnits
     *            represents the output unit. could be any type. (single, compound or mixed).
     * @param conversionRates
     *            a ConversionRates instance containing the unit conversion rates.
     */
    public ComplexUnitsConverter(MeasureUnitImpl inputUnit, MeasureUnitImpl outputUnits,
            ConversionRates conversionRates) {
        this.inputUnit_ = inputUnit;
        this.units_ = outputUnits.extractIndividualUnitsWithIndices();
        assert (!this.units_.isEmpty());

        this.init(conversionRates);
    }

    /**
     * Sorts units_, which must be populated before calling this, and populates
     * unitsConverters_.
     */
    private void init(ConversionRates conversionRates) {
        // Sort the units in a descending order.
        Collections.sort(this.units_,
                Collections.reverseOrder(new MeasureUnitImpl.MeasureUnitImplWithIndexComparator(conversionRates)));

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
        unitsConverters_ = new ArrayList<>();
        for (int i = 0, n = units_.size(); i < n; i++) {
            if (i == 0) { // first element
                unitsConverters_.add(new UnitsConverter(this.inputUnit_, units_.get(i).unitImpl, conversionRates));
            } else {
                unitsConverters_
                        .add(new UnitsConverter(units_.get(i - 1).unitImpl, units_.get(i).unitImpl, conversionRates));
            }
        }
    }

    /**
     * Returns true if the specified `quantity` of the `inputUnit`, expressed in terms of the biggest unit in the
     * MeasureUnit `outputUnit`, is greater than or equal to `limit`.
     * <p>
     * For example, if the input unit is `meter` and the target unit is `foot+inch`. Therefore, this function will
     * convert the `quantity` from `meter` to `foot`, then, it will compare the value in `foot` with the `limit`.
     */
    public boolean greaterThanOrEqual(BigDecimal quantity, BigDecimal limit) {
        assert !units_.isEmpty();

        // NOTE: First converter converts to the biggest quantity.
        return unitsConverters_.get(0).convert(quantity).multiply(EPSILON_MULTIPLIER).compareTo(limit) >= 0;
    }

    public static class ComplexConverterResult {
        public final int indexOfQuantity;
        public final List<Measure> measures;

        ComplexConverterResult(int indexOfQuantity, List<Measure> measures) {
            this.indexOfQuantity = indexOfQuantity;
            this.measures = measures;
        }
    }

    /**
     * Returns outputMeasures which is an array with the corresponding values.
     * - E.g. converting meters to feet and inches.
     * 1 meter --> 3 feet, 3.3701 inches
     * NOTE:
     * the smallest element is the only element that could have fractional values. And all
     * other elements are floored to the nearest integer
     */
    public ComplexConverterResult convert(BigDecimal quantity, Precision rounder) {
        BigInteger sign = BigInteger.ONE;
        if (quantity.compareTo(BigDecimal.ZERO) < 0 && unitsConverters_.size() > 1) {
            quantity = quantity.abs();
            sign = sign.negate();
        }

        // For N converters:
        // - the first converter converts from the input unit to the largest
        //   unit,
        // - N-1 converters convert to bigger units for which we want integers,
        // - the Nth converter (index N-1) converts to the smallest unit, which
        //   isn't (necessarily) an integer.
        List<BigInteger> intValues = new ArrayList<>(unitsConverters_.size() - 1);
        for (int i = 0, n = unitsConverters_.size(); i < n; ++i) {
            quantity = (unitsConverters_.get(i)).convert(quantity);

            if (i < n - 1) {
                // The double type has 15 decimal digits of precision. For choosing
                // whether to use the current unit or the next smaller unit, we
                // therefore nudge up the number with which the thresholding
                // decision is made. However after the thresholding, we use the
                // original values to ensure unbiased accuracy (to the extent of
                // double's capabilities).
                BigInteger flooredQuantity = quantity.multiply(EPSILON_MULTIPLIER).setScale(0, RoundingMode.FLOOR).toBigInteger();
                intValues.add(flooredQuantity);

                // Keep the residual of the quantity.
                // For example: `3.6 feet`, keep only `0.6 feet`
                BigDecimal remainder = quantity.subtract(BigDecimal.valueOf(flooredQuantity.longValue()));
                if (remainder.compareTo(BigDecimal.ZERO) == -1) {
                    quantity = BigDecimal.ZERO;
                } else {
                    quantity = remainder;
                }
            }
        }

        quantity = applyRounder(intValues, quantity, rounder);

        // Initialize empty measures.
        List<Measure> measures = new ArrayList<>(unitsConverters_.size());
        for (int i = 0; i < unitsConverters_.size(); i++) {
            measures.add(null);
        }

        // Package values into Measure instances in measures:
        int indexOfQuantity = -1;
        for (int i = 0, n = unitsConverters_.size(); i < n; ++i) {
            if (i < n - 1) {
                Measure measure = new Measure(intValues.get(i).multiply(sign), units_.get(i).unitImpl.build());
                measures.set(units_.get(i).index, measure);
            } else {
                indexOfQuantity = units_.get(i).index;
                Measure measure =
                        new Measure(quantity.multiply(BigDecimal.valueOf(sign.longValue())),
                                units_.get(i).unitImpl.build());
                measures.set(indexOfQuantity, measure);
            }
        }

        return new ComplexConverterResult(indexOfQuantity , measures);
    }

    /**
     * Applies the rounder to the quantity (last element) and bubble up any carried value to all the intValues.
     *
     * @return the rounded quantity
     */
    private BigDecimal applyRounder(List<BigInteger> intValues, BigDecimal quantity, Precision rounder) {
        if (rounder == null) {
            return quantity;
        }

        DecimalQuantity quantityBCD = new DecimalQuantity_DualStorageBCD(quantity);
        rounder.apply(quantityBCD);
        quantity = quantityBCD.toBigDecimal();

        if (intValues.size() == 0) {
            // There is only one element, Therefore, nothing to be done
            return quantity;
        }

        // Check if there's a carry, and bubble it back up the resulting intValues.
        int lastIndex = unitsConverters_.size() - 1;
        BigDecimal carry = unitsConverters_.get(lastIndex).convertInverse(quantity).multiply(EPSILON_MULTIPLIER)
                .setScale(0, RoundingMode.FLOOR);
        if (carry.compareTo(BigDecimal.ZERO) <= 0) { // carry is not greater than zero
            return quantity;
        }
        quantity = quantity.subtract(unitsConverters_.get(lastIndex).convert(carry));
        intValues.set(lastIndex - 1, intValues.get(lastIndex - 1).add(carry.toBigInteger()));

        // We don't use the first converter: that one is for the input unit
        for (int j = lastIndex - 1; j > 0; j--) {
            carry = unitsConverters_.get(j)
                    .convertInverse(BigDecimal.valueOf(intValues.get(j).longValue()))
                    .multiply(EPSILON_MULTIPLIER)
                    .setScale(0, RoundingMode.FLOOR);
            if (carry.compareTo(BigDecimal.ZERO) <= 0) { // carry is not greater than zero
                break;
            }
            intValues.set(j, intValues.get(j).subtract(unitsConverters_.get(j).convert(carry).toBigInteger()));
            intValues.set(j - 1, intValues.get(j - 1).add(carry.toBigInteger()));
        }

        return quantity;
    }

    @Override
    public String toString() {
        return "ComplexUnitsConverter [unitsConverters_=" + unitsConverters_ + ", units_=" + units_ + "]";
    }
}
