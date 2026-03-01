// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.ConversionRates;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.util.MeasureUnit;

/**
 * A MicroPropsGenerator which converts a measurement from one MeasureUnit to
 * another. In particular, the output MeasureUnit may be a mixed unit. (The
 * input unit may not be a mixed unit.)
 */
public class UnitConversionHandler implements MicroPropsGenerator {

    private final MicroPropsGenerator fParent;
    private MeasureUnit fOutputUnit;
    private ComplexUnitsConverter fComplexUnitConverter;

    /**
     * @param targetUnit Specifies the output MeasureUnit. The input MeasureUnit
     *     is derived from it: in case of a mixed unit, the biggest unit is
     *     taken as the input unit. If not a mixed unit, the input unit will be
     *     the same as the output unit and no unit conversion takes place.
     * @param parent    The parent MicroPropsGenerator.
     */
    public UnitConversionHandler(MeasureUnit targetUnit, MicroPropsGenerator parent) {
        this.fOutputUnit = targetUnit;
        this.fParent = parent;
        MeasureUnitImpl targetUnitImpl = MeasureUnitImpl.forIdentifier(targetUnit.getIdentifier());
        this.fComplexUnitConverter = new ComplexUnitsConverter(targetUnitImpl, new ConversionRates());
    }

    /**
     * Obtains the appropriate output values from the Unit Converter.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps result = this.fParent.processQuantity(quantity);

        quantity.roundToInfinity(); // Enables toDouble
        ComplexUnitsConverter.ComplexConverterResult complexConverterResult
                = this.fComplexUnitConverter.convert(quantity.toBigDecimal(), result.rounder);

        result.outputUnit = this.fOutputUnit;
        UsagePrefsHandler.mixedMeasuresToMicros(complexConverterResult, quantity, result);

        return result;
    }
}
