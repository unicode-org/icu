// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


package com.ibm.icu.impl.number;

import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.ConversionRates;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

import java.util.List;

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
     * @param inputUnit Specifies the input MeasureUnit. In case of Mixed unit, the input unit will be the biggest unit
     *                  in the Mixed unit and the output will be the input unit. Otherwise, the input unit will be
     *                  the output unit.
     * @param parent    The parent MicroPropsGenerator.
     */
    public UnitConversionHandler(MeasureUnit inputUnit, MicroPropsGenerator parent) {
        this.fOutputUnit = inputUnit;
        this.fParent = parent;
        MeasureUnitImpl inputUnitImpl = MeasureUnitImpl.forIdentifier(inputUnit.getIdentifier());
        this.fComplexUnitConverter = new ComplexUnitsConverter(inputUnitImpl, new ConversionRates());
    }

    /**
     * Obtains the appropriate output values from the Unit Converter.
     */
    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps result = this.fParent.processQuantity(quantity);

        quantity.roundToInfinity(); // Enables toDouble
        List<Measure> measures = this.fComplexUnitConverter.convert(quantity.toBigDecimal(), result.rounder);

        result.outputUnit = this.fOutputUnit;
        UsagePrefsHandler.mixedMeasuresToMicros(measures, quantity, result);

        return result;
    }
}
